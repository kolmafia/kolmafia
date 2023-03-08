package net.sourceforge.kolmafia.session;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;

public class RufusManager {
  private RufusManager() {}

  // First thing.  There's a big shadow entity.  In the place those rifts connect to.
  //
  // It's a big spire.  Kinda like a radio tower.
  // It's an orrery.  It probably looks like a model of a solar system, only gigantic.
  // It's a tongue.  Just... a big tongue, as far as I can tell.
  // It's something like a scythe.  Maybe just... a scythe.  But a big one.
  // It's a cauldron.  Big.  Shaped like an iron one, but not made of iron.
  // It's a... matrix.  A manifold.  It's kind of a... grid?  You'll know it when you see it.
  //
  // I've also detected an artifact I need somebody to recover for me.
  //
  // A shadow lighter,
  // A shadow heptahedron,
  // A shadow snowflake,
  // A shadow heart,
  // A shadow bucket,
  // A shadow wave,
  //
  // I can also always use samples of more mundane items from the rifts.
  //
  // Right now, 3 handfuls of shadow venom would be valuable.
  // Right now, 3 shadow bricks would be valuable.

  private static final Pattern ENTITY =
      Pattern.compile("(spire|orrery|tongue|scythe|cauldron|matrix)");
  private static final Pattern ARTIFACT =
      Pattern.compile("(lighter|heptahedron|snowflake|heart|bucket|wave)");
  private static final Pattern ITEMS = Pattern.compile("3 (.*?shadow .*?) would be valuable");

  public static void parseCall(final String text) {
    var entityMatcher = ENTITY.matcher(text);
    if (entityMatcher.find()) {
      String entity = "shadow " + entityMatcher.group(1);
      Preferences.setString("rufusDesiredEntity", entity);
    }
    var artifactMatcher = ARTIFACT.matcher(text);
    if (artifactMatcher.find()) {
      String artifact = "shadow " + artifactMatcher.group(1);
      Preferences.setString("rufusDesiredArtifact", artifact);
    }
    var itemMatcher = ITEMS.matcher(text);
    if (itemMatcher.find()) {
      // This will be a plural name
      String items = itemMatcher.group(1);
      // Convert to itemId and back to non-plural ItemName
      int itemId = ItemDatabase.getItemId(items, 3);
      String itemName = ItemDatabase.getItemName(itemId);
      Preferences.setString("rufusDesiredItems", itemName);
    }
  }

  public static void parseCallBack(final String text) {}

  public static void parseCallResponse(final String text, int option) {
    switch (option) {
      case 6 -> {
        // Hang up
        QuestDatabase.setQuestProgress(Quest.RUFUS, QuestDatabase.UNSTARTED);
        return;
      }
      case 1 -> {
        Preferences.setString("rufusQuestType", "entity");
        Preferences.setString("rufusQuestTarget", Preferences.getString("rufusDesiredEntity"));
      }
      case 2 -> {
        // You tell Rufus you'll retrieve the artifact for him.
        Preferences.setString("rufusQuestType", "artifact");
        Preferences.setString("rufusQuestTarget", Preferences.getString("rufusDesiredArtifact"));
      }
      case 3 -> {
        Preferences.setString("rufusQuestType", "items");
        Preferences.setString("rufusQuestTarget", Preferences.getString("rufusDesiredItems"));
      }
    }
    QuestDatabase.setQuestProgress(Quest.RUFUS, QuestDatabase.STARTED);
    Preferences.setString("rufusDesiredEntity", "");
    Preferences.setString("rufusDesiredArtifact", "");
    Preferences.setString("rufusDesiredItems", "");
  }

  public static void parseCallBackResponse(final String text, int option) {
    switch (option) {
      case 1 -> {
        // "Yeah, I got it."
        if (text.contains("Rufus's shadow lodestone")) {
          switch (Preferences.getString("rufusQuestType")) {
            case "artifact" -> {
              String artifact = Preferences.getString("rufusQuestTarget");
              int itemId = ItemDatabase.getExactItemId(artifact);
              ResultProcessor.removeItem(itemId);
            }
            case "items" -> {
              String item = Preferences.getString("rufusQuestTarget");
              int itemId = ItemDatabase.getExactItemId(item);
              ResultProcessor.processResult(ItemPool.get(itemId, -3));
            }
          }
          QuestDatabase.setQuestProgress(Quest.RUFUS, QuestDatabase.UNSTARTED);
          Preferences.setString("rufusQuestType", "");
          Preferences.setString("rufusQuestTarget", "");
        }
      }
      case 5 -> {
        // "I have not got it."
      }
      case 6 -> {
        // Hang up
        return;
      }
    }
  }
}
