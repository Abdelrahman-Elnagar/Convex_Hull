package com.myapp.hull.algorithms;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.myapp.hull.Point2D;

public class MergeHullFX implements ConvexHullAlgorithm {
    static class Point {
        int x;
        int y;
        
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
        @Override
        public String toString() {
            return "(" + x + "," + y + ")";
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Point point = (Point) obj;
            return x == point.x && y == point.y;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }
    @Override
    public List<Point2D> computeHull(List<Point2D> pointList) {
        // Convert List<Point2D> to Point[]
        Point[] points = new Point[pointList.size()];
        for (int i = 0; i < pointList.size(); i++) {
            Point2D p = pointList.get(i);
            points[i] = new Point((int) p.getX(), (int) p.getY());
        }
        
        // Handle base cases
        if (points == null || points.length == 0) {
            return new ArrayList<>();
        }
        
        // Remove duplicate points
        Set<Point> uniquePointsSet = new LinkedHashSet<>(Arrays.asList(points));
        points = uniquePointsSet.toArray(new Point[0]);
        
        // Special case for single point
        if (points.length == 1) {
            List<Point2D> result = new ArrayList<>();
            for (Point point : points) {
                result.add(new Point2D(point.x, point.y));
            }
            return result;
        }
        
        // Check if all points are collinear
        if (areAllCollinear(points)) {
            Point[] extremePoints = findExtremePoints(points);
            List<Point2D> result = new ArrayList<>();
            for (Point point : extremePoints) {
                result.add(new Point2D(point.x, point.y));
            }
            return result;
        }
        
        if (points.length <= 3) {
            List<Point> smallCaseHull = handleSmallCases(points);
            List<Point2D> result = new ArrayList<>();
            for (Point point : smallCaseHull) {
                result.add(new Point2D(point.x, point.y));
            }
            return result;
        }
        
        // Sort by x-coordinate
        Arrays.sort(points, (a, b) -> {
            int xCompare = Integer.compare(a.x, b.x);
            return xCompare != 0 ? xCompare : Integer.compare(a.y, b.y);
        });
        
        // Apply divide-and-conquer algorithm
        List<Point> hull = divideAndConquerHull(points);
        
        // Ensure the points are in the expected order (start with bottom-left point)
        arrangeHullStartingFromBottomLeft(hull);
        
        List<Point2D> result = new ArrayList<>();
        for (Point point : hull) {
            result.add(new Point2D(point.x, point.y));
        }
        return result;
    }
    
    private boolean areAllCollinear(Point[] points) {
        if (points.length <= 2) return true;
        
        Point p1 = points[0];
        Point p2 = points[1];
        
        for (int i = 2; i < points.length; i++) {
            if (!isCollinear(p1, p2, points[i])) {
                return false;
            }
        }
        return true;
    }
    
    private void arrangeHullStartingFromBottomLeft(List<Point> hull) {
        if (hull.size() <= 2) return;
        
        // Find the bottom-left point
        int bottomLeftIdx = 0;
        for (int i = 1; i < hull.size(); i++) {
            Point current = hull.get(i);
            Point bottomLeft = hull.get(bottomLeftIdx);
            
            if (current.y < bottomLeft.y || (current.y == bottomLeft.y && current.x < bottomLeft.x)) {
                bottomLeftIdx = i;
            }
        }
        
        // Rotate the list to start with the bottom-left point
        if (bottomLeftIdx != 0) {
            List<Point> rotated = new ArrayList<>();
            for (int i = 0; i < hull.size(); i++) {
                rotated.add(hull.get((i + bottomLeftIdx) % hull.size()));
            }
            hull.clear();
            hull.addAll(rotated);
        }
    }
    
    private List<Point> divideAndConquerHull(Point[] points) {
        // Base case
        if (points.length <= 3) {
            return handleSmallCases(points);
        }
        
        // Step 1: Divide the set into two halves
        int mid = points.length / 2;
        Point[] leftSet = Arrays.copyOfRange(points, 0, mid);
        Point[] rightSet = Arrays.copyOfRange(points, mid, points.length);
        
        // Step 2: Recursively find convex hulls of the two sets
        List<Point> leftHull = divideAndConquerHull(leftSet);
        List<Point> rightHull = divideAndConquerHull(rightSet);
        
        // Step 3: Merge the two convex hulls
        return mergeHulls(leftHull, rightHull);
    }
    
    private List<Point> handleSmallCases(Point[] points) {
        if (points.length <= 1) {
            return new ArrayList<>(Arrays.asList(points));
        }
        
        if (points.length == 2) {
            return new ArrayList<>(Arrays.asList(points));
        }
        
        // Check if all points are collinear
        boolean allCollinear = true;
        if (points.length >= 3) {
            Point p1 = points[0];
            Point p2 = points[1];
            
            for (int i = 2; i < points.length; i++) {
                if (!isCollinear(p1, p2, points[i])) {
                    allCollinear = false;
                    break;
                }
            }
        }
        
        if (allCollinear) {
            // Find the two extreme points for collinear sets
            Point[] extremePoints = findExtremePoints(points);
            return new ArrayList<>(Arrays.asList(extremePoints));
        } else if (points.length == 3) {
            // All three points form a triangle (convex hull)
            List<Point> hull = new ArrayList<>(Arrays.asList(points));
            // Ensure counterclockwise order
            arrangeCounterClockwise(hull);
            return hull;
        }
        
        return new ArrayList<>();
    }
    
    private boolean isCollinear(Point a, Point b, Point c) {
        // Check if three points lie on the same line
        // Use long to prevent overflow when points have large coordinates
        long cross = (long)(b.y - a.y) * (c.x - b.x) - (long)(c.y - b.y) * (b.x - a.x);
        return cross == 0;
    }
    
    private Point[] findExtremePoints(Point[] points) {
        // For single point, return it as both extreme points
        if (points.length == 1) {
            return new Point[]{points[0], points[0]};
        }
        
        // For collinear points, we need to find the two most distant points
        // First, determine if points are on vertical or horizontal line or diagonal
        boolean horizontalOrVerticalLine = true;
        int dx = points[1].x - points[0].x;
        int dy = points[1].y - points[0].y;
        
        // Find the two most distant points
        double maxDistance = 0;
        Point p1 = null, p2 = null;
        
        for (int i = 0; i < points.length; i++) {
            for (int j = i + 1; j < points.length; j++) {
                Point a = points[i];
                Point b = points[j];
                double distance = Math.sqrt(Math.pow(b.x - a.x, 2) + Math.pow(b.y - a.y, 2));
                
                if (distance > maxDistance) {
                    maxDistance = distance;
                    p1 = a;
                    p2 = b;
                }
            }
        }
        
        return new Point[]{p1, p2};
    }
    
    private List<Point> mergeHulls(List<Point> leftHull, List<Point> rightHull) {
        // If either hull is empty, return the other
        if (leftHull.isEmpty()) return rightHull;
        if (rightHull.isEmpty()) return leftHull;
        
        // Handle special case where both hulls are lines
        if (leftHull.size() == 2 && rightHull.size() == 2) {
            Point[] allPoints = {leftHull.get(0), leftHull.get(1), rightHull.get(0), rightHull.get(1)};
            if (areAllCollinear(allPoints)) {
                Point[] extremePoints = findExtremePoints(allPoints);
                return new ArrayList<>(Arrays.asList(extremePoints));
            }
        }
        
        // Find the upper and lower tangent lines between the two hulls
        int[] upperTangent = findUpperTangent(leftHull, rightHull);
        int[] lowerTangent = findLowerTangent(leftHull, rightHull);
        
        // Build the merged hull
        List<Point> mergedHull = new ArrayList<>();
        
        // Add points from left hull (clockwise from lower to upper tangent)
        int leftLower = lowerTangent[0];
        int leftUpper = upperTangent[0];
        
        int i = leftLower;
        do {
            mergedHull.add(leftHull.get(i));
            i = (i + 1) % leftHull.size();
        } while (i != (leftUpper + 1) % leftHull.size());
        
        // Add points from right hull (clockwise from upper to lower tangent)
        int rightUpper = upperTangent[1];
        int rightLower = lowerTangent[1];
        
        i = rightUpper;
        do {
            mergedHull.add(rightHull.get(i));
            i = (i + 1) % rightHull.size();
        } while (i != (rightLower + 1) % rightHull.size());
        
        // Remove any collinear points on the hull
        removeCollinearPoints(mergedHull);
        
        return mergedHull;
    }
    
    private void removeCollinearPoints(List<Point> hull) {
        if (hull.size() <= 2) return;
        
        List<Point> result = new ArrayList<>();
        int n = hull.size();
        
        for (int i = 0; i < n; i++) {
            int prev = (i - 1 + n) % n;
            int next = (i + 1) % n;
            
            Point p1 = hull.get(prev);
            Point p2 = hull.get(i);
            Point p3 = hull.get(next);
            
            // Check for collinearity with a small epsilon for floating point issues
            if (!isCollinear(p1, p2, p3)) {
                result.add(p2);
            } else {
                // Keep the middle point if it's collinear but is an extreme point in the sequence
                double dist1 = Math.sqrt(Math.pow(p3.x - p1.x, 2) + Math.pow(p3.y - p1.y, 2));
                double dist2 = Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2)) 
                             + Math.sqrt(Math.pow(p3.x - p2.x, 2) + Math.pow(p3.y - p2.y, 2));
                
                // If p2 is not between p1 and p3 (within some epsilon)
                if (Math.abs(dist1 - dist2) > 1e-9) {
                    result.add(p2);
                }
            }
        }
        
        hull.clear();
        hull.addAll(result);
    }
    
    private int[] findUpperTangent(List<Point> leftHull, List<Point> rightHull) {
        int leftSize = leftHull.size();
        int rightSize = rightHull.size();
        
        // Find rightmost point of left hull
        int leftIndex = 0;
        for (int i = 1; i < leftSize; i++) {
            Point current = leftHull.get(i);
            Point rightmost = leftHull.get(leftIndex);
            if (current.x > rightmost.x || (current.x == rightmost.x && current.y > rightmost.y)) {
                leftIndex = i;
            }
        }
        
        // Find leftmost point of right hull
        int rightIndex = 0;
        for (int i = 1; i < rightSize; i++) {
            Point current = rightHull.get(i);
            Point leftmost = rightHull.get(rightIndex);
            if (current.x < leftmost.x || (current.x == leftmost.x && current.y > leftmost.y)) {
                rightIndex = i;
            }
        }
        
        // Initialize upper tangent indices
        int upperLeftIndex = leftIndex;
        int upperRightIndex = rightIndex;
        
        boolean done = false;
        while (!done) {
            done = true;
            
            // Move right hull index
            while (true) {
                int nextRightIndex = (upperRightIndex + 1) % rightSize;
                Point currentLeft = leftHull.get(upperLeftIndex);
                Point currentRight = rightHull.get(upperRightIndex);
                Point nextRight = rightHull.get(nextRightIndex);
                
                int orientation = orientation(currentLeft, currentRight, nextRight);
                if (orientation > 0) {
                    upperRightIndex = nextRightIndex;
                    done = false;
                } else {
                    break;
                }
            }
            
            // Move left hull index
            while (true) {
                int nextLeftIndex = (upperLeftIndex - 1 + leftSize) % leftSize;
                Point nextLeft = leftHull.get(nextLeftIndex);
                Point currentLeft = leftHull.get(upperLeftIndex);
                Point currentRight = rightHull.get(upperRightIndex);
                
                int orientation = orientation(nextLeft, currentLeft, currentRight);
                if (orientation > 0) {
                    upperLeftIndex = nextLeftIndex;
                    done = false;
                } else {
                    break;
                }
            }
        }
        
        return new int[]{upperLeftIndex, upperRightIndex};
    }
    
    private int[] findLowerTangent(List<Point> leftHull, List<Point> rightHull) {
        int leftSize = leftHull.size();
        int rightSize = rightHull.size();
        
        // Find rightmost point of left hull
        int leftIndex = 0;
        for (int i = 1; i < leftSize; i++) {
            Point current = leftHull.get(i);
            Point rightmost = leftHull.get(leftIndex);
            if (current.x > rightmost.x || (current.x == rightmost.x && current.y < rightmost.y)) {
                leftIndex = i;
            }
        }
        
        // Find leftmost point of right hull
        int rightIndex = 0;
        for (int i = 1; i < rightSize; i++) {
            Point current = rightHull.get(i);
            Point leftmost = rightHull.get(rightIndex);
            if (current.x < leftmost.x || (current.x == leftmost.x && current.y < leftmost.y)) {
                rightIndex = i;
            }
        }
        
        // Initialize lower tangent indices
        int lowerLeftIndex = leftIndex;
        int lowerRightIndex = rightIndex;
        
        boolean done = false;
        while (!done) {
            done = true;
            
            // Move right hull index
            while (true) {
                int nextRightIndex = (lowerRightIndex - 1 + rightSize) % rightSize;
                Point currentLeft = leftHull.get(lowerLeftIndex);
                Point currentRight = rightHull.get(lowerRightIndex);
                Point nextRight = rightHull.get(nextRightIndex);
                
                int orientation = orientation(currentLeft, currentRight, nextRight);
                if (orientation < 0) {
                    lowerRightIndex = nextRightIndex;
                    done = false;
                } else {
                    break;
                }
            }
            
            // Move left hull index
            while (true) {
                int nextLeftIndex = (lowerLeftIndex + 1) % leftSize;
                Point nextLeft = leftHull.get(nextLeftIndex);
                Point currentLeft = leftHull.get(lowerLeftIndex);
                Point currentRight = rightHull.get(lowerRightIndex);
                
                int orientation = orientation(nextLeft, currentLeft, currentRight);
                if (orientation < 0) {
                    lowerLeftIndex = nextLeftIndex;
                    done = false;
                } else {
                    break;
                }
            }
        }
        
        return new int[]{lowerLeftIndex, lowerRightIndex};
    }
    
    private int orientation(Point p, Point q, Point r) {
        // Returns positive value for counterclockwise orientation,
        // negative for clockwise orientation, and 0 for collinear points
        long val = (long)(q.y - p.y) * (r.x - q.x) - (long)(q.x - p.x) * (r.y - q.y);
        if (val == 0) return 0;  // collinear
        return (val > 0) ? 1 : -1; // counterclockwise or clockwise
    }
    
    private boolean isCounterClockwise(List<Point> points) {
        if (points.size() < 3) return true;
        
        // Calculate the cross product
        int crossProduct = 0;
        for (int i = 0; i < points.size(); i++) {
            Point current = points.get(i);
            Point next = points.get((i + 1) % points.size());
            crossProduct += (current.x * next.y - current.y * next.x);
        }
        
        return crossProduct > 0; // Positive cross product means counterclockwise
    }
    
    private void arrangeCounterClockwise(List<Point> points) {
        if (points.size() < 3) return;
        
        // Find the center point
        int sumX = 0, sumY = 0;
        for (Point p : points) {
            sumX += p.x;
            sumY += p.y;
        }
        final double centerX = sumX / (double) points.size();
        final double centerY = sumY / (double) points.size();
        
        // Sort points counterclockwise
        Collections.sort(points, (p1, p2) -> {
            double angle1 = Math.atan2(p1.y - centerY, p1.x - centerX);
            double angle2 = Math.atan2(p2.y - centerY, p2.x - centerX);
            return Double.compare(angle1, angle2);
        });
    }
    
    public static Point[] createPoints(int[][] coords) {
        Point[] points = new Point[coords.length];
        for (int i = 0; i < coords.length; i++) {
            points[i] = new Point(coords[i][0], coords[i][1]);
        }
        return points;
    }
    
    public static void main(String[] args) {
        
    }
}