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
package com.leidoslabs.holeshot.imaging;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Hashable, Comparable identifier for an ingest image
 */
public class ImageKey implements Comparable<ImageKey> {

   private static final DateTimeFormatter DATE_TIME_FORMATTER =
         DateTimeFormatter.ofPattern("YYYYMMddHHmmss");

   private String collectionID;
   private ZonedDateTime collectTime;
   private ZonedDateTime ingestTime;


   /**
    * @param time 
    * @return Date in NITFtime format (YYYYMMddHHmmss)
    */
   public static long toNITFFormat(ZonedDateTime time) {
      return Long.parseLong(DATE_TIME_FORMATTER.format(time));
   }

   public ImageKey(String collectionID, ZonedDateTime collectTime, ZonedDateTime ingestTime) {
      this.collectionID = collectionID;
      this.collectTime = collectTime;
      this.ingestTime = ingestTime;
   }

   public ImageKey(ImageKey src) {
      this(src.collectionID, src.collectTime, src.ingestTime);
   }

   public long getIDATIM() {
      return toNITFFormat(collectTime);
   }

   public long getFDT() {
      return toNITFFormat(ingestTime);
   }

   /**
    * @return return ImageKey's name delimited by '/'
    */
   public String getFilePath() {
      return getName("/");
   }
   /**
    * @return ImageKey's name delimited by ':'
    */
   public String getName() {
      return getName(":");
   }

   /**
    * @param delimeter
    * @return ImageKey name: <collectionID> + delim +<collect time in NITF YYYYMMddHHmmss>
    */
   public String getName(String delimeter) {
      return String.join(delimeter, collectionID, getCollectTimeString());
   }

   public String getCollectionID() {
      return collectionID;
   }

   /**
    * @return Collection Time in NITF YYYYMMddHHmmss
    */
   public String getCollectTimeString() {
	   return Long.toString(getIDATIM());
   }
   public ZonedDateTime getCollectTime() {
      return collectTime;
   }

   public ZonedDateTime getIngestTime() {
      return ingestTime;
   }

   public void setCollectionID(String collectionID) {
      this.collectionID = collectionID;
   }

   public void setCollectTime(ZonedDateTime collectTime) {
      this.collectTime = collectTime;
   }

   public void setIngestTime(ZonedDateTime ingestTime) {
      this.ingestTime = ingestTime;
   }
   @Override
   public int hashCode() {
      return new HashCodeBuilder()
            .append(this.collectionID)
            .append(this.collectTime)
            .append(this.ingestTime)
            .toHashCode();
   }

   @Override
   public int compareTo(ImageKey o) {
      return getName().compareTo(o.getName());
   }
}
