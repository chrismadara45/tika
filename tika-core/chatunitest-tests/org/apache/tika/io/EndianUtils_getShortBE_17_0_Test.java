package org.apache.tika.io;

import org.apache.tika.io.EndianUtils;
import java.io.IOException;
import java.io.InputStream;
import org.apache.tika.exception.TikaException;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

public class EndianUtils_getShortBE_17_0_Test {

    @Test
    public void testGetShortBE() throws IOException, TikaException {
        // Test data
        byte[] testData = { 0x12, 0x34 };
        int offset = 0;
        // Expected result
        short expectedResult = (short) (testData[0] << 8 | testData[1]);
        // Actual result
        short actualResult = EndianUtils.getShortBE(testData, offset);
        // Assert the expected result matches the actual result
        assertEquals(expectedResult, actualResult);
    }
}
