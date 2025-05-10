package com.myapp.hull;

/**
 * Utility class for geometric calculations.
 */
public class Maths {
    
    /**
     * Checks if point c is to the left of the line from a to b.
     */
    public static boolean isLeftOf(Point2D a, Point2D b, Point2D c) {
        return (b.getX() - a.getX()) * (c.getY() - a.getY()) > (c.getX() - a.getX()) * (b.getY() - a.getY());
    }
    
    /**
     * Calculates the squared distance between two points.
     */
    public static double squaredDistance(double dx, double dy) {
        return dx * dx + dy * dy;
    }
} 