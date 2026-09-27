package net.sourceforge.kolmafia.textui.command;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CheckDataCommandTest extends AbstractCommandTestBase {

  private static final String LS = System.lineSeparator();

  @Test
  public void checkrepoShouldEmitFinshed() {
    // Not doing this with constructor because class has multiple commands
    this.command = "checkrepo";
    String output = execute("");
    String expected =
        "Found 0 repo files." + LS + "Local SVN repos scanned for possible duplicates." + LS;
    assertEquals(expected, output, "Unexpected output");
  }
}
