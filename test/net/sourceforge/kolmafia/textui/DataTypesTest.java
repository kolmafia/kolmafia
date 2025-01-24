package net.sourceforge.kolmafia.textui;

import org.junit.jupiter.api.Test;

class DataTypesTest {
  @Test
  public void itShouldRecognizeJackingAsLocation() {
    String name = "Professor Jacking\'s Huge-A-Ma-Tron";
    //name = "The Haunted Gallery";
    //String output = execute("to_location("+name+")");
    //assertTrue(false);
    var parsed = DataTypes.parseLocationValue(name, false);
    assertTrue(name.equals(parsed.toString()));
  }

}
