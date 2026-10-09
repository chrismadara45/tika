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
package org.apache.tika.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;

/**
 * Tests ecrits a la main pour tuer des mutants que ni les tests originaux
 * ni les tests generes par ChatUniTest ne detectent. IFT3913 tache 2.
 */
public class FilenameUtilsManualTest {

    @Test
    public void testEmbeddedNamePrefersResourceName() {
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, "first.txt");
        metadata.set(TikaCoreProperties.INTERNAL_PATH, "second.txt");
        assertEquals("first.txt", FilenameUtils.getSanitizedEmbeddedFileName(metadata, ".bin", 100));
    }

    @Test
    public void testEmbeddedNameFallsBackToInternalPath() {
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.INTERNAL_PATH, "dir/second.txt");
        metadata.set(TikaCoreProperties.EMBEDDED_RELATIONSHIP_ID, "third.png");
        assertEquals("second.txt", FilenameUtils.getSanitizedEmbeddedFileName(metadata, ".bin", 100));
    }

    @Test
    public void testEmbeddedNameFallsBackToRelationshipId() {
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.EMBEDDED_RELATIONSHIP_ID, "third.png");
        assertEquals("third.png", FilenameUtils.getSanitizedEmbeddedFileName(metadata, ".bin", 100));
    }

    @Test
    public void testEmbeddedNameWithoutAnyNameIsNull() {
        assertNull(FilenameUtils.getSanitizedEmbeddedFileName(new Metadata(), ".bin", 100));
    }
}
