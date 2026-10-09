/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.mime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

/**
 * Tests generes par ChatUniTest (modele local qwen2.5-coder:1.5b via Ollama),
 * puis corriges a la main. IFT3913 tache 2.
 */
public class MediaTypeGeneratedTest {

    // Corrige (MediaType_getBaseType_12_0_Test) : le test genere construisait
    // new MediaType("text/plain", "plain", ...) et n'appelait jamais getBaseType().
    @Test
    public void testGetBaseType() {
        MediaType withParams =
                new MediaType("text", "plain", Collections.singletonMap("charset", "UTF-8"));
        MediaType base = withParams.getBaseType();
        assertEquals("text", base.getType());
        assertEquals("plain", base.getSubtype());
        assertEquals(0, base.getParameters().size());
        assertEquals("text/plain", base.toString());

        MediaType noParams = new MediaType("text", "plain");
        assertSame(noParams, noParams.getBaseType());
    }

    // Corrige (MediaType_hasParameters_15_0_Test) : le premier cas attendait true
    // pour un type sans parametre, et le constructeur recevait "text/plain" comme type.
    @Test
    public void testHasParameters() {
        MediaType mediaType = new MediaType("text", "plain");
        assertFalse(mediaType.hasParameters());

        MediaType mediaType2 =
                new MediaType("application", "xml", Collections.singletonMap("encoding", "UTF-8"));
        assertTrue(mediaType2.hasParameters());
    }
}
