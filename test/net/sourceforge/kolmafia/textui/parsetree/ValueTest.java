package net.sourceforge.kolmafia.textui.parsetree;

import static internal.matchers.SignMatcher.Sign.NEGATIVE;
import static internal.matchers.SignMatcher.Sign.POSITIVE;
import static internal.matchers.SignMatcher.Sign.ZERO;
import static internal.matchers.SignMatcher.hasSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Objects;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.VYKEACompanionData;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.request.DiscoGiftCoRequest;
import net.sourceforge.kolmafia.request.NinjaStoreRequest;
import net.sourceforge.kolmafia.textui.DataTypes;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ValueTest {
  @Test
  void compareToNull() {
    var value = new Value(true);
    //noinspection ResultOfMethodCallIgnored
    assertThrows(ClassCastException.class, () -> value.compareTo(null));
  }

  @Nested
  class CompareTo {
    @Test
    void compareBooleans() {
      var value = new Value(true);
      assertThat(
          "booleans compare to booleans by int value (different values)",
          value.compareTo(new Value(false)),
          hasSign(POSITIVE));
      assertThat(
          "booleans compare to booleans by int value (same value)",
          value.compareTo(new Value(true)),
          hasSign(ZERO));
      assertThat("compare boolean to int", value.compareTo(new Value(5)), hasSign(NEGATIVE));
      assertThat(
          "booleans compare to strings by int value",
          value.compareTo(new Value("non numeric")),
          hasSign(POSITIVE));
      assertThat(
          "booleans compare to floats by float value",
          value.compareTo(new Value(1.4)),
          hasSign(NEGATIVE));
    }

    @Test
    void compareBounties() {
      var value = DataTypes.parseBountyValue("bean-shaped rock", true);
      assertThat(
          "bounties compare to bounties by string value (different value)",
          value.compareTo(DataTypes.parseBountyValue("triffid bark", true)),
          hasSign(NEGATIVE));
      assertThat(
          "bounties compare to bounties by string value (same value)",
          value.compareTo(DataTypes.parseBountyValue("bean-shaped rock", true)),
          hasSign(ZERO));
      assertThat(
          "bounties compare to ints by string value",
          value.compareTo(new Value(5)),
          hasSign(NEGATIVE));
      assertThat(
          "bounties compare to strings by string value",
          value.compareTo(new Value("a non numeric value")),
          hasSign(POSITIVE));
      assertThat(
          "bounties compare to boolean by string value",
          value.compareTo(new Value(false)),
          lessThanOrEqualTo(0));
    }

    @Test
    void compareBuffers() {
      var buffer = new StringBuffer("contents");
      var value = new Value(DataTypes.BUFFER_TYPE, "", buffer);
      // These comparisons are meaningless since it compares the string and not the buffer.
      // But behaviour is behaviour!
      assertThat(
          "buffers compare to buffers by string value (different value)",
          value.compareTo(new Value(DataTypes.BUFFER_TYPE, "", new StringBuffer("other"))),
          hasSign(ZERO));
      assertThat(
          "buffers compare to buffers by string value (same value)",
          value.compareTo(new Value(DataTypes.BUFFER_TYPE, "", buffer)),
          hasSign(ZERO));
      assertThat(
          "buffers compare to ints by string value",
          value.compareTo(new Value(5)),
          hasSign(NEGATIVE));
      assertThat(
          "buffers compare to strings by string value",
          value.compareTo(new Value("non numeric")),
          hasSign(NEGATIVE));
      assertThat(
          "buffers compare to floats by float value",
          value.compareTo(new Value(-30.4)),
          hasSign(POSITIVE));
    }

    @Test
    void compareClasses() {
      var value = Objects.requireNonNull(DataTypes.makeClassValue(4, true));
      assertThat(
          "classes compare to classes by int value (different value)",
          value.compareTo(DataTypes.makeClassValue(3, true)),
          hasSign(POSITIVE));
      assertThat(
          "classes compare to classes by int value (same value)",
          value.compareTo(DataTypes.makeClassValue(4, true)),
          hasSign(ZERO));
      assertThat(
          "classes compare to floats by float value",
          value.compareTo(new Value(2.4)),
          hasSign(POSITIVE));
      assertThat(
          "classes compare to ints by int value", value.compareTo(new Value(2)), hasSign(POSITIVE));
      assertThat(
          "classes compare to monsters by int value",
          value.compareTo(DataTypes.makeMonsterValue(99, true)),
          hasSign(NEGATIVE));
    }

    @Test
    void compareCoinmasters() {
      var value =
          Objects.requireNonNull(DataTypes.makeCoinmasterValue(DiscoGiftCoRequest.DISCO_GIFTCO));
      assertThat(
          "coinmasters compare to coinmasters by string value (different value)",
          value.compareTo(DataTypes.makeCoinmasterValue(NinjaStoreRequest.NINJA_STORE)),
          hasSign(NEGATIVE));
      assertThat(
          "coinmasters compare to coinmasters by string value (same value)",
          value.compareTo(DataTypes.makeCoinmasterValue(DiscoGiftCoRequest.DISCO_GIFTCO)),
          hasSign(ZERO));
      assertThat(
          "coinmasters compare to floats by float value",
          value.compareTo(new Value(2.4)),
          hasSign(NEGATIVE));
      assertThat(
          "coinmasters compare to ints by int value",
          value.compareTo(new Value(2)),
          hasSign(NEGATIVE));
      assertThat(
          "coinmasters compare to monsters by int value",
          value.compareTo(DataTypes.makeMonsterValue(453, true)),
          hasSign(NEGATIVE));
    }

    @Test
    void compareEffects() {
      var value = Objects.requireNonNull(DataTypes.makeEffectValue(10, true));
      assertThat(
          "effects compare to effects by int value (different value)",
          value.compareTo(DataTypes.makeEffectValue(9, true)),
          hasSign(POSITIVE));
      assertThat(
          "effects compare to effects by int value (same value)",
          value.compareTo(DataTypes.makeEffectValue(10, true)),
          hasSign(ZERO));
      assertThat(
          "effects compare to floats by float value",
          value.compareTo(new Value(3.8)),
          hasSign(POSITIVE));
      assertThat(
          "effects compare to ints by int value", value.compareTo(new Value(3)), hasSign(POSITIVE));
      assertThat(
          "effects compare to monsters by int value",
          value.compareTo(DataTypes.makeMonsterValue(134, true)),
          hasSign(NEGATIVE));
    }

    @Test
    void compareElements() {
      var value = Objects.requireNonNull(DataTypes.makeElementValue(Element.COLD));
      assertThat(
          "elements compare to elements by string value (different value)",
          value.compareTo(DataTypes.makeElementValue(Element.HOT)),
          hasSign(NEGATIVE));
      assertThat(
          "elements compare to elements by string value (same value)",
          value.compareTo(DataTypes.makeElementValue(Element.COLD)),
          hasSign(ZERO));
      assertThat(
          "elements compare to floats by float value", value.compareTo(new Value(3.8)), is(-1));
      assertThat("elements compare to ints by string value", value.compareTo(new Value(3)), is(-1));
      assertThat(
          "elements compare to locations by string value",
          value.compareTo(
              DataTypes.makeLocationValue(
                  AdventureDatabase.getAdventure("Fastest Adventurer Contest"))),
          hasSign(POSITIVE));
    }

    @Test
    void compareFamiliars() {
      var value = Objects.requireNonNull(DataTypes.makeFamiliarValue(91, true));
      assertThat(
          "familiars compare to familiars by int value (different value)",
          value.compareTo(DataTypes.makeFamiliarValue(140, true)),
          hasSign(NEGATIVE));
      assertThat(
          "familiars compare to familiars by int value (same value)",
          value.compareTo(DataTypes.makeFamiliarValue(91, true)),
          hasSign(ZERO));
      assertThat(
          "familiars compare to floats by float value",
          value.compareTo(new Value(8.8)),
          hasSign(POSITIVE));
    }

    @Test
    void compareInts() {
      var value = new Value(150);
      assertThat(
          "ints compare to ints by int value (different value)",
          value.compareTo(new Value(200)),
          hasSign(NEGATIVE));
      assertThat(
          "ints compare to ints by int value (same value)",
          value.compareTo(new Value(150)),
          hasSign(ZERO));
      assertThat(
          "ints compare to paths by int value (equivalent path)",
          new Value(7).compareTo(DataTypes.makePathValue(Path.TRENDY)),
          hasSign(ZERO));
      assertThat(
          "ints compare to monsters by int value (equivalent monster)",
          value.compareTo(DataTypes.makeMonsterValue(MonsterDatabase.findMonster("batrat"))),
          hasSign(ZERO));

      assertThat(
          "ints compare to thralls by int value",
          value.compareTo(DataTypes.makeThrallValue(2, true)),
          hasSign(POSITIVE));

      // Coinmasters have an int value of 0
      assertThat(
          "ints compare to coinmasters by int value",
          new Value(0).compareTo(DataTypes.makeCoinmasterValue(NinjaStoreRequest.NINJA_STORE)),
          hasSign(ZERO));
    }

    @Test
    void compareItems() {
      var value = Objects.requireNonNull(DataTypes.makeItemValue(33, true));
      assertThat(
          "items compare to items by int value (different value)",
          value.compareTo(DataTypes.makeItemValue(210, true)),
          hasSign(NEGATIVE));
      assertThat(
          "items compare to items by int value (same value)",
          value.compareTo(DataTypes.makeItemValue(33, true)),
          hasSign(ZERO));
      assertThat(
          "items compare to skills by int value",
          value.compareTo(DataTypes.makeSkillValue(30, true)),
          hasSign(POSITIVE));
    }

    @Test
    void compareFloats() {
      var value = new Value(5.2);
      assertThat(
          "floats compare to floats by float value (different value)",
          value.compareTo(new Value(4.69)),
          hasSign(POSITIVE));
      assertThat(
          "floats compare to floats by float value (same value)",
          value.compareTo(new Value(5.2)),
          hasSign(ZERO));
      assertThat(
          "floats compare to booleans by float value",
          value.compareTo(new Value(true)),
          hasSign(POSITIVE));
      assertThat(
          "floats compare to familiars by float value",
          value.compareTo(DataTypes.makeFamiliarValue(6, true)),
          hasSign(NEGATIVE));
    }

    @Test
    void compareMonsters() {
      var vampire = Objects.requireNonNull(DataTypes.makeMonsterValue(1, true));
      assertThat(
          "monsters with known ids compare to monsters with known ids by int value",
          vampire.compareTo(DataTypes.makeMonsterValue(MonsterDatabase.findMonster("dodecapede"))),
          hasSign(NEGATIVE));

      var aps =
          Objects.requireNonNull(
              DataTypes.makeMonsterValue(MonsterDatabase.findMonster("ancient protector spirit")));

      assertThat(
          "monsters with known ids compare to monsters with unknown ids by string value",
          vampire.compareTo(aps),
          hasSign(POSITIVE));
      assertThat(
          "monsters with unknown ids compare to monsters with known ids by string value",
          aps.compareTo(vampire),
          hasSign(NEGATIVE));
    }

    @Test
    void comparePaths() {
      var value = DataTypes.makePathValue(Path.AVATAR_OF_BORIS);
      assertThat(
          "paths compare to paths by int value (different value)",
          value.compareTo(DataTypes.makePathValue(Path.BEES_HATE_YOU)),
          hasSign(POSITIVE));
      assertThat(
          "paths compare to paths by int value (same value)",
          value.compareTo(DataTypes.makePathValue(Path.AVATAR_OF_BORIS)),
          hasSign(ZERO));
      assertThat(
          "paths compare to ints by int value (equivalent value)",
          value.compareTo(new Value(8)),
          hasSign(ZERO));

      // These are not comparable in this way. There is special handling in ASH to make this work
      // though, for backwards
      // compatibility.
      assertThat(
          "paths compare to strings by int value (would-be-equivalent value)",
          value.compareTo(new Value("Avatar of Boris")),
          hasSign(POSITIVE));
    }

    @Test
    void compareStrings() {
      var value = new Value("some random string");
      assertThat(
          "strings compare to strings by string value (different value)",
          value.compareTo(new Value("zzz")),
          hasSign(NEGATIVE));
      assertThat(
          "strings compare to strings by string value (same value)",
          value.compareTo(new Value("some random string")),
          hasSign(ZERO));
      assertThat(
          "strings compare to paths by string value (equivalent value)",
          new Value("Trendy").compareTo(DataTypes.makePathValue(Path.TRENDY)),
          hasSign(ZERO));
    }

    @Test
    void compareVykeas() {
      var companion = VYKEACompanionData.fromString("level 1 blood dresser");
      var value = Objects.requireNonNull(DataTypes.makeVykeaValue(companion, true));
      //noinspection EqualsWithItself
      assertThat(
          "vykeas compare to vykeas by vykea value (same value)",
          value.compareTo(value),
          hasSign(ZERO));
      assertThat(
          "vykeas compare to vykeas by vykea value (different type)",
          value.compareTo(
              DataTypes.makeVykeaValue(VYKEACompanionData.fromString("level 5 blood couch"), true)),
          hasSign(NEGATIVE));
      assertThat(
          "vykeas compare to vykeas by vykea value (same type, different rune)",
          value.compareTo(
              DataTypes.makeVykeaValue(
                  VYKEACompanionData.fromString("level 4 frenzy dresser"), true)),
          hasSign(POSITIVE));
      assertThat(
          "vykeas compare to vykeas by vykea value (same type, same rune, different level)",
          value.compareTo(
              DataTypes.makeVykeaValue(
                  VYKEACompanionData.fromString("level 2 blood dresser"), true)),
          hasSign(NEGATIVE));
      assertThat(
          "vykeas compare to vykeas by vykea value (same type, same rune, same level)",
          value.compareTo(
              DataTypes.makeVykeaValue(
                  VYKEACompanionData.fromString("level 1 blood dresser"), true)),
          hasSign(ZERO));

      assertThat(
          "vykeas compare to strings by string value (higher value)",
          value.compareTo(new Value("la")),
          hasSign(POSITIVE));
      assertThat(
          "vykeas compare to strings by strings value (lower value)",
          value.compareTo(new Value("lz")),
          hasSign(NEGATIVE));
    }
  }

  @Test
  void compareToIgnoreCase() {
    assertThat(new Value("aaa").compareToIgnoreCase(new Value("aAa")), hasSign(ZERO));
  }
}
