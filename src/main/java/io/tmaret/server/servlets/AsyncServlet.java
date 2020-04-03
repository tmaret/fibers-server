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
package io.tmaret.server.servlets;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

import javax.servlet.AsyncContext;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.HttpOutput;

import io.tmaret.server.work.Cpu;
import io.tmaret.server.work.Idle;
import io.tmaret.server.work.Io;

import static java.nio.channels.FileChannel.MapMode.READ_ONLY;
import static java.util.Objects.requireNonNull;
import static javax.servlet.http.HttpServletResponse.SC_OK;

/**
 * This servlet leverages async processing, non blocking IO
 * from the Servlet spec 3.1 and Jetty's direct ByteBuffer
 * capability.
 */
public class AsyncServlet extends HttpServlet {

    private final Cpu cpu;

    private final Io io;

    private final Idle idle;

    public AsyncServlet(Cpu cpu, Io io, Idle idle) {
        this.cpu = requireNonNull(cpu);
        this.io = requireNonNull(io);
        this.idle = requireNonNull(idle);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) {

        final AsyncContext async = req.startAsync();
        async.start(() -> {
            try {
                res.setStatus(SC_OK);
                res.setContentType("text/plain");
                res.setCharacterEncoding("utf-8");
                res.setContentLength(io.getFileLength());
                res.addHeader("X-Request-ID", cpu.process());
                res.addHeader("X-Latency", idle.process());

                HttpOutput sos = (HttpOutput) res.getOutputStream();
                MappedByteBuffer buffer = io.fileChannel().map(READ_ONLY, 0, io.getFileLength());
                ByteBuffer data = buffer.asReadOnlyBuffer();
                sos.setWriteListener(new WriteListener() {

                    @Override
                    public void onWritePossible() throws IOException {
                        while (sos.isReady()) {
                            if (!data.hasRemaining()) {
                                async.complete();
                                return;
                            }
                            sos.write(data);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        async.complete();
                    }
                });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}
