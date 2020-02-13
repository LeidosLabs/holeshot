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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class encapsulates band representations for an image. The band
 * representation values are based on those defined for NITF.
 */
public class BandRepresentations {

  private static final Logger LOGGER = LoggerFactory.getLogger(BandRepresentations.class);

  public static final String RED_BAND = "R";
  public static final String GREEN_BAND = "G";
  public static final String BLUE_BAND = "B";
  public static final String ALPHA_BAND = "A";
  public static final String NEAR_INFRARED_BAND = "N";
  public static final String MONO_BAND = "M";
  public static final String UNLABELED_BAND = "  ";
  private String[] myBandRepresentations;


  /**
   * Constructs a BandRepresentations object.
   *
   * @param bandReps An array of String band representations.
   *
   */
  public BandRepresentations(String[] bandReps) {
    myBandRepresentations = bandReps;
  }

  /**
   * Returns true if band representations contain an alpha band
   */
  public boolean hasAlpha() {
    return (getBandIndex(ALPHA_BAND) >= 0);
  }

  /**
   * Return the number of bands as indicated by the band representations.
   */
  public int getNumBands() {
    return myBandRepresentations.length;
  }

  /**
   * Returns the band index for the band with the given band name.
   *
   * @param bandName The name of the band whose index is desired.
   */
  protected int getBandIndex(String bandName) {
    int index = -1;
    int i = 0;
    while ((index < 0)
            && (i < getNumBands())) {
      if (myBandRepresentations[i].equals(bandName)) {
        index = i;
      }
      i++;
    }  // end while
    return index;
  }

  /**
   * NOTE this implementation has changed. It always returns bands in the
   * default order now (bands are NOT swapped). The reason is that there was no
   * way for clients using e.g. ImageSources to figure out that the bands had
   * been flipped (metadata was not correspondingly updated, etc.). Clients that
   * want band combinations should use the LegacyBandCombine classes or just
   * write their own band swapping code. Get array of band indices for the
   * preferred order of bands as returned by tile readers, which is to say, R,
   * G, B for color, or M for gray-scale, then anything else which might be
   * present.
   */
  public int[] getBandIndicesInPreferredOrder() {
    int bands = getNumBands();
    int[] indices = new int[bands];
    LOGGER.warn("getBandIndicesInPreferredOrder no longer swaps bands; it always returns the default band ordering now");
    for (int i = 0; i < indices.length; i++) {
      indices[i] = i;
    }
    /*
     if (bands < 3)
     {
     indices[0] = getBandIndex( MONO_BAND );
     if (indices[0] < 0)
     {
     indices[0] = 0;   // IREPBAND.1 is probably blank.
     }
     if (bands > 1)
     {
     int i = 1;
     for (int bandIndex = 0; bandIndex < getNumBands(); bandIndex++)
     {
     if
     (bandIndex != indices[0])
     {
     indices[i] = bandIndex;
     }  // end if
     }  // end for
     }
     }
     else
     {
     indices[0] = getBandIndex( RED_BAND );
     indices[1] = getBandIndex( GREEN_BAND );
     indices[2] = getBandIndex( BLUE_BAND );
      
     if(indices[0] == -1 || indices[1] == -1 || indices[2] == -1)
     {
     // if we aren't able to find IREPBANDn = R, G, B for some
     // reason then we have to default to something that works.
     // since this is just the preferred order, this isn't too
     // large of a problem.
     indices[0] = 0;
     indices[1] = 1;
     indices[2] = 2;
     }
     if (bands > 3)
     {
     int i = 3;
     for (int bandIndex = 0; bandIndex < bands; bandIndex++)
     {
     if
     ( (bandIndex != indices[0])
     &&
     (bandIndex != indices[1])
     &&
     (bandIndex != indices[2]) )
     {
     indices[i++] = bandIndex;
     }  // end if
     }  // end for
     }  // end if
     }  // end if
     if (LOGGER.isLoggable(Level.FINER)) {
     LOGGER.log(Level.FINER, "getBandIndicesInPreferredOrder");
     for (int i=0; i < indices.length; i++ ) {
     LOGGER.log(Level.FINER, "i[" + i + "]=" + indices[i]);
     }
     }*/
    return indices;
  }
}
