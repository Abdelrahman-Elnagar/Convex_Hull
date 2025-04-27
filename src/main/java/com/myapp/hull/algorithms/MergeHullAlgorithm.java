package com.myapp.hull.algorithms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

import com.myapp.hull.Point2D;

/**
 * Implementation of the Merge Hull algorithm using divide-and-conquer.
 * Time complexity: O(n log n)
 */
public class MergeHullAlgorithm implements ConvexHullAlgorithm {
    @Override
    public List<Point2D> computeHull(List<Point2D> points) {
        if (points == null || points.size() < 3) {
            return new ArrayList<>(points);
        }

        // Sort points by x-coordinate
        List<Point2D> sortedPoints = new ArrayList<>(points);
        sortedPoints.sort(Comparator.comparingDouble(Point2D::getX));

        return mergeHull(sortedPoints, 0, sortedPoints.size());
    }

    private List<Point2D> mergeHull(List<Point2D> points, int beg, int end) {
        if (end - beg < 4) {
            List<Point2D> convHull = new ArrayList<>();
            for (int i = beg; i < end; i++) {
                convHull.add(points.get(i));
            }
            if (convHull.size() == 3 && getRotation(convHull.get(0), convHull.get(1), convHull.get(2)) == Rotation.CW) {
                rotateTriangleCCW(convHull);
            }
            return convHull;
        }

        // Divide the points into two halves
        int mid = (end + beg) / 2;
        List<Point2D> leftHull = mergeHull(points, beg, mid);
        List<Point2D> rightHull = mergeHull(points, mid, end);

        // Merge the hulls
        List<Point2D> mergedHull = mergeHulls(leftHull, rightHull);
        
        // Keep merging until we get a stable convex hull
        while (true) {
            List<Point2D> newHull = grahamScan(mergedHull);
            if (newHull.size() == mergedHull.size() && 
                areHullsEqual(newHull, mergedHull)) {
                break;
            }
            mergedHull = newHull;
        }

        return mergedHull;
    }

    private List<Point2D> mergeHulls(List<Point2D> left, List<Point2D> right) {
        // Find the rightmost point of left hull
        int rightmostLeft = 0;
        for (int i = 1; i < left.size(); i++) {
            if (left.get(i).getX() > left.get(rightmostLeft).getX()) {
                rightmostLeft = i;
            }
        }

        // Find the leftmost point of right hull
        int leftmostRight = 0;
        for (int i = 1; i < right.size(); i++) {
            if (right.get(i).getX() < right.get(leftmostRight).getX()) {
                leftmostRight = i;
            }
        }

        // Find upper tangent
        int upperLeft = rightmostLeft;
        int upperRight = leftmostRight;
        boolean done = false;
        
        while (!done) {
            done = true;
            
            // Move right point clockwise
            while (getRotation(right.get(upperRight), left.get(upperLeft), 
                             right.get((upperRight + 1) % right.size())) == Rotation.CW) {
                upperRight = (upperRight + 1) % right.size();
            }
            
            // Move left point counter-clockwise
            while (getRotation(left.get(upperLeft), right.get(upperRight), 
                             left.get((upperLeft - 1 + left.size()) % left.size())) == Rotation.CCW) {
                upperLeft = (upperLeft - 1 + left.size()) % left.size();
                done = false;
            }
        }

        // Find lower tangent
        int lowerLeft = rightmostLeft;
        int lowerRight = leftmostRight;
        done = false;
        
        while (!done) {
            done = true;
            
            // Move right point counter-clockwise
            while (getRotation(right.get(lowerRight), left.get(lowerLeft), 
                             right.get((lowerRight - 1 + right.size()) % right.size())) == Rotation.CCW) {
                lowerRight = (lowerRight - 1 + right.size()) % right.size();
            }
            
            // Move left point clockwise
            while (getRotation(left.get(lowerLeft), right.get(lowerRight), 
                             left.get((lowerLeft + 1) % left.size())) == Rotation.CW) {
                lowerLeft = (lowerLeft + 1) % left.size();
                done = false;
            }
        }

        // Construct merged hull
        List<Point2D> merged = new ArrayList<>();
        
        // Add points from left hull up to upperLeft
        int current = upperLeft;
        while (current != lowerLeft) {
            merged.add(left.get(current));
            current = (current + 1) % left.size();
        }
        merged.add(left.get(lowerLeft));
        
        // Add points from right hull up to upperRight
        current = lowerRight;
        while (current != upperRight) {
            merged.add(right.get(current));
            current = (current + 1) % right.size();
        }
        merged.add(right.get(upperRight));
        
        return merged;
    }

    private boolean areHullsEqual(List<Point2D> hull1, List<Point2D> hull2) {
        if (hull1.size() != hull2.size()) {
            return false;
        }
        
        // Check if all points in hull1 are in hull2
        for (Point2D p : hull1) {
            boolean found = false;
            for (Point2D q : hull2) {
                if (p.equals(q)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private void rotateTriangleCCW(List<Point2D> convHull) {
        Point2D temp = convHull.get(1);
        convHull.set(1, convHull.get(2));
        convHull.set(2, temp);
    }

    private List<Point2D> grahamScan(List<Point2D> points) {
        if (points.size() <= 3) {
            return new ArrayList<>(points);
        }

        // Find pivot (lowest y, leftmost if tie)
        final Point2D pivot = findPivotPoint(points);

        // Sort by polar angle
        List<Point2D> sorted = new ArrayList<>(points);
        sorted.sort((p1, p2) -> {
            if (p1.equals(pivot)) return -1;
            if (p2.equals(pivot)) return 1;
            
            Rotation rot = getRotation(pivot, p1, p2);
            if (rot == Rotation.COLLINEAR) {
                return Double.compare(distanceSquared(pivot, p1), distanceSquared(pivot, p2));
            }
            return rot == Rotation.CCW ? -1 : 1;
        });

        // Build hull
        Stack<Point2D> hull = new Stack<>();
        hull.push(sorted.get(0));
        hull.push(sorted.get(1));

        for (int i = 2; i < sorted.size(); i++) {
            Point2D top = hull.pop();
            while (!hull.isEmpty() && getRotation(hull.peek(), top, sorted.get(i)) != Rotation.CCW) {
                top = hull.pop();
            }
            hull.push(top);
            hull.push(sorted.get(i));
        }

        return new ArrayList<>(hull);
    }

    private Point2D findPivotPoint(List<Point2D> points) {
        Point2D pivot = points.get(0);
        for (Point2D p : points) {
            if (p.getY() < pivot.getY() || 
                (p.getY() == pivot.getY() && p.getX() < pivot.getX())) {
                pivot = p;
            }
        }
        return pivot;
    }

    private enum Rotation {
        CW, CCW, COLLINEAR
    }

    private Rotation getRotation(Point2D a, Point2D b, Point2D c) {
        double cross = (b.getX() - a.getX()) * (c.getY() - a.getY()) - 
                      (b.getY() - a.getY()) * (c.getX() - a.getX());
        
        if (Math.abs(cross) < 1e-10) {
            return Rotation.COLLINEAR;
        }
        return cross > 0 ? Rotation.CCW : Rotation.CW;
    }

    private double distanceSquared(Point2D a, Point2D b) {
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        return dx * dx + dy * dy;
    }
} 