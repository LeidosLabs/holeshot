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
package com.leidoslabs.holeshot.imaging.geotiff;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.geotiff.GeoTiffStore;
import org.apache.sis.storage.geotiff.GeoTiffStoreProvider;
import org.image.common.util.CloseableUtils;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.spatial.GeolocationInformation;
import org.opengis.metadata.spatial.Georeferenceable;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.util.FactoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.jaiimageio.impl.plugins.tiff.TIFFIFD;
import com.github.jaiimageio.impl.plugins.tiff.TIFFImageMetadata;
import com.github.jaiimageio.impl.plugins.tiff.TIFFImageReader;
import com.github.jaiimageio.plugins.tiff.BaselineTIFFTagSet;
import com.github.jaiimageio.plugins.tiff.TIFFField;
import com.github.jaiimageio.plugins.tiff.TIFFTag;
import com.leidoslabs.holeshot.imaging.ImageKey;
import com.leidoslabs.holeshot.imaging.ImageSourceSegment;
import com.leidoslabs.holeshot.imaging.io.CheckedSupplier;
import com.leidoslabs.holeshot.imaging.io.TiledRenderedImage;
import com.leidoslabs.holeshot.imaging.photogrammetry.CameraModel;
import com.leidoslabs.holeshot.imaging.photogrammetry.MapProjectionCamera;
import com.sun.media.jai.codec.SeekableStream;
/**
 * ImageSourceSegment for GeoTiff.
 * Reads tiff from InputStream, extracts rendered image, extracts metadata as a Map<String, Object>,
 * and builds a camera model.
 */ 
public class GeoTIFFImageSourceSegment implements ImageSourceSegment {
   static {
      IIORegistry registry = IIORegistry.getDefaultInstance();
      ImageReaderSpi jaiProvider = lookupProviderByName(registry, "com.sun.media.imageioimpl.plugins.tiff.TIFFImageReaderSpi");
      registry.deregisterServiceProvider(jaiProvider);
   }
   private static final Logger LOGGER = LoggerFactory.getLogger(GeoTIFFImageSourceSegment.class);
   private final static DateTimeFormatter TIFF_DATE_FORMAT =  DateTimeFormatter.ofPattern( "yyyy':'MM':'dd' 'HH':'mm':'ss");
   private final static Pattern RATIONAL_PATTERN = Pattern.compile("^\\s*(\\S*)\\s*/\\s*(\\S*)\\s*$");
   private final static GeographicCRS WGS84 = CommonCRS.WGS84.geographic();


   private TIFFField[] tiffMetadata;
   private Map<String, Object> metadata;
   private final ImageKey imageKey;
   private MathTransform rasterToModel;
   private CoordinateReferenceSystem imageCRS;
   private final CheckedSupplier<InputStream, IOException> imageInputStreamSupplier;

   private Metadata storeMetadata;
   private final RenderedImage tiledRenderedImage;
   private final SeekableStream seekableStream;
   private final ImageInputStream imageInputStream;


   public static void main(String[] args) {
      File testFile = new File("F:\\dev\\leidoslabs.aws\\images\\059339011010_01_P001_MUL\\12SEP08090346-M1BS-059339011010_01_P001.TIF");
      try {
         ImageKey fallbackKey = new ImageKey("059339011010_01_P001_MUL", ZonedDateTime.now(), ZonedDateTime.now() );
         GeoTIFFImageSourceSegment seg = new GeoTIFFImageSourceSegment(fallbackKey, ()-> new FileInputStream(testFile));
      } catch (IOException e) {
         LOGGER.error(e.getMessage(), e);
      }
   }


   /**
    * Creates TiledRenderedImage, and builds camera model. Builds metadata as Map by reading tiff metadata
    * @param imageKey
    * @param inputStreamSupplier
    * @throws IOException
    */
   public GeoTIFFImageSourceSegment(ImageKey imageKey, CheckedSupplier<InputStream, IOException> inputStreamSupplier) throws IOException {
      try {
         this.imageInputStreamSupplier = inputStreamSupplier;
         this.imageKey = imageKey;

         seekableStream = SeekableStream.wrapInputStream(inputStreamSupplier.get(), true);
         IIORegistry registry = IIORegistry.getDefaultInstance();
         com.github.jaiimageio.impl.plugins.tiff.TIFFImageReaderSpi imageReaderSpi =
               lookupProviderByName(registry, "com.github.jaiimageio.impl.plugins.tiff.TIFFImageReaderSpi");

         TIFFImageReader imageReader = (TIFFImageReader)imageReaderSpi.createReaderInstance();
         imageInputStream = ImageIO.createImageInputStream(seekableStream);
         imageReader.setInput(imageInputStream);

         tiledRenderedImage = new TiledRenderedImage(imageReader, 0);

         final TIFFImageMetadata imageMetadata = ((TIFFImageMetadata)imageReader.getImageMetadata(0));
         final TIFFIFD rootIFD = imageMetadata.getRootIFD();
         tiffMetadata = rootIFD.getTIFFFields();
         this.metadata = Arrays.stream(tiffMetadata).collect(Collectors.toMap(k->k.getTag().getName(), v->getTiffValue(v)));

         initializeCameraModel();

      } catch (DataStoreException e) {
         throw new IOException(e);
      }

      imageKey.setCollectionID(getMetadata(String.class, imageKey.getCollectionID(), BaselineTIFFTagSet.TAG_DOCUMENT_NAME, BaselineTIFFTagSet.TAG_PAGE_NAME));

      final String collectTimeString = getMetadata(String.class, imageKey.getCollectTime().format(TIFF_DATE_FORMAT), BaselineTIFFTagSet.TAG_DATE_TIME);
      final ZonedDateTime collectTime =  ZonedDateTime.of(LocalDateTime.parse(collectTimeString, TIFF_DATE_FORMAT), ZoneId.of("UTC"));
      imageKey.setCollectTime(collectTime);
   }

   private static <T> Object getValue(TIFFField field, IntFunction<? extends T> mapper, IntFunction<T[]> generator) {
      final int count = field.getCount();
      Object result = null;
      if (count == 1) {
         result = mapper.apply(0);
      } else if (count > 1) {
         result = IntStream.range(0, count).mapToObj(mapper).toArray(generator);
      }
      return result;
   }

   private static <T> Object getValue(TIFFField field, T[] list) {
      final int count = field.getCount();
      Object result = null;
      if (count == 1) {
         result = list[0];
      } else if (count > 1) {
         result = list;
      }
      return result;
   }

   private Object getTiffValue(TIFFField field) {
      Object result = null;

      switch (field.getType()) {
      case TIFFTag.TIFF_ASCII:
         result = getValue(field, i->field.getAsString(i), String[]::new);
         break;
      case TIFFTag.TIFF_BYTE:
      case TIFFTag.TIFF_SBYTE:
         result = getValue(field, ArrayUtils.toObject(field.getAsBytes()));
         break;
      case TIFFTag.TIFF_DOUBLE:
         result = getValue(field, ArrayUtils.toObject(field.getAsDoubles()));
         break;
      case TIFFTag.TIFF_FLOAT:
         result = getValue(field, ArrayUtils.toObject(field.getAsFloats()));
         break;
      case TIFFTag.TIFF_IFD_POINTER:
         break;
      case TIFFTag.TIFF_SLONG:
      case TIFFTag.TIFF_LONG:
         result = getValue(field, ArrayUtils.toObject(field.getAsLongs()));
         break;
      case TIFFTag.TIFF_RATIONAL:
      case TIFFTag.TIFF_SRATIONAL:
         result = getValue(field, i-> {
            long[] rational = field.getAsRational(i);
            return ((double)rational[0]) / ((double)rational[1]);
         }, Double[]::new);
         break;
      case TIFFTag.TIFF_SHORT:
      case TIFFTag.TIFF_SSHORT:
         result = getValue(field, ArrayUtils.toObject(field.getAsInts()));
      case TIFFTag.TIFF_UNDEFINED:
         break;
      }
      return result;
   }


   private static <T> T getValue(IIOMetadataNode node, Function<String, T> converter) {
      final String stringValue = node.getAttribute("value");
      T value = converter.apply(stringValue);

      return value;
   }


   private <T> T getMetadata(Class<T> clazz, T defaultValue, Integer... tagInfo ) {
      return Arrays.stream(tagInfo)
            .map(k-> Arrays.stream(tiffMetadata).filter(t->t.getTagNumber() == k).map(t-> t.getData())
                  .findFirst().orElse(null))
            .filter(t->clazz.isInstance(t))
            .map(t->(T)t)
            .findFirst().orElse(defaultValue);
   }

   private void initializeCameraModel() throws IOException, DataStoreException {
      final GeoTiffStoreProvider storeProvider = DataStores.providers().stream().filter(s->s instanceof GeoTiffStoreProvider).map(s->(GeoTiffStoreProvider)s).findFirst().orElse(null);
      try (final InputStream imageInputStream = imageInputStreamSupplier.get();
            final GeoTiffStore store = (GeoTiffStore)storeProvider.open(new StorageConnector(imageInputStream))) {
         this.storeMetadata = store.getMetadata();
         final Georeferenceable spatial = storeMetadata.getSpatialRepresentationInfo().stream().filter(s->s instanceof Georeferenceable).map(s->(Georeferenceable)s).findFirst().orElse(null);

         String failureReason = null;
         if (spatial != null) {
            final Collection<? extends GeolocationInformation> geolocationInfo = spatial.getGeolocationInformation();
            if (geolocationInfo != null) {
               final CoordinateOperation geoInformation = geolocationInfo.stream().map(g->(CoordinateOperation)g).findFirst().orElse(null);
               this.rasterToModel = geoInformation.getMathTransform();
               final Collection<? extends ReferenceSystem> referenceSystemInfo = storeMetadata.getReferenceSystemInfo();
               if (referenceSystemInfo != null) {
                  this.imageCRS = referenceSystemInfo.stream().map(c->(CoordinateReferenceSystem)c).findFirst().orElse(WGS84);
               } else {
                  failureReason = "Couldn't parse ReferenceSystem information from image";
               }
            } else {
               failureReason = "Couldn't parse Geolocation information from image spatial information";
            }
         } else {
            failureReason = "Couldn't parse spatial information from image";
         }
         if (failureReason != null) {
            throw new IOException(String.format("Couldn't initialize CameraModel (Cause: %s)", failureReason));
         }
      }
   }


   @Override
   @JsonIgnore
   public ImageKey getImageKey() {
      return this.imageKey;
   }

   @Override
   public void close() throws Exception {
      CloseableUtils.close(imageInputStream, seekableStream);
   }

   @Override
   @JsonIgnore
   public RenderedImage getRenderedImage() {
      return this.tiledRenderedImage;
   }

   @Override
   public Map<String, Object> getMetadata() {
      return this.metadata;
   }

   @JsonIgnore
   public Map<Integer, Object> getMetadataByTiffTag() {
      return Arrays.stream(tiffMetadata).collect(Collectors.toMap(k->k.getTagNumber(), v->getTiffValue(v)));
   }

   @JsonIgnore
   public CoordinateReferenceSystem getImageCRS() {
      return imageCRS;
   }

   @JsonIgnore
   public MathTransform getRasterToModel() {
      return this.rasterToModel;
   }

   @Override
   @JsonIgnore
   public CameraModel getCameraModel() {
      CameraModel cameraModel = null;
      try {
         cameraModel = new MapProjectionCamera(this.rasterToModel, this.imageCRS, WGS84);
      } catch (FactoryException | NoninvertibleTransformException e) {
         LOGGER.error(e.getMessage(), e);
      }
      return cameraModel;
   }

   @Override
   @JsonIgnore
   public Object getSegmentMetadataObject() {
      return this;
   }

   private static <T> T lookupProviderByName(final ServiceRegistry registry, final String providerClassName) {
      try {
         return (T) registry.getServiceProviderByClass(Class.forName(providerClassName));
      }
      catch (ClassNotFoundException ignore) {
         return null;
      }
   }
}
