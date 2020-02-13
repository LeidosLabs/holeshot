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

package com.leidoslabs.holeshot.elt;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;

import com.leidoslabs.holeshot.elt.tileserver.TileRef;

/**
 * Currently unused
 * Fetches all Tiles in an image given top level tile in the background
 * using a swing worker thread, includes a ProgressMonitor
 */
public class TileFetcher implements PropertyChangeListener {
  private static final String PROGRESS_PROPERTY = "progress";
  
  private final int MAX_FETCH_THREADS = 4;
  private final Task task;
  private final ProgressMonitor progressMonitor;

  private final Collection<TileRef> tilesToFetch;
  private final int totalTiles;
  private final AtomicInteger tilesRead;

  /**
   * Fetches all Tiles in an image given top level tile in the background
   * using a swing worker thread
   * @param parentComponent Parent Component that displays progress monitor of background task
   * @param topLevelTile top level tile of image
   */
  public TileFetcher(Component parentComponent, TileRef topLevelTile) {
    tilesToFetch = topLevelTile.getAllTiles();
    totalTiles = tilesToFetch.size();
    tilesRead = new AtomicInteger(0);

    progressMonitor = new ProgressMonitor(parentComponent, "Fetch Tiles...", "", 0, 100);
    task = new Task();
    task.addPropertyChangeListener(this);
    task.execute();
  }


  private class Task extends SwingWorker<Void, Void> {
    @Override
    protected Void doInBackground() throws Exception {
      try {
        setProgress(0);

        final int numThreads = Math.min(ForkJoinPool.getCommonPoolParallelism(), MAX_FETCH_THREADS);
        final ForkJoinPool forkJoinPool = new ForkJoinPool(numThreads);
        
        forkJoinPool.submit(() -> {
           tilesToFetch.parallelStream().forEach(t->unsafeFetchTile(forkJoinPool, t));
        });
      } catch (RuntimeException e) {
        if (e.getCause() instanceof IOException) {
          throw (IOException)e.getCause();
        } else {
          throw e;
        }
      }
      return null;
    }
    @Override
    protected void done() {
      super.done();
    }

    private void unsafeFetchTile(ForkJoinPool forkJoinPool, TileRef tile) {
      if (progressMonitor.isCanceled()) {
        forkJoinPool.shutdown();
      } else {
        try {
          tile.getTileImage();
          setProgress((int)(100.0 * ((double)tilesRead.incrementAndGet() / totalTiles)));

        } catch (IOException e) {
          e.printStackTrace();
          throw new RuntimeException(e);
        }
      }
    }
  }


  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    if (PROGRESS_PROPERTY == evt.getPropertyName()) {
      final int progress = (Integer)evt.getNewValue();
      progressMonitor.setProgress(progress);

      final String message = String.format("Read %d of %d tiles", (int)((double)progress/100.0 * totalTiles), totalTiles);
      progressMonitor.setNote(message);
    }

  }

}
