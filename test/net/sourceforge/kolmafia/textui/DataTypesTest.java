package net.sourceforge.kolmafia.textui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DataTypesTest {
  @Test
  public void itShouldRecognizeJackingAsLocation() {
    String name = "Professor Jacking\'s Huge-A-Ma-Tron";
    name = "The Haunted Gallery";
    var parsed = DataTypes.parseLocationValue(name, false);
    String xxx = parsed.toString();
    assertTrue(name.equals(parsed.toString()));
  }

}
