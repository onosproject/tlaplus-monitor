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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * File source.
 */
public class FileSource implements Source {
    static final String SCHEME = "file";

    private final String uri;
    private final File file;
    private final Map<Integer, Partition> partitions = new ConcurrentHashMap<>();

    public FileSource(String uri) throws URISyntaxException, IOException {
        URI source = new URI(uri);
        if (!source.getScheme().equals(SCHEME)) {
            throw new IllegalStateException("Unknown source scheme " + source.getScheme());
        }

        this.uri = uri;
        this.file = new File(source.getPath());

        String timestampField = "timestamp";
        SimpleDateFormat timestampFormat = null;
        String query = source.getQuery();
        String[] queries = query.split("&");
        for (String q : queries) {
            String[] keyValue = q.split("=");
            if (keyValue[0].equals("timestamp")) {
                timestampField = keyValue[1];
            } else if (keyValue[0].equals("format")) {
                timestampFormat = new SimpleDateFormat(keyValue[1]);
            }
        }
        if (file.isFile()) {
            partitions.put(1, new FilePartition(1, file, timestampField, timestampFormat));
        } else {
            int id = 1;
            for (File child : file.listFiles()) {
                if (child.isFile()) {
                    partitions.put(id, new FilePartition(id, child, timestampField, timestampFormat));
                    id++;
                }
            }
        }
    }

    @Override
    public String uri() {
        return uri;
    }

    @Override
    public Partition getPartition(int partition) {
        return partitions.get(partition);
    }

    @Override
    public Collection<Partition> getPartitions() {
        return partitions.values();
    }

    @Override
    public String toString() {
        return uri;
    }
}
