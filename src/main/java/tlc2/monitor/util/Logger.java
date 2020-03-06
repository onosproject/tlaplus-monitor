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
package tlc2.monitor.util;

/**
 * Interface for TLCB loggers.
 */
public interface Logger {
    /**
     * Logs a raw message to the logger output.
     *
     * @param message the message to log
     */
    void log(String message);

    /**
     * Logs a formatted message to the logger output.
     *
     * @param message the message to log
     * @param args the format arguments
     */
    void log(String message, Object... args);
}
