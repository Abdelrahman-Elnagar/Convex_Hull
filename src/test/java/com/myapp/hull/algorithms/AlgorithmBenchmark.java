package com.myapp.hull.algorithms;

import com.myapp.hull.Point2D;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class AlgorithmBenchmark {
    @Param({"1000", "10000", "100000"})
    private int pointCount;
    
    private List<Point2D> points;
    private final Random random = new Random(42);
    
    @Setup
    public void setup() {
        points = new ArrayList<>();
        for (int i = 0; i < pointCount; i++) {
            points.add(new Point2D(
                random.nextDouble() * 1000,
                random.nextDouble() * 1000
            ));
        }
    }
    
    @Benchmark
    public void grahamScan(Blackhole bh) {
        ConvexHullAlgorithm algorithm = new GrahamScan();
        List<Point2D> hull = algorithm.computeHull(points);
        bh.consume(hull);
    }
    
    @Benchmark
    public void chansAlgorithm(Blackhole bh) {
        ConvexHullAlgorithm algorithm = new ChansAlgorithm();
        List<Point2D> hull = algorithm.computeHull(points);
        bh.consume(hull);
    }
    
    @Benchmark
    public void mergeHull(Blackhole bh) {
        ConvexHullAlgorithm algorithm = new MergeHullAlgorithm();
        List<Point2D> hull = algorithm.computeHull(points);
        bh.consume(hull);
    }
} 