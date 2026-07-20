package org.opengroup.osdu.workflow.provider.azure.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.exception.CoreException;

/**
 * Tests for {@link CursorUtils}
 */

@ExtendWith(MockitoExtension.class)
public class CursorUtilsTest {
  private static final String NORMAL_CURSOR =
      "{\"token\":\"-RID:~O0wNANEhDQECAAAAAAAAAA==#RT:1#TRC:1#ISV:2#IEO:65551\",\n" +
          "\"range\":\"{\\\"min\\\":\\\"\\\",\\\"max\\\":\\\"FF\\\",\\\"isMinInclusive\\\":true,\n" +
          "\\\"isMaxInclusive\\\":false}\"}";
  private static final String ENCODED_CURSOR = "eyJ0b2tlbiI6Ii1SSUQ6fk8wd05BTkVoRFFFQ0FBQUFBQUFBQUE9P" +
      "SNSVDoxI1RSQzoxI0lTVjoyI0lFTzo2NTU1MSIsCiJyYW5nZSI6IntcIm1pblwiOlwiXCIsXCJtYXhcIjpcIkZGXCIsX" +
      "CJpc01pbkluY2x1c2l2ZVwiOnRydWUsClwiaXNNYXhJbmNsdXNpdmVcIjpmYWxzZX0ifQ==";

  private CursorUtils cursorUtils;

  @BeforeEach
  public void setup() {
    this.cursorUtils = new CursorUtils();
  }

  @Test
  public void testEncodeCosmosCursor() {
    String encodedCursor = cursorUtils.encodeCosmosCursor(NORMAL_CURSOR);
    Assertions.assertEquals(ENCODED_CURSOR, encodedCursor);
  }

  @Test
  public void testEncodeCosmosCursorThrowsExceptionIfGivenNullCursor() {
    Assertions.assertThrows(CoreException.class, () -> cursorUtils.encodeCosmosCursor(null));
  }

  @Test
  public void testDecodeCosmosCursor() {
    String decodedCursor = cursorUtils.decodeCosmosCursor(ENCODED_CURSOR);
    Assertions.assertEquals(NORMAL_CURSOR, decodedCursor);
  }

  @Test
  public void testDecodeCosmosCursorThrowExceptionIfGivenNullCursor() {
    Assertions.assertThrows(CoreException.class, () -> cursorUtils.decodeCosmosCursor(null));
  }
}
