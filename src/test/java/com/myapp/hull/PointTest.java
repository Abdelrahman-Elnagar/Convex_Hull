package com.myapp.hull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;

class PointTest {
    @Test
    void testPoint2D() {
        Point2D p1 = new Point2D(1.0, 2.0);
        Point2D p2 = new Point2D(1.0, 2.0);
        Point2D p3 = new Point2D(2.0, 1.0);

        assertEquals(p1, p2);
        assertNotEquals(p1, p3);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertEquals("1.00 2.00", p1.toString());
    }

    @Test
    void testPointReaderWriter() throws IOException {
        // Create test points
        List<Point2D> points = Arrays.asList(
            new Point2D(1.0, 2.0),
            new Point2D(3.0, 4.0),
            new Point2D(5.0, 6.0)
        );

        // Write points to a temporary file
        Path tempFile = Files.createTempFile("points", ".txt");
        PointWriter.writePoints(points, tempFile.toString());

        // Read points back
        List<Point2D> readPoints = PointReader.readPoints(tempFile.toString());

        // Verify
        assertEquals(points.size(), readPoints.size());
        for (int i = 0; i < points.size(); i++) {
            assertEquals(points.get(i), readPoints.get(i));
        }

        // Clean up
        Files.delete(tempFile);
    }
} 