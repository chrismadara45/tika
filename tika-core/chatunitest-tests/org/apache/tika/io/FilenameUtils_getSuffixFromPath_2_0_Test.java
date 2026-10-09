package org.apache.tika.io;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.tika.extractor.EmbeddedDocumentUtil;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.utils.StringUtils;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

public class FilenameUtils_getSuffixFromPath_2_0_Test {

    private static final MimeTypes MIME_TYPES = MimeTypes.getDefaultMimeTypes();

    private static final Pattern PROTOCOL_PATTERN = Pattern.compile("[A-Za-z0-9]{1,10}://+");

    private final static char[] RESERVED_FILENAME_CHARACTERS = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F, '?', ':', '*', '<', '>', '|', '"', '\'' };

    private final static HashSet<Character> RESERVED = new HashSet<>(38);

    private final static Pattern ASCII_NUMERIC = Pattern.compile("\\A\\.(?i)[a-z0-9]{1,5}\\Z");

    @Test
    public void testGetSuffixFromPath() throws IOException {
        String path = "example.txt";
        assertEquals("txt", FilenameUtils.getSuffixFromPath(path));
    }

    @Test
    public void testGetSuffixFromPathWithProtocol() throws IOException {
        String path = "http://example.com/file.txt";
        assertEquals("", FilenameUtils.getSuffixFromPath(path));
    }

    @Test
    public void testGetSuffixFromPathWithReservedCharacter() throws IOException {
        String path = "file.\\txt";
        assertEquals("txt", FilenameUtils.getSuffixFromPath(path));
    }
}
