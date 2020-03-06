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
package tlc2.monitor;

import tlc2.output.AbstractSpecWriter;
import util.TLAConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Model writer.
 */
final class ModelWriter extends AbstractSpecWriter {
    ModelWriter() {
        super(true);
    }

    /**
     * Copies the given configuration file to the model buffer.
     *
     * @param configFile the configuration file to copy
     */
    void copyConfig(File configFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                cfgBuffer.append(line).append(TLAConstants.CR);
            }
        }
    }
}
