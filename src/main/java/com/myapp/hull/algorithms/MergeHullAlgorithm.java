package com.myapp.hull.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.myapp.hull.Point2D;

/**
 * Implementation of the Merge Hull algorithm using divide-and-conquer.
 * Time complexity: O(n log n)
 */
public class MergeHullAlgorithm implements ConvexHullAlgorithm {
    private List<List<Point2D>> computationSteps = new ArrayList<>();
    
    @Override
    public List<Point2D> computeHull(List<Point2D> points) {
        // Clear previous computation steps
        computationSteps.clear();
        
        // Handle base cases
        if (points == null || points.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Remove duplicate points
        Set<Point2D> uniquePointsSet = new LinkedHashSet<>(points);
        Point2D[] pointsArray = uniquePointsSet.toArray(new Point2D[0]);
        
        // Special case for single point
        if (pointsArray.length == 1) {
            computationSteps.add(new ArrayList<>(Arrays.asList(pointsArray)));
            return new ArrayList<>(Arrays.asList(pointsArray));
        }
        
        // Check if all points are collinear
        if (areAllCollinear(pointsArray)) {
            Point2D[] extremePoints = findExtremePoints(pointsArray);
            computationSteps.add(new ArrayList<>(Arrays.asList(extremePoints)));
            return new ArrayList<>(Arrays.asList(extremePoints));
        }
        
        if (pointsArray.length <= 3) {
            List<Point2D> result = handleSmallCases(pointsArray);
            computationSteps.add(new ArrayList<>(result));
            return result;
        }
        
        // Sort by x-coordinate
        Arrays.sort(pointsArray, (a, b) -> {
            int xCompare = Double.compare(a.getX(), b.getX());
            return xCompare != 0 ? xCompare : Double.compare(a.getY(), b.getY());
        });
        
        // Record initial sorted points
        computationSteps.add(new ArrayList<>(Arrays.asList(pointsArray)));
        
        // Apply divide-and-conquer algorithm
        List<Point2D> hull = divideAndConquerHull(pointsArray);
        
        // Ensure the points are in the expected order (start with bottom-left point)
        arrangeHullStartingFromBottomLeft(hull);
        
        // Record final hull
        computationSteps.add(new ArrayList<>(hull));
        
        return hull;
    }
    
    private boolean areAllCollinear(Point2D[] points) {
        if (points.length <= 2) return true;
        
        Point2D p1 = points[0];
        Point2D p2 = points[1];
        
        for (int i = 2; i < points.length; i++) {
            if (!isCollinear(p1, p2, points[i])) {
                return false;
            }
        }
        return true;
    }
    
    private void arrangeHullStartingFromBottomLeft(List<Point2D> hull) {
        if (hull.size() <= 2) return;
        
        // Find the bottom-left point
        int bottomLeftIdx = 0;
        for (int i = 1; i < hull.size(); i++) {
            Point2D current = hull.get(i);
            Point2D bottomLeft = hull.get(bottomLeftIdx);
            
            if (current.getY() < bottomLeft.getY() || 
                (current.getY() == bottomLeft.getY() && current.getX() < bottomLeft.getX())) {
                bottomLeftIdx = i;
            }
        }
        
        // Rotate the list to start with the bottom-left point
        if (bottomLeftIdx != 0) {
            List<Point2D> rotated = new ArrayList<>();
            for (int i = 0; i < hull.size(); i++) {
                rotated.add(hull.get((i + bottomLeftIdx) % hull.size()));
            }
            hull.clear();
            hull.addAll(rotated);
        }
    }
    
    private List<Point2D> divideAndConquerHull(Point2D[] points) {
        // Base case
        if (points.length <= 3) {
            List<Point2D> result = handleSmallCases(points);
            computationSteps.add(new ArrayList<>(result));
            return result;
        }
        
        // Step 1: Divide the set into two halves
        int mid = points.length / 2;
        Point2D[] leftSet = Arrays.copyOfRange(points, 0, mid);
        Point2D[] rightSet = Arrays.copyOfRange(points, mid, points.length);
        
        // Record split visualization step
        List<Point2D> splitStep = new ArrayList<>();
        splitStep.addAll(Arrays.asList(leftSet));
        splitStep.add(new Point2D(Double.NaN, Double.NaN)); // separator
        splitStep.addAll(Arrays.asList(rightSet));
        computationSteps.add(splitStep);
        
        // Step 2: Recursively find convex hulls of the two sets
        List<Point2D> leftHull = divideAndConquerHull(leftSet);
        List<Point2D> rightHull = divideAndConquerHull(rightSet);
        
        // Record sub-hulls visualization step
        List<Point2D> subHullStep = new ArrayList<>();
        subHullStep.addAll(leftHull);
        subHullStep.add(new Point2D(Double.NaN, Double.NaN)); // separator
        subHullStep.addAll(rightHull);
        computationSteps.add(subHullStep);
        
        // Step 3: Merge the two convex hulls
        List<Point2D> merged = mergeHulls(leftHull, rightHull);
        
        // Record merged hull
        computationSteps.add(merged);
        
        return merged;
    }
    
    private List<Point2D> handleSmallCases(Point2D[] points) {
        if (points.length <= 1) {
            return new ArrayList<>(Arrays.asList(points));
        }
        
        if (points.length == 2) {
            return new ArrayList<>(Arrays.asList(points));
        }
        
        // Check if all points are collinear
        boolean allCollinear = true;
        if (points.length >= 3) {
            Point2D p1 = points[0];
            Point2D p2 = points[1];
            
            for (int i = 2; i < points.length; i++) {
                if (!isCollinear(p1, p2, points[i])) {
                    allCollinear = false;
                break;
                }
            }
        }
        
        if (allCollinear) {
            // Find the two extreme points for collinear sets
            Point2D[] extremePoints = findExtremePoints(points);
            return new ArrayList<>(Arrays.asList(extremePoints));
        } else if (points.length == 3) {
            // All three points form a triangle (convex hull)
            List<Point2D> hull = new ArrayList<>(Arrays.asList(points));
            // Ensure counterclockwise order
            arrangeCounterClockwise(hull);
            return hull;
        }
        
        return new ArrayList<>();
    }
    
    private boolean isCollinear(Point2D a, Point2D b, Point2D c) {
        // Check if three points lie on the same line
        double cross = (b.getY() - a.getY()) * (c.getX() - b.getX()) - 
                      (c.getY() - b.getY()) * (b.getX() - a.getX());
        return Math.abs(cross) < 1e-10; // Using small epsilon for floating point comparison
    }
    
    private Point2D[] findExtremePoints(Point2D[] points) {
        // For single point, return it as both extreme points
        if (points.length == 1) {
            return new Point2D[]{points[0], points[0]};
        }
        
        // Find the two most distant points
        double maxDistance = 0;
        Point2D p1 = null, p2 = null;
        
        for (int i = 0; i < points.length; i++) {
            for (int j = i + 1; j < points.length; j++) {
                Point2D a = points[i];
                Point2D b = points[j];
                double distance = Math.sqrt(Math.pow(b.getX() - a.getX(), 2) + 
                                         Math.pow(b.getY() - a.getY(), 2));
                
                if (distance > maxDistance) {
                    maxDistance = distance;
                    p1 = a;
                    p2 = b;
                }
            }
        }
        
        return new Point2D[]{p1, p2};
    }
    
    private List<Point2D> mergeHulls(List<Point2D> leftHull, List<Point2D> rightHull) {
        // If either hull is empty, return the other
        if (leftHull.isEmpty()) return rightHull;
        if (rightHull.isEmpty()) return leftHull;
        
        // Handle special case where both hulls are lines
        if (leftHull.size() == 2 && rightHull.size() == 2) {
            Point2D[] allPoints = {leftHull.get(0), leftHull.get(1), 
                                 rightHull.get(0), rightHull.get(1)};
            if (areAllCollinear(allPoints)) {
                Point2D[] extremePoints = findExtremePoints(allPoints);
                return new ArrayList<>(Arrays.asList(extremePoints));
            }
        }
        
        // Find the upper and lower tangent lines between the two hulls
        int[] upperTangent = findUpperTangent(leftHull, rightHull);
        int[] lowerTangent = findLowerTangent(leftHull, rightHull);
        
        // Build the merged hull
        List<Point2D> mergedHull = new ArrayList<>();
        
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
    
    private void removeCollinearPoints(List<Point2D> hull) {
        if (hull.size() <= 2) return;
        
        List<Point2D> result = new ArrayList<>();
        int n = hull.size();
        
        for (int i = 0; i < n; i++) {
            int prev = (i - 1 + n) % n;
            int next = (i + 1) % n;
            
            Point2D p1 = hull.get(prev);
            Point2D p2 = hull.get(i);
            Point2D p3 = hull.get(next);
            
            if (!isCollinear(p1, p2, p3)) {
                result.add(p2);
            } else {
                // Keep the middle point if it's collinear but is an extreme point
                double dist1 = Math.sqrt(Math.pow(p3.getX() - p1.getX(), 2) + 
                                       Math.pow(p3.getY() - p1.getY(), 2));
                double dist2 = Math.sqrt(Math.pow(p2.getX() - p1.getX(), 2) + 
                                       Math.pow(p2.getY() - p1.getY(), 2)) +
                             Math.sqrt(Math.pow(p3.getX() - p2.getX(), 2) + 
                                      Math.pow(p3.getY() - p2.getY(), 2));
                
                if (Math.abs(dist1 - dist2) > 1e-9) {
                    result.add(p2);
                }
            }
        }
        
        hull.clear();
        hull.addAll(result);
    }
    
    private int[] findUpperTangent(List<Point2D> leftHull, List<Point2D> rightHull) {
        int leftSize = leftHull.size();
        int rightSize = rightHull.size();
        
        // Find rightmost point of left hull
        int leftIndex = 0;
        for (int i = 1; i < leftSize; i++) {
            Point2D current = leftHull.get(i);
            Point2D rightmost = leftHull.get(leftIndex);
            if (current.getX() > rightmost.getX() || 
                (current.getX() == rightmost.getX() && current.getY() > rightmost.getY())) {
                leftIndex = i;
            }
        }
        
        // Find leftmost point of right hull
        int rightIndex = 0;
        for (int i = 1; i < rightSize; i++) {
            Point2D current = rightHull.get(i);
            Point2D leftmost = rightHull.get(rightIndex);
            if (current.getX() < leftmost.getX() || 
                (current.getX() == leftmost.getX() && current.getY() > leftmost.getY())) {
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
                Point2D currentLeft = leftHull.get(upperLeftIndex);
                Point2D currentRight = rightHull.get(upperRightIndex);
                Point2D nextRight = rightHull.get(nextRightIndex);
                
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
                Point2D nextLeft = leftHull.get(nextLeftIndex);
                Point2D currentLeft = leftHull.get(upperLeftIndex);
                Point2D currentRight = rightHull.get(upperRightIndex);
                
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
    
    private int[] findLowerTangent(List<Point2D> leftHull, List<Point2D> rightHull) {
        int leftSize = leftHull.size();
        int rightSize = rightHull.size();
        
        // Find rightmost point of left hull
        int leftIndex = 0;
        for (int i = 1; i < leftSize; i++) {
            Point2D current = leftHull.get(i);
            Point2D rightmost = leftHull.get(leftIndex);
            if (current.getX() > rightmost.getX() || 
                (current.getX() == rightmost.getX() && current.getY() < rightmost.getY())) {
                leftIndex = i;
            }
        }
        
        // Find leftmost point of right hull
        int rightIndex = 0;
        for (int i = 1; i < rightSize; i++) {
            Point2D current = rightHull.get(i);
            Point2D leftmost = rightHull.get(rightIndex);
            if (current.getX() < leftmost.getX() || 
                (current.getX() == leftmost.getX() && current.getY() < leftmost.getY())) {
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
                Point2D currentLeft = leftHull.get(lowerLeftIndex);
                Point2D currentRight = rightHull.get(lowerRightIndex);
                Point2D nextRight = rightHull.get(nextRightIndex);
                
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
                Point2D nextLeft = leftHull.get(nextLeftIndex);
                Point2D currentLeft = leftHull.get(lowerLeftIndex);
                Point2D currentRight = rightHull.get(lowerRightIndex);
                
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
    
    private int orientation(Point2D p, Point2D q, Point2D r) {
        // Returns positive value for counterclockwise orientation,
        // negative for clockwise orientation, and 0 for collinear points
        double val = (q.getY() - p.getY()) * (r.getX() - q.getX()) - 
                    (q.getX() - p.getX()) * (r.getY() - q.getY());
        if (Math.abs(val) < 1e-10) return 0;  // collinear
        return (val > 0) ? 1 : -1; // counterclockwise or clockwise
    }
    
    private boolean isCounterClockwise(List<Point2D> points) {
        if (points.size() < 3) return true;
        
        // Calculate the cross product
        double crossProduct = 0;
        for (int i = 0; i < points.size(); i++) {
            Point2D current = points.get(i);
            Point2D next = points.get((i + 1) % points.size());
            crossProduct += (current.getX() * next.getY() - current.getY() * next.getX());
        }
        
        return crossProduct > 0; // Positive cross product means counterclockwise
    }
    
    private void arrangeCounterClockwise(List<Point2D> points) {
        if (points.size() < 3) return;
        
        // Find the center point
        double sumX = 0, sumY = 0;
        for (Point2D p : points) {
            sumX += p.getX();
            sumY += p.getY();
        }
        final double centerX = sumX / points.size();
        final double centerY = sumY / points.size();
        
        // Sort points counterclockwise
        Collections.sort(points, (p1, p2) -> {
            double angle1 = Math.atan2(p1.getY() - centerY, p1.getX() - centerX);
            double angle2 = Math.atan2(p2.getY() - centerY, p2.getX() - centerX);
            return Double.compare(angle1, angle2);
        });
    }
    
    public List<List<Point2D>> getComputationSteps() {
        return new ArrayList<>(computationSteps);
    }
} 