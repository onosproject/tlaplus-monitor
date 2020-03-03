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
package tlc2.overrides.source;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Sources.
 */
public class Sources {
    /**
     * Returns the source for the given URI.
     *
     * @param uri the source URI
     * @return the source
     */
    public static Source getSource(String uri) {
        try {
            URI source = new URI(uri);
            switch (source.getScheme()) {
                case "kafka":
                    String path = source.getPath().substring(1);
                    if (path.equals("")) {
                        throw new IllegalStateException("No topic specified");
                    }
                    return new KafkaSource(source.getHost(), source.getPort(), path);
                default:
                    throw new IllegalStateException("Unknown consumer scheme");
            }
        } catch (URISyntaxException | IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
