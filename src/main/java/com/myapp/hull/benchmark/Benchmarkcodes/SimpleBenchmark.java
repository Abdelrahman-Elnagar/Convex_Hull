package com.myapp.hull.benchmark.Benchmarkcodes;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.myapp.hull.Point2D;
import com.myapp.hull.algorithms.ChansAlgorithm;
import com.myapp.hull.algorithms.ConvexHullAlgorithm;
import com.myapp.hull.algorithms.GrahamScan;
import com.myapp.hull.algorithms.MergeHullAlgorithm;

public class SimpleBenchmark {
    private static final int WARMUP_ITERATIONS = 5;
    private static final int MEASUREMENT_ITERATIONS = 10;
    private static final int[] POINT_COUNTS = {
        1000, 5000, 10000, 50000, 100000, 
        500000, 1000000, 2000000, 5000000, 10000000
    };

    public static void main(String[] args) {
        System.out.println("Starting convex hull benchmarks...");
        
        try (FileWriter writer = new FileWriter("benchmark_results.csv")) {
            // Write header with more detailed information
            writer.write("Algorithm,PointCount,Time(ms),Complexity,Iteration,MemoryUsage(MB)\n");
            
            // Run benchmarks for each algorithm
            runBenchmarks(new GrahamScan(), "GrahamScan", writer);
            runBenchmarks(new ChansAlgorithm(), "ChansAlgorithm", writer);
            runBenchmarks(new MergeHullAlgorithm(), "MergeHull", writer);
            
            System.out.println("\nBenchmarks completed. Results saved to benchmark_results.csv");
        } catch (IOException e) {
            System.err.println("Error writing results: " + e.getMessage());
        }
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
            for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                // Force garbage collection before measurement
                System.gc();
                
                long startTime = System.nanoTime();
                List<Point2D> hull = algorithm.computeHull(points);
                long endTime = System.nanoTime();
                
                // Calculate memory usage
                long memoryUsage = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
                
                // Calculate time in milliseconds
                double timeMs = (endTime - startTime) / 1_000_000.0;
                
                // Determine complexity
                String complexity = determineComplexity(algorithmName);
                
                // Write results for each iteration
                writer.write(String.format("%s,%d,%.3f,%s,%d,%d\n", 
                    algorithmName, pointCount, timeMs, complexity, i + 1, memoryUsage));
                
                System.out.printf("Iteration %d: %.3f ms, Memory: %d MB\n", 
                    i + 1, timeMs, memoryUsage);
            }
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