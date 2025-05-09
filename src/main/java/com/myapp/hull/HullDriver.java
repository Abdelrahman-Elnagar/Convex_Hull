package com.myapp.hull;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.myapp.hull.algorithms.ChansAlgorithm;
import com.myapp.hull.algorithms.ConvexHullAlgorithm;
import com.myapp.hull.algorithms.GrahamScan;
import com.myapp.hull.algorithms.MergeHullAlgorithm;

/**
 * Command-line driver for convex hull algorithms.
 * Usage: java HullDriver --algorithm=graham|chan|merge --input=path.txt
 */
public class HullDriver {
    private static final Map<String, ConvexHullAlgorithm> ALGORITHMS = new HashMap<>();
    
    static {
        ALGORITHMS.put("graham", new GrahamScan());
        ALGORITHMS.put("chan", new ChansAlgorithm());
        ALGORITHMS.put("merge", new MergeHullAlgorithm());
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            printUsage();
            System.exit(1);
        }

        String algorithmName = null;
        String inputFile = null;

        // Parse command line arguments
        for (String arg : args) {
            if (arg.startsWith("--algorithm=")) {
                algorithmName = arg.substring("--algorithm=".length());
            } else if (arg.startsWith("--input=")) {
                inputFile = arg.substring("--input=".length());
            }
        }

        // Validate arguments
        if (algorithmName == null || inputFile == null) {
            printUsage();
            System.exit(1);
        }

        ConvexHullAlgorithm algorithm = ALGORITHMS.get(algorithmName);
        if (algorithm == null) {
            System.err.println("Invalid algorithm: " + algorithmName);
            printUsage();
            System.exit(1);
        }

        try {
            // Read points from file
            List<Point2D> points = PointReader.readPoints(inputFile);
            System.out.println("Read " + points.size() + " points from " + inputFile);

            // Compute convex hull
            long startTime = System.nanoTime();
            List<Point2D> hull = algorithm.computeHull(points);
            long endTime = System.nanoTime();
            
            // Print results
            System.out.println("Computed hull with " + hull.size() + " points");
            System.out.println("Time taken: " + (endTime - startTime) / 1_000_000.0 + " ms");
            
            // Write hull to output file
            String outputFile = inputFile.replace(".txt", "_hull.txt");
            PointWriter.writePoints(hull, outputFile);
            System.out.println("Wrote hull to " + outputFile);

        } catch (IOException e) {
            System.err.println("Error processing file: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.err.println("Usage: java HullDriver --algorithm=graham|chan|merge --input=path.txt");
        System.err.println("Algorithms:");
        System.err.println("  graham - Graham Scan algorithm (O(n log n))");
        System.err.println("  chan   - Chan's algorithm (O(n log h))");
        System.err.println("  merge  - Merge Hull algorithm (O(n log n))");
    }
} 