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
          "compare boolean to other boolean", value.compareTo(new Value(false)), hasSign(POSITIVE));
      assertThat(
          "compare boolean to same boolean", value.compareTo(new Value(true)), hasSign(ZERO));
      assertThat("compare boolean to int", value.compareTo(new Value(5)), hasSign(NEGATIVE));
      assertThat(
          "compare boolean to string",
          value.compareTo(new Value("non numeric")),
          hasSign(POSITIVE));
      assertThat("compare boolean to float", value.compareTo(new Value(1.4)), hasSign(NEGATIVE));
    }

    @Test
    void compareBounties() {
      var value = DataTypes.parseBountyValue("bean-shaped rock", true);
      assertThat(
          "compare bounty to other bounty",
          value.compareTo(DataTypes.parseBountyValue("triffid bark", true)),
          hasSign(NEGATIVE));
      assertThat(
          "compare bounty to same bounty",
          value.compareTo(DataTypes.parseBountyValue("bean-shaped rock", true)),
          hasSign(ZERO));
      assertThat("compare bounty to int", value.compareTo(new Value(5)), hasSign(NEGATIVE));
      assertThat(
          "compare bounty to string",
          value.compareTo(new Value("a non numeric value")),
          hasSign(POSITIVE));
      assertThat(
          "compare bounty to boolean", value.compareTo(new Value(false)), lessThanOrEqualTo(0));
    }

    @Test
    void compareBuffers() {
      var buffer = new StringBuffer("contents");
      var value = new Value(DataTypes.BUFFER_TYPE, "", buffer);
      // These comparisons are meaningless since it compares the string and not the buffer.
      // But behaviour is behaviour!
      assertThat(
          "compare buffer to other buffer",
          value.compareTo(new Value(DataTypes.BUFFER_TYPE, "", new StringBuffer("other"))),
          hasSign(ZERO));
      assertThat(
          "compare buffer to same buffer",
          value.compareTo(new Value(DataTypes.BUFFER_TYPE, "", buffer)),
          hasSign(ZERO));
      assertThat("compare buffer to int", value.compareTo(new Value(5)), hasSign(NEGATIVE));
      assertThat(
          "compare buffer to string", value.compareTo(new Value("non numeric")), hasSign(NEGATIVE));
      assertThat("compare buffer to float", value.compareTo(new Value(-30.4)), hasSign(POSITIVE));
    }

    @Test
    void compareClasses() {
      var value = Objects.requireNonNull(DataTypes.makeClassValue(4, true));
      assertThat(
          "compare class to other class",
          value.compareTo(DataTypes.makeClassValue(3, true)),
          hasSign(POSITIVE));
      assertThat(
          "compare class to same class",
          value.compareTo(DataTypes.makeClassValue(4, true)),
          hasSign(ZERO));
      assertThat("compare class to float", value.compareTo(new Value(2.4)), hasSign(POSITIVE));
      assertThat("compare class to int", value.compareTo(new Value(2)), hasSign(POSITIVE));
      assertThat(
          "compare class to monster",
          value.compareTo(DataTypes.makeMonsterValue(99, true)),
          hasSign(NEGATIVE));
    }

    @Test
    void compareCoinmasters() {
      var value =
          Objects.requireNonNull(DataTypes.makeCoinmasterValue(DiscoGiftCoRequest.DISCO_GIFTCO));
      assertThat(
          "compare coinmaster to other coinmaster",
          value.compareTo(DataTypes.makeCoinmasterValue(NinjaStoreRequest.NINJA_STORE)),
          hasSign(NEGATIVE));
      assertThat(
          "compare coinmaster to same coinmaster",
          value.compareTo(DataTypes.makeCoinmasterValue(DiscoGiftCoRequest.DISCO_GIFTCO)),
          hasSign(ZERO));
      assertThat("compare coinmaster to float", value.compareTo(new Value(2.4)), hasSign(NEGATIVE));
      assertThat("compare coinmaster to int", value.compareTo(new Value(2)), hasSign(NEGATIVE));
      assertThat(
          "compare coinmaster to monster",
          value.compareTo(DataTypes.makeMonsterValue(453, true)),
          hasSign(NEGATIVE));
    }

    @Test
    void compareEffects() {
      var value = Objects.requireNonNull(DataTypes.makeEffectValue(10, true));
      assertThat(
          "compare effect to other effect",
          value.compareTo(DataTypes.makeEffectValue(9, true)),
          hasSign(POSITIVE));
      assertThat(
          "compare effect to same effect",
          value.compareTo(DataTypes.makeEffectValue(10, true)),
          hasSign(ZERO));
      assertThat("compare effect to float", value.compareTo(new Value(3.8)), hasSign(POSITIVE));
      assertThat("compare effect to int", value.compareTo(new Value(3)), hasSign(POSITIVE));
      assertThat(
          "compare effect to monster",
          value.compareTo(DataTypes.makeMonsterValue(134, true)),
          hasSign(NEGATIVE));
    }

    @Test
    void compareElements() {
      var value = Objects.requireNonNull(DataTypes.makeElementValue(Element.COLD));
      assertThat(
          "compare element to other element",
          value.compareTo(DataTypes.makeElementValue(Element.HOT)),
          hasSign(NEGATIVE));
      assertThat(
          "compare element to same element",
          value.compareTo(DataTypes.makeElementValue(Element.COLD)),
          hasSign(ZERO));
      assertThat("compare element to float", value.compareTo(new Value(3.8)), is(-1));
      assertThat("compare element to int", value.compareTo(new Value(3)), is(-1));
      assertThat(
          "compare element to location",
          value.compareTo(
              DataTypes.makeLocationValue(
                  AdventureDatabase.getAdventure("Fastest Adventurer Contest"))),
          hasSign(POSITIVE));
    }

    @Test
    void compareFamiliars() {
      var value = Objects.requireNonNull(DataTypes.makeFamiliarValue(91, true));
      assertThat(
          "compare familiar to other familiar",
          value.compareTo(DataTypes.makeFamiliarValue(140, true)),
          hasSign(NEGATIVE));
      assertThat(
          "compare familiar to same familiar",
          value.compareTo(DataTypes.makeFamiliarValue(91, true)),
          hasSign(ZERO));
      assertThat("compare familiar to float", value.compareTo(new Value(8.8)), hasSign(POSITIVE));
    }

    @Test
    void compareInts() {
      var value = new Value(150);
      assertThat("compare int to other int", value.compareTo(new Value(200)), hasSign(NEGATIVE));
      assertThat("compare int to same int", value.compareTo(new Value(150)), hasSign(ZERO));
      assertThat(
          "compare int to equivalent path",
          new Value(7).compareTo(DataTypes.makePathValue(Path.TRENDY)),
          hasSign(ZERO));
      assertThat(
          "compare int to equivalent monster",
          value.compareTo(DataTypes.makeMonsterValue(MonsterDatabase.findMonster("batrat"))),
          hasSign(ZERO));

      assertThat(
          "compare int to thrall",
          value.compareTo(DataTypes.makeThrallValue(2, true)),
          hasSign(POSITIVE));

      // Coinmasters have an int value of 0
      assertThat(
          "compare int to coinmaster",
          new Value(0).compareTo(DataTypes.makeCoinmasterValue(NinjaStoreRequest.NINJA_STORE)),
          hasSign(ZERO));
    }

    @Test
    void compareItems() {
      var value = Objects.requireNonNull(DataTypes.makeItemValue(33, true));
      assertThat(
          "compare item to other item",
          value.compareTo(DataTypes.makeItemValue(210, true)),
          hasSign(NEGATIVE));
      assertThat(
          "compare item to same item",
          value.compareTo(DataTypes.makeItemValue(33, true)),
          hasSign(ZERO));
      assertThat(
          "compare item to skill",
          value.compareTo(DataTypes.makeSkillValue(30, true)),
          hasSign(POSITIVE));
    }

    @Test
    void compareFloats() {
      var value = new Value(5.2);
      assertThat(
          "compare float to other float", value.compareTo(new Value(4.69)), hasSign(POSITIVE));
      assertThat("compare float to same float", value.compareTo(new Value(5.2)), hasSign(ZERO));
      assertThat("compare float to boolean", value.compareTo(new Value(true)), hasSign(POSITIVE));
      assertThat(
          "compare float to familiar",
          value.compareTo(DataTypes.makeFamiliarValue(6, true)),
          hasSign(NEGATIVE));
    }

    @Test
    void compareMonsters() {
      var vampire = Objects.requireNonNull(DataTypes.makeMonsterValue(1, true));
      assertThat(
          "compare two known monster ids by id",
          vampire.compareTo(DataTypes.makeMonsterValue(MonsterDatabase.findMonster("dodecapede"))),
          hasSign(NEGATIVE));

      var aps =
          Objects.requireNonNull(
              DataTypes.makeMonsterValue(MonsterDatabase.findMonster("ancient protector spirit")));

      assertThat(
          "compare unknown monster id to known monster id by name",
          vampire.compareTo(aps),
          hasSign(POSITIVE));
      assertThat(
          "compare known monster id to unknown monster id by name",
          aps.compareTo(vampire),
          hasSign(NEGATIVE));
    }

    @Test
    void comparePaths() {
      var value = DataTypes.makePathValue(Path.AVATAR_OF_BORIS);
      assertThat(
          "compare path to other path",
          value.compareTo(DataTypes.makePathValue(Path.BEES_HATE_YOU)),
          hasSign(POSITIVE));
      assertThat(
          "compare path to same path",
          value.compareTo(DataTypes.makePathValue(Path.AVATAR_OF_BORIS)),
          hasSign(ZERO));
      assertThat("compare path to equivalent int", value.compareTo(new Value(8)), hasSign(ZERO));

      // These are not comparable in this way. There is special handling in ASH to make this work
      // though, for backwards
      // compatibility.
      assertThat(
          "compare path to equivalent string",
          value.compareTo(new Value("Avatar of Boris")),
          hasSign(POSITIVE));
    }

    @Test
    void compareStrings() {
      var value = new Value("some random string");
      assertThat(
          "compare string to other string", value.compareTo(new Value("zzz")), hasSign(NEGATIVE));
      assertThat(
          "compare string to same string",
          value.compareTo(new Value("some random string")),
          hasSign(ZERO));
      assertThat(
          "compare string to equivalent path",
          new Value("Trendy").compareTo(DataTypes.makePathValue(Path.TRENDY)),
          hasSign(ZERO));
    }

    @Test
    void compareVykeas() {
      var companion = VYKEACompanionData.fromString("level 1 blood dresser");
      var value = Objects.requireNonNull(DataTypes.makeVykeaValue(companion, true));
      //noinspection EqualsWithItself
      assertThat("compare vykea to same vykea", value.compareTo(value), hasSign(ZERO));
      assertThat(
          "compare vykea to different type vykea",
          value.compareTo(
              DataTypes.makeVykeaValue(VYKEACompanionData.fromString("level 5 blood couch"), true)),
          hasSign(NEGATIVE));
      assertThat(
          "compare vykea to same type different rune vykea",
          value.compareTo(
              DataTypes.makeVykeaValue(
                  VYKEACompanionData.fromString("level 4 frenzy dresser"), true)),
          hasSign(POSITIVE));
      assertThat(
          "compare vykea to same type same rune different level vykea",
          value.compareTo(
              DataTypes.makeVykeaValue(
                  VYKEACompanionData.fromString("level 2 blood dresser"), true)),
          hasSign(NEGATIVE));
      assertThat(
          "compare vykea to same type same rune same level vykea",
          value.compareTo(
              DataTypes.makeVykeaValue(
                  VYKEACompanionData.fromString("level 1 blood dresser"), true)),
          hasSign(ZERO));

      assertThat("compare vykea to string", value.compareTo(new Value("la")), hasSign(POSITIVE));
      assertThat("compare vykea to string", value.compareTo(new Value("lz")), hasSign(NEGATIVE));
    }
  }

  @Test
  void compareToIgnoreCase() {
    assertThat(new Value("aaa").compareToIgnoreCase(new Value("aAa")), hasSign(ZERO));
  }
}
