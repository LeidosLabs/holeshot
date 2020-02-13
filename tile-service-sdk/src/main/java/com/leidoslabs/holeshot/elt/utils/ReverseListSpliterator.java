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

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * A SplitIterator that Iterates over a linked list in reverse order
 * @param <T>
 */
public class ReverseListSpliterator<T> implements Spliterator<T> {
  private ListIterator<T> listIterator;
  private int currentIndex;
  
  public ReverseListSpliterator(LinkedList<T> sourceList, int startIndex) {
    this.currentIndex = startIndex;
    this.listIterator = sourceList.listIterator(startIndex);
  }
  public ReverseListSpliterator(LinkedList<T> sourceList) {
    this(sourceList, sourceList.size());
  }
  public ReverseListSpliterator(LinkedList<T> sourceList, T startAt) {
    this(sourceList, sourceList.indexOf(startAt));
  }

  @Override
  public boolean tryAdvance(Consumer<? super T> action) {
    boolean result = (currentIndex > 0); 
    if (result) {
      action.accept(listIterator.previous());
      --currentIndex;
    }
    return result;
  }

  @Override
  public Spliterator<T> trySplit() {
    return null;
  }

  @Override
  public long estimateSize() {
    return currentIndex;
  }

  @Override
  public int characteristics() {
    return SIZED | IMMUTABLE;
  }
  
}
