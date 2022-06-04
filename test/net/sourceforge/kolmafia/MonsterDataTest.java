package net.sourceforge.kolmafia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import net.sourceforge.kolmafia.MonsterData.Attribute;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;
import net.sourceforge.kolmafia.session.EncounterManager.EncounterType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MonsterDataTest {

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
      String attributes = "EA: spooky";
      Map<Attribute, Object> attributeMap = MonsterData.attributeStringToMap(name, attributes);
      assertEquals(1, attributeMap.size());
      Object EA = attributeMap.get(Attribute.EA);
      assertTrue(EA instanceof Element);
      assertEquals(Element.SPOOKY, (Element) EA);
      String normalized = MonsterData.attributeMapToString(attributeMap);
      assertEquals(attributes, normalized);
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
  }
}
