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
package com.leidoslabs.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.kdtree.KdTree;

/**
 * Wrapper class for KdTree provided by 
 *     org.locationtech.jtsto match KDBush functionality
 * @param <T>
 */
public class KDBush<T> {
	
    private KdTree tree = new KdTree();
    private Function<T, Coordinate> getCoordinate;
    private ArrayList<T> points;

    /**
     * Initializes KDBush, inserts Points(as coordinate) to KDtree, also keeps track of points in a List
     * @param points
     * @param getCoordinate function from T -> coordinate
     */
    public KDBush(T[] points, Function<T, Coordinate> getCoordinate) {
        
        this.getCoordinate = getCoordinate;
        this.points = new ArrayList<>(points.length);

        for(T point : points) {
            tree.insert(getCoordinate.apply(point), point);
            this.points.add(point);
        }
    }
    
    public KDBush(List<T> points, Function<T, Coordinate> getCoordinate) {

        this.getCoordinate = getCoordinate;
        this.points = new ArrayList<>(points.size());

        for(T point : points) {
            tree.insert(getCoordinate.apply(point), point);
            this.points.add(point);
        }
    }

    /*
        Returns a list of all points in the tree
    */
    public List<T> getPoints() {
        return points;
    }

    /**
     * Returns a list of all points within the rectuangular area specified
     * @param minX
     * @param minY
     * @param maxX
     * @param maxY
     * @return
     */
    public List<T> range(double minX, double minY, double maxX, double maxY) {

        List<T> toReturn = new LinkedList<>();

        //Uses a KdNodeVisitor to add all nodes to toReturn
        //This is a workaround because tree.query() returns a List (not List<KdNode>)
        tree.query(new Envelope(minX, maxX, minY, maxY), (node) -> {
            toReturn.add((T)node.getData());
        });

        return toReturn;
    }
    
    /**
     * Returns a list of all points within a radius of the point specified
     * @param x
     * @param y
     * @param r
     * @return
     */
    public List<T> within(double x, double y, double r) {

        //Finds all points in a bounding box around the point, using 2*r as side lengths
        List<T> box = range(x-r, y-r, x+r, y+r);
        List<T> results = new LinkedList<>();

        double r2 = r * r;
        
        //Only considers points inside a radius r around the point
        for(T t : box) {

            Coordinate coord = getCoordinate.apply(t);

            double dx = coord.getX() - x;
            double dy = coord.getY() - y;

            // test distance from (x,y)
            if(dx*dx + dy*dy <= r2){
                results.add(t);
            }
        }
        
        return results;
    }

    public String toString() {
        return points.toString();
    }
}
