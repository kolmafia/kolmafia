package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class LatteRequestTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("LatteRequest");
    Preferences.reset("LatteRequest");
  }

  private LatteRequestTest() {}

  @ParameterizedTest
  @CsvSource(
      value = {
        "Latte: Autumnal carrot Latte with a shake of cinnamon|pumpkin,carrot,cinnamon",
        "Latte: Blue chalk and cajun spice Latte with dwarf cream|chalk,cajun,dwarf",
        "Latte: Hellish lizard milk Latte dusted with flour|hellion,lizard,flour",
        "Latte: Carrot pumpkin spice Latte with a shot of vanilla|carrot,pumpkin,vanilla"
      },
      delimiter = '|')
  public void canParseName(String name, String expected) {
    LatteRequest.parseName(name);
    assertEquals(expected, Preferences.getString("latteIngredients"));
  }

  @Test
  public void canParseRefillResponse() {
    LatteRequest.parseResponse(
        "choice.php?pwd&whichchoice=1329&option=1&l3=6MQI6aq4KVr0JD0jd1gipq4LhmptVwaIG2KseCJknX3c0M7DLLWaKAQNqVv8h1LjT1hyaGt4eXBGSkpLQ3FuNU9kOTlNdz09&l2=cdDxyYfNxiWNQBxfbF%2FmC1QeYifAGOSFGrsuVltKDglKn%2FNB40pD9Fs3L%2F4w8v4fZTU0b1I3aDBhVFF1MXQ3SnI4YnhGUT09&l1=ncqOW6ME3y5UWfHXGD6jUZqMbCfQiS%2Bb1ySnRf1wxbEI6z3BSTdBhrndeu6y6ZydQ2ROYUh4UGc5Z2V5NFNmODJzcFNsUT09",
        html("request/refill_latte.html"));
    assertEquals("vanilla,cinnamon,carrot", Preferences.getString("latteIngredients"));
  }
}
