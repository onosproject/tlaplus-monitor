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
package tlc2.overrides;

import tlc2.monitor.sink.KafkaSink;
import tlc2.monitor.sink.Sink;
import tlc2.monitor.util.Logger;
import tlc2.monitor.util.ModuleLogger;
import tlc2.value.IValue;
import tlc2.value.impl.BoolValue;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * TLA+ module for publishing alerts to a configurable sink.
 */
public class Alerts {
    private static final Logger LOGGER = new ModuleLogger();

    public static final String SINK_ENV = "TLC_ALERTS_SINK";

    private static final Sink SINK;

    static {
        String uri = System.getenv(SINK_ENV);
        if (uri != null) {
            Sink sink = null;
            try {
                sink = new KafkaSink(uri);
            } catch (URISyntaxException | IOException e) {
                LOGGER.log("Failed to load sink %s: %s", uri, e);
                e.printStackTrace();
            }
            SINK = sink;
        } else {
            SINK = null;
        }
    }

    private static void assertSink() {
        if (SINK == null) {
            throw new IllegalStateException("No sink configured. Are you sure TLC is running in monitor mode?");
        }
    }

    @TLAPlusOperator(identifier = "Alert", module = "Alerts")
    public static synchronized IValue alert(IValue value) throws IOException {
        assertSink();
        SINK.produce(value);
        return BoolValue.ValTrue;
    }
}
