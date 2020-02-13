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

import java.math.BigInteger;

import org.locationtech.jts.geom.Envelope;

import com.leidoslabs.holeshot.elt.tileserver.TileserverImage;
import com.leidoslabs.holeshot.tileserver.mrf.jaxb.MRFMETA;
import com.leidoslabs.holeshot.tileserver.mrf.jaxb.MRFMETA.GeoTags;
import com.leidoslabs.holeshot.tileserver.mrf.jaxb.MRFMETA.GeoTags.BoundingBox;
import com.leidoslabs.holeshot.tileserver.mrf.jaxb.MRFMETA.Raster;
import com.leidoslabs.holeshot.tileserver.mrf.jaxb.MRFMETA.Rsets;
import com.leidoslabs.holeshot.tileserver.mrf.jaxb.ObjectFactory;
import com.leidoslabs.holeshot.tileserver.v1.TilePyramidDescriptor;

/**
 * MRFMetadata for given tileserver image
 */
public class MRFMetadataFile {
   private final TileserverImage tileserverImage;
   
   public MRFMetadataFile(TileserverImage tileserverImage) {
      this.tileserverImage = tileserverImage;
   }
   
   public MRFMETA getMRFMETA() {
      final ObjectFactory objectFactory = new ObjectFactory();

      final TilePyramidDescriptor descriptor = tileserverImage.getTilePyramidDescriptor();

      final MRFMETA meta = objectFactory.createMRFMETA();
      final Raster raster = objectFactory.createMRFMETARaster();
      final Raster.Size size = objectFactory.createMRFMETARasterSize();
      size.setX(BigInteger.valueOf(descriptor.getWidth()));
      size.setY(BigInteger.valueOf(descriptor.getHeight()));
      size.setC((short)tileserverImage.getNumBands());
      raster.setSize(size);
      raster.setCompression("PNG");
      final Raster.DataValues dataValues = objectFactory.createMRFMETARasterDataValues();
      dataValues.setNoData((short)0);
      raster.setDataValues(dataValues);
      final Raster.PageSize pageSize = objectFactory.createMRFMETARasterPageSize();
      pageSize.setX(BigInteger.valueOf(descriptor.getTileWidth()));
      pageSize.setY(BigInteger.valueOf(descriptor.getTileHeight()));
      pageSize.setC((short)1);
      raster.setPageSize(pageSize);
      final Rsets rsets = objectFactory.createMRFMETARsets();
      rsets.setModel("uniform");
      meta.setRsets(rsets);
      final GeoTags geoTags = objectFactory.createMRFMETAGeoTags();
      final BoundingBox bbox = objectFactory.createMRFMETAGeoTagsBoundingBox();
      Envelope geoEnvelope = descriptor.getBounds().getEnvelopeInternal();
      bbox.setMaxx(geoEnvelope.getMaxX());
      bbox.setMaxy(geoEnvelope.getMaxY());
      bbox.setMinx(geoEnvelope.getMinX());
      bbox.setMiny(geoEnvelope.getMinY());
      geoTags.setBoundingBox(bbox);
      meta.setGeoTags(geoTags);
      meta.setRaster(raster);
      return meta;
   }
   
   

}
