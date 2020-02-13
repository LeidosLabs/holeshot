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
package com.leidoslabs.holeshot.elt.utils;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

public class EnvelopeCollector {
  public static class OfEnvelope 
  implements Collector<Envelope, Envelope, Envelope> {
    @Override
    public Supplier<Envelope> supplier() {
      return () -> new Envelope();
    }

    @Override
    public BiConsumer<Envelope, Envelope> accumulator() {
      return (builder, t) -> builder.expandToInclude(t);
    }

    @Override
    public BinaryOperator<Envelope> combiner() {
      return (left, right) -> {
        left.expandToInclude(right);
        return left;
      };
    }

    @Override
    public Function<Envelope, Envelope> finisher() {
      return Function.identity();
    }

    @Override
    public Set<Characteristics> characteristics() {
      return EnumSet.of(Characteristics.UNORDERED);
    }
  }
  public static class OfCoordinate 
  implements Collector<Coordinate, Envelope, Envelope> {
    @Override
    public Supplier<Envelope> supplier() {
      return () -> new Envelope();
    }

    @Override
    public BiConsumer<Envelope, Coordinate> accumulator() {
      return (builder, t) -> builder.expandToInclude(t);
    }

    @Override
    public BinaryOperator<Envelope> combiner() {
      return (left, right) -> {
        left.expandToInclude(right);
        return left;
      };
    }

    @Override
    public Function<Envelope, Envelope> finisher() {
      return Function.identity();
    }

    @Override
    public Set<Characteristics> characteristics() {
      return EnumSet.of(Characteristics.UNORDERED);
    }
  }
}