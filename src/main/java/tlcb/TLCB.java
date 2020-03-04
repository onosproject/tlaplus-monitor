package tlcb;

import tlc2.overrides.source.Partition;
import tlc2.overrides.source.Record;
import tlc2.overrides.source.Source;
import tlc2.overrides.source.Sources;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TLCB processes a TLC stream in batches.
 */
public class TLCB {
    private static final long DEFAULT_WINDOW = 1000 * 60;

    public static void main(String[] args) throws Exception {
        String source = null;
        String sink = null;
        Long window = null;
        List<String> tlcArgs = new ArrayList<>();
        int i = 0;
        while (i < args.length) {
            switch (args[i]) {
                case "-source":
                    source = args[i + 1];
                    i += 2;
                    break;
                case "-sink":
                    sink = args[i + 1];
                    i += 2;
                    break;
                case "-window":
                    window = Duration.parse(args[i + 1]).toMillis();
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
        new TLCB(source, sink, window, tlcArgs).run();
    }

    private final String source;
    private final String sink;
    private final long window;
    private final List<String> args;

    public TLCB(String source, String sink, long window, List<String> args) {
        this.source = source;
        this.sink = sink;
        this.window = window;
        this.args = args;
    }

    /**
     * Runs the batch TLC model checker.
     */
    public void run() throws Exception {
        Source source = Sources.getSource(this.source);
        long nextWindow = getStartTime(source);
        for (; ; ) {
            final long window = nextWindow;
            new Thread(() -> {
                try {
                    runWindow(source, window);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
            nextWindow += window;
            Thread.sleep(nextWindow - System.currentTimeMillis());
        }
    }

    /**
     * Computes the start time.
     */
    private long getStartTime(Source source) throws IOException {
        long startTime = System.currentTimeMillis();
        for (Partition partition : source.getPartitions()) {
            long firstOffset = partition.offset(0);
            if (firstOffset != 0) {
                Record firstRecord = partition.get(firstOffset);
                startTime = Math.min(startTime, firstRecord.timestamp());
            }
        }
        return startTime;
    }

    /**
     * Runs the given window.
     */
    private void runWindow(Source source, long startTime) throws Exception {
        List<TLCRunner> runners = new ArrayList<>(source.getPartitions().size());
        for (Partition partition : source.getPartitions()) {
            TLCRunner runner = new TLCRunner();

            List<String> args = new ArrayList<>(this.args);
            args.add("-metadir");
            args.add(String.format("/opt/tlaplus/data/%d", partition.id()));
            args.add("-continue");

            Map<String, String> env = new HashMap<>();
            env.put("TRACE_SOURCE", this.source);
            env.put("TRACE_PARTITION", String.valueOf(partition.id()));
            env.put("TRACE_START_TIME", String.valueOf(startTime));
            env.put("TRACE_END_TIME", String.valueOf(startTime + window));
            env.put("ALERT_SINK", this.sink);

            runner.start(args, env);
            runners.add(runner);
        }

        for (TLCRunner runner : runners) {
            runner.join();
        }
    }
}
