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

public class Benchmark {
private static final int WARMUP_ITERATIONS = 5;
private static final int MEASUREMENT_ITERATIONS = 10;
// Define a more granular range of point counts from 1 to 10,000,000
private static final int[] POINT_COUNTS = generatePointCounts(1, 5000000);

// List of algorithms to benchmark
private static final ConvexHullAlgorithm[] ALGORITHMS = {
    new GrahamScan(),
    new ChansAlgorithm(),
    new MergeHullAlgorithm()
};

private static final String[] ALGORITHM_NAMES = {
    "GrahamScan",
    "ChansAlgorithm",
    "MergeHull"
};
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
public static void main(String[] args) {
    System.out.println("Starting convex hull benchmarks...");
    
    try (FileWriter writer = new FileWriter("benchmark_results.csv")) {
        // Write header with detailed information
        writer.write("Algorithm,PointCount,Time(ms),Complexity,Iteration,MemoryUsage(MB),HullSize\n");
        
        // For each point count
        for (int pointCount : POINT_COUNTS) {
            System.out.println("\nTesting with " + pointCount + " points");
            
            // Generate the same set of points for all algorithms
            List<Point2D> points = generateRandomPoints(pointCount);
            
            // Test each algorithm with this point set
            for (int algIndex = 0; algIndex < ALGORITHMS.length; algIndex++) {
                ConvexHullAlgorithm algorithm = ALGORITHMS[algIndex];
                String algorithmName = ALGORITHM_NAMES[algIndex];
                
                System.out.println("  Running " + algorithmName);
                
                // Skip warmup for very small point counts to save time
                if (pointCount > 100) {
                    // Warmup phase for this algorithm
                    for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                        algorithm.computeHull(new ArrayList<>(points)); // Use a copy to prevent any side effects
                    }
                }
                
                List<Point2D> hull = null; // Store hull for size measurement
                
                // Measurement phase for this algorithm
                for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                    // Reduce iterations for very large point counts to make benchmark more practical
                    if (pointCount > 1000000 && i >= 3) break;
                    
                    // Force garbage collection before measurement
                    System.gc();
                    
                    long startTime = System.nanoTime();
                    hull = algorithm.computeHull(new ArrayList<>(points)); // Use a copy
                    long endTime = System.nanoTime();
                    
                    // Calculate memory usage
                    long memoryUsage = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
                    
                    // Calculate time in milliseconds
                    double timeMs = (endTime - startTime) / 1_000_000.0;
                    
                    // Determine complexity
                    String complexity = determineComplexity(algorithmName);
                    
                    // Get hull size (number of points in the convex hull)
                    int hullSize = (hull != null) ? hull.size() : 0;
                    
                    // Write results for each iteration
                    writer.write(String.format("%s,%d,%.3f,%s,%d,%d,%d\n", 
                        algorithmName, pointCount, timeMs, complexity, i + 1, memoryUsage, hullSize));
                    
                    System.out.printf("    Iteration %d: %.3f ms, Memory: %d MB, Hull size: %d\n", 
                        i + 1, timeMs, memoryUsage, hullSize);
                }
            }
        }
        
        System.out.println("\nBenchmarks completed. Results saved to benchmark_results.csv");
    } catch (IOException e) {
        System.err.println("Error writing results: " + e.getMessage());
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

/**
 * Generates an array of point counts with logarithmic scaling
 * @param start The minimum number of points
 * @param end The maximum number of points
 * @return An array of point counts
 */
private static int[] generatePointCounts(int start, int end) {
    List<Integer> counts = new ArrayList<>();
    
    // Start with very small counts (1, 2, 5, 10, etc.)
    if (start == 1) {
        counts.add(1);
        counts.add(2);
        counts.add(5);
        counts.add(10);
        counts.add(20);
        counts.add(50);
        counts.add(100);
        counts.add(200);
        counts.add(500);
    }
    
    // Add counts with increasing intervals
    for (int i = 1000; i <= end; i *= 2) {
        counts.add(i);
        
        // Add an intermediate point if we're not at the highest powers yet
        if (i * 1.5 <= end && i != 5000000) {
            counts.add((int)(i * 1.5));
        }
        
        // Add another intermediate point for better distribution
        if (i * 5 <= end && i < 1000000) {
            counts.add(i * 5);
        }
    }
    
    // Ensure the end value is included if it's not already
    if (counts.get(counts.size() - 1) != end && end > counts.get(counts.size() - 1)) {
        counts.add(end);
    }
    
    // Convert List<Integer> to int[]
    int[] result = new int[counts.size()];
    for (int i = 0; i < counts.size(); i++) {
        result[i] = counts.get(i);
    }
    
    return result;
}
}