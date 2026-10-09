package org.apache.tika.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.tika.io.EndianUtils;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.InputStream;
import org.apache.tika.exception.TikaException;

public class EndianUtils_getIntLE_20_1_Test {

    @Test
    public void testGetIntLE() throws IOException {
        byte[] data = new byte[] { 0x12, 0x34, 0x56, 0x78 };
        int expected = 0x78563412;
        int result = EndianUtils.getIntLE(data);
        assertEquals(expected, result);
    }
}
