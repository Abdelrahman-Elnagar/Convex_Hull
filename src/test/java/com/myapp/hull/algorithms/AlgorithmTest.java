package com.myapp.hull.algorithms;

import com.myapp.hull.Point2D;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class AlgorithmTest {
    private static final Random RANDOM = new Random(42); // Fixed seed for reproducibility
    
    private static Stream<ConvexHullAlgorithm> algorithms() {
        return Stream.of(
            new GrahamScan(),
            new ChansAlgorithm(),
            new MergeHullAlgorithm()
        );
    }
    
    @ParameterizedTest
    @MethodSource("algorithms")
    void testEmptyInput(ConvexHullAlgorithm algorithm) {
        List<Point2D> points = new ArrayList<>();
        List<Point2D> hull = algorithm.computeHull(points);
        assertTrue(hull.isEmpty());
    }
    
    @ParameterizedTest
    @MethodSource("algorithms")
    void testSinglePoint(ConvexHullAlgorithm algorithm) {
        List<Point2D> points = List.of(new Point2D(0, 0));
        List<Point2D> hull = algorithm.computeHull(points);
        assertEquals(1, hull.size());
        assertEquals(points.get(0), hull.get(0));
    }
    
    @ParameterizedTest
    @MethodSource("algorithms")
    void testCollinearPoints(ConvexHullAlgorithm algorithm) {
        List<Point2D> points = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            points.add(new Point2D(i, i));
        }
        List<Point2D> hull = algorithm.computeHull(points);
        assertEquals(2, hull.size());
        assertTrue(hull.contains(points.get(0)));
        assertTrue(hull.contains(points.get(points.size() - 1)));
    }
    
    @ParameterizedTest
    @MethodSource("algorithms")
    void testRandomPoints(ConvexHullAlgorithm algorithm) {
        List<Point2D> points = generateRandomPoints(100);
        List<Point2D> hull = algorithm.computeHull(points);
        
        // Verify hull properties
        assertTrue(hull.size() >= 3 || points.size() < 3);
        verifyConvexity(hull);
        verifyAllPointsInside(points, hull);
    }
    
    @Test
    void testAllAlgorithmsAgree() {
        List<Point2D> points = generateRandomPoints(1000);
        List<Point2D> hull1 = new GrahamScan().computeHull(points);
        List<Point2D> hull2 = new ChansAlgorithm().computeHull(points);
        List<Point2D> hull3 = new MergeHullAlgorithm().computeHull(points);
        
        assertEquals(hull1.size(), hull2.size());
        assertEquals(hull1.size(), hull3.size());
        assertTrue(hull1.containsAll(hull2));
        assertTrue(hull1.containsAll(hull3));
    }
    
    private List<Point2D> generateRandomPoints(int count) {
        List<Point2D> points = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            points.add(new Point2D(
                RANDOM.nextDouble() * 1000,
                RANDOM.nextDouble() * 1000
            ));
        }
        return points;
    }
    
    private void verifyConvexity(List<Point2D> hull) {
        if (hull.size() < 3) return;
        
        for (int i = 0; i < hull.size(); i++) {
            Point2D p1 = hull.get(i);
            Point2D p2 = hull.get((i + 1) % hull.size());
            Point2D p3 = hull.get((i + 2) % hull.size());
            
            double cross = (p2.getX() - p1.getX()) * (p3.getY() - p1.getY()) -
                         (p2.getY() - p1.getY()) * (p3.getX() - p1.getX());
            
            assertTrue(cross <= 0, "Points are not in CCW order");
        }
    }
    
    private void verifyAllPointsInside(List<Point2D> points, List<Point2D> hull) {
        for (Point2D point : points) {
            boolean onHull = hull.contains(point);
            boolean inside = isPointInsideHull(point, hull);
            assertTrue(onHull || inside, "Point is outside the hull");
        }
    }
    
    private boolean isPointInsideHull(Point2D point, List<Point2D> hull) {
        if (hull.size() < 3) return true;
        
        for (int i = 0; i < hull.size(); i++) {
            Point2D p1 = hull.get(i);
            Point2D p2 = hull.get((i + 1) % hull.size());
            
            double cross = (p2.getX() - p1.getX()) * (point.getY() - p1.getY()) -
                         (p2.getY() - p1.getY()) * (point.getX() - p1.getX());
            
            if (cross > 0) return false;
        }
        return true;
    }
} 