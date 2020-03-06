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

/**
 * Source partition.
 */
public interface Partition {

    /**
     * Returns the partition identifier.
     *
     * @return the partition identifier
     */
    int id();

    /**
     * Returns the position of the first offset exceeding the given timestamp.
     *
     * @param timestamp the timestamp for which to search
     * @return the first offset exceeding the given timestamp
     */
    long offset(long timestamp) throws IOException;

    /**
     * Gets the given offset.
     *
     * @param offset the offset to get
     * @return the record
     * @throws IOException if the get fails
     */
    Record get(long offset) throws IOException;
}
