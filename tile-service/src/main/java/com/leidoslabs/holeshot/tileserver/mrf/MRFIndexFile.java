/*
 * Licensed to The Leidos Corporation under one or more contributor license agreements.  
 * See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 * The Leidos Corporation licenses this file to You under the Apache License, Version 2.0
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
package com.leidoslabs.holeshot.tileserver.mrf;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.leidoslabs.holeshot.elt.tileserver.TileRef;
import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.tileserver.service.S3Handler;
import com.leidoslabs.holeshot.imaging.ImageKey;

/**
 * Populate sorted MRFTileRefs from a tileserver image of S3 image collection + timestamp prefix
 */
public class MRFIndexFile {
	private final SortedSet<MRFTileRef> outputTiles;
	private static final Pattern KEY_PATTERN = Pattern.compile("([^/]*)/([^/]*)/([^/]*)/([^/]*)/([^/]*)/([^/]*).png");
	private static final Pattern IMAGE_INDEX_PATTERN = Pattern.compile("(?:.*/)*image.idx$");

	
	/**
	 * Populate outputTiles from fully specified tile URL
	 * @param s3Handler
	 * @param region
	 * @param bucket
	 * @param prefix
	 * @throws NoSuchElementException
	 * @throws IllegalStateException
	 * @throws Exception
	 */
	public MRFIndexFile(S3Handler s3Handler, String region, String bucket, String prefix) throws NoSuchElementException, IllegalStateException, Exception {
		this();
		crawlOffsetMap(region, bucket, prefix);
	}
	
	
	/**
	 * Using a TileserverImage's tiles, populate outputTiles
	 * @param image
	 * @param s3Handler
	 * @param region
	 * @param bucket
	 * @param prefix
	 * @throws NoSuchElementException
	 * @throws IllegalStateException
	 * @throws Exception
	 */
	public MRFIndexFile(TileserverImage image, S3Handler s3Handler, String region, String bucket, String prefix) throws NoSuchElementException, IllegalStateException, Exception {
		this();
		readIndexFile(image, s3Handler, region, bucket, prefix);
	}

	public MRFIndexFile() {
		this.outputTiles = new TreeSet<MRFTileRef>();
	}

	public MRFIndexFile(ImageKey image, SortedSet<MRFTileRef> outputTiles) {
		this();
		outputTiles.forEach(t -> this.outputTiles.add(t));
		//this.outputTiles.addAll(outputTiles);
	}

	public SortedSet<MRFTileRef> getOutputTiles() {
		return outputTiles;
	}


	/**
	 * Write outputTiles to given OutputStream
	 * @param os OutputStream to be written to
	 * @throws IOException
	 */
	public void writeToOutputStream(OutputStream os) throws IOException {
		try (DataOutputStream dos = new DataOutputStream(os)) {
			Iterator<MRFTileRef> iter = outputTiles.iterator();
			long offset = 0;
			while (iter.hasNext()) {
				MRFTileRef entry = iter.next();
				final long entrySize = entry.getObjectSize();
				dos.writeLong(offset);
				dos.writeLong(entrySize);
				offset += entrySize;
			}
		}
	}

	/**
	 * @return Sum of sizes of output tiles
	 */
	public long getDataSize() {
		return outputTiles.stream().mapToLong(s->s.getObjectSize()).sum();
	}

	/**
	 * return MRFTileRef given s3 object key and object size
	 * @param key
	 * @param objectSize
	 * @return
	 */
	private MRFTileRef getTileRef(String key, long objectSize) {
		MRFTileRef result = null;
		if (key != null) {
			Matcher matcher = KEY_PATTERN.matcher(key);
			if (matcher.matches()) {
				final String collectionID = matcher.group(1);
				final String timestamp = matcher.group(2);
				final int rset = Integer.parseInt(matcher.group(3));
				final int x = Integer.parseInt(matcher.group(4));
				final int y = Integer.parseInt(matcher.group(5));
				final int band = Integer.parseInt(matcher.group(6));

				result = new MRFTileRef(collectionID, timestamp, rset, x, y, band, objectSize); 
			}
		}
		return result;
	}

	/**
	 * Populate output tiles by crawling over s3 objects
	 * @param region
	 * @param bucket
	 * @param prefix
	 */
	private void crawlOffsetMap(String region, String bucket, String prefix) {
		ListObjectsIterator.listObjects(bucket, prefix).stream()
		.map(o->getTileRef(o.getKey(), o.getSize()))
		.filter(t->t!=null)
		.forEach(t->outputTiles.add(t));
	}

	
	/**
	 * For each tile in a tileserverimage, read offset and size from index file, then add to outputTiles
	 * @param image
	 * @param s3Handler
	 * @param region
	 * @param bucket
	 * @param prefix
	 * @throws NoSuchElementException
	 * @throws IllegalStateException
	 * @throws Exception
	 */
	private void readIndexFile(TileserverImage image, S3Handler s3Handler, String region, String bucket, String prefix) throws NoSuchElementException, IllegalStateException, Exception {
		final byte[] indexFileBytes = readIndexFileBytes(s3Handler, region, bucket, prefix);

		final TileRef topTile = image.getTopTile();

		int maxRset = image.getMaxRLevel();
		final int numBands = image.getNumBands();

		try (ByteArrayInputStream bis = new ByteArrayInputStream(indexFileBytes)) {
			try (DataInputStream dis = new DataInputStream(bis)) {
				for (int rset=0;rset<=maxRset;++rset) {
					Dimension rsetTileDim = topTile.getRowsAndColumnsForRset(rset);
					for (int band=0;band<numBands;++band) {
						for (int y=0;y<rsetTileDim.height;++y) {
							for (int x=0;x<rsetTileDim.width;++x) {
								final long offset = readLong(dis);
								final long size = readLong(dis);

								if (offset < 0 || size < 0) {
									throw new EOFException("Insufficient number of entries in indexFile");
								}
								MRFTileRef tileref = new MRFTileRef(image.getCollectionID(), image.getTimestamp(), rset, x, y, band, size);
								outputTiles.add(tileref);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Read a long from 
	 * @param dis 
	 * @return long result, -1 if error
	 * @throws IOException
	 */
	private long readLong(DataInputStream dis) throws IOException {
		long result = -1;
		if (dis.available() > 0) {
			result = dis.readLong();
		}
		return result;
	}

	/**
	 * Index file byte array from writeFromS3 call 
	 * @param s3Handler
	 * @param region
	 * @param bucket
	 * @param prefix
	 * @return
	 * @throws NoSuchElementException
	 * @throws IllegalStateException
	 * @throws Exception
	 */
	private byte[] readIndexFileBytes(S3Handler s3Handler, String region, String bucket, String prefix) throws NoSuchElementException, IllegalStateException, Exception {
		byte[] result = null;
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			s3Handler.writeFromS3(prefix, bos, true);
			result = bos.toByteArray();
		}
		return result;
	}


}
