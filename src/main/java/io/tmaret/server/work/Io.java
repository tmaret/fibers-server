/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.tmaret.server.work;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.Files.newInputStream;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;

public class Io {

    public static final int DEFAULT_FILE_LENGTH = 100 * 1024;

    private final Map<Integer, Path> files = new ConcurrentHashMap<>();

    public FileChannel fileChannel(int fileLength) {
        try {
            return new RandomAccessFile(files.computeIfAbsent(fileLength,
                    this::createFile).toFile(), "r").getChannel();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream inputstream(int fileLength) {
        try {
            return newInputStream(files.computeIfAbsent(fileLength,
                    this::createFile));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Path createFile(int fileLength) {
        try {
            Path filePath = createTempFile("io-busy-" + fileLength, ".tmp");
            try (BufferedWriter writer = newBufferedWriter(filePath)) {
                writer.write(randomNumeric(fileLength));
                writer.flush();
            }
            return filePath;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
