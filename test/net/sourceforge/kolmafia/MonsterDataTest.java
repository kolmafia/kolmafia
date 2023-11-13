package net.sourceforge.kolmafia;

import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withMoxie;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.helpers.Player;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import net.sourceforge.kolmafia.MonsterData.Attribute;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EncounterManager.EncounterType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class MonsterDataTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("MonsterDataTest");
    Preferences.reset("MonsterDataTest");
  }

  @Nested
  class Attributes {
    @Test
    public void canNormalizeSimpleNumericAttribiuteString() {
      String name = "scary monster";
      String attributes = "Def: 13 HP: 13 Atk: 13";
      Map<Attribute, Object> attributeMap = MonsterData.attributeStringToMap(name, attributes);
      assertEquals(3, attributeMap.size());
      Object attack = attributeMap.get(Attribute.ATTACK);
      assertTrue(attack instanceof Integer);
      assertEquals(13, (Integer) attack);
      Object defense = attributeMap.get(Attribute.DEFENSE);
      assertTrue(defense instanceof Integer);
      assertEquals(13, (Integer) defense);
      Object HP = attributeMap.get(Attribute.HP);
      assertTrue(HP instanceof Integer);
      assertEquals(13, (Integer) HP);
      String normalized = MonsterData.attributeMapToString(attributeMap);
      assertEquals("Atk: 13 Def: 13 HP: 13", normalized);
    }

    @Test
    public void canHandleNumericAttributesWithExpressions() {
      String name = "scary monster";
      String attributes = "Atk: [MOX+100] Def: [MUS+100] HP: [HP*2]";
      Map<Attribute, Object> attributeMap = MonsterData.attributeStringToMap(name, attributes);
      assertEquals(3, attributeMap.size());
      Object attack = attributeMap.get(Attribute.ATTACK);
      assertTrue(attack instanceof String);
      assertEquals("MOX+100", (String) attack);
      Object defense = attributeMap.get(Attribute.DEFENSE);
      assertTrue(defense instanceof String);
      assertEquals("MUS+100", (String) defense);
      Object HP = attributeMap.get(Attribute.HP);
      assertTrue(HP instanceof String);
      assertEquals("HP*2", (String) HP);
      String normalized = MonsterData.attributeMapToString(attributeMap);
      assertEquals(attributes, normalized);
    }

    @Test
    public void canHandleAttributesWithStrings() {
      String name = "scary monster";
      String attributes =
          "Poison: \"Really Quite Poisoned\" Manuel: \"scary monster\" Wiki: \"scary monster\"";
      Map<Attribute, Object> attributeMap = MonsterData.attributeStringToMap(name, attributes);
      assertEquals(3, attributeMap.size());
      Object wiki = attributeMap.get(Attribute.WIKI_NAME);
      assertTrue(wiki instanceof String);
      assertEquals("scary monster", (String) wiki);
      Object manuel = attributeMap.get(Attribute.MANUEL_NAME);
      assertTrue(manuel instanceof String);
      assertEquals("scary monster", (String) manuel);
      Object poison = attributeMap.get(Attribute.POISON);
      assertTrue(poison instanceof Integer);
      assertEquals(3, (Integer) poison);
      String normalized = MonsterData.attributeMapToString(attributeMap);
      assertEquals(attributes, normalized);
    }

    @Test
    public void canHandleKeywordAttributes() {
      String name = "scary monster";
      String attributes = "BOSS ULTRARARE GHOST";
      Map<Attribute, Object> attributeMap = MonsterData.attributeStringToMap(name, attributes);
      assertEquals(3, attributeMap.size());
      Object boss = attributeMap.get(Attribute.BOSS);
      assertTrue(boss instanceof Boolean);
      assertEquals(true, (Boolean) boss);
      Object ultrarare = attributeMap.get(Attribute.ULTRARARE);
      assertTrue(ultrarare instanceof Boolean);
      assertEquals(true, (Boolean) ultrarare);
      Object ghost = attributeMap.get(Attribute.GHOST);
      assertTrue(ghost instanceof Boolean);
      assertEquals(true, (Boolean) ghost);
      String normalized = MonsterData.attributeMapToString(attributeMap);
      assertEquals(attributes, normalized);
    }

    @Test
    public void canHandleIntegerAttributes() {
      String name = "scary monster";
      String attributes = "Meat: 100 Group: 13";
      Map<Attribute, Object> attributeMap = MonsterData.attributeStringToMap(name, attributes);
      assertEquals(2, attributeMap.size());
      Object meat = attributeMap.get(Attribute.MEAT);
      assertTrue(meat instanceof Integer);
      assertEquals(100, (Integer) meat);
      Object group = attributeMap.get(Attribute.GROUP);
      assertTrue(group instanceof Integer);
      assertEquals(13, (Integer) group);
      String normalized = MonsterData.attributeMapToString(attributeMap);
      assertEquals(attributes, normalized);
    }

    @Test
    public void canHandleElementalAttack() {
      String name = "scary monster";
      // No elemental attack
      String attributes = "";
      Map<Attribute, Object> attributeMap = MonsterData.attributeStringToMap(name, attributes);
      assertEquals(0, attributeMap.size());

      // One elemental attack
      attributes = "EA: spooky";
      attributeMap = MonsterData.attributeStringToMap(name, attributes);
      assertEquals(1, attributeMap.size());
      Object EA = attributeMap.get(Attribute.EA);
      assertTrue(EA instanceof Element);
      assertEquals(Element.SPOOKY, (Element) EA);
      String normalized = MonsterData.attributeMapToString(attributeMap);
      assertEquals(attributes, normalized);

      // Two elemental attack
      attributes = "EA: spooky EA: sleaze";
      attributeMap = MonsterData.attributeStringToMap(name, attributes);
      assertEquals(1, attributeMap.size());
      EA = attributeMap.get(Attribute.EA);
      assertTrue(EA instanceof EnumSet);
      EnumSet<Element> elements = (EnumSet<Element>) EA;
      assertEquals(2, elements.size());
      assertTrue(elements.contains(Element.SPOOKY));
      assertTrue(elements.contains(Element.SLEAZE));
      normalized = MonsterData.attributeMapToString(attributeMap);
      assertEquals("EA: sleaze EA: spooky", normalized);
    }

    @Test
    public void canHandleElementalDefense() {
      String name = "scary monster";
      String attributes = "ED: spooky";
      Map<Attribute, Object> attributeMap = MonsterData.attributeStringToMap(name, attributes);
      assertEquals(1, attributeMap.size());
      Object ED = attributeMap.get(Attribute.ED);
      assertTrue(ED instanceof Element);
      assertEquals(Element.SPOOKY, (Element) ED);
      String normalized = MonsterData.attributeMapToString(attributeMap);
      assertEquals(attributes, normalized);
    }

    @Test
    public void canHandleElement() {
      String name = "scary monster";
      String attributes = "E: spooky";
      Map<Attribute, Object> attributeMap = MonsterData.attributeStringToMap(name, attributes);
      assertEquals(2, attributeMap.size());
      Object EA = attributeMap.get(Attribute.EA);
      assertTrue(EA instanceof Element);
      assertEquals(Element.SPOOKY, (Element) EA);
      Object ED = attributeMap.get(Attribute.ED);
      assertTrue(ED instanceof Element);
      assertEquals(Element.SPOOKY, (Element) ED);
      String normalized = MonsterData.attributeMapToString(attributeMap);
      assertEquals(attributes, normalized);
    }

    @Test
    public void canHandlePhylum() {
      String name = "scary monster";
      String attributes = "P: weird";
      Map<Attribute, Object> attributeMap = MonsterData.attributeStringToMap(name, attributes);
      assertEquals(1, attributeMap.size());
      Object phylum = attributeMap.get(Attribute.PHYLUM);
      assertTrue(phylum instanceof Phylum);
      assertEquals(Phylum.WEIRD, (Phylum) phylum);
      String normalized = MonsterData.attributeMapToString(attributeMap);
      assertEquals(attributes, normalized);
    }

    @Test
    public void canHandleDefaultedAttributes() {
      String name = "scary monster";
      String attributes = "Scale: ? Cap: ? Floor: ?";
      Map<Attribute, Object> attributeMap = MonsterData.attributeStringToMap(name, attributes);
      assertEquals(3, attributeMap.size());
      Object scale = attributeMap.get(Attribute.SCALE);
      assertTrue(scale instanceof String);
      assertEquals("?", (String) scale);
      Object cap = attributeMap.get(Attribute.CAP);
      assertTrue(cap instanceof String);
      assertEquals("?", (String) cap);
      Object floor = attributeMap.get(Attribute.FLOOR);
      assertTrue(floor instanceof String);
      assertEquals("?", (String) floor);
      String normalized = MonsterData.attributeMapToString(attributeMap);
      assertEquals(attributes, normalized);

      // Make a monster and see that defaults have been applied
      int id = 13;
      String[] images = {"scary.gif"};
      MonsterData monster = new MonsterData(name, id, images, attributes);
      assertEquals(MonsterData.DEFAULT_SCALE, monster.getScale());
      assertEquals(MonsterData.DEFAULT_CAP, monster.getCap());
      assertEquals(MonsterData.DEFAULT_FLOOR, monster.getFloor());
    }

    @Test
    public void canMakeMonsterDataFromAttributes() {
      String name = "scary monster";
      int id = 13;
      String[] images = {"scary.gif"};
      String attributes =
          "BOSS NOBANISH NOCOPY FREE WANDERER Def: 37 HP: 666 Atk: 13 Init: 1000 E: spooky Phys: [50] Elem: 50 Meat: 100 SprinkleMin: 10 SprinkleMax: 20 Group: 13 P: horror GHOST Poison: \"Really Quite Poisoned\" Manuel: \"scary monster\" Wiki: \"scary monster\" Article: a";
      MonsterData monster = new MonsterData(name, id, images, attributes);
      assertEquals(13, monster.getRawAttack());
      assertEquals(37, monster.getRawDefense());
      assertEquals(666, monster.getRawHP());
      assertEquals(1000, monster.getRawInitiative());
      assertEquals(Element.SPOOKY, monster.getAttackElement());
      assertEquals(Element.SPOOKY, monster.getDefenseElement());
      assertEquals(50, monster.getPhysicalResistance());
      assertEquals(50, monster.getElementalResistance());
      assertEquals(100, monster.getBaseMeat());
      assertEquals(10, monster.getMinSprinkles());
      assertEquals(20, monster.getMaxSprinkles());
      assertEquals(13, monster.getGroup());
      assertEquals(Phylum.HORROR, monster.getPhylum());
      assertEquals(3, monster.getPoison());
      assertTrue(monster.isBoss());
      assertTrue(monster.isNoBanish());
      assertTrue(monster.isNoCopy());
      EnumSet<EncounterType> type = monster.getType();
      assertTrue(type.contains(EncounterType.FREE_COMBAT));
      assertTrue(type.contains(EncounterType.WANDERER));
      Set<String> subtypes = monster.getSubTypes();
      assertTrue(subtypes.contains("ghost"));
      assertEquals("scary monster", monster.getWikiName());
      assertEquals("scary monster", monster.getManuelName());
      assertEquals("a", monster.getArticle());
      // *** note that MonsterData constructor does not normalize
      // *** the attributes string, although it does parse it.
      // *** This may change.
      assertEquals(attributes, monster.getAttributes());
    }

    @Test
    public void canMakeElementalAttackMonsters() {
      String name = "scary monster";
      int id = 13;
      String[] images = {"scary.gif"};

      // No Elemental Attack
      String attributes = "Atk: 13";
      MonsterData monster = new MonsterData(name, id, images, attributes);
      assertEquals(13, monster.getRawAttack());
      assertEquals(Element.NONE, monster.getAttackElement());
      assertEquals(1, monster.getAttackElements().size());
      assertTrue(monster.getAttackElements().contains(Element.NONE));
      assertEquals(attributes, monster.getAttributes());

      // One Elemental Attack
      attributes = "Atk: 13 EA: spooky";
      monster = new MonsterData(name, id, images, attributes);
      assertEquals(13, monster.getRawAttack());
      assertEquals(Element.SPOOKY, monster.getAttackElement());
      assertEquals(1, monster.getAttackElements().size());
      assertTrue(monster.getAttackElements().contains(Element.SPOOKY));
      assertEquals(attributes, monster.getAttributes());

      // Two Elemental Attacks
      attributes = "Atk: 13 EA: spooky EA: sleaze";
      monster = new MonsterData(name, id, images, attributes);
      assertEquals(13, monster.getRawAttack());
      // ** We pick the "last" - in enum order - elemental attack
      assertEquals(Element.SPOOKY, monster.getAttackElement());
      assertEquals(2, monster.getAttackElements().size());
      assertTrue(monster.getAttackElements().contains(Element.SPOOKY));
      assertTrue(monster.getAttackElements().contains(Element.SLEAZE));
      // *** MonsterData constructor does not normalize
      assertEquals(attributes, monster.getAttributes());
      // *** If it did, we would get thisL
      Map<Attribute, Object> attributeMap = MonsterData.attributeStringToMap(name, attributes);
      String normalized = MonsterData.attributeMapToString(attributeMap);
      assertEquals("Atk: 13 EA: sleaze EA: spooky", normalized);
    }

    @ParameterizedTest
    @CsvSource({"0, true", "5, true", "9, true", "10, false"})
    void firstTenSnowmenAreFree(final int freeFights, final boolean free) {
      var cleanups = new Cleanups(withProperty("_snojoFreeFights", freeFights));

      try (cleanups) {
        var monster = new MonsterData("X-32-F Combat Training Snowman", 0, new String[] {}, "");

        var matcher = contains(EncounterType.FREE_COMBAT);
        assertThat(monster.getType(), free ? matcher : not(matcher));
      }
    }

    @ParameterizedTest
    @CsvSource({"0, true", "5, true", "9, true", "10, false"})
    void firstTenNEPMonstersFree(final int freeTurns, final boolean free) {
      var cleanups = new Cleanups(withProperty("_neverendingPartyFreeTurns", freeTurns));

      try (cleanups) {
        var monster = new MonsterData("biker", 0, new String[] {}, "");

        var matcher = contains(EncounterType.FREE_COMBAT);
        assertThat(monster.getType(), free ? matcher : not(matcher));
      }
    }
  }

  @Nested
  class ItemDrops {
    @Test
    void noItemDrops() {
      var monster = MonsterDatabase.findMonster("giant amorphous blob");

      var builder = new StringBuilder();
      monster.appendItemDrops(builder);

      assertThat(builder.toString(), not(containsString("Drops: ")));
    }

    @ParameterizedTest
    @CsvSource({
      // Test regular drops
      "fluffy bunny, 'bunny liver (75)'",
      "skeleton with a mop, 'beer-soaked mop (10), ice-cold Willer (30), ice-cold Willer (30)'",
      // Test mix of pp and no pp
      "Dr. Awkward, 'Drowsy Sword (100 no pp), Staff of Fats (100 no pp), fumble formula (5 pp only)'",
      // Test mix of normal and accordion
      "bar, 'bar skin (35), baritone accordion (stealable accordion)'",
      // Test mix of item drops and bounty drops
      "novelty tropical skeleton, 'cherry (0), cherry (0), grapefruit (0), grapefruit (0), orange (0), orange (0), strawberry (0), strawberry (0), lemon (0), lemon (0), novelty fruit hat (0 cond), cherry stem (bounty)'",
      // Test fractional drops
      "stench zombie, 'Dreadsylvanian Almanac page (1 no mod), Freddy Kruegerand (5 no mod), muddy skirt (0.1 cond)'",
      // Test multi-drops
      "skulldozer, '20 skeleton (100), 10 skeleton bone (100), skulldozer egg (5)'",
      // Test variable multi-drops
      "gingerbread pigeon, '1-3 sprinkles (100)'"
    })
    void itemDropsAreRenderedProperly(final String monsterName, final String itemDropString) {
      var monster = MonsterDatabase.findMonster(monsterName);

      var builder = new StringBuilder();
      monster.appendItemDrops(builder);

      assertThat(builder.toString(), equalTo("<br />Drops: " + itemDropString));
    }
  }

  @Nested
  class MeatDrops {
    @Test
    void monsterWithoutMeatDisplaysNothing() {
      var monster = MonsterDatabase.findMonster("giant amorphous blob");

      var builder = new StringBuilder();
      monster.appendMeat(builder);

      assertThat(builder.toString(), not(containsString("Meat: ")));
    }

    @Test
    void meatDropsAreRenderedWithoutBonuses() {
      var monster = MonsterDatabase.findMonster("Knob Goblin Embezzler");

      var builder = new StringBuilder();
      monster.appendMeat(builder);

      assertThat(builder.toString(), equalTo("<br />Meat: 800 - 1200"));
    }

    @Test
    void statefulMeatDropsAreRenderedWithBonuses() {
      var monster = MonsterDatabase.findMonster("Knob Goblin Embezzler");

      var cleanups = Player.withEffect(EffectPool.FROSTY);

      try (cleanups) {
        var builder = new StringBuilder();
        monster.appendMeat(builder, true);

        assertThat(builder.toString(), equalTo("<br />Meat: 2400 - 3600"));
      }
    }
  }

  @Nested
  class SprinkleDrops {
    @Test
    void monsterWithoutSprinklesDisplaysNothing() {
      var monster = MonsterDatabase.findMonster("giant amorphous blob");

      var builder = new StringBuilder();
      monster.appendSprinkles(builder);

      assertThat(builder.toString(), not(containsString("Sprinkles: ")));
    }

    @ParameterizedTest
    @CsvSource({
      "gingerbread finance bro, '28 - 32'",
      "Judge Fudge, '100'",
    })
    void sprinkleDropsAreRenderedWithoutBonuses(final String monsterName, final String dropString) {
      var monster = MonsterDatabase.findMonster(monsterName);

      var builder = new StringBuilder();
      monster.appendSprinkles(builder);

      assertThat(builder.toString(), equalTo("<br />Sprinkles: " + dropString));
    }

    @ParameterizedTest
    @CsvSource({
      "gingerbread finance bro, '42 - 48'",
      "Judge Fudge, '150'",
    })
    void statefulSprinkleDropsAreRenderedWithBonuses(
        final String monsterName, final String dropString) {
      var monster = MonsterDatabase.findMonster(monsterName);

      var cleanups = withEffect(EffectPool.SPRINKLE_SENSE);

      try (cleanups) {
        var builder = new StringBuilder();
        monster.appendSprinkles(builder, true);

        assertThat(builder.toString(), equalTo("<br />Sprinkles: " + dropString));
      }
    }
  }

  @Nested
  class ShouldSteal {
    @Test
    public void shouldntStealIfNoItems() {
      var cleanups = withMoxie(10000);

      try (cleanups) {
        var monster = MonsterDatabase.findMonster("crate");
        assertFalse(monster.shouldSteal());
      }
    }

    @Test
    public void shouldntStealIfAllItemsAreNoPP() {
      var cleanups = withMoxie(10000);

      try (cleanups) {
        var monster = MonsterDatabase.findMonster("Arizona bark scorpion");
        assertFalse(monster.shouldSteal());
      }
    }

    @Test
    public void shouldntStealIfItemsWillSurelyDrop() {
      var cleanups = withMoxie(10000);

      try (cleanups) {
        var monster = MonsterDatabase.findMonster("Astronomer");
        assertFalse(monster.shouldSteal());
      }
    }

    @ParameterizedTest
    @CsvSource({"100,true", "1,false"})
    public void shouldStealGoallessIfOutMoxieing(int moxie, boolean shouldSteal) {
      var cleanups = withMoxie(moxie);

      try (cleanups) {
        var monster = MonsterDatabase.findMonster("scary clown");
        assertThat(monster.shouldSteal(), is(shouldSteal));
      }
    }
  }
}
