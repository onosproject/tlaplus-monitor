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
import tlc2.monitor.source.Source;
import tlc2.monitor.util.ConsoleLogger;
import tlc2.monitor.util.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Monitors a source and evaluates an infinite stream of traces using TLC.
 * <p>
 * Sources can consume traces from multiple partitions. The source monitor assumes traces within a partition
 * are ordered, but traces cannot be ordered across partitions. A {@link PartitionMonitor} is run for each
 * partition provided by the source.
 */
final class SourceMonitor {
    private static final Logger LOGGER = new ConsoleLogger();

    private final Source source;
    private final Sink sink;
    private final TLCMonitorConfig config;

    SourceMonitor(Source source, Sink sink, TLCMonitorConfig config) {
        this.source = source;
        this.sink = sink;
        this.config = config;
    }

    /**
     * Runs the source checker.
     */
    public void run() throws Exception {
        List<Thread> threads = new ArrayList<>(source.getPartitions().size());
        for (Partition partition : source.getPartitions()) {
            Thread thread = new Thread(() -> monitor(partition));
            thread.start();
            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }

    /**
     * Starts the monitor for the given partition.
     *
     * @param partition the partition for which to run the monitor
     */
    private void monitor(Partition partition) {
        PartitionMonitor checker = new PartitionMonitor(partition, source, sink, config);
        LOGGER.log("Starting partition monitor for %s partition %d", source, partition.id());
        try {
            checker.run();
        } catch (Exception e) {
            LOGGER.log("An error occurred while monitoring %s partition %d", source, partition.id());
            e.printStackTrace();
        }
    }
}
