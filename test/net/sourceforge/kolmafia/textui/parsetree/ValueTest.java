package net.sourceforge.kolmafia.textui.parsetree;

import static internal.matchers.SignMatcher.Sign.NEGATIVE;
import static internal.matchers.SignMatcher.Sign.POSITIVE;
import static internal.matchers.SignMatcher.Sign.ZERO;
import static internal.matchers.SignMatcher.hasSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
      // booleans compare as ints, except to strings
      var value = new Value(true);
      assertThat("true > false", value.compareTo(new Value(false)), hasSign(POSITIVE));
      assertThat("true == true", value.compareTo(new Value(true)), hasSign(ZERO));
      assertThat("true < 5", value.compareTo(new Value(5)), hasSign(NEGATIVE));
      assertThat(
          "true > \"non numeric\"", value.compareTo(new Value("non numeric")), hasSign(POSITIVE));
      assertThat("true < 1.4", value.compareTo(new Value(1.4)), hasSign(NEGATIVE));
    }

    @Test
    void compareBounties() {
      // bounties compare as names
      var value = DataTypes.parseBountyValue("bean-shaped rock", true);
      assertThat(
          "$bounty[bean-shaped rock] < $bounty[triffid bark]",
          value.compareTo(DataTypes.parseBountyValue("triffid bark", true)),
          hasSign(NEGATIVE));
      assertThat(
          "$bounty[bean-shaped rock] == $bounty[bean-shaped rock]",
          value.compareTo(DataTypes.parseBountyValue("bean-shaped rock", true)),
          hasSign(ZERO));
      assertThat("$bounty[bean-shaped rock] > 5", value.compareTo(new Value(5)), hasSign(POSITIVE));
      assertThat(
          "$bounty[bean-shaped rock] > \"a non numeric value\"",
          value.compareTo(new Value("a non numeric value")),
          hasSign(POSITIVE));
      assertThat(
          "$bounty[bean-shaped rock] < false",
          value.compareTo(new Value(false)),
          hasSign(NEGATIVE));
    }

    @Test
    void compareBuffers() {
      // buffers compare as strings
      var contents = "contents";
      var buffer = new StringBuffer(contents);
      var value = new Value(DataTypes.BUFFER_TYPE, contents, buffer);
      // These comparisons use the contentString of the value and not the buffer.
      // But behaviour is behaviour!
      assertThat(
          "\"contents\".replace_string(\"\", \"\") < \"other\".replace_string(\"\", \"\")",
          value.compareTo(new Value(DataTypes.BUFFER_TYPE, "other", new StringBuffer("other"))),
          hasSign(NEGATIVE));
      assertThat(
          "\"contents\".replace_string(\"\", \"\") == \"contents\".replace_string(\"\", \"\")",
          value.compareTo(new Value(DataTypes.BUFFER_TYPE, contents, buffer)),
          hasSign(ZERO));
      assertThat("\"contents\" > 5", value.compareTo(new Value(5.0)), hasSign(POSITIVE));
      assertThat(
          "\"contents\" < \"non numeric\"",
          value.compareTo(new Value("non numeric")),
          hasSign(NEGATIVE));
      assertThat("\"contents\" > -30.4", value.compareTo(new Value(-30.4)), hasSign(POSITIVE));
    }

    @Test
    void compareClasses() {
      // classes compare as ids
      var value = Objects.requireNonNull(DataTypes.makeClassValue(4, true));
      assertThat(
          "$class[4] > $class[3]",
          value.compareTo(DataTypes.makeClassValue(3, true)),
          hasSign(POSITIVE));
      assertThat(
          "$class[4] == $class[3]",
          value.compareTo(DataTypes.makeClassValue(4, true)),
          hasSign(ZERO));
      assertThat("$class[4] > 2.4", value.compareTo(new Value(2.4)), hasSign(POSITIVE));
      assertThat("$class[4] > 2", value.compareTo(new Value(2)), hasSign(POSITIVE));
      assertThat(
          "$class[4] < $monster[99]",
          value.compareTo(DataTypes.makeMonsterValue(99, true)),
          hasSign(NEGATIVE));
    }

    @Test
    void compareCoinmasters() {
      // coinmasters compare as names, or int 0
      var value =
          Objects.requireNonNull(DataTypes.makeCoinmasterValue(DiscoGiftCoRequest.DISCO_GIFTCO));
      assertThat(
          "$coinmaster[Disco GiftCo] < $coinmaster[Niña Store]",
          value.compareTo(DataTypes.makeCoinmasterValue(NinjaStoreRequest.NINJA_STORE)),
          hasSign(NEGATIVE));
      assertThat(
          "$coinmaster[Disco GiftCo] < $coinmaster[Disco GiftCo]",
          value.compareTo(DataTypes.makeCoinmasterValue(DiscoGiftCoRequest.DISCO_GIFTCO)),
          hasSign(ZERO));
      assertThat(
          "$coinmaster[Disco GiftCo] > 2.4", value.compareTo(new Value(2.4)), hasSign(POSITIVE));
      assertThat("$coinmaster[Disco GiftCo] > 2", value.compareTo(new Value(2)), hasSign(POSITIVE));
      assertThat(
          "$coinmaster[Disco GiftCo] < $monster[453]",
          value.compareTo(DataTypes.makeMonsterValue(453, true)),
          hasSign(NEGATIVE));
    }

    @Test
    void compareEffects() {
      // effects compare as ids
      var value = Objects.requireNonNull(DataTypes.makeEffectValue(10, true));
      assertThat(
          "$effect[10] > $effect[9]",
          value.compareTo(DataTypes.makeEffectValue(9, true)),
          hasSign(POSITIVE));
      assertThat(
          "$effect[10] == $effect[9]",
          value.compareTo(DataTypes.makeEffectValue(10, true)),
          hasSign(ZERO));
      assertThat("$effect[10] > 3.8", value.compareTo(new Value(3.8)), hasSign(POSITIVE));
      assertThat("$effect[10] > 3", value.compareTo(new Value(3)), hasSign(POSITIVE));
      assertThat(
          "$effect[10] < 134",
          value.compareTo(DataTypes.makeMonsterValue(134, true)),
          hasSign(NEGATIVE));
    }

    @Test
    void compareElements() {
      // elements compare as names
      var value = Objects.requireNonNull(DataTypes.makeElementValue(Element.COLD));
      assertThat(
          "$element[cold] < $element[hot]",
          value.compareTo(DataTypes.makeElementValue(Element.HOT)),
          hasSign(NEGATIVE));
      assertThat(
          "$element[cold] == $element[cold]",
          value.compareTo(DataTypes.makeElementValue(Element.COLD)),
          hasSign(ZERO));
      assertThat(
          "$element[cold] < \"string\"", value.compareTo(new Value("string")), hasSign(NEGATIVE));
      assertThat("$element[cold] > 3.8", value.compareTo(new Value(3.8)), hasSign(POSITIVE));
      assertThat("$element[cold] > 3", value.compareTo(new Value(3)), hasSign(POSITIVE));
      assertThat(
          "$element[cold] > $location[Fastest Adventurer Contest]",
          value.compareTo(
              DataTypes.makeLocationValue(
                  AdventureDatabase.getAdventure("Fastest Adventurer Contest"))),
          hasSign(POSITIVE));
    }

    @Test
    void compareFamiliars() {
      // familiars compare as ids
      var value = Objects.requireNonNull(DataTypes.makeFamiliarValue(91, true));
      assertThat(
          "$familiar[91] < $familiar[140]",
          value.compareTo(DataTypes.makeFamiliarValue(140, true)),
          hasSign(NEGATIVE));
      assertThat(
          "$familiar[91] == $familiar[91]",
          value.compareTo(DataTypes.makeFamiliarValue(91, true)),
          hasSign(ZERO));
      assertThat("$familiar[91] > 8.8", value.compareTo(new Value(8.8)), hasSign(POSITIVE));
    }

    @Test
    void compareInts() {
      var value = new Value(150);
      assertThat("150 < 200", value.compareTo(new Value(200)), hasSign(NEGATIVE));
      assertThat("150 == 150", value.compareTo(new Value(150)), hasSign(ZERO));
      assertThat("150 == \"150\"", value.compareTo(new Value("150")), hasSign(ZERO));
      assertThat(
          "7 == $path[Trendy]",
          new Value(7).compareTo(DataTypes.makePathValue(Path.TRENDY)),
          hasSign(ZERO));
      assertThat(
          "7 == $monster[batrat]",
          value.compareTo(DataTypes.makeMonsterValue(MonsterDatabase.findMonster("batrat"))),
          hasSign(ZERO));
      assertThat(
          "7 > $thrall[2]", value.compareTo(DataTypes.makeThrallValue(2, true)), hasSign(POSITIVE));
      assertThat(
          "0 < $coinmaster[Niña Store]",
          new Value(0).compareTo(DataTypes.makeCoinmasterValue(NinjaStoreRequest.NINJA_STORE)),
          hasSign(NEGATIVE));
      assertThat("\"11\" < 2", new Value("11").compareTo(new Value(2)), hasSign(NEGATIVE));
    }

    @Test
    void compareItems() {
      // items compare as ids
      var value = Objects.requireNonNull(DataTypes.makeItemValue(33, true));
      assertThat(
          "$item[33] < $item[210]",
          value.compareTo(DataTypes.makeItemValue(210, true)),
          hasSign(NEGATIVE));
      assertThat(
          "$item[33] == $item[33]",
          value.compareTo(DataTypes.makeItemValue(33, true)),
          hasSign(ZERO));
      assertThat(
          "$item[33] > $skill[30]",
          value.compareTo(DataTypes.makeSkillValue(30, true)),
          hasSign(POSITIVE));
    }

    @Test
    void compareFloats() {
      var value = new Value(5.2);
      assertThat("5.2 > 4.69", value.compareTo(new Value(4.69)), hasSign(POSITIVE));
      assertThat("5.2 == 5.2", value.compareTo(new Value(5.2)), hasSign(ZERO));
      assertThat("5.2 > true", value.compareTo(new Value(true)), hasSign(POSITIVE));
      assertThat(
          "5.2 < $familiar[6]",
          value.compareTo(DataTypes.makeFamiliarValue(6, true)),
          hasSign(NEGATIVE));
    }

    @Test
    void compareMonsters() {
      // monster compare as ids if known, otherwise names
      var vampire = Objects.requireNonNull(DataTypes.makeMonsterValue(1, true));
      assertThat(
          "$monster[spooky vampire] < $monster[dodecapede]",
          vampire.compareTo(DataTypes.makeMonsterValue(MonsterDatabase.findMonster("dodecapede"))),
          hasSign(NEGATIVE));

      var aps =
          Objects.requireNonNull(
              DataTypes.makeMonsterValue(MonsterDatabase.findMonster("ancient protector spirit")));

      assertThat(
          "$monster[spooky vampire] > $monster[ancient protector spirit]",
          vampire.compareTo(aps),
          hasSign(POSITIVE));
      assertThat(
          "$monster[ancient protector spirit] < $monster[spooky vampire]",
          aps.compareTo(vampire),
          hasSign(NEGATIVE));

      var ed =
          Objects.requireNonNull(
              DataTypes.makeMonsterValue(MonsterDatabase.findMonster("Ed the Undying")));
      var ed1 =
          Objects.requireNonNull(
              DataTypes.makeMonsterValue(MonsterDatabase.findMonster("Ed the Undying (1)")));
      assertNotEquals(ed, ed1, "$monster[Ed the Undying] != $monster[Ed the Undying (1)]");
    }

    @Test
    void edSerializesConsistently() {
      var ed =
          Objects.requireNonNull(
              DataTypes.makeMonsterValue(MonsterDatabase.findMonster("Ed the Undying (1)")));
      assertEquals(ed.dumpValue(), "Ed the Undying (1)");
    }

    @Test
    void comparePaths() {
      // paths compare as ids, except to strings as strings
      var value = DataTypes.makePathValue(Path.AVATAR_OF_BORIS);
      assertThat(
          "$path[Avatar of Boris] > $path[Bees Hate You]",
          value.compareTo(DataTypes.makePathValue(Path.BEES_HATE_YOU)),
          hasSign(POSITIVE));
      assertThat(
          "$path[Avatar of Boris] == $path[Avatar of Boris]",
          value.compareTo(DataTypes.makePathValue(Path.AVATAR_OF_BORIS)),
          hasSign(ZERO));
      assertThat("$path[Avatar of Boris] == 8", value.compareTo(new Value(8)), hasSign(ZERO));
      assertThat(
          "$path[Avatar of Boris] == \"Avatar of Boris\"",
          value.compareTo(new Value("Avatar of Boris")),
          hasSign(ZERO));
    }

    @Test
    void compareStrings() {
      var value = new Value("some random string");
      assertThat(
          "\"some random string\" < \"zzz\"", value.compareTo(new Value("zzz")), hasSign(NEGATIVE));
      assertThat(
          "\"some random string\" == \"some random string\"",
          value.compareTo(new Value("some random string")),
          hasSign(ZERO));
      assertThat(
          "\"Trendy\" == $path[Trendy]",
          new Value("Trendy").compareTo(DataTypes.makePathValue(Path.TRENDY)),
          hasSign(ZERO));
      assertThat("\"25\" == 25", new Value("25").compareTo(new Value(25)), hasSign(ZERO));
    }

    @Test
    void compareVykeas() {
      // vykeas compare by type, then rune, then level
      var companion = VYKEACompanionData.fromString("level 1 blood dresser");
      var value = Objects.requireNonNull(DataTypes.makeVykeaValue(companion, true));
      //noinspection EqualsWithItself
      assertThat(
          "$vykea[level 1 blood dresser] == $vykea[level 1 blood dresser]",
          value.compareTo(value),
          hasSign(ZERO));
      assertThat(
          "$vykea[level 1 blood dresser] < $vykea[level 5 blood couch]",
          value.compareTo(
              DataTypes.makeVykeaValue(VYKEACompanionData.fromString("level 5 blood couch"), true)),
          hasSign(NEGATIVE));
      assertThat(
          "$vykea[level 1 blood dresser] > $vykea[level 4 frenzy dresser]",
          value.compareTo(
              DataTypes.makeVykeaValue(
                  VYKEACompanionData.fromString("level 4 frenzy dresser"), true)),
          hasSign(POSITIVE));
      assertThat(
          "$vykea[level 1 blood dresser] < $vykea[level 2 blood dresser]",
          value.compareTo(
              DataTypes.makeVykeaValue(
                  VYKEACompanionData.fromString("level 2 blood dresser"), true)),
          hasSign(NEGATIVE));
      assertThat(
          "$vykea[level 1 blood dresser] == $vykea[level 1 blood dresser]",
          value.compareTo(
              DataTypes.makeVykeaValue(
                  VYKEACompanionData.fromString("level 1 blood dresser"), true)),
          hasSign(ZERO));

      assertThat(
          "$vykea[level 1 blood dresser] > \"la\"",
          value.compareTo(new Value("la")),
          hasSign(POSITIVE));
      assertThat(
          "$vykea[level 1 blood dresser] < \"lz\"",
          value.compareTo(new Value("lz")),
          hasSign(NEGATIVE));
    }
  }

  @Test
  void compareToIgnoreCase() {
    assertThat("aaa ≈ aAa", new Value("aaa").compareToIgnoreCase(new Value("aAa")), hasSign(ZERO));
  }
}
