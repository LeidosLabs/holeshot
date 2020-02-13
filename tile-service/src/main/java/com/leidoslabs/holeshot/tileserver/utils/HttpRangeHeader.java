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
package com.leidoslabs.holeshot.tileserver.utils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;

/**
 * Class for parsing rangeHeader strings
 */
public class HttpRangeHeader {
   private ImmutableList<Range<Long>> ranges;
   private static Pattern RANGE_HEADER_PATTERN = Pattern.compile("Range:\\s*bytes\\s*=\\s*((?:\\d+-\\d+\\s*,\\s*)*(?:\\d+-\\d+\\s*))", Pattern.CASE_INSENSITIVE); 

   private HttpRangeHeader(List<Range<Long>> ranges) {
      this.ranges = ImmutableList.copyOf(ranges);
   }
   
   public List<Range<Long>> getRanges() {
      return ranges;
   }

   public static HttpRangeHeader parse(String rangeHeader) {
      HttpRangeHeader header = null;
      Matcher matcher = RANGE_HEADER_PATTERN.matcher(rangeHeader);
      if (matcher.matches()) {
         List<Range<Long>> ranges = 
               Stream.of(matcher.group(0).split(",")).map(r->r.split("="))
               .map(r->Range.closed(Long.parseLong(r[0]), Long.parseLong(r[1]))).collect(Collectors.toList());
         header = new HttpRangeHeader(ranges);
      }
      return header;
   }

   @Override
   public String toString() {
      return String.format("bytes=%s", ranges.stream().map(HttpRangeHeader::toString).collect(Collectors.joining(", ")));
   }
   
   private static String toString(Range<Long> longRange) {
      return String.format("%d-%d", longRange.lowerEndpoint(), longRange.upperEndpoint());
   }
}
