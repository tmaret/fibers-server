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

import static java.util.concurrent.ThreadLocalRandom.current;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.apache.commons.codec.digest.DigestUtils.sha256;

public class Cpu {

    public static final int DEFAULT_CPU_ITERATIONS = 10_000;

    public String process(int iterations) {
        byte[] value = new byte[256];
        current().nextBytes(value);
        for (int i = 0; i < iterations; i++) {
            value = sha256(value);
        }
        return encodeHexString(sha256(value));
    }
}
