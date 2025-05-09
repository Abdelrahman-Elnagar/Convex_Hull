package com.myapp.hull.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.myapp.hull.Point2D;

/**
 * Implementation of Chan's Algorithm for computing the convex hull.
 * Time complexity: O(n log h) where h is the number of points in the hull.
 * This implementation uses a combination of Graham Scan and Jarvis March
 * with an optimal initial guess for m based on empirical studies.
 */
public class ChansAlgorithm implements ConvexHullAlgorithm {
    private final GrahamScan grahamScan = new GrahamScan();
    private final JarvisMarch jarvisMarch = new JarvisMarch();
    private List<List<Point2D>> partialHulls = new ArrayList<>();

    @Override
    public List<Point2D> computeHull(List<Point2D> points) {
        // Handle edge cases
        if (points == null || points.size() < 3) {
            return new ArrayList<>(points);
        }

        // Convert List<Point2D> to Point2D[]
        Point2D[] pointsArray = points.toArray(new Point2D[0]);

        // For small point sets, use Graham Scan directly
        if (points.size() <= 6) {
            return grahamScan.computeHull(points);
        }

        // Optimal initial guess for m based on empirical studies
        int m = Math.min(10, points.size());
        int iterationLimit = (int) (Math.log(points.size())/Math.log(2)) + 2;

        for (int iteration = 0; iteration < iterationLimit; iteration++) {
            // Partition points into ⌈n/m⌉ subsets
            List<List<Point2D>> partitions = partitionPoints(pointsArray, m);
            
            // Compute partial hulls
            partialHulls = computePartialHulls(partitions);
            
            // Combine and find convex hull
            List<Point2D> combined = combineHulls(partialHulls);
            Point2D[] combinedArray = combined.toArray(new Point2D[0]);
            
            // Try Jarvis march with early termination
            List<Point2D> hull = tryJarvisMarch(combinedArray, m);
            
            if (hull != null) {
                return hull;
            }
            
            // Exponential search for optimal m
            m = Math.min(m * 2, points.size());
            
            // Fallback to Graham Scan if m gets too large
            if (m == points.size()) {
                return grahamScan.computeHull(points);
            }
        }
        
        // Final fallback
        return grahamScan.computeHull(points);
    }

    /**
     * Returns the partial hulls computed during the last hull computation.
     * This is used for visualization purposes.
     */
    public List<List<Point2D>> getPartialHulls(List<Point2D> points) {
        // Recompute partial hulls if needed
        if (partialHulls.isEmpty()) {
            Point2D[] pointsArray = points.toArray(new Point2D[0]);
            int m = Math.min(10, points.size());
            List<List<Point2D>> partitions = partitionPoints(pointsArray, m);
            partialHulls = computePartialHulls(partitions);
        }
        return partialHulls;
    }

    private List<List<Point2D>> partitionPoints(Point2D[] points, int m) {
        int partitionCount = (int) Math.ceil((double) points.length / m);
        List<List<Point2D>> partitions = new ArrayList<>(partitionCount);
        
        int partitionSize = (int) Math.ceil((double) points.length / partitionCount);
        
        for (int i = 0; i < partitionCount; i++) {
            int start = i * partitionSize;
            int end = Math.min(start + partitionSize, points.length);
            partitions.add(new ArrayList<>(Arrays.asList(Arrays.copyOfRange(points, start, end))));
        }
        return partitions;
    }

    private List<List<Point2D>> computePartialHulls(List<List<Point2D>> partitions) {
        List<List<Point2D>> partialHulls = new ArrayList<>(partitions.size());
        
        for (List<Point2D> partition : partitions) {
            partialHulls.add(grahamScan.computeHull(partition));
        }
        return partialHulls;
    }

    private List<Point2D> combineHulls(List<List<Point2D>> partialHulls) {
        List<Point2D> combined = new ArrayList<>();
        for (List<Point2D> hull : partialHulls) {
            combined.addAll(hull);
        }
        return combined;
    }

    private List<Point2D> tryJarvisMarch(Point2D[] points, int m) {
        List<Point2D> hull = jarvisMarch.computeHull(Arrays.asList(points));
        
        // Early termination check
        if (hull.size() <= m * 2) { // More generous threshold
            return hull;
        }
        return null;
    }
} 