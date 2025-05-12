package com.myapp.hull.benchmark;

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

public class SmallBenchmark {
    private static final int[] POINT_COUNTS = new int[10000];
    
    static {
        // Initialize array with values from 1 to 10000
        for (int i = 0; i < 10000; i++) {
            POINT_COUNTS[i] = i + 1;
        }
    }

    public static void main(String[] args) {
        System.out.println("Starting small convex hull benchmarks...");
        
        try (FileWriter writer = new FileWriter("small_benchmark_results.csv")) {
            // Write header
            writer.write("Algorithm,PointCount,Time(ms),MemoryUsage(MB)\n");
            
            // Run benchmarks for each algorithm
            runBenchmarks(new GrahamScan(), "GrahamScan", writer);
            runBenchmarks(new ChansAlgorithm(), "ChansAlgorithm", writer);
            runBenchmarks(new MergeHullAlgorithm(), "MergeHull", writer);
            
            System.out.println("\nBenchmarks completed. Results saved to small_benchmark_results.csv");
        } catch (IOException e) {
            System.err.println("Error writing results: " + e.getMessage());
        }
    }

    private static void runBenchmarks(ConvexHullAlgorithm algorithm, String algorithmName, FileWriter writer) 
            throws IOException {
        System.out.println("\nRunning benchmarks for " + algorithmName);
        
        for (int pointCount : POINT_COUNTS) {
            // Generate points
            List<Point2D> points = generateRandomPoints(pointCount);
            
            // Single measurement
            long startTime = System.nanoTime();
            List<Point2D> hull = algorithm.computeHull(points);
            long endTime = System.nanoTime();
            
            // Calculate memory usage
            long memoryUsage = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
            
            // Calculate time in milliseconds
            double timeMs = (endTime - startTime) / 1_000_000.0;
            
            // Write results
            writer.write(String.format("%s,%d,%.3f,%d\n", 
                algorithmName, pointCount, timeMs, memoryUsage));
            
            if (pointCount % 100 == 0) {
                System.out.printf("Processed %d points: %.3f ms, Memory: %d MB\n", 
                    pointCount, timeMs, memoryUsage);
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
} 