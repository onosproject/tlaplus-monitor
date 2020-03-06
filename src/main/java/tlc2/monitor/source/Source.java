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

import java.util.Collection;

/**
 * Monitoring source.
 * <p>
 * Sources may provide one or many partitions. Each partition is assumed to provide a global total order
 * to provide deterministic traces for the model checker. Order is not assumed across partitions.
 */
public interface Source {

    /**
     * Returns the source URI.
     *
     * @return the source URI
     */
    String uri();

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
