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

  // The two disabled tests are an attempt to load display case html, parse it and then verify
  // that the shelf data is correct.  This was supposed to be the first step towards a better test
  // for DisplayCaseManager but jaadams5 has been unable to figure out the correct injection point.
  // That would be something that fetches and processes displaycollection.php/who=
  // DisplayCaseRequest is an obvious candidate but the visibility of DisplayCaseParser is
  // problematic and it is time to move on.
  // The disabled status should cause someone to revisit this eventually.

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
    assertEquals(1, DisplayCaseManager.getShelves().size());
    assertEquals("-none-", DisplayCaseManager.getHeader(0));
  }

  @Test
  public void itShouldHaveSomeShelves() {
    // This file has three shelves.
    String displayCase = null;
    String fileName = "request/test_displaycollection_shelves.html";
    try {
      displayCase = Files.readString(Path.of(fileName));
    } catch (Exception e) {
      fail("Exception " + e);
    }
    assertNotNull(displayCase, "Could not read case data.");
    assertTrue(displayCase.length() > 0, "Case data is empty.");
    DisplayCaseManager.update(displayCase);
    // Test file has three shelves
    assertEquals(2, DisplayCaseManager.getShelves().size());
    assertEquals("-none-", DisplayCaseManager.getHeader(0));
    assertEquals("Being punctual", DisplayCaseManager.getHeader(1));
  }
}
