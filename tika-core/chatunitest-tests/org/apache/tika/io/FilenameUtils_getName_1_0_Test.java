package org.apache.tika.io;

import org.apache.tika.io.FilenameUtils;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
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

public class FilenameUtils_getName_1_0_Test {

    @Test
    public void testGetNameEmptyString() {
        String result = FilenameUtils.getName("");
        assertEquals(StringUtils.EMPTY, result);
    }

    @Test
    public void testGetNameNullString() {
        String result = FilenameUtils.getName(null);
        assertEquals(StringUtils.EMPTY, result);
    }

    @Test
    public void testGetNameWithNoSpecialCharacters() {
        String result = FilenameUtils.getName("example.txt");
        assertEquals("example.txt", result);
    }

    @Test
    public void testGetNameWithReservedFilenameCharacter() {
        String result = FilenameUtils.getName("filename:with:colon.txt");
        assertEquals("filename:with:colon.txt", result);
    }

    @Test
    public void testGetNameWithReservedFilenameCharactersAndPeriod() {
        String result = FilenameUtils.getName("filename:.with:colon.txt");
        assertEquals(".with:colon.txt", result);
    }

    @Test
    public void testGetNameWithReservedFilenameCharactersAndUnderscore() {
        String result = FilenameUtils.getName("filename:_with:colon.txt");
        assertEquals("_with:colon.txt", result);
    }

    @Test
    public void testGetNameWithReservedFilenameCharactersAndDash() {
        String result = FilenameUtils.getName("filename:-with:colon.txt");
        assertEquals("-with:colon.txt", result);
    }

    @Test
    public void testGetNameWithReservedFilenameCharactersAndUpperCase() {
        String result = FilenameUtils.getName("FILENAME:WITH:COLON.TXT");
        assertEquals("FILENAME:WITH:COLON.TXT", result);
    }

    @Test
    public void testGetNameWithReservedFilenameCharactersAndLowerCase() {
        String result = FilenameUtils.getName("filename:with:colon.txt");
        assertEquals("filename:with:colon.txt", result);
    }

    @Test
    public void testGetNameWithReservedFilenameCharactersAndPeriodAtStart() {
        String result = FilenameUtils.getName(".with:colon.txt");
        assertEquals(".with:colon.txt", result);
    }

    @Test
    public void testGetNameWithReservedFilenameCharactersAndUnderscoreAtStart() {
        String result = FilenameUtils.getName("_with:colon.txt");
        assertEquals("_with:colon.txt", result);
    }

    @Test
    public void testGetNameWithReservedFilenameCharactersAndDashAtStart() {
        String result = FilenameUtils.getName("-with:colon.txt");
        assertEquals("-with:colon.txt", result);
    }

    @Test
    public void testGetNameWithReservedFilenameCharactersAndUpperCaseAtStart() {
        String result = FilenameUtils.getName("FILENAME:WITH:COLON.TXT");
        assertEquals("FILENAME:WITH:COLON.TXT", result);
    }

    @Test
    public void testGetNameWithReservedFilenameCharactersAndLowerCaseAtStart() {
        String result = FilenameUtils.getName("filename:with:colon.txt");
        assertEquals("filename:with:colon.txt", result);
    }
}
