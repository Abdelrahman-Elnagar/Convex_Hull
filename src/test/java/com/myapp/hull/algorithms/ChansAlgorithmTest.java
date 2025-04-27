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

class ChansAlgorithmTest {
    private final ChansAlgorithm chansAlgorithm = new ChansAlgorithm();
    private final GrahamScan grahamScan = new GrahamScan();

    @Test
    void testEmptyInput() {
        List<Point2D> points = List.of();
        List<Point2D> hull = chansAlgorithm.computeHull(points);
        assertTrue(hull.isEmpty());
    }

    @Test
    void testSinglePoint() {
        List<Point2D> points = List.of(new Point2D(1.0, 1.0));
        List<Point2D> hull = chansAlgorithm.computeHull(points);
        assertEquals(1, hull.size());
        assertEquals(points.get(0), hull.get(0));
    }

    @Test
    void testTwoPoints() {
        List<Point2D> points = Arrays.asList(
            new Point2D(1.0, 1.0),
            new Point2D(2.0, 2.0)
        );
        List<Point2D> hull = chansAlgorithm.computeHull(points);
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
        List<Point2D> hull = chansAlgorithm.computeHull(points);
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

        List<Point2D> hull = chansAlgorithm.computeHull(points);
        List<Point2D> expectedHull = grahamScan.computeHull(points);
        
        // Verify hulls are equivalent
        assertEquals(expectedHull.size(), hull.size());
        assertTrue(hull.containsAll(expectedHull));
        
        // Verify convexity
        verifyConvexity(hull);
    }

    @Test
    void testLargeHull() {
        Random random = new Random(42);
        List<Point2D> points = new ArrayList<>();
        
        // Generate points on a circle (large hull)
        int n = 1000;
        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n;
            points.add(new Point2D(
                Math.cos(angle),
                Math.sin(angle)
            ));
        }

        List<Point2D> hull = chansAlgorithm.computeHull(points);
        assertEquals(n, hull.size());
        verifyConvexity(hull);
    }

    @Test
    void testSmallHull() {
        Random random = new Random(42);
        List<Point2D> points = new ArrayList<>();
        
        // Generate many points close to a line (small hull)
        int n = 1000;
        for (int i = 0; i < n; i++) {
            double x = random.nextDouble();
            double y = x + random.nextDouble() * 0.1; // Points close to y = x
            points.add(new Point2D(x, y));
        }

        List<Point2D> hull = chansAlgorithm.computeHull(points);
        assertTrue(hull.size() < n); // Should have small hull
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