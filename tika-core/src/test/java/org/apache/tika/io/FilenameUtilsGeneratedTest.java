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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests generes par ChatUniTest (modele local qwen2.5-coder:1.5b via Ollama),
 * puis corriges a la main. IFT3913 tache 2.
 */
public class FilenameUtilsGeneratedTest {

    // Corrige (FilenameUtils_normalize_0_0_Test) : appel par reflexion inutile remplace
    // par un appel direct, cas null reellement teste, et oracle "%20" faux
    // (l'espace n'est pas un caractere reserve).
    @Test
    public void testNormalize() {
        assertThrows(IllegalArgumentException.class, () -> FilenameUtils.normalize(null));
        assertEquals("", FilenameUtils.normalize(""));
        assertEquals("abc123", FilenameUtils.normalize("abc123"));
        assertEquals(" abc ", FilenameUtils.normalize(" abc "));
    }

    // Generes tels quels (FilenameUtils_getName_1_0_Test).
    @Test
    public void testGetNameEmptyString() {
        assertEquals("", FilenameUtils.getName(""));
    }

    @Test
    public void testGetNameNullString() {
        assertEquals("", FilenameUtils.getName(null));
    }

    @Test
    public void testGetNameWithNoSpecialCharacters() {
        assertEquals("example.txt", FilenameUtils.getName("example.txt"));
    }

    // Corriges (FilenameUtils_getName_1_0_Test) : 11 tests generes supposaient que ':'
    // etait conserve, alors que getName le traite comme un separateur de chemin.
    // Les 3 doublons exacts ont ete retires.
    @Test
    public void testGetNameWithColon() {
        assertEquals("colon.txt", FilenameUtils.getName("filename:with:colon.txt"));
        assertEquals("colon.txt", FilenameUtils.getName("filename:.with:colon.txt"));
        assertEquals("colon.txt", FilenameUtils.getName("filename:_with:colon.txt"));
        assertEquals("colon.txt", FilenameUtils.getName("filename:-with:colon.txt"));
        assertEquals("COLON.TXT", FilenameUtils.getName("FILENAME:WITH:COLON.TXT"));
    }

    @Test
    public void testGetNameWithColonAtStart() {
        assertEquals("colon.txt", FilenameUtils.getName(".with:colon.txt"));
        assertEquals("colon.txt", FilenameUtils.getName("_with:colon.txt"));
        assertEquals("colon.txt", FilenameUtils.getName("-with:colon.txt"));
    }

    // Corriges (FilenameUtils_getSuffixFromPath_2_0_Test) : les 3 oracles etaient faux.
    // Le suffixe inclut le point, une URL garde son extension, et "file.\\txt"
    // donne le nom "txt" qui n'a pas d'extension.
    @Test
    public void testGetSuffixFromPath() {
        assertEquals(".txt", FilenameUtils.getSuffixFromPath("example.txt"));
    }

    @Test
    public void testGetSuffixFromPathWithProtocol() {
        assertEquals(".txt", FilenameUtils.getSuffixFromPath("http://example.com/file.txt"));
    }

    @Test
    public void testGetSuffixFromPathWithBackslash() {
        assertEquals("", FilenameUtils.getSuffixFromPath("file.\\txt"));
    }
}
