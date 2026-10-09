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

import org.junit.jupiter.api.Test;

/**
 * Tests generes par ChatUniTest (modele local qwen2.5-coder:1.5b via Ollama),
 * puis corriges a la main. IFT3913 tache 2.
 */
public class EndianUtilsGeneratedTest {

    // Genere tel quel (EndianUtils_getIntLE_20_1_Test), aucune correction de logique.
    @Test
    public void testGetIntLE() {
        byte[] data = new byte[]{0x12, 0x34, 0x56, 0x78};
        int expected = 0x78563412;
        int result = EndianUtils.getIntLE(data);
        assertEquals(expected, result);
    }

    // Genere tel quel (EndianUtils_getShortBE_16_0_Test), aucune correction de logique.
    @Test
    public void testGetShortBE() {
        byte[] data = {0x12, 0x34};
        short result = EndianUtils.getShortBE(data);
        assertEquals(0x1234, result);
    }

    // Genere tel quel (EndianUtils_getShortBE_17_0_Test), aucune correction de logique.
    @Test
    public void testGetShortBEWithOffset() {
        byte[] testData = {0x12, 0x34};
        int offset = 0;
        short expectedResult = (short) (testData[0] << 8 | testData[1]);
        short actualResult = EndianUtils.getShortBE(testData, offset);
        assertEquals(expectedResult, actualResult);
    }

    // Corrige (EndianUtils_getUByte_30_1_Test) : l'oracle genere attendait 0x1234,
    // alors que getUByte ne lit qu'un seul octet.
    @Test
    public void testGetUByte() {
        byte[] data = new byte[]{0x12, 0x34};
        short expectedValue = 0x12;
        short actualValue = EndianUtils.getUByte(data, 0);
        assertEquals(expectedValue, actualValue);
    }
}
