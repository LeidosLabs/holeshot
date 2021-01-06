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

package com.leidoslabs.holeshot.analytics.common.model;

/**
 * Tile object augmented with information about expected request likelihood
 */
public class ScoredTile {

    private int x;
    private int y;
    private int rSet;
    private int score;

    /**
     * A tile within a tile pyramid along with a standardized score
     * @param rSet The rSet of the tile
     * @param x The column of the tile in the rSet
     * @param y The row of the tile in the rSet
     * @param score The score corresponding to the likelihood of the tile being accessed imminently
     */
    public ScoredTile(int rSet, int x, int y, int score) {
        this.x = x;
        this.y = y;
        this.rSet = rSet;
        this.score = score;
    }

    /**
     * For deserialization
     */
    public ScoredTile() { }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getrSet() {
        return rSet;
    }

    public void setrSet(int rSet) {
        this.rSet = rSet;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }
}
