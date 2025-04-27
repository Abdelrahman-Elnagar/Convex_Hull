package com.myapp.hull.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.myapp.hull.Point2D;

/**
 * Implementation of the Graham Scan algorithm for computing the convex hull.
 * Time complexity: O(n log n)
 */
public class GrahamScan implements ConvexHullAlgorithm {
    @Override
    public List<Point2D> computeHull(List<Point2D> points) {
        if (points == null || points.size() < 3) {
            return new ArrayList<>(points);
        }

        // Find the pivot point (lowest y, leftmost if tie)
        Point2D pivot = findPivot(points);
        
        // Sort points by polar angle relative to pivot
        List<Point2D> sortedPoints = new ArrayList<>(points);
        sortedPoints.sort((p1, p2) -> {
            if (p1.equals(pivot)) return -1;
            if (p2.equals(pivot)) return 1;
            
            double cross = crossProduct(pivot, p1, p2);
            if (cross == 0) {
                // If collinear, sort by distance from pivot
                return Double.compare(distanceSquared(pivot, p1), distanceSquared(pivot, p2));
            }
            return cross > 0 ? -1 : 1;
        });

        // Build the hull using a stack
        Stack<Point2D> hull = new Stack<>();
        hull.push(sortedPoints.get(0));
        hull.push(sortedPoints.get(1));

        for (int i = 2; i < sortedPoints.size(); i++) {
            Point2D top = hull.pop();
            while (!hull.isEmpty() && 
                   crossProduct(hull.peek(), top, sortedPoints.get(i)) <= 0) {
                top = hull.pop();
            }
            hull.push(top);
            hull.push(sortedPoints.get(i));
        }

        return new ArrayList<>(hull);
    }

    private Point2D findPivot(List<Point2D> points) {
        Point2D pivot = points.get(0);
        for (Point2D p : points) {
            if (p.getY() < pivot.getY() || 
                (p.getY() == pivot.getY() && p.getX() < pivot.getX())) {
                pivot = p;
            }
        }
        return pivot;
    }

    private double crossProduct(Point2D a, Point2D b, Point2D c) {
        return (b.getX() - a.getX()) * (c.getY() - a.getY()) - 
               (b.getY() - a.getY()) * (c.getX() - a.getX());
    }

    private double distanceSquared(Point2D a, Point2D b) {
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        return dx * dx + dy * dy;
    }
} 