package org.apache.tika.mime;

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
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

public class MediaType_hasParameters_15_0_Test {

    @Test
    public void testHasParameters() {
        // Test case 1: No parameters
        MediaType mediaType = new MediaType("text/plain", "plain");
        assertTrue(mediaType.hasParameters());
        // Test case 2: Parameters present
        MediaType mediaType2 = new MediaType("application/xml", "xml", Collections.singletonMap("encoding", "UTF-8"));
        assertTrue(mediaType2.hasParameters());
    }
}
