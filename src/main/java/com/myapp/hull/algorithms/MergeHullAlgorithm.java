package com.myapp.hull.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
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
        // Clear previous computation steps for fresh computation (for animation)
        computationSteps.clear();

        // Remove duplicate points 
        Set<Point2D> uniquePointsSet = new LinkedHashSet<>(points);
        Point2D[] pointsArray = uniquePointsSet.toArray(new Point2D[0]);

        // Handle base cases : null and 2 points or less
        if (points == null || points.isEmpty()){
            return new ArrayList<>();
        }
           
        // Sort by x-coordinate and then by y-coordinate in case of ties
        Arrays.sort(pointsArray, (a, b) -> Double.compare(a.getX(), b.getX()) != 0 ? 
            Double.compare(a.getX(), b.getX()) : Double.compare(a.getY(), b.getY())
        );
      
        // Record initial sorted points
        computationSteps.add(new ArrayList<>(Arrays.asList(pointsArray)));
        
        // Apply divide-and-conquer algorithm
        List<Point2D> hull = divideAndConquerHull(pointsArray);
        
        // Record final hull
        computationSteps.add(new ArrayList<>(hull));
        
        return hull;
    }
    
    private List<Point2D> divideAndConquerHull(Point2D[] points) {
        // Handle base cases : null and 2 points or less
        if (points.length <= 2) {
            computationSteps.add(new ArrayList<>(Arrays.asList(points)));
            return new ArrayList<>(Arrays.asList(points));
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
    
    
    private boolean isCollinear(Point2D a, Point2D b, Point2D c) {
       return orientation(a, b, c) == 0;
    }
    
    private List<Point2D> mergeHulls(List<Point2D> leftHull, List<Point2D> rightHull) {
        // If either hull is empty, return the other
        if (leftHull.isEmpty()) return rightHull;
        if (rightHull.isEmpty()) return leftHull;
        
        // Find the upper and lower tangent lines between the two hulls
        int[] upperTangent = findUpperTangent(leftHull, rightHull);
        int[] lowerTangent = findLowerTangent(leftHull, rightHull);
        
        // Build the merged hull
        List<Point2D> mergedHull = new ArrayList<>();
        
        // Add points from left hull (clockwise from lower tanged to upper tangent
        // and discard the rest of the points as they do not belong in the new hull)
        int leftLower = lowerTangent[0];
        int leftUpper = upperTangent[0];
    
       int i = leftLower;
        while (i != leftUpper) {
            mergedHull.add(leftHull.get(i));
            i = (i + 1) % leftHull.size();
        }
        mergedHull.add(leftHull.get(leftUpper));

        // Add points from right hull (clockwise from upper to lower tangent
        // and discard the rest of the points as they do not belong in the new hull)
        int rightUpper = upperTangent[1];
        int rightLower = lowerTangent[1];
        i = rightUpper;
        while (i != rightLower) {
            mergedHull.add(rightHull.get(i));
            i = (i + 1) % rightHull.size();
        }
        mergedHull.add(rightHull.get(rightLower));

        // Remove collinear points from the merged hull
        removeCollinearPoints(mergedHull);

        return mergedHull;
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
                // If two points are equally to the right, take the one higher
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
                // If two points are equally to the left, take the one higher
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
                // Check if we have looped back to the starting point
                if(nextRightIndex==rightIndex) {
                    break;
                }
                Point2D currentLeft = leftHull.get(upperLeftIndex);
                Point2D currentRight = rightHull.get(upperRightIndex);
                // Get the candidate next right point
                Point2D nextRight = rightHull.get(nextRightIndex);
                
                int orientation = orientation(currentLeft, currentRight, nextRight);
                
                if (orientation <= 0) {
                    upperRightIndex = nextRightIndex;
                    done = false;
                } else {
                    break;
                }
            }
            
            // Move left hull index
            while (true) {
                int nextLeftIndex = (upperLeftIndex - 1 + leftSize) % leftSize;
                // Check if we have looped back to the starting point
                if(nextLeftIndex==leftIndex) {
                    break;
                }
                Point2D currentLeft = leftHull.get(upperLeftIndex);
                Point2D currentRight = rightHull.get(upperRightIndex);
                // Get the candidate next left point
                Point2D nextLeft = leftHull.get(nextLeftIndex);
                
                int orientation = orientation(nextLeft, currentLeft, currentRight);
                if (orientation <= 0) {
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
                // If two points are equally to the right, take the one lower
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
                // If two points are equally to the left, take the one lower
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
                // Check if we have looped back to the starting point
                if(nextRightIndex==rightIndex) {
                    break;
                }
                Point2D currentLeft = leftHull.get(lowerLeftIndex);
                Point2D currentRight = rightHull.get(lowerRightIndex);
                // Get the candidate next right point
                Point2D nextRight = rightHull.get(nextRightIndex);
                
                int orientation = orientation(currentLeft, currentRight, nextRight);
                if (orientation >= 0) {
                    lowerRightIndex = nextRightIndex;
                    done = false;
                } else {
                    break;
                }
            }

            // Move left hull index
            while (true) {
                int nextLeftIndex = (lowerLeftIndex + 1) % leftSize;
                // Check if we have looped back to the starting point
                if(nextLeftIndex==leftIndex) {
                    break;
                }     
                Point2D currentLeft = leftHull.get(lowerLeftIndex);
                Point2D currentRight = rightHull.get(lowerRightIndex);
                // Get the candidate next left point
                Point2D nextLeft = leftHull.get(nextLeftIndex);

                int orientation = orientation(nextLeft, currentLeft, currentRight);
                if (orientation >= 0) {
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
 
    public List<List<Point2D>> getComputationSteps() {
        return new ArrayList<>(computationSteps);
    }
} 