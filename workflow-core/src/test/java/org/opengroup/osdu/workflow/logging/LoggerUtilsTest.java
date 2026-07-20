package org.opengroup.osdu.workflow.logging;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class LoggerUtilsTest {
  public static final int STRING_LENGTH_FOR_LOGGING = 1000;

  @Test
  void testGetTruncatedData_givenNullString() {
    String data = null;
    String obtainedString = LoggerUtils.getTruncatedData(data);
    assertEquals("", obtainedString);
  }

  @Test
  void testGetTruncatedData_givenShortLengthString() {
    String data = "short-string";
    String obtainedString = LoggerUtils.getTruncatedData(data);
    assertEquals(data, obtainedString);
  }

  @Test
  void testGetTruncatedData_givenLongLengthString() {
    String data = StringUtils.repeat("a", STRING_LENGTH_FOR_LOGGING + 1);
    String obtainedString = LoggerUtils.getTruncatedData(data);
    String expectedString = data.substring(0, STRING_LENGTH_FOR_LOGGING) + "... (truncated)";
    assertEquals(expectedString, obtainedString);
  }
}
