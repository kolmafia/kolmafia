package net.sourceforge.kolmafia.textui.parsetree;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
          "compare boolean to other boolean",
          value.compareTo(new Value(false)),
          greaterThanOrEqualTo(1));
      assertThat("compare boolean to same boolean", value.compareTo(new Value(true)), is(0));
      assertThat("compare boolean to int", value.compareTo(new Value(5)), lessThanOrEqualTo(-1));
      assertThat(
          "compare boolean to string",
          value.compareTo(new Value("non numeric")),
          greaterThanOrEqualTo(1));
      assertThat(
          "compare boolean to float", value.compareTo(new Value(1.4)), lessThanOrEqualTo(-1));
    }

    @Test
    void compareBounties() {
      var value = DataTypes.parseBountyValue("bean-shaped rock", true);
      assertThat(
          "compare bounty to other bounty",
          value.compareTo(DataTypes.parseBountyValue("triffid bark", true)),
          lessThanOrEqualTo(-1));
      assertThat(
          "compare bounty to same bounty",
          value.compareTo(DataTypes.parseBountyValue("bean-shaped rock", true)),
          is(0));
      assertThat("compare bounty to int", value.compareTo(new Value(5)), lessThanOrEqualTo(-1));
      assertThat(
          "compare bounty to string",
          value.compareTo(new Value("a non numeric value")),
          greaterThanOrEqualTo(1));
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
          is(0));
      assertThat(
          "compare buffer to same buffer",
          value.compareTo(new Value(DataTypes.BUFFER_TYPE, "", buffer)),
          is(0));
      assertThat("compare buffer to int", value.compareTo(new Value(5)), lessThanOrEqualTo(-1));
      assertThat(
          "compare buffer to string",
          value.compareTo(new Value("non numeric")),
          lessThanOrEqualTo(-1));
      assertThat(
          "compare buffer to float", value.compareTo(new Value(-30.4)), greaterThanOrEqualTo(1));
    }

    @Test
    void compareClasses() {
      var value = Objects.requireNonNull(DataTypes.makeClassValue(4, true));
      assertThat(
          "compare class to other class",
          value.compareTo(DataTypes.makeClassValue(3, true)),
          greaterThanOrEqualTo(1));
      assertThat(
          "compare class to same class", value.compareTo(DataTypes.makeClassValue(4, true)), is(0));
      assertThat(
          "compare class to float", value.compareTo(new Value(2.4)), greaterThanOrEqualTo(1));
      assertThat("compare class to int", value.compareTo(new Value(2)), greaterThanOrEqualTo(1));
      assertThat(
          "compare class to monster",
          value.compareTo(DataTypes.makeMonsterValue(99, true)),
          lessThanOrEqualTo(-1));
    }

    @Test
    void compareCoinmasters() {
      var value =
          Objects.requireNonNull(DataTypes.makeCoinmasterValue(DiscoGiftCoRequest.DISCO_GIFTCO));
      assertThat(
          "compare coinmaster to other coinmaster",
          value.compareTo(DataTypes.makeCoinmasterValue(NinjaStoreRequest.NINJA_STORE)),
          lessThanOrEqualTo(-1));
      assertThat(
          "compare coinmaster to same coinmaster",
          value.compareTo(DataTypes.makeCoinmasterValue(DiscoGiftCoRequest.DISCO_GIFTCO)),
          is(0));
      assertThat(
          "compare coinmaster to float", value.compareTo(new Value(2.4)), lessThanOrEqualTo(-1));
      assertThat("compare coinmaster to int", value.compareTo(new Value(2)), lessThanOrEqualTo(-1));
      assertThat(
          "compare coinmaster to monster",
          value.compareTo(DataTypes.makeMonsterValue(453, true)),
          lessThanOrEqualTo(-1));
    }

    @Test
    void compareEffects() {
      var value = Objects.requireNonNull(DataTypes.makeEffectValue(10, true));
      assertThat(
          "compare effect to other effect",
          value.compareTo(DataTypes.makeEffectValue(9, true)),
          greaterThanOrEqualTo(1));
      assertThat(
          "compare effect to same effect",
          value.compareTo(DataTypes.makeEffectValue(10, true)),
          is(0));
      assertThat(
          "compare effect to float", value.compareTo(new Value(3.8)), greaterThanOrEqualTo(1));
      assertThat("compare effect to int", value.compareTo(new Value(3)), greaterThanOrEqualTo(1));
      assertThat(
          "compare effect to monster",
          value.compareTo(DataTypes.makeMonsterValue(134, true)),
          lessThanOrEqualTo(-1));
    }

    @Test
    void compareElements() {
      var value = Objects.requireNonNull(DataTypes.makeElementValue(Element.COLD));
      assertThat(
          "compare element to other element",
          value.compareTo(DataTypes.makeElementValue(Element.HOT)),
          lessThanOrEqualTo(-1));
      assertThat(
          "compare element to same element",
          value.compareTo(DataTypes.makeElementValue(Element.COLD)),
          is(0));
      assertThat("compare element to float", value.compareTo(new Value(3.8)), is(-1));
      assertThat("compare element to int", value.compareTo(new Value(3)), is(-1));
      assertThat(
          "compare element to location",
          value.compareTo(
              DataTypes.makeLocationValue(
                  AdventureDatabase.getAdventure("Fastest Adventurer Contest"))),
          greaterThanOrEqualTo(1));
    }

    @Test
    void compareFamiliars() {
      var value = Objects.requireNonNull(DataTypes.makeFamiliarValue(91, true));
      assertThat(
          "compare familiar to other familiar",
          value.compareTo(DataTypes.makeFamiliarValue(140, true)),
          lessThanOrEqualTo(-1));
      assertThat(
          "compare familiar to same familiar",
          value.compareTo(DataTypes.makeFamiliarValue(91, true)),
          is(0));
      assertThat(
          "compare familiar to float", value.compareTo(new Value(8.8)), greaterThanOrEqualTo(1));
    }

    @Test
    void compareInts() {
      var value = new Value(150);
      assertThat(
          "compare int to other int", value.compareTo(new Value(200)), lessThanOrEqualTo(-1));
      assertThat("compare int to same int", value.compareTo(new Value(150)), is(0));
      assertThat(
          "compare int to equivalent path",
          new Value(7).compareTo(DataTypes.makePathValue(Path.TRENDY)),
          is(0));
      assertThat(
          "compare int to equivalent monster",
          value.compareTo(DataTypes.makeMonsterValue(MonsterDatabase.findMonster("batrat"))),
          is(0));

      assertThat(
          "compare int to thrall",
          value.compareTo(DataTypes.makeThrallValue(2, true)),
          greaterThanOrEqualTo(1));

      // Coinmasters have an int value of 0
      assertThat(
          "compare int to coinmaster",
          new Value(0).compareTo(DataTypes.makeCoinmasterValue(NinjaStoreRequest.NINJA_STORE)),
          is(0));
    }

    @Test
    void compareItems() {
      var value = Objects.requireNonNull(DataTypes.makeItemValue(33, true));
      assertThat(
          "compare item to other item",
          value.compareTo(DataTypes.makeItemValue(210, true)),
          lessThanOrEqualTo(-1));
      assertThat(
          "compare item to same item", value.compareTo(DataTypes.makeItemValue(33, true)), is(0));
      assertThat(
          "compare item to skill",
          value.compareTo(DataTypes.makeSkillValue(30, true)),
          greaterThanOrEqualTo(1));
    }

    @Test
    void compareFloats() {
      var value = new Value(5.2);
      assertThat(
          "compare float to other float",
          value.compareTo(new Value(4.69)),
          greaterThanOrEqualTo(1));
      assertThat("compare float to same float", value.compareTo(new Value(5.2)), is(0));
      assertThat(
          "compare float to boolean", value.compareTo(new Value(true)), greaterThanOrEqualTo(1));
      assertThat(
          "compare float to familiar",
          value.compareTo(DataTypes.makeFamiliarValue(6, true)),
          lessThanOrEqualTo(-1));
    }

    @Test
    void compareMonsters() {
      var vampire = Objects.requireNonNull(DataTypes.makeMonsterValue(1, true));
      assertThat(
          "compare two known monster ids by id",
          vampire.compareTo(DataTypes.makeMonsterValue(MonsterDatabase.findMonster("dodecapede"))),
          lessThanOrEqualTo(-1));

      var aps =
          Objects.requireNonNull(
              DataTypes.makeMonsterValue(MonsterDatabase.findMonster("ancient protector spirit")));

      assertThat(
          "compare unknown monster id to known monster id by name",
          vampire.compareTo(aps),
          greaterThanOrEqualTo(1));
      assertThat(
          "compare known monster id to unknown monster id by name",
          aps.compareTo(vampire),
          lessThanOrEqualTo(-1));
    }

    @Test
    void comparePaths() {
      var value = DataTypes.makePathValue(Path.AVATAR_OF_BORIS);
      assertThat(
          "compare path to other path",
          value.compareTo(DataTypes.makePathValue(Path.BEES_HATE_YOU)),
          greaterThanOrEqualTo(1));
      assertThat(
          "compare path to same path",
          value.compareTo(DataTypes.makePathValue(Path.AVATAR_OF_BORIS)),
          is(0));
      assertThat(
          "compare path to equivalent string",
          value.compareTo(new Value("Avatar of Boris")),
          is(0));
      assertThat("compare path to equivalent int", value.compareTo(new Value(8)), is(0));
    }

    @Test
    void compareStrings() {
      var value = new Value("some random string");
      assertThat(
          "compare string to other string",
          value.compareTo(new Value("zzz")),
          lessThanOrEqualTo(-1));
      assertThat(
          "compare string to same string", value.compareTo(new Value("some random string")), is(0));
      assertThat(
          "compare string to equivalent path",
          new Value("Trendy").compareTo(DataTypes.makePathValue(Path.TRENDY)),
          is(0));
    }

    @Test
    void compareVykeas() {
      var companion = VYKEACompanionData.fromString("level 1 blood dresser");
      var value = Objects.requireNonNull(DataTypes.makeVykeaValue(companion, true));
      //noinspection EqualsWithItself
      assertThat("compare vykea to same vykea", value.compareTo(value), is(0));
      assertThat(
          "compare vykea to different type vykea",
          value.compareTo(
              DataTypes.makeVykeaValue(VYKEACompanionData.fromString("level 5 blood couch"), true)),
          lessThanOrEqualTo(-1));
      assertThat(
          "compare vykea to same type different rune vykea",
          value.compareTo(
              DataTypes.makeVykeaValue(
                  VYKEACompanionData.fromString("level 4 frenzy dresser"), true)),
          greaterThanOrEqualTo(1));
      assertThat(
          "compare vykea to same type same rune different level vykea",
          value.compareTo(
              DataTypes.makeVykeaValue(
                  VYKEACompanionData.fromString("level 2 blood dresser"), true)),
          lessThanOrEqualTo(-1));
      assertThat(
          "compare vykea to same type same rune same level vykea",
          value.compareTo(
              DataTypes.makeVykeaValue(
                  VYKEACompanionData.fromString("level 1 blood dresser"), true)),
          is(0));

      assertThat(
          "compare vykea to string", value.compareTo(new Value("la")), greaterThanOrEqualTo(1));
      assertThat(
          "compare vykea to string", value.compareTo(new Value("lz")), lessThanOrEqualTo(-1));
    }
  }

  @Test
  void compareToIgnoreCase() {
    assertThat(new Value("aaa").compareToIgnoreCase(new Value("aAa")), is(0));
  }
}
