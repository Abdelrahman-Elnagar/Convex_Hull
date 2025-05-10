package com.myapp.hull;

import java.util.Objects;

/**
 * Represents a point in 2D space with double precision coordinates.
 */
public class Point2D implements Comparable<Point2D> {
    private final double x;
    private final double y;
    private double angle;
    private int group;
    private boolean stillNeeded;

    public Point2D(double x, double y) {
        this.x = x;
        this.y = y;
        this.stillNeeded = true;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public int getGroup() {
        return group;
    }

    public void setGroup(int group) {
        this.group = group;
    }

    public boolean isStillNeeded() {
        return stillNeeded;
    }

    public void setStillNeeded(boolean stillNeeded) {
        this.stillNeeded = stillNeeded;
    }

    @Override
    public int compareTo(Point2D other) {
        return Double.compare(this.angle, other.angle);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point2D point2D = (Point2D) o;
        return Double.compare(point2D.x, x) == 0 && Double.compare(point2D.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return String.format("%.2f %.2f", x, y);
    }
} 