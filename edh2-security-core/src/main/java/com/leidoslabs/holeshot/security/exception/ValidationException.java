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

package com.leidoslabs.holeshot.security.exception;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Base exception for all validation problems.
 * 
 * @author jonesw
 *
 * @deprecated Use either javax.validation.ConstraintViolationException or
 * javax.validation.ValidationException instead.
 */
@SuppressWarnings("serial")
@Deprecated
public class ValidationException extends RuntimeException {
  private Collection<String> explanations;

  public ValidationException() {
    super();
  }

  public ValidationException(String message) {
    super(message);
  }

  public ValidationException(String message, Throwable cause) {
    super(message, cause);
  }

  public ValidationException(Throwable cause) {
    super(cause);
  }

  public ValidationException(String message, Collection<String> explanations) {
    super(message);
    this.explanations = explanations;
  }

  public ValidationException(String message, Throwable cause, Collection<String> explanations) {
    super(message, cause);
    this.explanations = explanations;
  }

  public ValidationException(Throwable cause, Collection<String> explanations) {
    super(cause);
    this.explanations = explanations;
  }

  public ValidationException(Collection<String> explanations) {
    super();
    this.explanations = explanations;
  }

  public void addExplanation(Collection<String> explanations) {
    if (explanations != null) {
      if (this.explanations == null) {
        this.explanations = new ArrayList<>();
      }
      this.explanations.addAll(explanations);
    }
  }

  public void addExplanation(String explanation) {
    addExplanation(Arrays.asList(explanation));
  }

  public Collection<String> getExplanations() {
    return explanations;
  }

}
