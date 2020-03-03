package tlcb;

import tlc2.overrides.source.Partition;
import tlc2.overrides.source.Source;
import tlc2.overrides.source.Sources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TLCB processes a TLC stream in batches.
 */
public class TLCB {
    public static void main(String[] args) throws Exception {
        String source = null;
        String sink = null;
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
        new TLCB(source, sink, tlcArgs).run();
    }

    private final String source;
    private final String sink;
    private final List<String> args;

    public TLCB(String source, String sink, List<String> args) {
        this.source = source;
        this.sink = sink;
        this.args = args;
    }

    /**
     * Runs the batch TLC model checker.
     */
    public void run() throws Exception {
        Source source = Sources.getSource(this.source);
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
            env.put("ALERT_SINK", this.sink);
            runner.start(args, env);
            runners.add(runner);
        }

        for (TLCRunner runner : runners) {
            runner.join();
        }
    }
}
