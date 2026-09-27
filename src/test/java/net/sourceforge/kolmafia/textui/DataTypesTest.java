package net.sourceforge.kolmafia.textui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.sourceforge.kolmafia.textui.parsetree.Value;
import org.junit.jupiter.api.Test;

class DataTypesTest {
  @Test
  public void itShouldRecognizeJackingAsLocation() {
    String name = "Professor Jacking's Huge-A-Ma-Tron";
    Value parsed = DataTypes.parseLocationValue(name, false);
    assertEquals(name, parsed.toString());
  }
}
