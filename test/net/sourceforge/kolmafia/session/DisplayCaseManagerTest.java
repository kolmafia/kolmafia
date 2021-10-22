package net.sourceforge.kolmafia.session;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.utilities.ByteBufferUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
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
    String fileName = "displaycollection.txt";
    File file = new File(KoLConstants.DATA_LOCATION, fileName);
    assertTrue(file.exists(), file.getPath() + " does not exist.");
    byte[] bytes = ByteBufferUtilities.read(file);
    String displayCase = StringUtilities.getEncodedString(bytes, "UTF-8");
    assertNotNull(displayCase, "Could not read case data.");
    assertTrue(displayCase.length() > 0, "Case data is empty.");
    DisplayCaseManager.update(displayCase);
    // Test file has one shelf named -none-
    assertEquals(DisplayCaseManager.getShelves().size(), 1);
    assertEquals(DisplayCaseManager.getHeader(0), "-none-");
  }
}
