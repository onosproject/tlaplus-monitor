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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import tlc2.overrides.JsonUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * File partition.
 */
public class FilePartition implements Partition {
    private final int id;
    private final File file;
    private final List<TimestampedNode> nodes;

    FilePartition(int id, File file, String timestampField, SimpleDateFormat timestampFormat) throws IOException {
        this.id = id;
        this.file = file;
        this.nodes = parseFile(file, timestampField, timestampFormat);
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public long indexOf(long timestamp) throws IOException {
        for (int i = 0; i < nodes.size(); i++) {
            TimestampedNode node = nodes.get(i);
            if (node.timestamp >= timestamp) {
                return i + 1;
            }
        }
        return nodes.size();
    }

    @Override
    public Record get(long index) throws IOException {
        if (index > nodes.size()) {
            return null;
        }
        TimestampedNode node = nodes.get((int) index - 1);
        return new Record(index, JsonUtils.getValue(node.node), node.timestamp);
    }

    @Override
    public String toString() {
        return file.getAbsolutePath();
    }

    private static List<TimestampedNode> parseFile(File file, String timestampField, SimpleDateFormat timestampFormat) throws IOException {
        List<TimestampedNode> nodes = new CopyOnWriteArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                JsonNode node = mapper.readTree(line);
                if (node.getNodeType() != JsonNodeType.OBJECT) {
                    throw new IOException("Failed to parse JSON: not an object");
                }

                long timestamp;
                JsonNode timestampValue = node.get(timestampField);
                if (timestampValue.getNodeType() == JsonNodeType.NUMBER) {
                    timestamp = timestampValue.asLong();
                } else {
                    if (timestampFormat == null) {
                        throw new IOException("Failed to parse JSON timestamp: no format specified");
                    }
                    try {
                        timestamp = timestampFormat.parse(timestampValue.asText()).getTime();
                    } catch (ParseException e) {
                        throw new IOException("Failed to parse JSON timestamp", e);
                    }
                }
                nodes.add(new TimestampedNode(timestamp, node));
            }
        }
        return nodes;
    }

    private static class TimestampedNode {
        private final long timestamp;
        private final JsonNode node;

        TimestampedNode(long timestamp, JsonNode node) {
            this.timestamp = timestamp;
            this.node = node;
        }
    }
}
