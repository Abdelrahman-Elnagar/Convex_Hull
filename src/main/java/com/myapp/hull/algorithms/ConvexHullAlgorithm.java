package com.myapp.hull.algorithms;

import java.util.List;

import com.myapp.hull.Point2D;

/**
 * Interface defining the contract for convex hull algorithms.
 * A convex hull is the smallest convex polygon that contains all the given points.
 * 
 * @author Your Name
 * @version 1.0
 */
public interface ConvexHullAlgorithm {
    /**
     * Computes the convex hull of a given set of points.
     * The returned list contains the vertices of the convex hull in counter-clockwise order.
     * 
     * @param points The input set of points
     * @return A list of points forming the convex hull in CCW order
     * @throws IllegalArgumentException if points is null
     */
    List<Point2D> computeHull(List<Point2D> points);
} 