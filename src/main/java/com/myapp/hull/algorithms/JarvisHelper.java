package com.myapp.hull.algorithms;

import java.util.List;

import com.myapp.hull.Point2D;

/**
 * Helper class for Jarvis March with binary search.
 * Used by Chan's Algorithm to find the next extreme point.
 */
class JarvisHelper {
    /**
     * Finds the next point in the convex hull using binary search.
     *
     * @param current The current point on the hull
     * @param next The next point in the current group
     * @param groups List of convex hulls (each hull is a list of points)
     * @return The next extreme point
     */
    static Point2D findNextPoint(Point2D current, Point2D next, List<List<Point2D>> groups) {
        Point2D candidate = next;
        
        for (List<Point2D> group : groups) {
            if (group.isEmpty()) continue;
            
            // Binary search for the tangent point
            int left = 0;
            int right = group.size();
            
            while (left < right) {
                int mid = (left + right) / 2;
                Point2D midPoint = group.get(mid);
                
                // Check if mid is the tangent point
                if (isTangent(current, midPoint, group, mid)) {
                    candidate = getBetterCandidate(current, candidate, midPoint);
                    break;
                }
                
                // Determine search direction
                int nextMid = (mid + 1) % group.size();
                int prevMid = (mid - 1 + group.size()) % group.size();
                
                double cross1 = crossProduct(current, midPoint, group.get(nextMid));
                double cross2 = crossProduct(current, midPoint, group.get(prevMid));
                
                if (cross1 >= 0 && cross2 >= 0) {
                    candidate = getBetterCandidate(current, candidate, midPoint);
                    break;
                } else if (cross1 < 0) {
                    left = mid + 1;
                } else {
                    right = mid;
                }
            }
        }
        
        return candidate;
    }
    
    private static boolean isTangent(Point2D current, Point2D mid, List<Point2D> group, int midIndex) {
        int next = (midIndex + 1) % group.size();
        int prev = (midIndex - 1 + group.size()) % group.size();
        
        double cross1 = crossProduct(current, mid, group.get(next));
        double cross2 = crossProduct(current, mid, group.get(prev));
        
        return cross1 >= 0 && cross2 >= 0;
    }
    
    private static Point2D getBetterCandidate(Point2D current, Point2D candidate1, Point2D candidate2) {
        double cross = crossProduct(current, candidate1, candidate2);
        return cross > 0 ? candidate2 : candidate1;
    }
    
    private static double crossProduct(Point2D a, Point2D b, Point2D c) {
        return (b.getX() - a.getX()) * (c.getY() - a.getY()) - 
               (b.getY() - a.getY()) * (c.getX() - a.getX());
    }
} 