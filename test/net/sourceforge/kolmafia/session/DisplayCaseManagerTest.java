package net.sourceforge.kolmafia.session;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DisplayCaseManagerTest {
  @BeforeEach
  public void clearCase() {
    DisplayCaseManager.clearCache();
  }

  @Test
  public void clearingCacheShouldLeaveThingsEmpty() {
    assertFalse(DisplayCaseManager.collectionRetrieved, "Collection retrieved?");
    assertEquals(DisplayCaseManager.getHeaders().size(), 0, "Number of headers");
    assertEquals(DisplayCaseManager.getShelves().size(), 0, "Number of shelves");
  }

  @Test
  public void itShouldNotFetchShelfHeaderThatDoesNotExist() {
    assertNull(DisplayCaseManager.getHeader(-1), "Index below bounds.");
    assertNull(DisplayCaseManager.getHeader(1), "Index above bounds.");
  }

  @Test
  public void itShouldHaveSomeContents() {
    // This file was generated from CafeBabe's Display Case which had no shelves at the time.
    String displayCase = null;
    String fileName = "request/test_displaycollection_no_shelves.html";
    try {
      displayCase = Files.readString(Path.of(fileName));
    } catch (Exception e) {
      fail("Exception " + e);
    }
    assertNotNull(displayCase, "Could not read case data.");
    assertTrue(displayCase.length() > 0, "Case data is empty.");
    DisplayCaseManager.update(displayCase);
    // Test file has one shelf named -none-
    assertEquals(DisplayCaseManager.getShelves().size(), 1);
    assertEquals(DisplayCaseManager.getHeader(0), "-none-");
  }
}
