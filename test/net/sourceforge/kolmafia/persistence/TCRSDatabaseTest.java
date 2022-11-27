package net.sourceforge.kolmafia.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TCRSDatabaseTest {

  private static Stream<Arguments> guesses() {
    // Tea Tree teas are useful because there are a lot of them, and they're all already in ItemPool
    return Stream.of(
        // Arguments.of(AscensionClass.SEAL_CLUBBER, ZodiacSign.MONGOOSE, 340, "nitrogenated wobbly
        // Knob Goblin steroids", "Effect: \"Healthy Blue Glow\", Effect Duration: 23"),
        Arguments.of(
            AscensionClass.SEAL_CLUBBER,
            ZodiacSign.MONGOOSE,
            418,
            "enhanced quadruple-magnetized spinning maroon jittery Ferrigno's Elixir of Power",
            "Effect: \"Orchid Blood\", Effect Duration: 12"),
        Arguments.of(
            AscensionClass.SEAL_CLUBBER,
            ZodiacSign.MONGOOSE,
            422,
            "altered galvanized twirling huge potion of potency",
            "Effect: \"Sparkly!\", Effect Duration: 26"),
        Arguments.of(
            AscensionClass.SAUCEROR,
            ZodiacSign.MARMOT,
            ItemPool.PHIAL_OF_HOTNESS,
            "colloidal blue phial of hotness",
            "Effect: \"Lifted Spirits\", Effect Duration: 69"),
        Arguments.of(
            AscensionClass.TURTLE_TAMER,
            ZodiacSign.PLATYPUS,
            ItemPool.HALF_ORCHID,
            "galvanized narrow half-orchid",
            "Effect: \"Charrrming\", Effect Duration: 55"),
        Arguments.of(
            AscensionClass.SAUCEROR,
            ZodiacSign.OPOSSUM,
            ItemPool.VITALI_TEA,
            "super-modified ghostly cuppa Vitali tea",
            "Effect: \"Feline Ferocity\", Effect Duration: 14"),
        Arguments.of(
            AscensionClass.ACCORDION_THIEF,
            ZodiacSign.MARMOT,
            ItemPool.MONSTROSI_TEA,
            "moist super-activated deionized cuppa Monstrosi tea",
            "Effect: \"Happy Salamander\", Effect Duration: 65"),
        Arguments.of(
            AscensionClass.ACCORDION_THIEF,
            ZodiacSign.MARMOT,
            ItemPool.IMPREGNABILI_TEA,
            "tarnished concentrated jittery cuppa Impregnabili tea",
            "Effect: \"Burning Ears\", Effect Duration: 69"),
        Arguments.of(
            AscensionClass.DISCO_BANDIT,
            ZodiacSign.MARMOT,
            ItemPool.CHARI_TEA,
            "boiled cuppa Chari tea",
            "Effect: \"Dancing Prowess\", Effect Duration: 54"),
        Arguments.of(
            AscensionClass.DISCO_BANDIT,
            ZodiacSign.WOMBAT,
            ItemPool.TOAST_TEA,
            "quadruple-dry boiled alkaline cuppa Toast tea",
            "Effect: \"Bestial Sympathy\", Effect Duration: 12"),
        Arguments.of(
            AscensionClass.ACCORDION_THIEF,
            ZodiacSign.BLENDER,
            ItemPool.BATTERY_CAR,
            "vacuum-sealed tumbling battery (car)",
            "Effect: \"Starry-Eyed\", Effect Duration: 24"),
        // Sauceror/Vole Spooky Powder rolls an overflow for effect and as such keeps its original
        // effect
        Arguments.of(
            AscensionClass.SAUCEROR,
            ZodiacSign.VOLE,
            1441 /* Spooky Powder */,
            "irradiated altered powder",
            "Effect: \"Spookypants\", Effect Duration: 69"),
        Arguments.of(
            AscensionClass.TURTLE_TAMER,
            ZodiacSign.MONGOOSE,
            ItemPool.MEDIOCRI_TEA,
            "concentrated corrupted colloidal cuppa Mediocri tea",
            "Effect: \"Spirit of Alph\", Effect Duration: 55"),
        Arguments.of(
            AscensionClass.TURTLE_TAMER,
            ZodiacSign.PACKRAT,
            ItemPool.NEUROPLASTICI_TEA,
            "enhanced cuppa Neuroplastici tea",
            "Effect: \"Rat-Faced\", Effect Duration: 57"),
        Arguments.of(
            AscensionClass.DISCO_BANDIT,
            ZodiacSign.PACKRAT,
            ItemPool.LOYAL_TEA,
            "frozen wobbly cuppa Loyal tea",
            "Effect: \"Tiki Temerity\", Effect Duration: 29"));
  }

  @ParameterizedTest
  @MethodSource("guesses")
  void guessItem(
      final AscensionClass ascensionClass,
      final ZodiacSign sign,
      final int itemId,
      final String expectedName,
      final String expectedMods) {
    var item = TCRSDatabase.guessItem(ascensionClass, sign, itemId);
    assertThat(item.name, equalTo(expectedName));
    assertThat(item.modifiers, equalTo(expectedMods));
  }

  @Test
  void guessAll() {
    for (var ascensionClass : AscensionClass.standardClasses) {
      for (var sign : ZodiacSign.standardZodiacSigns) {
        TCRSDatabase.load(ascensionClass, sign, true);
        for (var i : ItemDatabase.entrySet()) {
          var itemId = i.getKey();
          var itemName = i.getValue();
          if (!EquipmentDatabase.getItemType(itemId).contains("potion")
              || itemName.contains("jazz soap")
              || itemName.contains("Binarrrca")
              || itemName.contains("Love Potion #")
              || itemId >= 10950 // database files only go up to here as of now
          ) continue;
          var expected = TCRSDatabase.guessItem(ascensionClass, sign, itemId);
          assertThat(
              ascensionClass + "/" + sign + " " + itemId,
              expected.name,
              equalTo(TCRSDatabase.getTCRSName(itemId)));
        }
      }
    }
  }
}
