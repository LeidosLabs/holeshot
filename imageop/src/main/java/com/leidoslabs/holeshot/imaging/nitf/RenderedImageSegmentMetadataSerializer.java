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

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.apache.commons.lang3.StringUtils;
import org.codice.imaging.nitf.core.common.NitfFormatException;
import org.codice.imaging.nitf.core.image.ImageCoordinates;
import org.codice.imaging.nitf.core.image.ImageSegment;
import org.codice.imaging.nitf.core.tre.Tre;
import org.codice.imaging.nitf.core.tre.TreEntry;
import org.codice.imaging.nitf.core.tre.TreGroup;
import org.image.common.util.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.leidoslabs.holeshot.imaging.ImageKey;
import com.leidoslabs.holeshot.imaging.photogrammetry.rpc.ErrorEstimate;
import com.leidoslabs.holeshot.imaging.photogrammetry.rpc.NormalizationCoefficients;
import com.leidoslabs.holeshot.imaging.photogrammetry.rpc.RPCBTre;
import com.leidoslabs.holeshot.imaging.photogrammetry.rpc.RPCCameraModel;
import com.leidoslabs.holeshot.imaging.photogrammetry.rpc.RationalCameraPolynomial;

/**
 * JSON Serialization for ImageSegmentMetadata
 * Created by parrise on 8/28/17.
 */
public class RenderedImageSegmentMetadataSerializer extends JsonSerializer<RenderedImageSegment> {
   private static final Logger LOGGER = LoggerFactory.getLogger(RenderedImageSegmentMetadataSerializer.class);

   private static final int DOUBLE_PRECISION = 16;

  private String igeoValue;

  @Override
  public void serialize(RenderedImageSegment rendered,
                        JsonGenerator jsonGenerator,
                        SerializerProvider serializerProvider) throws IOException {
    if (rendered == null) {
      jsonGenerator.writeNull();
      return;
    }
    try {
      ImageSegment imageSegment = rendered.getImageSegment();
      jsonGenerator.writeStartObject();
      writeFieldIfNotNull("IID1", imageSegment.getIdentifier(), jsonGenerator);
      jsonGenerator.writeNumberField("IDATIM", ImageKey.toNITFFormat(imageSegment.getImageDateTime().getZonedDateTime()));
      jsonGenerator.writeNumberField("FDT", ImageKey.toNITFFormat(ZonedDateTime.now(ZoneOffset.UTC)));
      writeFieldIfNotNull("TGTID", imageSegment.getImageTargetId().textValue(), jsonGenerator);
      writeFieldIfNotNull("IID2", imageSegment.getImageIdentifier2(), jsonGenerator);
      writeFieldIfNotNull("ISCLAS", imageSegment.getSecurityMetadata().getSecurityClassification().getTextEquivalent(), jsonGenerator);
      writeFieldIfNotNull("ISCLSY", imageSegment.getSecurityMetadata().getSecurityClassificationSystem(), jsonGenerator);
      writeFieldIfNotNull("ISCODE", imageSegment.getSecurityMetadata().getCodewords(), jsonGenerator);
      writeFieldIfNotNull("ISCTLH", imageSegment.getSecurityMetadata().getControlAndHandling(), jsonGenerator);
      writeFieldIfNotNull("ISREL", imageSegment.getSecurityMetadata().getReleaseInstructions(), jsonGenerator);
      writeFieldIfNotNull("ISDCTP", imageSegment.getSecurityMetadata().getDeclassificationType(), jsonGenerator);
      writeFieldIfNotNull("ISDCDT", imageSegment.getSecurityMetadata().getDeclassificationDate(), jsonGenerator);
      writeFieldIfNotNull("ISDCXM", imageSegment.getSecurityMetadata().getDeclassificationExemption(), jsonGenerator);
      writeFieldIfNotNull("ISDG", imageSegment.getSecurityMetadata().getDowngrade(), jsonGenerator);
      writeFieldIfNotNull("ISDGDT", imageSegment.getSecurityMetadata().getDowngradeDate(), jsonGenerator);
      writeFieldIfNotNull("ISCLTX", imageSegment.getSecurityMetadata().getClassificationText(), jsonGenerator);
      writeFieldIfNotNull("ISCATP", imageSegment.getSecurityMetadata().getClassificationAuthorityType(), jsonGenerator);
      writeFieldIfNotNull("ISCAUT", imageSegment.getSecurityMetadata().getClassificationAuthority(), jsonGenerator);
      writeFieldIfNotNull("ISCRSN", imageSegment.getSecurityMetadata().getClassificationReason(), jsonGenerator);
      writeFieldIfNotNull("ISSRDT", imageSegment.getSecurityMetadata().getSecuritySourceDate(), jsonGenerator);
      writeFieldIfNotNull("ISCTLN", imageSegment.getSecurityMetadata().getSecurityControlNumber(), jsonGenerator);
      writeFieldIfNotNull("ISORCE", imageSegment.getImageSource(), jsonGenerator);
      jsonGenerator.writeNumberField("NROWS", imageSegment.getNumberOfRows());
      jsonGenerator.writeNumberField("NCOLS", imageSegment.getNumberOfColumns());
      writeFieldIfNotNull("PVTYPE", imageSegment.getPixelValueType().getTextEquivalent(), jsonGenerator);
      writeFieldIfNotNull("IREP", imageSegment.getImageRepresentation().getTextEquivalent(), jsonGenerator);
      writeFieldIfNotNull("ICAT", imageSegment.getImageCategory().getTextEquivalent(), jsonGenerator);
      jsonGenerator.writeNumberField("ABPP", imageSegment.getActualBitsPerPixelPerBand());
      writeFieldIfNotNull("PJUST", imageSegment.getPixelJustification().getTextEquivalent(), jsonGenerator);
      writeFieldIfNotNull("ICORDS", imageSegment.getImageCoordinatesRepresentation().getTextEquivalent(imageSegment.getFileType()), jsonGenerator);
      if (imageSegment.getImageCoordinates() != null) {
        ImageCoordinates ic = imageSegment.getImageCoordinates();
        String igeoValue =
            ic.getCoordinate00().getSourceFormat() +
            ic.getCoordinate0MaxCol().getSourceFormat() +
            ic.getCoordinateMaxRowMaxCol().getSourceFormat() +
            ic.getCoordinateMaxRow0().getSourceFormat();
        jsonGenerator.writeStringField("IGEOLO",igeoValue);
      }
      jsonGenerator.writeNumberField("NICOM", imageSegment.getImageComments().size());

      int commentNumber = 1;
      for (String comment : imageSegment.getImageComments()) {
        jsonGenerator.writeStringField("ICOM" + commentNumber, comment);
        ++commentNumber;
      }
      writeFieldIfNotNull("IC", imageSegment.getImageCompression().getTextEquivalent(), jsonGenerator);
      writeFieldIfNotNull("COMRAT", imageSegment.getCompressionRate(), jsonGenerator);
      jsonGenerator.writeNumberField("NBANDS", imageSegment.getNumBands());
      writeFieldIfNotNull("IMODE", imageSegment.getImageMode().getTextEquivalent(), jsonGenerator);
      jsonGenerator.writeNumberField("NBPR", imageSegment.getNumberOfBlocksPerRow());
      jsonGenerator.writeNumberField("NBPC", imageSegment.getNumberOfBlocksPerColumn());
      jsonGenerator.writeNumberField("NPPBV", imageSegment.getNumberOfPixelsPerBlockVertical());
      jsonGenerator.writeNumberField("NPPBH", imageSegment.getNumberOfPixelsPerBlockHorizontal());
      jsonGenerator.writeNumberField("NBPP", imageSegment.getNumberOfBitsPerPixelPerBand());
      jsonGenerator.writeNumberField("IDLVL", imageSegment.getImageDisplayLevel());
      jsonGenerator.writeNumberField("IALVL", imageSegment.getAttachmentLevel());
      writeFieldIfNotNull("ILOC", imageSegment.getImageLocationRow() + "" + imageSegment.getImageLocationColumn(), jsonGenerator);
      writeFieldIfNotNull("IMAG", imageSegment.getImageMagnification(), jsonGenerator);
      //jsonGenerator.writeObjectField("TRES",imageSegment.getTREsFlat());
      
      RPCBTre rpcBTre = ((RPCCameraModel) rendered.getCameraModel()).getNitfRpcBTag();
      jsonGenerator.writeObject(rpcBTre);
      for (Tre tre : imageSegment.getTREsRawStructure().getTREs()) {
        jsonGenerator.writeObjectFieldStart(tre.getName());
        for (TreEntry entry : tre.getEntries()) {
          writeTreEntry(entry, jsonGenerator, serializerProvider);
        }
        jsonGenerator.writeEndObject();
      }

      jsonGenerator.writeEndObject();
    } catch (NitfFormatException nfe) {
      throw new IOException("Problem Encoding NITF ImageSegment Metadata", nfe);
    }
  }

  private void writeTreEntry(TreEntry entry,
                             JsonGenerator jsonGenerator,
                             SerializerProvider serializerProvider) throws IOException {

    if (entry.isSimpleField()) {
      writeFieldIfNotNull(entry.getName(), entry.getFieldValue(),jsonGenerator);
    } else if (entry.hasGroups()) {
      int i = 1;
      for (TreGroup group : entry.getGroups()) {
        for (TreEntry groupEntry : group.getEntries()) {
          writeFieldIfNotNull(groupEntry.getName() + "_" + i, groupEntry.getFieldValue(),jsonGenerator);
        }
        ++i;
      }
    } else {
      LOGGER.warn("WARNING: TRE not simple & not group!");
    }

  }

  private final void writeFieldIfNotNull(String fieldName, String value, JsonGenerator jsonGenerator) throws IOException {
    if (StringUtils.isNotBlank(value)) {
       final String trimmedValue = value.trim();
       if (NumberUtils.isInteger(trimmedValue)) {
          final long numValue = Long.parseLong(trimmedValue);
          jsonGenerator.writeNumberField(fieldName, numValue);
       } else if (NumberUtils.isDouble(value)) {
          final BigDecimal numValue = new BigDecimal(trimmedValue);
          numValue.setScale(DOUBLE_PRECISION, RoundingMode.FLOOR);
          jsonGenerator.writeNumberField(fieldName, numValue);
       } else {
         jsonGenerator.writeStringField(fieldName, trimmedValue);
       }
    }
  }
  
}
