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

import tlc2.overrides.source.KafkaSource;
import tlc2.overrides.source.Source;
import tlc2.value.impl.Value;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Trace utilities.
 */
public class Traces {
    private static final String TRACES_SOURCE = "TRACES_SOURCE";

    private static final Source SOURCE;

    static {
        String consumerInfo = System.getenv(TRACES_SOURCE);
        if (consumerInfo != null) {
            try {
                URI consumerUri = new URI(consumerInfo);
                switch (consumerUri.getScheme()) {
                    case "kafka":
                        String path = consumerUri.getPath().substring(1);
                        if (path.equals("")) {
                            throw new IllegalStateException("No topic specified");
                        }
                        SOURCE = new KafkaSource(consumerUri.getHost(), consumerUri.getPort(), path);
                        break;
                    default:
                        throw new IllegalStateException("Unknown consumer scheme");
                }
            } catch (URISyntaxException | IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            SOURCE = null;
        }
    }

    @TLAPlusOperator(identifier = "NextTrace", module = "Traces")
    public static synchronized Value nextTrace() throws IOException {
        if (SOURCE == null) {
            throw new IllegalStateException("No consumer configured. Is TLC running in monitor mode?");
        }
        return (Value) SOURCE.consume();
    }
}
