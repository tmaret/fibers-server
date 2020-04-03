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

import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.Files.newInputStream;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;

public class Io {

    private final Path filePath;

    private final int fileLength;

    public Io(int fileLength) {
        this.fileLength = fileLength;
        try {
            filePath = createTempFile("io-busy", ".tmp");
            try (BufferedWriter writer = newBufferedWriter(filePath)) {
                writer.write(randomNumeric(fileLength));
                writer.flush();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public int getFileLength() {
        return fileLength;
    }

    public FileChannel fileChannel() {
        try {
            return new RandomAccessFile(filePath.toFile(), "r").getChannel();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream inputstream() {
        try {
            return newInputStream(filePath);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
