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
package org.image.common.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for AutoCloseables.
 *
 */
public class CloseableUtils {
	/**
	 * Closes all closeables and reports first exception if one is encountered
	 * @param closeables Varargs of AutoClosable
	 * @throws IOException The first exception encountered in closing all closeables
	 */
   public static void close(AutoCloseable... closeables) throws IOException {
      if (closeables != null) {
         final List<Exception> exceptions = Arrays.stream(closeables)
         .map(c->close(c))
         .filter(e->e != null)
         .collect(Collectors.toList());

         if (!exceptions.isEmpty()) {
            final String errorMessage = String.format("Reporting first of %d exceptions in closing %d closeables", exceptions.size(), closeables.length);
            throw new IOException(errorMessage, exceptions.get(0));
         }
      }
   }

   private static Exception close(AutoCloseable closeable) {
      Exception result = null;
      if (closeable != null) {
         try {
            closeable.close();
         } catch (Exception e) {
            result = e;
         }
      }
      return result;
   }
}
