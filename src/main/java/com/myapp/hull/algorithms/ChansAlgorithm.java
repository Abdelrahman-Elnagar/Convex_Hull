package com.myapp.hull.algorithms;

import java.util.ArrayList;
import java.util.List;

import com.myapp.hull.Point2D;

/**
 * Implementation of Chan's Algorithm for computing the convex hull.
 * Time complexity: O(n log h) where h is the number of points in the hull.
 */
public class ChansAlgorithm implements ConvexHullAlgorithm {
    private final GrahamScan grahamScan = new GrahamScan();
    private final List<Point2D> hull = new ArrayList<>();
    private final List<List<Point2D>> groups = new ArrayList<>();
    private final List<List<Point2D>> miniHulls = new ArrayList<>();
    private static final int MAX_GROUP_SIZE = 100;
    private static final int MAX_ITERATIONS = 20;

    @Override
    public List<Point2D> computeHull(List<Point2D> points) {
        if (points == null || points.size() < 3) {
            return new ArrayList<>(points);
        }

        int m = Math.min(MAX_GROUP_SIZE, points.size()); // Start with reasonable group size
        int iterations = 0;
        List<Point2D> result;
        
        do {
            result = tryComputeHull(points, m);
            m = Math.min(m * 2, points.size()); // Double the guess, but don't exceed n
            iterations++;
        } while (result == null && iterations < MAX_ITERATIONS);

        if (result == null) {
            // Fallback to Graham Scan if Chan's algorithm fails
            return grahamScan.computeHull(points);
        }

        return result;
    }

    private List<Point2D> tryComputeHull(List<Point2D> points, int m) {
        // Clear previous results
        hull.clear();
        groups.clear();
        miniHulls.clear();

        // Phase 1: Partition and compute mini-hulls
        partitionPoints(points, m);
        computeMiniHulls();

        // Phase 2: Jarvis March over mini-hulls
        return performJarvisMarch(m);
    }

    private void partitionPoints(List<Point2D> points, int m) {
        int n = points.size();
        int numGroups = (n + m - 1) / m; // Ceiling division

        for (int i = 0; i < numGroups; i++) {
            int start = i * m;
            int end = Math.min(start + m, n);
            groups.add(new ArrayList<>(points.subList(start, end)));
        }
    }

    private void computeMiniHulls() {
        for (List<Point2D> group : groups) {
            miniHulls.add(grahamScan.computeHull(group));
        }
    }

    private List<Point2D> performJarvisMarch(int m) {
        // Find the leftmost point
        Point2D current = findLeftmostPoint();
        hull.add(current);
        
        // Find the next point in each group
        Point2D next = findNextPointInGroups(current);
        hull.add(next);
        
        // Continue until we complete the hull or exceed m steps
        for (int i = 2; i <= m; i++) {
            Point2D nextPoint = JarvisHelper.findNextPoint(current, next, miniHulls);
            
            if (nextPoint.equals(hull.get(0))) {
                return new ArrayList<>(hull); // Hull is complete
            }
            
            hull.add(nextPoint);
            current = next;
            next = nextPoint;
        }
        
        return null; // Hull not complete within m steps
    }

    private Point2D findLeftmostPoint() {
        Point2D leftmost = null;
        double minX = Double.POSITIVE_INFINITY;
        
        for (List<Point2D> hull : miniHulls) {
            for (Point2D p : hull) {
                if (p.getX() < minX) {
                    minX = p.getX();
                    leftmost = p;
                }
            }
        }
        
        return leftmost;
    }

    private Point2D findNextPointInGroups(Point2D current) {
        Point2D next = null;
        double maxAngle = Double.NEGATIVE_INFINITY;
        
        for (List<Point2D> hull : miniHulls) {
            for (Point2D p : hull) {
                if (!p.equals(current)) {
                    double angle = Math.atan2(p.getY() - current.getY(), p.getX() - current.getX());
                    if (angle > maxAngle) {
                        maxAngle = angle;
                        next = p;
                    }
                }
            }
        }
        
        return next;
    }
} 