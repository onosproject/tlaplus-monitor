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

import tlc2.value.IValue;

import java.io.IOException;

/**
 * Monitoring sink.
 */
public interface Sink {

    /**
     * Returns the sink URI.
     *
     * @return the sink URI
     */
    String uri();

    /**
     * Produces a value.
     *
     * @param value the value to produce
     */
    void produce(IValue value) throws IOException;
}
