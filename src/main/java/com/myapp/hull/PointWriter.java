package com.myapp.hull;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Utility class for writing points to a text file.
 * Each point is written on a separate line with x and y coordinates separated by whitespace.
 */
public class PointWriter {
    /**
     * Writes points to a file.
     *
     * @param points List of points to write
     * @param filePath Path to the output file
     * @throws IOException If there's an error writing to the file
     */
    public static void writePoints(List<Point2D> points, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Point2D point : points) {
                writer.write(point.toString());
                writer.newLine();
            }
        }
    }
} 