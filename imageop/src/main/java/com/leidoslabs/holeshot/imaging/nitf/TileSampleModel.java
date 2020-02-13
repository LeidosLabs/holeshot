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
package com.leidoslabs.holeshot.imaging.nitf;

class TileSampleModel {
   private long numElementsToRead;
   private long readOffset;
   private int numberOfReads;
   
   private TileSampleModel() {
   }
   private TileSampleModel(TileSampleModel src) {
      this.numberOfReads = src.numberOfReads;
      this.readOffset = src.readOffset;
      this.numElementsToRead = src.numElementsToRead;
   }
   public long getNumElementsToRead() {
      return numElementsToRead;
   }
   public long getReadOffset() {
      return readOffset;
   }
   public int getNumberOfReads() {
      return numberOfReads;
   }
   
   public static class Factory {
      private TileSampleModel template;
      public Factory() {
         template = new TileSampleModel();
      }
      public void setNumElementsToRead(long numElementsToRead) {
         template.numElementsToRead = numElementsToRead;
      }
      public void setReadOffset(long readOffset) {
         template.readOffset = readOffset;
      }
      public void setNumberOfReads(int numberOfReads) {
         template.numberOfReads = numberOfReads;
      }
      public TileSampleModel build() {
         return new TileSampleModel(template);
      }
   }
}