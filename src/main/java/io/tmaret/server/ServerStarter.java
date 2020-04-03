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
package io.tmaret.server;

import java.util.concurrent.Callable;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.tmaret.server.servlets.AsyncServlet;
import io.tmaret.server.servlets.SyncServlet;
import io.tmaret.server.work.Cpu;
import io.tmaret.server.work.Idle;
import io.tmaret.server.work.Io;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.defaultThreadFactory;

public class ServerStarter implements Callable<Integer> {

    private static final Logger LOG = LoggerFactory.getLogger(ServerStarter.class);

    @Option(names = { "-h", "--help" }, usageHelp = true, description = "Display a help message")
    private boolean helpRequested = false;

    @Option(names = {"-p", "--port"}, defaultValue = "9000", description = "The server port to listen for connections (default: ${DEFAULT-VALUE})")
    private int port = 9000;

    @Option(names = {"-c", "--cpu-iterations"}, defaultValue = "10000", description = "The number of SHA-256 iterations included in the response processing (default: ${DEFAULT-VALUE})")
    private int cpuIterations = 10_000;

    @Option(names = {"-f", "--file-size"}, defaultValue = "102400", description = "The size in Byte of the file included in the response (default: ${DEFAULT-VALUE})")
    private int fileLength = 100 * 1024;

    @Option(names = {"-d", "--delay-idle"}, defaultValue = "0", description = "The delay in ms to sleep when processing the response (default: ${DEFAULT-VALUE})")
    private long idleDelay = 0;

    @Option(names = {"-t", "--thread-factory"}, defaultValue = "kernel", description = "Thread factory implementation in: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})")
    private ThreadSupport threadSupport;

    private enum ThreadSupport {
        kernel,
        fibers
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new ServerStarter()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        ThreadFactory threadFactory = (threadSupport == ThreadSupport.fibers) ? fiberThreadFactory() : defaultThreadFactory();
        LOG.info("Start server on port {} with thread support: {}, cpu: {}, io: {}, idle: {}" ,
                port, threadSupport, cpuIterations, idleDelay, idleDelay);

        Io io = new Io(fileLength);
        Cpu cpu = new Cpu(cpuIterations);
        Idle idle = new Idle(idleDelay);

        ThreadPool threadPool = new QueuedThreadPool(MAX_VALUE, 8, 60000, -1, null ,null, threadFactory);
        Server server = new Server(threadPool);

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);

        ServletHolder asyncHolder = new ServletHolder(new AsyncServlet(cpu, io, idle));
        asyncHolder.setAsyncSupported(true);
        handler.addServletWithMapping(asyncHolder, "/async");

        ServletHolder syncHolder = new ServletHolder(new SyncServlet(cpu, io, idle));
        handler.addServletWithMapping(syncHolder, "/sync");

        getRuntime().addShutdownHook(new Thread(() -> stop(server)));

        server.start();
        server.join();

        return 0;
    }

    private ThreadFactory fiberThreadFactory() {
        AtomicInteger counter = new AtomicInteger();
        return r -> Thread.builder()
                .task(r)
                .name("fiber-", counter.getAndIncrement())
                .virtual()
                .build();
    }

    private static void stop(Server server) {
        try {
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
