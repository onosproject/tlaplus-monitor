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
import tlc2.output.SpecWriterUtilities;
import util.TLAConstants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TLCB processes a TLC stream in batches.
 */
public class TLCMonitor {
    private static final Pattern DURATION_PATTERN = Pattern.compile("([0-9]+)([dhms])");
    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");
    private static final long DEFAULT_WINDOW = 1000 * 60;

    /**
     * Runs the TLC monitor.
     *
     * @param args the monitor arguments
     */
    public static void main(String[] args) throws Exception {
        // Parse the command line arguments.
        TLCMonitorConfig config = parseArgs(args);

        // If monitoring is enabled, run the TLC monitor.
        // Otherwise, run the TLC main with the original arguments.
        if (config.isMonitoringEnabled()) {
            // A KafkaSource and KafkaSink are explicitly constructed from the source and sink URIs...
            // TODO: Support additional sources and sinks based on the URI scheme
            final Source source = new KafkaSource(config.getSource());
            final Sink sink = config.getSink() != null ? new KafkaSink(config.getSink()) : null;

            // Run the TLC monitor
            new TLCMonitor(source, sink, config).run();
        } else {
            TLC.main(args);
        }
    }

    private final Source source;
    private final Sink sink;
    private final TLCMonitorConfig config;

    public TLCMonitor(Source source, Sink sink, TLCMonitorConfig config) {
        this.source = source;
        this.sink = sink;
        this.config = config;
    }

    /**
     * Runs the batch TLC model checker.
     */
    public void run() throws Exception {
        SourceMonitor monitor = new SourceMonitor(source, sink, config);
        monitor.run();
    }

    /**
     * Parses command line arguments for the TLC monitor.
     *
     * @param args the arguments to parse
     * @return the parsed arguments
     */
    private static TLCMonitorConfig parseArgs(String[] args) throws IOException {
        boolean monitor = false;
        String source = null;
        String sink = null;
        long window = DEFAULT_WINDOW;
        List<String> tlcArgs = new ArrayList<>();

        String spec = null;
        String config = null;

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
                case "-config":
                    config = args[i + 1];
                    i += 2;
                    break;
                default:
                    if (args[i].startsWith("-")) {
                        tlcArgs.add(args[i]);
                        tlcArgs.add(args[i + 1]);
                        i += 2;
                    } else {
                        spec = args[i];
                        i++;
                    }
                    break;
            }
        }

        if (spec == null) {
            throw new IllegalArgumentException("No TLA+ specification provided");
        }

        // If monitoring is enabled, configure action constraints.
        File directory = new File(System.getProperty("user.dir"));
        if (monitor) {
            // If no source is configured, throw an exception.
            if (source == null) {
                throw new IllegalArgumentException("-source must be specified when -monitor is enabled");
            }

            // If no configuration was set, check if a model configuration exists.
            File specFile = spec.endsWith(".tla") ? new File(spec) : new File(spec + ".tla");
            File configFile = null;
            if (config == null) {
                File file = new File(specFile.getParent(), specFile.getName().replace(".tla", ".cfg"));
                if (file.exists()) {
                    configFile = file;
                }
            } else {
                configFile = config.endsWith(".cfg") ? new File(config) : new File(config + ".cfg");
            }

            // If a configuration is set, modify it to add the sliding window constraint.
            if (configFile != null) {
                // Create a temporary root module and configuration file.
                directory = specFile.getParentFile();
                File tempSpecFile = new File(TMP_DIR, "MC.tla");
                Files.copy(specFile.toPath(), tempSpecFile.toPath());
                File tempConfigFile = new File(TMP_DIR, "MC.cfg");
                if (!tempConfigFile.createNewFile()) {
                    throw new IllegalStateException("Failed to create temporary configuration file");
                }

                // Create the temporary module and configuration file with an action constraint.
                ModelWriter writer = new ModelWriter();
                writer.copyConfig(configFile);
                writer.addPrimer("MC", specFile.getName().replace(".tla", ""));
                writer.addFormulaList(
                    SpecWriterUtilities.createSourceContent(
                        "~UpperBound",
                        TLAConstants.Schemes.ACTIONCONSTRAINT_SCHEME),
                    TLAConstants.KeyWords.ACTION_CONSTRAINT,
                    WINDOW_CONSTRAINT);
                writer.writeFiles(tempSpecFile, tempConfigFile);

                // Set the root module and configuration.
                tlcArgs.add("-config");
                tlcArgs.add(tempConfigFile.getAbsolutePath());
                tlcArgs.add(tempSpecFile.getAbsolutePath());
            } else {
                tlcArgs.add(spec);
            }
        } else {
            tlcArgs.add(spec);
        }
        return new TLCMonitorConfig(monitor, directory, source, sink, window, tlcArgs);
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

    private static final String WINDOW_CONSTRAINT = "windowConstraint";

    /**
     * Builds the module and model configuration.
     */
    private static void buildModel(String rootModule, File specFile, File configFile) throws IOException {
        ModelWriter writer = new ModelWriter();
        writer.addPrimer(TLAConstants.Files.MODEL_CHECK_FILE_BASENAME, rootModule);
        String constraintValue = "~UpperBound";
        writer.addFormulaList(
            SpecWriterUtilities.createSourceContent(
                constraintValue,
                TLAConstants.Schemes.ACTIONCONSTRAINT_SCHEME),
            TLAConstants.KeyWords.ACTION_CONSTRAINT,
            WINDOW_CONSTRAINT);
        writer.writeFiles(specFile, configFile);
    }
}
