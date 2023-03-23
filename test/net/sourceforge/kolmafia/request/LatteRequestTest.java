package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class LatteRequestTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("LatteRequest");
    Preferences.reset("LatteRequest");
  }

  private LatteRequestTest() {}
  ;

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Latte: Autumnal carrot Latte with a shake of cinnamon|pumpkin,carrot,cinnamon",
        "Latte: Blue chalk and cajun spice Latte with dwarf cream|chalk,cajun,dwarf",
        "Latte: Hellish lizard milk Latte dusted with flour|hellion,lizard,flour"
      })
  public void canParseName(String nameAndExpected) {
    String[] parsedNameAndExpected = nameAndExpected.split("\\|");
    String name = parsedNameAndExpected[0];
    String expected = parsedNameAndExpected[1];
    LatteRequest.parseName(name);
    assertEquals(expected, Preferences.getString("latteIngredients"));
  }
}
