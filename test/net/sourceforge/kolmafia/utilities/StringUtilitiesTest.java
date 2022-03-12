package net.sourceforge.kolmafia.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class StringUtilitiesTest {
  @Test
  void formatsDateFitForLastModified() {
    var formatted = StringUtilities.formatDate(0L);
    assertEquals("Thu, 01 Jan 1970 00:00:00 GMT", formatted);
  }

  @Test
  void readsLastModifiedDate() {
    var parsed = StringUtilities.parseDate("Wed, 21 Oct 2015 07:28:00 GMT");
    var date = ZonedDateTime.of(2015, 10, 21, 7, 28, 0, 0, ZoneId.of("GMT"));
    assertEquals(Instant.from(date).toEpochMilli(), parsed);
  }

  @Test
  void isVowel() {
    assertTrue(StringUtilities.isVowel('a'));
    assertTrue(StringUtilities.isVowel('e'));
    assertTrue(StringUtilities.isVowel('i'));
    assertTrue(StringUtilities.isVowel('o'));
    assertTrue(StringUtilities.isVowel('u'));

    assertFalse(StringUtilities.isVowel('x'));
    assertFalse(StringUtilities.isVowel('p'));
  }
}
