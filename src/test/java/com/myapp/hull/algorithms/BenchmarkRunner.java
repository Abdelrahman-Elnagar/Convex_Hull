package com.myapp.hull.algorithms;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class BenchmarkRunner {
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(AlgorithmBenchmark.class.getSimpleName())
                .output("benchmark_results.txt")
                .forks(1)
                .build();

        new Runner(opt).run();
    }
} 