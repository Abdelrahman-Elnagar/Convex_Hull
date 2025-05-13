package com.myapp.hull.benchmark.Benchmarkcodes;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.myapp.hull.Point2D;
import com.myapp.hull.algorithms.ChansAlgorithm;
import com.myapp.hull.algorithms.ConvexHullAlgorithm;
import com.myapp.hull.algorithms.GrahamScan;
import com.myapp.hull.algorithms.MergeHullAlgorithm;

public class SmallBenchmark {
    // Test with more reasonable increments
    private static final int[] POINT_COUNTS = {
        10, 20, 50, 100, 200, 500, 750, 1000, 1500, 2000, 3000, 4000, 5000, 
        6000, 7000, 8000, 9000, 10000
    };
    
    private static final int WARMUP_ITERATIONS = 3;
    private static final int BENCHMARK_ITERATIONS = 5;
    
    public static void main(String[] args) {
        System.out.println("Starting improved convex hull benchmarks...");
        
        // Pre-generate all point sets to ensure all algorithms use the same data
        Map<Integer, List<Point2D>> allPointSets = new HashMap<>();
        for (int pointCount : POINT_COUNTS) {
            allPointSets.put(pointCount, generateRandomPoints(pointCount));
        }
        
        List<ConvexHullAlgorithm> algorithms = new ArrayList<>();
        algorithms.add(new GrahamScan());
        algorithms.add(new ChansAlgorithm());
        algorithms.add(new MergeHullAlgorithm());
        
        try (FileWriter writer = new FileWriter("small_benchmark_results.csv")) {
            // Write header
            writer.write("Algorithm,PointCount,Time(ms),MemoryUsage(MB)\n");
            
            // Run benchmarks for each algorithm
            for (ConvexHullAlgorithm algorithm : algorithms) {
                String algorithmName = algorithm.getClass().getSimpleName();
                runBenchmarks(algorithm, algorithmName, allPointSets, writer);
            }
            
            System.out.println("\nBenchmarks completed. Results saved to small_benchmark_results.csv");
        } catch (IOException e) {
            System.err.println("Error writing results: " + e.getMessage());
        }
    }
    
    private static void runBenchmarks(
            ConvexHullAlgorithm algorithm, 
            String algorithmName, 
            Map<Integer, List<Point2D>> allPointSets, 
            FileWriter writer) throws IOException {
        
        System.out.println("\nRunning benchmarks for " + algorithmName);
        
        // Run garbage collection before each algorithm to reduce interference
        System.gc();
        
        for (int pointCount : POINT_COUNTS) {
            List<Point2D> points = allPointSets.get(pointCount);
            
            // Warm-up runs to allow JVM to optimize
            System.out.printf("Warming up %s with %d points...\n", algorithmName, pointCount);
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                algorithm.computeHull(new ArrayList<>(points));
            }
            
            // Benchmark runs
            double totalTimeMs = 0;
            long totalMemoryUsage = 0;
            
            System.out.printf("Benchmarking %s with %d points...\n", algorithmName, pointCount);
            for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
                // Force garbage collection between runs
                System.gc();
                
                long startMemory = getUsedMemory();
                
                long startTime = System.nanoTime();
                List<Point2D> hull = algorithm.computeHull(new ArrayList<>(points));
                long endTime = System.nanoTime();
                
                long endMemory = getUsedMemory();
                
                double timeMs = (endTime - startTime) / 1_000_000.0;
                long memoryUsed = endMemory - startMemory;
                
                totalTimeMs += timeMs;
                totalMemoryUsage += memoryUsed > 0 ? memoryUsed : 0; // Avoid negative values due to GC
                
                System.out.printf("  Trial %d: %.3f ms, Memory: %d MB, Hull size: %d\n",
                        i + 1, timeMs, memoryUsed / (1024 * 1024), hull.size());
            }
            
            // Calculate averages
            double avgTimeMs = totalTimeMs / BENCHMARK_ITERATIONS;
            long avgMemoryUsageMB = (totalMemoryUsage / BENCHMARK_ITERATIONS) / (1024 * 1024);
            
            // Write results
            writer.write(String.format("%s,%d,%.3f,%d\n",
                    algorithmName, pointCount, avgTimeMs, avgMemoryUsageMB));
            
            System.out.printf("Average for %d points: %.3f ms, Memory: %d MB\n",
                    pointCount, avgTimeMs, avgMemoryUsageMB);
        }
    }
    
    private static long getUsedMemory() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
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