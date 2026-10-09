package org.apache.tika.io;

import org.apache.tika.io.EndianUtils;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.InputStream;
import org.apache.tika.exception.TikaException;

public class EndianUtils_getUByte_30_1_Test {

    private byte[] data;

    private int offset;

    @BeforeEach
    public void setUp() throws IOException, TikaException {
        // Initialize the data and offset for testing
        this.data = new byte[] { 0x12, 0x34 };
        this.offset = 0;
    }

    @Test
    public void testGetUByte() {
        // Expected value based on the input data and offset
        short expectedValue = 0x1234;
        // Actual value obtained from the focal method
        short actualValue = EndianUtils.getUByte(data, offset);
        // Verify that the actual value matches the expected value
        assertEquals(expectedValue, actualValue);
    }
}
