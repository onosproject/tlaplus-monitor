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

import tlc2.TLC;
import tlc2.monitor.sink.KafkaSink;
import tlc2.monitor.sink.Sink;
import tlc2.monitor.source.KafkaSource;
import tlc2.monitor.source.Source;
import tlc2.monitor.util.ConsoleLogger;
import tlc2.monitor.util.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TLCB processes a TLC stream in batches.
 */
public class TLCMonitor {
    private static final Logger LOGGER = new ConsoleLogger();
    private static final Pattern DURATION_PATTERN = Pattern.compile("([0-9]+)([dhms])");

    private static final long DEFAULT_WINDOW = 1000 * 60;

    /**
     * Runs the TLC monitor.
     *
     * @param args the monitor arguments
     */
    public static void main(String[] args) throws Exception {
        // Parse the command line arguments.
        Arguments arguments = parseArguments(args);

        // If monitoring is enabled, run the TLC monitor.
        // Otherwise, run the TLC main with the original arguments.
        if (arguments.monitor) {
            if (arguments.source == null) {
                LOGGER.log("-source flag required");
                System.exit(1);
            }

            // A KafkaSource and KafkaSink are explicitly constructed from the source and sink URIs...
            // TODO: Support additional sources and sinks based on the URI scheme
            final Source source = new KafkaSource(arguments.source);
            final Sink sink = arguments.sink != null ? new KafkaSink(arguments.sink) : null;

            // Run the TLC monitor
            new TLCMonitor(source, sink, arguments.window, arguments.args).run();
        } else {
            TLC.main(args);
        }
    }

    private final Source source;
    private final Sink sink;
    private final long window;
    private final List<String> args;

    public TLCMonitor(Source source, Sink sink, long window, List<String> args) {
        this.source = source;
        this.sink = sink;
        this.window = window;
        this.args = args;
    }

    /**
     * Runs the batch TLC model checker.
     */
    public void run() throws Exception {
        SourceMonitor monitor = new SourceMonitor(source, sink, window, args);
        monitor.run();
    }

    /**
     * Parses command line arguments for the TLC monitor.
     *
     * @param args the arguments to parse
     * @return the parsed arguments
     */
    private static Arguments parseArguments(String[] args) {
        boolean monitor = false;
        String source = null;
        String sink = null;
        Long window = null;
        List<String> tlcArgs = new ArrayList<>();

        int i = 0;
        while (i < args.length) {
            switch (args[i]) {
                case "-monitor":
                    monitor = true;
                    i += 1;
                    break;
                case "-source":
                    source = args[i + 1];
                    i += 2;
                    break;
                case "-sink":
                    sink = args[i + 1];
                    i += 2;
                    break;
                case "-window":
                    window = parseWindow(args[i + 1]);
                    i += 2;
                    break;
                default:
                    if (args[i].startsWith("-")) {
                        tlcArgs.add(args[i]);
                        tlcArgs.add(args[i + 1]);
                    } else {
                        tlcArgs.add(args[i]);
                        i++;
                    }
                    break;
            }
        }
        if (window == null) {
            window = DEFAULT_WINDOW;
        }
        return new Arguments(monitor, source, sink, window, tlcArgs);
    }

    /**
     * Parses a window argument.
     *
     * @param arg the string window argument
     * @return the parsed window length in milliseconds
     */
    private static long parseWindow(String arg) {
        Matcher matcher = DURATION_PATTERN.matcher(arg.toLowerCase());
        long window = 0;
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);
            switch (unit) {
                case "d":
                    window += Duration.ofDays(value).toMillis();
                    break;
                case "h":
                    window += Duration.ofHours(value).toMillis();
                    break;
                case "m":
                    window += Duration.ofMinutes(value).toMillis();
                    break;
                case "s":
                    window += Duration.ofSeconds(value).toMillis();
                    break;
            }
        }
        return window;
    }

    /**
     * Command line arguments.
     */
    private static class Arguments {
        final boolean monitor;
        final String source;
        final String sink;
        final long window;
        final List<String> args;

        Arguments(boolean monitor, String source, String sink, long window, List<String> args) {
            this.monitor = monitor;
            this.source = source;
            this.sink = sink;
            this.window = window;
            this.args = args;
        }
    }
}
