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

package com.leidoslabs.holeshot.analytics.emr;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class JobMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobMain.class);
  
    private final SparkSession sc;
    String logLocation;
    JavaSparkContext jsc;
    
    public JobMain() {
        LOGGER.info("Starting spark session");
    	 this.sc = SparkSession.builder().appName("Log Parsing").config("spark.sql.files.maxPartitionBytes", LogMRUtils.MAX_PARTITION_SIZE).getOrCreate();
    	 this.jsc = new JavaSparkContext(sc.sparkContext());
         //This config ensures that textFile command handles sub-directories correctly
    	 this.jsc.hadoopConfiguration().set("mapreduce.input.fileinputformat.input.dir.recursive","true");
         logLocation = LogMRUtils.INPUT_LOG_LOCATION;
    }
    
    
    public void runJob() {
    	Tuple2<Long, Long> times = getStartEndTimes();
    	JavaRDD<String> logs = jsc.textFile(logLocation);
    	JavaPairRDD<String, Integer> parsedLogs = logs.flatMapToPair(LogMRUtils.parseAndGeoRefFunctional());
        
    	// counts now consists both of {geohash: count} entries (where count is total hits for a geohash) 
        // and {geohash#<username>: count} entries (where count is geohash hits per user)
        JavaPairRDD<String, Integer> counts = parsedLogs.reduceByKey(Integer::sum);
        
        // We collect each partition above, convert the KV pairs to the approximate JSON body for our ES index, and post to our analytics endpoint
        JavaRDD<String> putResponses = counts.mapPartitions(LogMRUtils.uploadFunctional(times._1, times._2));
        
        List<String> responses = putResponses.coalesce(1, true).collect();
        if (responses.stream().anyMatch(s -> s.contains("ERROR"))){
            // TODO log the error, retry if retriable, and keep going if non-fatal
        	LOGGER.error("Error encountered during dump to elastic search");
        	this.sc.stop();
        	System.exit(1);
        }
        LOGGER.debug("HTTP results" + responses.toString());
        
        // If we are running locally, output our responses as text files.
        if (LogMRUtils.LOCAL_MODE) {
            String fileName = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
            String path = LogMRUtils.OUTPUT_LOCATION + File.separator + fileName;
            this.jsc.parallelize(responses).saveAsTextFile(path);
        }

        this.sc.stop();
    }
    
    /**
     * @return Determine the start/end time (in unix time) of log collection period
     *  by parsing the log filenames. compatible with hdfs.
     */
    private Tuple2<Long, Long> getStartEndTimes(){

        Pattern p = Pattern.compile("20\\d\\d-\\d\\d-\\d\\d-\\d\\d-\\d\\d-\\d\\d");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        LOGGER.info("Processing start and end times from logs at " + logLocation);
        List<Long> collectionTimes = jsc.wholeTextFiles(logLocation)
        						   		.map(t -> {
        							   			Matcher m = p.matcher(t._1);
        							   			m.find();
        							   			return sdf.parse(m.group()).getTime();
        							   		}).collect();
        Long startTime = Collections.min(collectionTimes);
        Long endTime = Collections.max(collectionTimes);
        return new Tuple2<>(startTime, endTime);
        
    }
    

    
    public static void main(String[] args) {        
        JobMain job = new JobMain();
        job.runJob();
    }
    
    

}