/*
 * Copyright 2020-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tlc2.monitor;

import tlc2.monitor.sink.Sink;
import tlc2.monitor.source.Partition;
import tlc2.monitor.source.Record;
import tlc2.monitor.source.Source;
import tlc2.monitor.util.ConsoleLogger;
import tlc2.monitor.util.Logger;
import tlc2.overrides.Alerts;
import tlc2.overrides.Traces;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Monitors a partition and evaluates an infinite stream of traces using TLC.
 * <p>
 * The {@code PartitionMonitor} evaluates traces from a single partition, splitting the partition stream into
 * windows and evaluating each time window within a separate TLC process.
 */
final class PartitionMonitor {
    private static final Logger LOGGER = new ConsoleLogger();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss.SSS");

    private final Partition partition;
    private final Source source;
    private final Sink sink;
    private final TLCMonitorConfig config;

    PartitionMonitor(Partition partition, Source source, Sink sink, TLCMonitorConfig config) {
        this.partition = partition;
        this.source = source;
        this.sink = sink;
        this.config = config;
    }

    /**
     * Runs the partition monitor.
     * <p>
     * The partition monitor uses trace timestamps to evaluate batches of traces within a sliding time window.
     * The monitor begins evaluating traces starting at the first time available in the partitino stream.
     * For each window, the monitor will create a separate TLC process to consume and evaluate the traces.
     */
    public void run() throws Exception {
        LOGGER.log("Starting monitor for %s", partition);
        long time = replayPartition(partition);
        for (; ; ) {
            final long windowStartTime = time;
            final long windowEndTime = time + config.getWindow();
            new Thread(() -> {
                try {
                    runWindow(partition, windowStartTime, windowEndTime);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
            time += config.getWindow() / 2;
            Thread.sleep(config.getWindow() / 2);
        }
    }

    /**
     * Runs TLC processes to check existing offsets in the given partition.
     *
     * @param partition the partition from which to consume traces
     * @return the time (in milliseconds) up to which traces were checked
     */
    private long replayPartition(Partition partition) throws Exception {
        long firstOffset = partition.indexOf(0);
        long time = System.currentTimeMillis();
        if (firstOffset != 0) {
            Record firstRecord = partition.get(firstOffset);
            long startTime = firstRecord.timestamp();
            long lastOffset = partition.indexOf(Long.MAX_VALUE);
            Record lastRecord = partition.get(lastOffset);
            LOGGER.log("Checking existing offsets in %s from offset %d to offset %d", partition, firstOffset, lastOffset);
            long endTime = lastRecord.timestamp();
            time = startTime;
            while (time < endTime) {
                long windowStartTime = time;
                long windowEndTime = time + config.getWindow();
                runWindow(partition, windowStartTime, windowEndTime);
                time += config.getWindow() / 2;
            }
        }
        return time;
    }

    /**
     * Runs a TLC process to evaluate traces within the given window.
     */
    private void runWindow(Partition partition, long startTime, long endTime) throws Exception {
        LOGGER.log("Checking traces in %s spanning time window %s to %s",
            partition,
            DATE_FORMAT.format(new Date(startTime)),
            DATE_FORMAT.format(new Date(endTime)));

        TLCRunner runner = new TLCRunner();

        // Set the environment variables to configure the source and sink for the Traces and Alerts modules.
        Map<String, String> env = new HashMap<>();
        env.put(Traces.SOURCE_ENV, source.uri());
        env.put(Traces.PARTITION_ENV, String.valueOf(partition.id()));
        env.put(Traces.WINDOW_START_ENV, String.valueOf(startTime));
        env.put(Traces.WINDOW_END_ENV, String.valueOf(endTime));
        env.put(Alerts.SINK_ENV, sink.uri());
        env.put("CLASSPATH", String.format("%s:%s", System.getProperty("java.class.path"), config.getDirectory().getAbsolutePath()));

        // Modify the TLC arguments to ensure the metadir is writable and TLC does not exit on invariant violations.
        List<String> args = new ArrayList<>(config.getArgs());
        if (!args.contains("-metadir")) {
            args.add("-metadir");
            args.add(String.format("/opt/tlaplus/data/%d", partition.id()));
        }
        if (!args.contains("-continue")) {
            args.add("-continue");
        }

        LOGGER.log("Starting TLC process...");
        runner.start(args, env);
        runner.join();
    }
}
