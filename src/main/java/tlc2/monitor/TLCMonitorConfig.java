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

import java.io.File;
import java.util.List;

/**
 * TLC monitor configuration.
 */
final class TLCMonitorConfig {
    private final boolean monitor;
    private final File moduleDir;
    private final File metaDir;
    private final String source;
    private final String sink;
    private final long window;
    private final List<String> args;

    TLCMonitorConfig(boolean monitor, File moduleDir, File metaDir, String source, String sink, long window, List<String> args) {
        this.monitor = monitor;
        this.moduleDir = moduleDir;
        this.metaDir = metaDir;
        this.source = source;
        this.sink = sink;
        this.window = window;
        this.args = args;
    }

    /**
     * Returns whether monitoring is enabled.
     *
     * @return whether monitoring is enabled
     */
    boolean isMonitoringEnabled() {
        return monitor;
    }

    /**
     * Returns the module directory.
     *
     * @return the module directory
     */
    File getModuleDir() {
        return moduleDir;
    }

    /**
     * Returns the metadata directory.
     *
     * @return the metadata directory
     */
    File getMetaDir() {
        return metaDir;
    }

    /**
     * Returns the source URI.
     *
     * @return the source URI
     */
    String getSource() {
        return source;
    }

    /**
     * Returns the sink URI.
     *
     * @return the sink URI
     */
    String getSink() {
        return sink;
    }

    /**
     * Returns the sliding window length.
     *
     * @return the sliding window length in milliseconds
     */
    long getWindow() {
        return window;
    }

    /**
     * Returns the TLC arguments.
     *
     * @return the TLC arguments
     */
    List<String> getArgs() {
        return args;
    }
}
