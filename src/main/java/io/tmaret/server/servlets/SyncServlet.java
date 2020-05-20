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
import java.io.InputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.tmaret.server.work.Cpu;
import io.tmaret.server.work.Idle;
import io.tmaret.server.work.Io;

import static io.tmaret.server.work.Cpu.DEFAULT_CPU_ITERATIONS;
import static io.tmaret.server.work.Idle.DEFAULT_DELAY;
import static io.tmaret.server.work.Io.DEFAULT_FILE_LENGTH;
import static java.lang.Integer.parseInt;
import static java.util.Objects.requireNonNull;
import static javax.servlet.http.HttpServletResponse.SC_OK;

/**
 * This servlet leverages sync processing and blocking IO.
 */
public class SyncServlet extends HttpServlet {

    private final Cpu cpu;

    private final Io io;

    private final Idle idle;

    public SyncServlet(Cpu cpu, Io io, Idle idle) {
        this.cpu = requireNonNull(cpu);
        this.io = requireNonNull(io);
        this.idle = requireNonNull(idle);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setStatus(SC_OK);
        res.setContentType("text/plain");
        res.setCharacterEncoding("utf-8");
        res.addHeader("X-Request-ID",
                cpu.process(getParameter(req, "cpuIterations", DEFAULT_CPU_ITERATIONS)));
        res.addHeader("X-Latency",
                idle.process(getParameter(req, "idleDelay", DEFAULT_DELAY)));
        int fileLength = getParameter(req, "fileLength", DEFAULT_FILE_LENGTH);
        res.setContentLength(fileLength);

        try (InputStream is = io.inputstream(fileLength)) {
            is.transferTo(res.getOutputStream());
        }
    }

    static int getParameter(HttpServletRequest req, String paramName, int defaultValue) {
        return (req.getParameter(paramName) != null) ? parseInt(req.getParameter(paramName)) : defaultValue;
    }

}
