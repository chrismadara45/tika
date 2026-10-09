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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Tests ecrits a la main pour tuer des mutants que ni les tests originaux
 * ni les tests generes par ChatUniTest ne detectent. IFT3913 tache 2.
 */
public class MediaTypeManualTest {

    @Test
    public void testEquals() {
        MediaType built = new MediaType("text", "plain");
        MediaType parsed = MediaType.parse("text/plain");
        MediaType other = MediaType.parse("text/html");
        assertTrue(built.equals(parsed));
        assertFalse(built.equals(other));
        assertFalse(built.equals("text/plain"));
        assertFalse(built.equals(null));
    }

    @Test
    public void testCompareTo() {
        MediaType plain = new MediaType("text", "plain");
        MediaType html = MediaType.parse("text/html");
        assertEquals(0, plain.compareTo(MediaType.parse("text/plain")));
        assertTrue(html.compareTo(plain) < 0);
        assertTrue(plain.compareTo(html) > 0);
    }

    @Test
    public void testConvenienceFactories() {
        assertEquals("application/xml", MediaType.application("xml").toString());
        assertEquals("audio/mpeg", MediaType.audio("mpeg").toString());
        assertEquals("image/png", MediaType.image("png").toString());
        assertEquals("text/csv", MediaType.text("csv").toString());
        assertEquals("video/mp4", MediaType.video("mp4").toString());
    }

    @Test
    public void testSetOfTypesIgnoresNull() {
        Set<MediaType> types = MediaType.set(MediaType.TEXT_PLAIN, null, MediaType.TEXT_HTML);
        assertEquals(2, types.size());
        assertTrue(types.contains(MediaType.TEXT_PLAIN));
        assertTrue(types.contains(MediaType.TEXT_HTML));
        assertThrows(UnsupportedOperationException.class, () -> types.add(MediaType.OCTET_STREAM));
    }

    @Test
    public void testSetOfStringsIgnoresUnparseable() {
        Set<MediaType> types = MediaType.set("text/plain", "nonsense", "text/html");
        assertEquals(2, types.size());
        assertTrue(types.contains(MediaType.TEXT_PLAIN));
        assertTrue(types.contains(MediaType.TEXT_HTML));
    }
}
