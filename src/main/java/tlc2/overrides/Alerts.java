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

import tlc2.overrides.monitoring.KafkaProducer;
import tlc2.overrides.monitoring.Producer;
import tlc2.value.IValue;
import tlc2.value.impl.BoolValue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Alert utilities.
 */
public class Alerts {
    private static final String ALERT_URI = "ALERT_URI";

    private static final Producer producer;

    static {
        String producerInfo = System.getenv(ALERT_URI);
        if (producerInfo != null) {
            try {
                URI producerUri = new URI(producerInfo);
                switch (producerUri.getScheme()) {
                    case "kafka":
                        producer = new KafkaProducer(producerUri.getAuthority(), producerUri.getPort(), producerUri.getPath());
                        break;
                    default:
                        throw new IllegalStateException("unknown producer scheme");
                }
            } catch (URISyntaxException | IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            producer = null;
        }
    }

    @TLAPlusOperator(identifier = "Alert", module = "Alerts")
    public static synchronized IValue alert(IValue value) throws IOException {
        if (producer == null) {
            throw new IllegalStateException("No producer configured. Is TLC running in monitor mode?");
        }
        producer.produce(value);
        return BoolValue.ValTrue;
    }
}
