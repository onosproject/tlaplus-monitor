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

import tlc2.overrides.JsonUtils;
import tlc2.value.IValue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * File sink.
 */
public class FileSink implements Sink {
    static final String SCHEME = "file";

    private final String uri;
    private final FileWriter writer;

    public FileSink(String uri) throws URISyntaxException, IOException {
        URI sink = new URI(uri);
        if (!sink.getScheme().equals(SCHEME)) {
            throw new IllegalStateException("Unknown sink scheme " + sink.getScheme());
        }

        this.uri = uri;
        this.writer = new FileWriter(new File(sink.getPath()));
    }

    @Override
    public String uri() {
        return uri;
    }

    @Override
    public void produce(IValue value) throws IOException {
        writer.write(JsonUtils.getNode(value).toString() + "\n");
    }

    @Override
    public String toString() {
        return uri;
    }
}
