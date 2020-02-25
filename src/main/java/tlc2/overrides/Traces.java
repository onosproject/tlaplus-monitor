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

import tlc2.overrides.monitoring.Consumer;
import tlc2.overrides.monitoring.KafkaConsumer;
import tlc2.value.IValue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Trace utilities.
 */
public class Traces {
    private static final String TRACE_URI = "TRACE_URI";

    private static final Consumer consumer;

    static {
        String consumerInfo = System.getenv(TRACE_URI);
        if (consumerInfo != null) {
            try {
                URI consumerUri = new URI(consumerInfo);
                switch (consumerUri.getScheme()) {
                    case "kafka":
                        consumer = new KafkaConsumer(consumerUri.getAuthority(), consumerUri.getPort(), consumerUri.getPath());
                        break;
                    default:
                        throw new IllegalStateException("unknown consumer scheme");
                }
            } catch (URISyntaxException | IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            consumer = null;
        }
    }

    @TLAPlusOperator(identifier = "NextTrace", module = "Traces")
    public static synchronized IValue nextTrace() throws IOException {
        if (consumer == null) {
            throw new IllegalStateException("No consumer configured. Is TLC running in monitor mode?");
        }
        return consumer.consume();
    }
}
