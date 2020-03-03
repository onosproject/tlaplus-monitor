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

import tlc2.overrides.sink.KafkaSink;
import tlc2.overrides.sink.Sink;
import tlc2.value.IValue;
import tlc2.value.impl.BoolValue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Alert utilities.
 */
public class Alerts {
    private static final String ALERT_SINK = "ALERT_SINK";

    private static final Sink SINK;

    static {
        String producerInfo = System.getenv(ALERT_SINK);
        if (producerInfo != null) {
            try {
                URI producerUri = new URI(producerInfo);
                switch (producerUri.getScheme()) {
                    case "kafka":
                        String path = producerUri.getPath().substring(1);
                        if (path.equals("")) {
                            throw new IllegalStateException("No topic specified");
                        }
                        SINK = new KafkaSink(producerUri.getHost(), producerUri.getPort(), path);
                        break;
                    default:
                        throw new IllegalStateException("Unknown producer scheme");
                }
            } catch (URISyntaxException | IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            SINK = null;
        }
    }

    @TLAPlusOperator(identifier = "PublishAlert", module = "Alerts")
    public static synchronized IValue publishAlert(IValue value) throws IOException {
        if (SINK == null) {
            throw new IllegalStateException("No producer configured. Is TLC running in monitor mode?");
        }
        SINK.produce(value);
        return BoolValue.ValTrue;
    }
}
