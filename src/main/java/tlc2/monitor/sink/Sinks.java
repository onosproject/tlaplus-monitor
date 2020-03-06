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
package tlc2.monitor.sink;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Sink utilities.
 */
public class Sinks {
    /**
     * Gets the sink for the given URI.
     *
     * @param sink the sink URI
     * @return the sink
     */
    public static Sink getSink(String sink) throws URISyntaxException, IOException {
        URI uri = new URI(sink);
        switch (uri.getScheme()) {
            case KafkaSink.SCHEME:
                return new KafkaSink(sink);
            case FileSink.SCHEME:
                return new FileSink(sink);
            default:
                throw new IllegalArgumentException("Unknown sink scheme " + uri.getScheme());
        }
    }
}
