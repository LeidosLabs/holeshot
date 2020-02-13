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

package com.leidoslabs.holeshot.chipper.jaxrs;

import java.net.MalformedURLException;
import java.net.URL;

import javax.ws.rs.ext.ParamConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX RS Converter for URLs
 */
public class URLConverter implements ParamConverter<URL> {
   private static final Logger LOGGER = LoggerFactory.getLogger(URLConverter.class);

   @Override
   public URL fromString(String value) {
      URL result = null;
      if (value != null) {
         try {
            result = new URL(value);
         } catch (MalformedURLException e) {
            LOGGER.error(e.getMessage(), e);
         }
      }
      return result;
   }

   @Override
   public String toString(URL value) {
      String result = null;
      if (value != null) {
         result = value.toString();
      }
      return result;
   }

}
