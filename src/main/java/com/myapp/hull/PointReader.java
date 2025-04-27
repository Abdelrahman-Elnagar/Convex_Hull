package com.myapp.hull;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for reading points from a text file.
 * Each line should contain two whitespace-separated numbers representing x and y coordinates.
 */
public class PointReader {
    /**
     * Reads points from a file.
     *
     * @param filePath Path to the input file
     * @return List of points read from the file
     * @throws IOException If there's an error reading the file
     * @throws NumberFormatException If the file contains invalid numbers
     */
    public static List<Point2D> readPoints(String filePath) throws IOException {
        List<Point2D> points = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    String[] parts = line.split("\\s+");
                    if (parts.length != 2) {
                        throw new IllegalArgumentException("Each line must contain exactly two numbers");
                    }
                    double x = Double.parseDouble(parts[0]);
                    double y = Double.parseDouble(parts[1]);
                    points.add(new Point2D(x, y));
                }
            }
        }
        return points;
    }
} 