package com.myapp.hull.algorithms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.myapp.hull.Point2D;

/**
 * Implementation of Jarvis March (Gift Wrapping) algorithm for computing the convex hull.
 * Time complexity: O(nh) where n is the number of points and h is the number of points in the hull.
 * This implementation correctly handles collinear points.
 */
public class JarvisMarch implements ConvexHullAlgorithm {
    @Override
    public List<Point2D> computeHull(List<Point2D> points) {
        if (points == null || points.size() < 2) {
            return new ArrayList<>(points);
        }

        // Convert List<Point2D> to Point2D[]
        Point2D[] pointsArray = points.toArray(new Point2D[0]);

        // Find leftmost point to start the march
        Point2D start = pointsArray[0];
        for (int i = 1; i < pointsArray.length; i++) {
            if (pointsArray[i].getX() < start.getX()) {
                start = pointsArray[i];
            }
        }

        Point2D current = start;
        // Use set because this algorithm might try to insert duplicate points
        Set<Point2D> result = new HashSet<>();
        result.add(start);
        List<Point2D> collinearPoints = new ArrayList<>();

        while (true) {
            Point2D nextTarget = pointsArray[0];
            for (int i = 1; i < pointsArray.length; i++) {
                if (pointsArray[i] == current) {
                    continue;
                }
                double val = crossProduct(current, nextTarget, pointsArray[i]);
                // If val > 0 it means points[i] is on left of current -> nextTarget. Make it the nextTarget.
                if (val > 0) {
                    nextTarget = pointsArray[i];
                    // Reset collinear points because we now have a new nextTarget
                    collinearPoints = new ArrayList<>();
                } else if (val == 0) { // If val is 0 then current, nextTarget and points[i] are collinear
                    // If it's collinear point then pick the further one but add closer one to list of collinear points
                    if (distance(current, nextTarget, pointsArray[i]) < 0) {
                        collinearPoints.add(nextTarget);
                        nextTarget = pointsArray[i];
                    } else { // Just add points[i] to collinearPoints list
                        collinearPoints.add(pointsArray[i]);
                    }
                }
                // Else if val < 0 then nothing to do since points[i] is on right side of current -> nextTarget
            }

            // Add all points in collinearPoints to result
            for (Point2D p : collinearPoints) {
                result.add(p);
            }
            // If nextTarget is same as start it means we have formed an envelope and it's done
            if (nextTarget == start) {
                break;
            }
            // Add nextTarget to result and set current to nextTarget
            result.add(nextTarget);
            current = nextTarget;
        }

        return new ArrayList<>(result);
    }

    /**
     * Returns < 0 if 'b' is closer to 'a' compared to 'c', == 0 if 'b' and 'c' are same distance from 'a'
     * or > 0 if 'c' is closer to 'a' compared to 'b'.
     */
    private double distance(Point2D a, Point2D b, Point2D c) {
        double y1 = a.getY() - b.getY();
        double y2 = a.getY() - c.getY();
        double x1 = a.getX() - b.getX();
        double x2 = a.getX() - c.getX();
        return Double.compare(y1 * y1 + x1 * x1, y2 * y2 + x2 * x2);
    }

    /**
     * Cross product to find where c belongs in reference to vector ab.
     * If result > 0 it means 'c' is on left of ab
     *    result == 0 it means 'a','b' and 'c' are collinear
     *    result < 0  it means 'c' is on right of ab
     */
    private double crossProduct(Point2D a, Point2D b, Point2D c) {
        double y1 = a.getY() - b.getY();
        double y2 = a.getY() - c.getY();
        double x1 = a.getX() - b.getX();
        double x2 = a.getX() - c.getX();
        return y2 * x1 - y1 * x2;
    }

    // Utility method to create points from coordinate pairs
    public static Point2D[] createPoints(int[][] coords) {
        Point2D[] points = new Point2D[coords.length];
        for (int i = 0; i < coords.length; i++) {
            points[i] = new Point2D(coords[i][0], coords[i][1]);
        }
        return points;
    }
}
