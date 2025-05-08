package com.myapp.hull.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import com.myapp.hull.Point2D;

/**
 * Graham Scan algorithm for finding the convex hull of a set of points.
 * This implementation correctly handles collinear points.
 */
public class GrahamScan implements ConvexHullAlgorithm {
    @Override
    public List<Point2D> computeHull(List<Point2D> points) {
        if (points == null || points.size() < 2) {
            return new ArrayList<>(points);
        }

        // Convert List<Point2D> to Point2D[]
        Point2D[] pointsArray = points.toArray(new Point2D[0]);

        // Find the lowest point (and leftmost if tied)
        Point2D start = pointsArray[0];
        for (int i = 1; i < pointsArray.length; i++) {
            if (start.getY() > pointsArray[i].getY() || 
                (start.getY() == pointsArray[i].getY() && start.getX() > pointsArray[i].getX())) {
                start = pointsArray[i];
            }
        }

        // Sort points by polar angle with respect to the start point
        sortByPolarAngle(pointsArray, start);

        // Graham scan algorithm
        Stack<Point2D> stack = new Stack<>();
        stack.push(pointsArray[0]);
        
        if (pointsArray.length > 1) {
            stack.push(pointsArray[1]);
        }
        
        // Process each point in the sorted array (starting from the 3rd point)
        for (int i = 2; i < pointsArray.length; i++) {
            // Pop the top point from the stack - this is our current 'top of hull' candidate
            Point2D top = stack.pop();
            
            // While we can make a right turn or have collinear points (cross product <= 0)
            // and the stack isn't empty, we need to potentially remove points
            while (!stack.isEmpty() && crossProduct(stack.peek(), top, pointsArray[i]) <= 0) {
                // Special case: if the points are collinear (cross product == 0)
                if (crossProduct(stack.peek(), top, pointsArray[i]) == 0) {
                    // For collinear points, we want to keep only the furthest one
                    if (distanceSquared(stack.peek(), pointsArray[i]) > distanceSquared(stack.peek(), top)) {
                        // If the new point is further, it becomes our new candidate
                        top = pointsArray[i];
                    }
                    // We break because we've handled this collinear case
                    break;
                }
                // For non-collinear right turns (cross product < 0), simply remove the bad point
                top = stack.pop();
            }
            
            // Push our current best 'top' candidate back onto the stack
            stack.push(top);
            
            // Now decide whether to add the current point to the hull
            if (stack.size() < 2 || 
                // Add if it makes a non-collinear turn with last two points
                crossProduct(stack.get(stack.size() - 2), stack.peek(), pointsArray[i]) != 0) {
                stack.push(pointsArray[i]);
            } 
            // Special collinear case - only add if it's further than current last point
            else if (distanceSquared(stack.get(stack.size() - 2), pointsArray[i]) > 
                    distanceSquared(stack.get(stack.size() - 2), stack.peek())) {
                // Replace last point with current point if it's further
                stack.pop();
                stack.push(pointsArray[i]);
            }
        }
        
        return new ArrayList<>(stack);
    }

    /**
     * Sort points by polar angle, and by distance for collinear points
     */
    private void sortByPolarAngle(Point2D[] points, Point2D start) {
        Arrays.sort(points, (p1, p2) -> {
            if (p1 == start) {
                return -1;
            }
            if (p2 == start) {
                return 1;
            }
            
            double cp = crossProduct(start, p1, p2);
            if (cp == 0) {
                // For collinear points, sort by distance from start
                return Double.compare(distanceSquared(start, p1), distanceSquared(start, p2));
            } else {
                // Sort by polar angle (counter-clockwise)
                return -Double.compare(cp, 0);
            }
        });
    }

    /**
     * Calculate the squared distance between two points
     */
    private double distanceSquared(Point2D a, Point2D b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        return dx * dx + dy * dy;
    }

    /**
     * Cross product to determine orientation of three points.
     * If result > 0: counter-clockwise turn
     * If result = 0: collinear
     * If result < 0: clockwise turn
     */
    private double crossProduct(Point2D a, Point2D b, Point2D c) {
        double y1 = a.getY() - b.getY();
        double y2 = a.getY() - c.getY();
        double x1 = a.getX() - b.getX();
        double x2 = a.getX() - c.getX();
        return y2 * x1 - y1 * x2;
    }
} 