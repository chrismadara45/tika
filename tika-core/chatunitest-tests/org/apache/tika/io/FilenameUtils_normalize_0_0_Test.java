package org.apache.tika.io;

import org.apache.tika.io.FilenameUtils;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.tika.extractor.EmbeddedDocumentUtil;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.utils.StringUtils;

public class FilenameUtils_normalize_0_0_Test {

    @Test
    public void testNormalize() throws Exception {
        Method normalizeMethod = FilenameUtils.class.getDeclaredMethod("normalize", String.class);
        normalizeMethod.setAccessible(true);
        // Test with null name
        assertEquals("", (String) normalizeMethod.invoke(null, ""));
        // Test with empty string
        assertEquals("", (String) normalizeMethod.invoke(null, ""));
        // Test with a valid name
        assertEquals("abc123", (String) normalizeMethod.invoke(null, "abc123"));
        assertEquals("%20", (String) normalizeMethod.invoke(null, " abc "));
    }
}
