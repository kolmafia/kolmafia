package net.sourceforge.kolmafia.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.MonsterData.Attribute;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MonsterManuelManagerTest {

  @BeforeAll
  private static void beforeAll() {
    // Simulate logging out and back in again.
    Preferences.saveSettingsToFile = false;
  }

  @AfterAll
  private static void afterAll() {
    MonsterManuelManager.flushCache();
  }

  @BeforeEach
  private void beforeEach() {
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

  static String loadHTMLResponse(String path) throws IOException {
    // Load the responseText from saved HTML file
    return Files.readString(Paths.get(path)).trim();
  }

  @Test
  public void thatCanDetectNewMonster() throws IOException {
    String text = loadHTMLResponse("request/test_monster_manuel_monster1.html");
    MonsterData monster = MonsterDatabase.findMonster("a massive prism of grey goo");
    int monsterId = monster.getId();
    var cleanups = unregisterMonster(monster);
    try (cleanups) {
      MonsterData newMonster = MonsterManuelManager.registerMonster(monsterId, text);
      String attributes = newMonster.getAttributes();
      assertEquals(
          "BOSS NOCOPY Atk: 10 Def: 10 HP: 1000 Init: -10000 P: construct Article: a", attributes);
      List<String> factoids = MonsterManuelManager.getFactoids(monsterId);
      assertEquals(3, factoids.size());
    }
  }

  @Test
  public void thatCanDetectNewScalingMonster() throws IOException {
    String text = loadHTMLResponse("request/test_monster_manuel_monster2.html");
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
  public void thatCanDetectNewArticle() throws IOException {
    String text = loadHTMLResponse("request/test_monster_manuel_monster3.html");
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
