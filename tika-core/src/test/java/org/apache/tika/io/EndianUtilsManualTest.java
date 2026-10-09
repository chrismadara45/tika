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

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Test;

/**
 * Tests ecrits a la main pour tuer des mutants que ni les tests originaux
 * ni les tests generes par ChatUniTest ne detectent. IFT3913 tache 2.
 */
public class EndianUtilsManualTest {

    private static ByteArrayInputStream stream(int... bytes) {
        byte[] data = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            data[i] = (byte) bytes[i];
        }
        return new ByteArrayInputStream(data);
    }

    @Test
    public void testReadZeroIsNotUnderrun() throws Exception {
        assertEquals(0L, EndianUtils.readUIntLE(stream(0, 0, 0, 0)));
        assertEquals(0L, EndianUtils.readUIntBE(stream(0, 0, 0, 0)));
        assertEquals(0, EndianUtils.readIntME(stream(0, 0, 0, 0)));
        assertEquals(0, EndianUtils.readIntLE(stream(0, 0, 0, 0)));
        assertEquals(0, EndianUtils.readIntBE(stream(0, 0, 0, 0)));
        assertEquals(0, EndianUtils.readUShortLE(stream(0, 0)));
        assertEquals(0, EndianUtils.readUShortBE(stream(0, 0)));
    }

    @Test
    public void testReadUIntBEUnderrun() {
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readUIntBE(stream(0xFF, 0xFF, 0xFF)));
    }

    @Test
    public void testReadIntLEAndBE() throws Exception {
        assertEquals(0x78563412, EndianUtils.readIntLE(stream(0x12, 0x34, 0x56, 0x78)));
        assertEquals(0x12345678, EndianUtils.readIntBE(stream(0x12, 0x34, 0x56, 0x78)));
        assertEquals(0xFCFDFEFF, EndianUtils.readIntLE(stream(0xFF, 0xFE, 0xFD, 0xFC)));
        assertEquals(0xFFFEFDFC, EndianUtils.readIntBE(stream(0xFF, 0xFE, 0xFD, 0xFC)));
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readIntLE(stream(0x12, 0x34, 0x56)));
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readIntBE(stream(0x12, 0x34, 0x56)));
    }

    @Test
    public void testReadUShortAndShort() throws Exception {
        assertEquals(0x1234, EndianUtils.readUShortLE(stream(0x34, 0x12)));
        assertEquals(0x3412, EndianUtils.readUShortBE(stream(0x34, 0x12)));
        assertEquals(0xFFFE, EndianUtils.readUShortLE(stream(0xFE, 0xFF)));
        assertEquals((short) -2, EndianUtils.readShortLE(stream(0xFE, 0xFF)));
        assertEquals((short) -2, EndianUtils.readShortBE(stream(0xFF, 0xFE)));
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readUShortLE(stream(0x34)));
        assertThrows(EndianUtils.BufferUnderrunException.class,
                () -> EndianUtils.readUShortBE(stream(0x34)));
    }

    @Test
    public void testGetIntBE() {
        byte[] data = new byte[]{0x12, 0x34, 0x56, 0x78};
        assertEquals(0x12345678, EndianUtils.getIntBE(data));
        byte[] shifted = new byte[]{0x00, (byte) 0xFF, (byte) 0xFE, (byte) 0xFD, (byte) 0xFC};
        assertEquals(0xFFFEFDFC, EndianUtils.getIntBE(shifted, 1));
    }

    @Test
    public void testGetUShortLEAndShortLE() {
        assertEquals(0x1234, EndianUtils.getUShortLE(new byte[]{0x34, 0x12}));
        byte[] data = new byte[]{0x00, (byte) 0xFE, (byte) 0xFF};
        assertEquals(0xFFFE, EndianUtils.getUShortLE(data, 1));
        assertEquals((short) -2, EndianUtils.getShortLE(data, 1));
        assertEquals((short) 0x1234, EndianUtils.getShortLE(new byte[]{0x34, 0x12}));
    }

    @Test
    public void testGetUIntLEAndBE() {
        byte[] data = new byte[]{0x01, 0x02, 0x03, (byte) 0x84};
        assertEquals(0x84030201L, EndianUtils.getUIntLE(data));
        assertEquals(0x01020384L, EndianUtils.getUIntBE(data));
        byte[] shifted = new byte[]{0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        assertEquals(0xFFFFFFFFL, EndianUtils.getUIntLE(shifted, 1));
        assertEquals(0xFFFFFFFFL, EndianUtils.getUIntBE(shifted, 1));
    }

    @Test
    public void testGetLongLE() {
        byte[] data = new byte[]{0x7F, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, (byte) 0x88};
        assertEquals(0x8807060504030201L, EndianUtils.getLongLE(data, 1));
        assertEquals(0x070605040302017FL, EndianUtils.getLongLE(data, 0));
    }

    @Test
    public void testUbyteToInt() {
        assertEquals(255, EndianUtils.ubyteToInt((byte) 0xFF));
        assertEquals(128, EndianUtils.ubyteToInt((byte) 0x80));
        assertEquals(127, EndianUtils.ubyteToInt((byte) 0x7F));
        assertEquals(0, EndianUtils.ubyteToInt((byte) 0x00));
    }
}
