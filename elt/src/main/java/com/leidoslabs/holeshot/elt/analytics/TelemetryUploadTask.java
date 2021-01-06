/*
 * Licensed to Leidos, Inc. under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 * Leidos, Inc. licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.leidoslabs.holeshot.elt.analytics;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.leidoslabs.holeshot.elt.UserConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.leidoslabs.holeshot.elt.analytics.TelemetryRollingFileAppender.Rollover;

/**
 * A Runnable that sends new log data to Kinesis Firehose through an HTTP API 
 */
public class TelemetryUploadTask implements Runnable {

	// Firehose has a limit of 500 records per PutRecordsBatch
	private static final int MAX_RECORDS = 500;
	// max size of each record to ensure total payload size is < 4MB
	private static final int MAX_RECORDS_SIZE = 7900;
	// We schedule a telemetry shutdown if we encounter CONSECUTIVE_ERRORS consecutive errors
	private static final int CONSECUTIVE_ERRORS = 3;
	private static final String FILE_NAME = String.join(File.separator, System.getProperty("user.home"), ".holeshot", "elt.log");

	private static final String STREAM_NAME = "advanced-analytics-eltLogging";
	private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryUploadTask.class);
	
	private TelemetryRollingFileAppender appender;
	private ObjectMapper mapper;

	private long curStart = 0L;
	private long curEnd = 0L;
	private Long lastUploadTime = null;
	private int curErrors = 0;
	

	TelemetryUploadTask() {
		this.mapper = new ObjectMapper();
		this.appender = (TelemetryRollingFileAppender) org.apache.log4j.Logger.getRootLogger().getAppender("file");
	}

	/**
	 * Uploads new telemetry data in logs to Kinesis firehose
	 * 
	 * If our log file file has rolled over once since our last upload (i.e, the old
	 * elt.log is moved to elt.log.1, and new logs are written to a clean, new
	 * elt.log) then we upload the remaining part of elt.log.1, and then upload the
	 * current state of the new file. This will be done as 2 back to back uploads
	 * 
	 * Note: If multiple rollovers occur inbetween telemetry uploads, then we only
	 * upload the current state of the elt.log file. (some data is ommited) This
	 * shouldn't happen unless the max file size for logs is too small, or the
	 * telemetry interval is too long
	 */
	@Override
	public void run() {
		Rollover rollovers = Rollover.NONE;
		String fileName = FILE_NAME;

		if (lastUploadTime != null) {
			rollovers = this.appender.rolloversSince(lastUploadTime);
		}

		if (rollovers == Rollover.ONE) {
			fileName += ".1";
		}
		if (rollovers == Rollover.MULTI) {
			fileName += ".1";
			this.curStart = 0L;
			LOGGER.warn("Multiple rollovers have occured since last telemetry upload. Some data will be ommited");
		}
		
		List<String> recordsRaw = null ;
		try {
			this.appender.getReadLock().lock();
			try (RandomAccessFile logFile = new RandomAccessFile(fileName, "r")) {
				this.curEnd = logFile.length();

				String line;
				logFile.seek(this.curStart);
				recordsRaw = new ArrayList<>();
				while (logFile.getFilePointer() <= this.curEnd && (line = logFile.readLine()) != null) {
					recordsRaw.add(line + "\n");
				}

			} finally {
				this.appender.getReadLock().unlock();
			}
			
			LOGGER.debug(String.format("Uploading Logs..."));
			upload(recordsRaw);
			curErrors = 0;
		}

		catch (IOException e) {
			LOGGER.error("Exception occurred while uploading logs");
			e.printStackTrace();
			curErrors += 1;
			// If we encounter >= CONSECUTIVE_ERRORS consecutive IOExceptions,
			// we trigger a telemetry shutdown
			if (curErrors >= CONSECUTIVE_ERRORS) {
				throw new RuntimeException(e.getMessage());
			}
		}
		this.curStart = this.curEnd;
		lastUploadTime = System.currentTimeMillis();
		if (rollovers == Rollover.ONE || rollovers == Rollover.MULTI) {
			this.curStart = 0L;
			this.run();
		}
	}

	private String createPayload(List<String> recordsEncoded) throws IOException {
		String payloadJSON = "";
		FirehosePayload payload = new FirehosePayload();
		payload.setDeliveryStreamName(STREAM_NAME);
		payload.setRecords(recordsEncoded);
		try {
			payloadJSON = this.mapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			LOGGER.error("Exception occurred when serializing payload");
			throw e;
		}
		return payloadJSON;
	}

	/**
	 * Partitions log data
	 * 
	 * @param recordsRaw list of lines from log file
	 * @throws IOException
	 */
	private void upload(List<String> recordsRaw) throws IOException {
		try {
			for (List<String> recordPartition : Lists.partition(recordsRaw, MAX_RECORDS)) {

				List<String> recordsEncoded = recordPartition.stream().map((record) -> {
					// first we encode each line to base64 (firehose requirement)
					String encoded = Base64.getEncoder().encodeToString(record.getBytes());
					// We then limit the maximum size of an encoded record be slightly less than
					// 8000 chars so when encoded in utf-8 the total payload is guaranteed to be less than the 4MB limit
					return encoded.length() <= MAX_RECORDS_SIZE ? encoded : encoded.substring(0, MAX_RECORDS_SIZE);
				}).collect(Collectors.toList());
				String payload = createPayload(recordsEncoded);
				postRecords(payload);
			}

		} catch (IOException e) {
			throw e;
		}
	}

	/**
	 * POSTs formatted payload to our telemetry firehose api
	 * 
	 * @param payload firehose payload
	 * @throws IOException
	 */
	private void postRecords(String payload) throws IOException {
		try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
			URI recourseURL = new URI(UserConfiguration.getTelemetryEndpoint());
			HttpPost post = new HttpPost(recourseURL);

			StringEntity input = new StringEntity(payload, ContentType.APPLICATION_JSON);

			post.addHeader("x-api-key", UserConfiguration.getTelemetryApiKey());
			post.addHeader("Content-Type", "application/json");
			post.setEntity(input);
			CloseableHttpResponse response = client.execute(post);

			int code = response.getStatusLine().getStatusCode();
			if (code >= 300) {
				LOGGER.error(response.getStatusLine().toString());
			} else {
				LOGGER.debug("Successful telemetry upload");
			}
			response.close();
		} catch (URISyntaxException e) {
			LOGGER.error(e.toString());
		} catch (IOException e) {
			LOGGER.error("Exception occurred when POSTing to telemetry API");
			throw e;
		}
	}

	private static class FirehosePayload {
		@JsonProperty("DeliveryStreamName")
		private String deliveryStreamName;
		@JsonProperty("Records")
		private List<Map<String, String>> records;

		public String getDeliveryStreamName() {
			return deliveryStreamName;
		}

		public void setDeliveryStreamName(String deliveryStreamName) {
			this.deliveryStreamName = deliveryStreamName;
		}

		public void setRecords(List<String> recordsEncoded) {
			this.records = new ArrayList<>();
			recordsEncoded.forEach(recordEncoded -> {
				Map<String, String> map = new HashMap<>();
				map.put("Data", recordEncoded);
				this.records.add(map);
			});
		}

		public List<Map<String, String>> getRecords() {
			return this.records;
		}

	}

}
