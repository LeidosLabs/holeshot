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

package com.leidoslabs.holeshot.security.auth;

/**
 * Exception to use when a security access string is either null or empty
 * 
 * @author jdmcmaho
 *
 */
@SuppressWarnings("serial")
public class EmptyAccessStringException extends RuntimeException {

  /**
   * Constructor
   * 
   * @param msg Exception message
   * @param t Parent exception
   */
  public EmptyAccessStringException(String msg, Throwable t) {
    super(msg, t);
  }

  /**
   * Constructor
   * 
   * @param msg Exception message
   */
  public EmptyAccessStringException(String msg) {
    super(msg);
  }
}
