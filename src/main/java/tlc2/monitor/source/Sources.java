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
package tlc2.monitor.source;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Source utilities.
 */
public final class Sources {
    /**
     * Gets the source for the given URI.
     *
     * @param source the source URI
     * @return the source
     */
    public static Source getSource(String source) throws URISyntaxException, IOException {
        URI uri = new URI(source);
        switch (uri.getScheme()) {
            case KafkaSource.SCHEME:
                return new KafkaSource(source);
            case FileSource.SCHEME:
                return new FileSource(source);
            default:
                throw new IllegalArgumentException("Unknown source scheme " + uri.getScheme());
        }
    }
}
