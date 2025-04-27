package com.myapp.hull.algorithms;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import com.myapp.hull.Point2D;

public class ComplexityBenchmarkRunner {
    private static final int WARMUP_ITERATIONS = 3;
    private static final int MEASUREMENT_ITERATIONS = 5;
    private static final int[] POINT_COUNTS = {100000, 500000, 1000000, 2000000, 5000000};

    public static void main(String[] args) throws IOException {
        System.out.println("Starting complexity benchmarks...");
        
        try (FileWriter writer = new FileWriter("complexity_analysis.csv")) {
            // Write header
            writer.write("Algorithm,PointCount,Time(ms),Complexity\n");
            
            // Run benchmarks for each algorithm
            runBenchmarks(new GrahamScan(), "GrahamScan", writer);
            runBenchmarks(new ChansAlgorithm(), "ChansAlgorithm", writer);
            runBenchmarks(new MergeHullAlgorithm(), "MergeHull", writer);
        }
        
        System.out.println("Benchmarks completed. Results saved to complexity_analysis.csv");
    }

    private static void runBenchmarks(ConvexHullAlgorithm algorithm, String algorithmName, FileWriter writer) 
            throws IOException {
        System.out.println("\nRunning benchmarks for " + algorithmName);
        
        for (int pointCount : POINT_COUNTS) {
            System.out.println("Testing with " + pointCount + " points");
            
            // Generate points
            List<Point2D> points = generateRandomPoints(pointCount);
            
            // Warmup
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                algorithm.computeHull(points);
            }
            
            // Measurement
            long totalTime = 0;
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                long startTime = System.nanoTime();
                algorithm.computeHull(points);
                long endTime = System.nanoTime();
                totalTime += (endTime - startTime);
            }
            
            // Calculate average time in milliseconds
            double avgTime = (totalTime / MEASUREMENT_ITERATIONS) / 1_000_000.0;
            
            // Determine complexity
            String complexity = determineComplexity(algorithmName);
            
            // Write results
            writer.write(String.format("%s,%d,%.3f,%s\n", 
                algorithmName, pointCount, avgTime, complexity));
            
            System.out.printf("Average time: %.3f ms\n", avgTime);
        }
    }

    private static List<Point2D> generateRandomPoints(int count) {
        Random random = new Random(42); // Fixed seed for reproducibility
        List<Point2D> points = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            points.add(new Point2D(random.nextDouble() * 1000, random.nextDouble() * 1000));
        }
        return points;
    }

    private static String determineComplexity(String algorithmName) {
        switch (algorithmName) {
            case "GrahamScan":
                return "O(n log n)";
            case "ChansAlgorithm":
                return "O(n log h)";
            case "MergeHull":
                return "O(n log n)";
            default:
                return "Unknown";
        }
    }
} 