package com.myapp.hull.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.myapp.hull.Point2D;

class MergeHullAlgorithmTest {
    private final MergeHullAlgorithm mergeHull = new MergeHullAlgorithm();
    private final GrahamScan grahamScan = new GrahamScan();

    @Test
    void testEmptyInput() {
        List<Point2D> points = List.of();
        List<Point2D> hull = mergeHull.computeHull(points);
        assertTrue(hull.isEmpty());
    }

    @Test
    void testSinglePoint() {
        List<Point2D> points = List.of(new Point2D(1.0, 1.0));
        List<Point2D> hull = mergeHull.computeHull(points);
        assertEquals(1, hull.size());
        assertEquals(points.get(0), hull.get(0));
    }

    @Test
    void testTwoPoints() {
        List<Point2D> points = Arrays.asList(
            new Point2D(1.0, 1.0),
            new Point2D(2.0, 2.0)
        );
        List<Point2D> hull = mergeHull.computeHull(points);
        assertEquals(2, hull.size());
        assertTrue(hull.containsAll(points));
    }

    @Test
    void testSquare() {
        List<Point2D> points = Arrays.asList(
            new Point2D(0.0, 0.0),
            new Point2D(1.0, 0.0),
            new Point2D(1.0, 1.0),
            new Point2D(0.0, 1.0),
            new Point2D(0.5, 0.5)  // Interior point
        );
        List<Point2D> hull = mergeHull.computeHull(points);
        assertEquals(4, hull.size());
        assertTrue(hull.contains(new Point2D(0.0, 0.0)));
        assertTrue(hull.contains(new Point2D(1.0, 0.0)));
        assertTrue(hull.contains(new Point2D(1.0, 1.0)));
        assertTrue(hull.contains(new Point2D(0.0, 1.0)));
        assertFalse(hull.contains(new Point2D(0.5, 0.5)));
    }

    @Test
    void testRandomPoints() {
        Random random = new Random(42); // Fixed seed for reproducibility
        List<Point2D> points = new ArrayList<>();
        
        // Generate 100 random points
        for (int i = 0; i < 100; i++) {
            points.add(new Point2D(
                random.nextDouble() * 100,
                random.nextDouble() * 100
            ));
        }

        List<Point2D> hull = mergeHull.computeHull(points);
        List<Point2D> expectedHull = grahamScan.computeHull(points);
        
        // Verify hulls are equivalent
        assertEquals(expectedHull.size(), hull.size());
        assertTrue(hull.containsAll(expectedHull));
        
        // Verify convexity
        verifyConvexity(hull);
    }

    @Test
    void testSplitHull() {
        // Create two separate convex hulls
        List<Point2D> leftHull = Arrays.asList(
            new Point2D(0.0, 0.0),
            new Point2D(1.0, 0.0),
            new Point2D(1.0, 1.0),
            new Point2D(0.0, 1.0)
        );
        
        List<Point2D> rightHull = Arrays.asList(
            new Point2D(2.0, 0.0),
            new Point2D(3.0, 0.0),
            new Point2D(3.0, 1.0),
            new Point2D(2.0, 1.0)
        );
        
        List<Point2D> allPoints = new ArrayList<>();
        allPoints.addAll(leftHull);
        allPoints.addAll(rightHull);
        
        List<Point2D> hull = mergeHull.computeHull(allPoints);
        assertEquals(6, hull.size()); // Should have 6 points in the merged hull
        verifyConvexity(hull);
    }

    @Test
    void testNestedHulls() {
        // Create nested convex hulls
        List<Point2D> points = Arrays.asList(
            new Point2D(0.0, 0.0),
            new Point2D(3.0, 0.0),
            new Point2D(3.0, 3.0),
            new Point2D(0.0, 3.0),
            new Point2D(1.0, 1.0),
            new Point2D(2.0, 1.0),
            new Point2D(2.0, 2.0),
            new Point2D(1.0, 2.0)
        );
        
        List<Point2D> hull = mergeHull.computeHull(points);
        assertEquals(4, hull.size()); // Should only have outer hull points
        verifyConvexity(hull);
    }

    private void verifyConvexity(List<Point2D> hull) {
        for (int i = 0; i < hull.size(); i++) {
            Point2D a = hull.get(i);
            Point2D b = hull.get((i + 1) % hull.size());
            Point2D c = hull.get((i + 2) % hull.size());
            
            double cross = (b.getX() - a.getX()) * (c.getY() - a.getY()) - 
                          (b.getY() - a.getY()) * (c.getX() - a.getX());
            assertTrue(cross >= 0, "Points should be in counter-clockwise order");
        }
    }
} 