package org.apache.tika.mime;

import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MediaType_getBaseType_12_0_Test {

    private MediaType mediaType;

    @BeforeEach
    public void setUp() {
        mediaType = new MediaType("text/plain", "plain", Collections.emptyMap());
    }

    @Test
    public void testGetBaseType() {
        assertEquals(mediaType.getType(), "text/plain");
        assertEquals(mediaType.getSubtype(), "plain");
        assertEquals(mediaType.getParameters().size(), 0);
    }
}
