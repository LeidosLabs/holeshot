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

package com.leidoslabs.holeshot.elt.imagechain;

import org.joml.Vector3d;

/**
 * Vertex/Texture coordinate tuple
 */
public class VertexTexturePair {
  private Vector3d vertexCoord;
  private Vector3d textureCoord;
  
  public VertexTexturePair(Vector3d vertexCoord, Vector3d textureCoord) {
    this.vertexCoord = vertexCoord;
    this.textureCoord = textureCoord;
  }

  public Vector3d getVertexCoord() {
    return vertexCoord;
  }

  public Vector3d getTextureCoord() {
    return textureCoord;
  }
}

