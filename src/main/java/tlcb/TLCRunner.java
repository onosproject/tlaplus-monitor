package tlcb;

import tlc2.TLC;
import tlc2.output.EC;
import tlc2.tool.fp.FPSetFactory;

import java.io.*;
import java.util.*;

/**
 * Runs TLC in a separate process.
 */
class TLCRunner {
    private static final String TLC_CLASS = TLC.class.getName();
    private static final List<String> JVM_ARGS = Arrays.asList(
        "-XX:+UseParallelGC",
        "-Dfile.encoding=UTF-8",
        "-Dtlc2.tool.fp.FPSet.impl=" + FPSetFactory.getImplementationDefault(),
        "-DTLA-Library=/opt/tlaplus/lib/tlaplus-monitor-0.1-jar-with-dependencies.jar"
    );

    private Process process;
    private StreamPump stdOut;
    private StreamPump stdErr;

    /**
     * Starts the TLC process.
     */
    void start(Collection<String> args, Map<String, String> env) throws IOException {
        final ProcessBuilder processBuilder = createProcess(args, env);
        process = processBuilder.start();

        final BufferedInputStream stdOutReader = new BufferedInputStream(process.getInputStream());
        final BufferedInputStream stdErrReader = new BufferedInputStream(process.getErrorStream());

        stdOut = new StreamPump(stdOutReader, System.out);
        stdErr = new StreamPump(stdErrReader, System.err);

        stdOut.start();
        stdErr.start();
    }

    /**
     * Waits for the TLC process to complete.
     *
     * @return the exit code
     */
    int join() {
        try {
            process.waitFor();
            return process.exitValue();
        } catch (final InterruptedException ie) {
            System.out.println("TLC process was interrupted: " + ie.getMessage());
        } finally {
            stdOut.stop();
            stdErr.stop();
        }
        return EC.ExitStatus.ERROR_SYSTEM;
    }

    private ProcessBuilder createProcess(Collection<String> args, Map<String, String> env) {
        final boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        final String jvm = System.getProperty("java.home")
            + File.separator
            + "bin"
            + File.separator
            + "java"
            + (isWindows ? ".exe" : "");
        final List<String> command = new ArrayList<String>();
        command.add(jvm);
        command.addAll(JVM_ARGS);
        command.add(TLC_CLASS);
        command.addAll(args);

        final ProcessBuilder processBuilder = new ProcessBuilder(command);
        final Map<String, String> environment = processBuilder.environment();
        environment.put("CLASSPATH", System.getProperty("java.class.path"));
        environment.putAll(env);

        return processBuilder;
    }


    private static class StreamPump implements Runnable {
        private static final int WAIT_SLEEP = 125;

        private final InputStream inputStream;
        private final OutputStream outputStream;

        private volatile boolean shouldStop;

        StreamPump(final InputStream is, final OutputStream os) {
            this.inputStream = is;
            this.outputStream = os;
            this.shouldStop = false;
        }

        void start() {
            new Thread(this).start();
        }

        public void run() {
            try {
                while (!shouldStop) {
                    while ((inputStream.available() > 0) && !shouldStop) {
                        if (outputStream != null) {
                            outputStream.write(inputStream.read());
                        } else {
                            inputStream.read();
                        }
                    }
                    if (outputStream != null) {
                        outputStream.flush();
                    }

                    try {
                        Thread.sleep(WAIT_SLEEP);
                    } catch (final Exception e) {
                    }
                }
            } catch (final Exception e) {
            }
        }

        void stop() {
            shouldStop = true;
        }
    }
}
