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
        // start by stack to store the points
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
            
            // Push our current best 'top' candidate back onto the stack that we took to test on the last three points
            stack.push(top);
            
            // Now decide whether to add the current point pointsArray[i] to the hull because we checked the case of collinearity with the prev two points but really didn't act yet 
            if (stack.size() < 2 || 
                // Add if it makes a non-collinear turn with last two points
                crossProduct(stack.get(stack.size() - 2), stack.peek(), pointsArray[i]) != 0) {
                stack.push(pointsArray[i]);
            } 
            // Special collinear case - only add if it's further than current last point
            else if (distanceSquared(stack.get(stack.size() - 2), pointsArray[i]) > 
                    distanceSquared(stack.get(stack.size() - 2), stack.peek())) {
                // Replace last point with current point if it's further
                stack.pop(); // remove the cuurent and add it , if not the furthest then do nothing
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
                return Double.compare(distanceSquared(start, p1), distanceSquared(start, p2)); //+ve if first greater, -ve othewise
            } else {
                // Sort by polar angle (counter-clockwise) why the negative because cp returened the +ve for counter clockwise so we reverise it so sort function put it in the begging 
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
    // ((qx-px)*(ry-py)-(qy-py)*(rx-px))
    // the result is 0, the points are collinear;
    // if it is positive, the three points constitute a "left turn" or counter-clockwise orientation,
    // otherwise a "right turn" or clockwise orientation (for counter-clockwise numbered points).
    private double crossProduct(Point2D p, Point2D q, Point2D r) {
        return (q.getX() - p.getX()) * (r.getY() - p.getY())
             - (q.getY() - p.getY()) * (r.getX() - p.getX());
    }
} 