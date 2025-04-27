package com.myapp.hull.algorithms;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import com.myapp.hull.Point2D;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@Threads(1)
public class ComplexityBenchmark {
    private List<Point2D> points;
    private Random random;
    private ConvexHullAlgorithm grahamScan;
    private ConvexHullAlgorithm chansAlgorithm;
    private ConvexHullAlgorithm mergeHull;

    @Param({"100000", "500000", "1000000", "2000000", "5000000"})
    public int pointCount;

    @Setup(Level.Trial)
    public void setup() {
        random = new Random(42); // Fixed seed for reproducibility
        grahamScan = new GrahamScan();
        chansAlgorithm = new ChansAlgorithm();
        mergeHull = new MergeHullAlgorithm();
        points = generateRandomPoints(pointCount);
    }

    private List<Point2D> generateRandomPoints(int count) {
        List<Point2D> points = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            points.add(new Point2D(random.nextDouble() * 1000, random.nextDouble() * 1000));
        }
        return points;
    }

    @Benchmark
    public void benchmarkGrahamScan(Blackhole bh) {
        List<Point2D> hull = grahamScan.computeHull(points);
        bh.consume(hull);
    }

    @Benchmark
    public void benchmarkChansAlgorithm(Blackhole bh) {
        List<Point2D> hull = chansAlgorithm.computeHull(points);
        bh.consume(hull);
    }

    @Benchmark
    public void benchmarkMergeHull(Blackhole bh) {
        List<Point2D> hull = mergeHull.computeHull(points);
        bh.consume(hull);
    }
} 