package net.sourceforge.kolmafia.request.concoction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SushiRequestTest {
  @ParameterizedTest
  @CsvSource({
    "beefy nigiri,1,-1,-1,-1,-1",
    "glistening maki,5,-1,-1,-1,-1",
    "Jack LaLanne roll,4,-1,3817,-1,-1",
    "magical omniscient Santa roll,5,3642,10417,-1,-1",
    "salty beefy maki,4,3495,-1,-1,-1",
    "Yuletide sneaky Santa roll,6,10416,10417,-1,-1",
    "tempura avocado bento box with eel sauce,7,-1,-1,3686,3819",
    "tempura green and red bean bento box with peppermint eel sauce,7,-1,-1,10425,10416",
    "tempura cauliflower bento box with Mer-kin weaksauce,7,-1,-1,3690,6396",
  })
  void testSushiNameConstruction(
      String sushiName, int id, int topping, int filling, int veggie, int dipping) {
    String generatedName = SushiRequest.sushiName(id, topping, filling, veggie, dipping);
    assertThat(generatedName, is(sushiName));

    int generatedId = SushiRequest.nameToId(sushiName);
    assertThat(generatedId, is(id));

    int generatedTopping = SushiRequest.nameToTopping(sushiName);
    assertThat(generatedTopping, is(topping));

    int generatedFilling = SushiRequest.nameToFilling1(sushiName);
    assertThat(generatedFilling, is(filling));

    int generatedVeggie = SushiRequest.nameToVeggie(sushiName);
    assertThat(generatedVeggie, is(veggie));

    int generatedDipping = SushiRequest.nameToDipping(sushiName);
    assertThat(generatedDipping, is(dipping));
  }
}
