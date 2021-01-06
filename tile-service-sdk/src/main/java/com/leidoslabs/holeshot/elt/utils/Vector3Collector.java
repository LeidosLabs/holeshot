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

package com.leidoslabs.holeshot.elt.utils;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector3f;
import com.google.common.collect.ImmutableSet;

/**
 * Collector for collecting Pairs<Integer, Float> into Pairs<Integer, Vector3f>
 * Designed for the use case of collecting a 1D float buffer of 3 channel data (e.g. [r1, b1, g1, r2, b2, g3, ...]) 
 * as a collection of Integer, Vector3f pairs, e.g ((1, Vec3(r1, b1, g1)), ...)
 */
public class Vector3Collector implements Collector<Pair<Integer, Float>, MutablePair<Integer, Vector3f>, Pair<Integer, Vector3f>> {
  private static final int bands = 3;

  public Vector3Collector() {
  }

  @Override
  public Supplier<MutablePair<Integer, Vector3f>> supplier() {
    return () -> MutablePair.of(Integer.MAX_VALUE, new Vector3f());
  }

  @Override
  public BiConsumer<MutablePair<Integer, Vector3f>, Pair<Integer, Float>> accumulator() {
    return (pair1, pair2) -> {
      pair1.right.setComponent(pair2.getLeft() % bands, pair2.getRight());
      pair1.setLeft(Math.min(pair1.left, pair2.getLeft()));
    };
  }

  @Override
  public BinaryOperator<MutablePair<Integer, Vector3f>> combiner() {
    return (pair1, pair2) -> MutablePair.of(Math.min(pair1.left, pair2.left), pair1.right.add(pair2.right.x, pair2.right.y, pair2.right.z));
  }

  @Override
  public Function<MutablePair<Integer, Vector3f>, Pair<Integer, Vector3f>> finisher() {
    return p -> Pair.of(p.left / bands, p.right);
  }

  @Override
  public Set<Characteristics> characteristics() {
    return ImmutableSet.<Characteristics>builder().add(Characteristics.UNORDERED, Characteristics.CONCURRENT).build();
  }
}
