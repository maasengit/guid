package com.abc.guid;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class GUIDGeneratorBenchmarkTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GUIDGeneratorBenchmarkTest.class);

    public static void main(String[] args) throws Exception {
        final Options opt = new OptionsBuilder()
                .include(GUIDGeneratorBenchmarkTest.class.getSimpleName())
                .warmupIterations(2)
                .warmupTime(TimeValue.seconds(1))
                .measurementIterations(10)
                .measurementTime(TimeValue.seconds(1))
                .forks(1)
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    public void testNextId() {
        GUIDGenerator.nextId();
    }

    @Benchmark
    public void testJavaUUID() {
        UUID.randomUUID();
    }

}
