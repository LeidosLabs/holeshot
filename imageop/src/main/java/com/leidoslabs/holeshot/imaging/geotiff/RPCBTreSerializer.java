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

import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.leidoslabs.holeshot.imaging.photogrammetry.rpc.RPCBTre;

/**
 * Serialize RPCTre as JSON
 */
public class RPCBTreSerializer extends JsonSerializer<RPCBTre> {
   
   private static void write(JsonGenerator jsonGenerator, String fieldName, Optional<?> opt) throws IOException {
      if (opt.isPresent()) {
          jsonGenerator.writeObjectField(fieldName, opt.get());  
      }
   }
   
   private static void writeCoefficients(JsonGenerator jsonGenerator, String fieldBasename, Optional<double[]> opt) throws IOException {
      if (opt.isPresent()) {
         double[] coeffs = opt.get();
         
         for (int i=0;i<coeffs.length;++i) {
            jsonGenerator.writeNumberField(String.format("%s%d", fieldBasename, i +1), coeffs[i]);
         }
      }
   }

   @Override
   public void serialize(RPCBTre rpcBTre,
         JsonGenerator jsonGenerator,
         SerializerProvider serializerProvider) throws IOException {

      if (rpcBTre != null) {
         jsonGenerator.writeObjectFieldStart(rpcBTre.getTagName());
         
         write(jsonGenerator,  "ERR_BIAS", rpcBTre.getErrorBias());
         write(jsonGenerator,  "ERR_RAND", rpcBTre.getErrorRand());
         write(jsonGenerator,  "SAMP_OFF", rpcBTre.getSampleOffset());
         write(jsonGenerator,  "LINE_OFF", rpcBTre.getLineOffset());
         write(jsonGenerator,  "LONG_OFF", rpcBTre.getGeodeticLonOffset());
         write(jsonGenerator,  "LAT_OFF", rpcBTre.getGeodeticLatOffset());
         write(jsonGenerator,  "HEIGHT_OFF", rpcBTre.getGeodeticHeightOffset());
         write(jsonGenerator,  "SAMP_SCALE", rpcBTre.getSampleScale());
         write(jsonGenerator,  "LINE_SCALE", rpcBTre.getLineScale());
         write(jsonGenerator,  "LONG_SCALE", rpcBTre.getGeodeticLonScale());
         write(jsonGenerator,  "LAT_SCALE", rpcBTre.getGeodeticLatScale());
         write(jsonGenerator,  "HEIGHT_SCALE", rpcBTre.getGeodeticHeightScale());
         
         writeCoefficients(jsonGenerator, "LINE_NUM_COEFF_", rpcBTre.getLineNumeratorCoeff());
         writeCoefficients(jsonGenerator, "LINE_DEN_COEFF_", rpcBTre.getLineDenominatorCoeff());
         writeCoefficients(jsonGenerator, "SAMP_NUM_COEFF_", rpcBTre.getSampleNumeratorCoeff());
         writeCoefficients(jsonGenerator, "SAMP_DEN_COEFF_", rpcBTre.getSampleDenomoniatorCoeff());
         
         jsonGenerator.writeEndObject();
      }
   }
}
