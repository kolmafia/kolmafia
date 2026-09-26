package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.MonsterData.Attribute;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MonsterManuelManagerTest {

  @AfterAll
  public static void afterAll() {
    MonsterManuelManager.flushCache();
  }

  @BeforeEach
  public void beforeEach() {
    MonsterManuelManager.flushCache();
  }

  public static Cleanups unregisterMonster(MonsterData monster) {
    var cleanups = new Cleanups();
    MonsterDatabase.unregisterMonster(monster);
    cleanups.add(() -> MonsterDatabase.registerMonster(monster));
    return cleanups;
  }

  public static Cleanups setArticle(MonsterData monster, String newArticle) {
    var cleanups = new Cleanups();
    String article = monster.getArticle();
    monster.setArticle(newArticle);
    cleanups.add(() -> monster.setArticle(article));
    return cleanups;
  }

  @Test
  public void thatCanDetectNewMonster() {
    String text = html("request/test_monster_manuel_monster1.html");
    MonsterData monster = MonsterDatabase.findMonster("ancient unspeakable bugbear");
    int monsterId = monster.getId();
    var cleanups = unregisterMonster(monster);
    try (cleanups) {
      MonsterData newMonster = MonsterManuelManager.registerMonster(monsterId, text);
      String attributes = newMonster.getAttributes();
      assertEquals(
          "Atk: 140 Def: 126 HP: 150 Init: 50 P: beast ED: spooky Article: an", attributes);
      List<String> factoids = MonsterManuelManager.getFactoids(monsterId);
      assertEquals(3, factoids.size());
    }
  }

  @Test
  public void thatCanDetectNewScalingMonster() {
    String text = html("request/test_monster_manuel_monster2.html");
    MonsterData monster = MonsterDatabase.findMonster("Adventurer echo");
    int monsterId = monster.getId();
    var cleanups = unregisterMonster(monster);
    try (cleanups) {
      MonsterData newMonster = MonsterManuelManager.registerMonster(monsterId, text);
      String attributes = newMonster.getAttributes();
      assertEquals("Scale: ? Cap: ? Floor: ? Init: -10000 P: dude Article: an", attributes);
      List<String> factoids = MonsterManuelManager.getFactoids(monsterId);
      assertEquals(3, factoids.size());
    }
  }

  @Test
  public void thatCanDetectNewArticle() {
    String text = html("request/test_monster_manuel_monster3.html");
    MonsterData monster = MonsterDatabase.findMonster("pygmy orderlies");
    int monsterId = monster.getId();
    assertEquals("some", monster.getArticle());
    var cleanups = setArticle(monster, "scary");
    try (cleanups) {
      MonsterData newMonster = MonsterManuelManager.registerMonster(monsterId, text);
      assertEquals("scary", newMonster.getArticle());
      assertTrue(MonsterManuelManager.updates.containsKey(monsterId));
      Map<Attribute, Object> update = MonsterManuelManager.updates.get(monsterId);
      assertEquals(1, update.size());
      assertTrue(update.containsKey(Attribute.ARTICLE));
      Object value = update.get(Attribute.ARTICLE);
      assertTrue(value instanceof String);
      assertEquals("some", (String) value);
    }
  }
}
