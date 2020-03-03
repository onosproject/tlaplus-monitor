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

import java.util.Collection;

/**
 * Consumer is a monitoring consumer.
 */
public interface Source {

    /**
     * Returns a partition by ID.
     *
     * @param partition the partition ID
     * @return the partition
     */
    Partition getPartition(int partition);

    /**
     * Returns the partitions for the source.
     *
     * @return the partitions for the source
     */
    Collection<Partition> getPartitions();
}
