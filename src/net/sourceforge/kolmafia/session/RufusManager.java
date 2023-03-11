package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
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
  private static final Pattern ITEMS = Pattern.compile("Right now, 3 (.*?) would be valuable");

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
      // Convert to itemId and back to non-plural item name
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
        QuestDatabase.setQuestProgress(Quest.RUFUS, QuestDatabase.STARTED);
      }
      case 2 -> {
        // You tell Rufus you'll retrieve the artifact for him.
        Preferences.setString("rufusQuestType", "artifact");
        Preferences.setString("rufusQuestTarget", Preferences.getString("rufusDesiredArtifact"));
        QuestDatabase.setQuestProgress(Quest.RUFUS, QuestDatabase.STARTED);
      }
      case 3 -> {
        Preferences.setString("rufusQuestType", "items");
        String itemName = Preferences.getString("rufusDesiredItems");
        Preferences.setString("rufusQuestTarget", itemName);
        handleShadowItems(ItemDatabase.getExactItemId(itemName));
      }
    }
    if (text.contains("Shadow Affinity")) {
      Preferences.setBoolean("_shadowAffinityToday", true);
    }
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

  public static void handleShadowItems(String itemName) {
    if (QuestDatabase.isQuestStarted(Quest.RUFUS)
        && Preferences.getString("rufusQuestType").equals("items")
        && Preferences.getString("rufusQuestTarget").equals(itemName)) {
      handleShadowItems(ItemDatabase.getExactItemId(itemName));
    }
  }

  private static void handleShadowItems(int itemId) {
    int count = InventoryManager.getCount(itemId);
    QuestDatabase.setQuestProgress(Quest.RUFUS, (count >= 3) ? "step1" : QuestDatabase.STARTED);
  }

  //  Rufus wants you to go into a Shadow Rift and defeat a shadow scythe.
  //  Call Rufus and let him know you defeated that monster.
  public static final Pattern RUFUS_ENTITY_PATTERN = Pattern.compile("defeat a (.*?)\\.");

  //  Rufus wants you to go into a Shadow Rift and find a shadow bucket.
  //  Call Rufus and tell him you found his shadow bucket.
  public static final Pattern RUFUS_ARTIFACT_PATTERN = Pattern.compile("find a (.*?)\\.");

  //  Rufus wants you to find him 3 wisps of shadow flame from Shadow Rifts.
  //  Call Rufus and tell him you've got the 3 wisps of shadow flame he wanted.
  public static final Pattern RUFUS_ITEMS_PATTERN =
      Pattern.compile("find him 3 (.*?) from Shadow Rifts\\.");

  public static String handleQuestLog(String details) {
    details = details.trim();
    if (details.startsWith("Rufus wants you")) {
      // The quest has been started
      Matcher entityMatcher = RUFUS_ENTITY_PATTERN.matcher(details);
      if (entityMatcher.find()) {
        Preferences.setString("rufusQuestType", "entity");
        Preferences.setString("rufusQuestTarget", entityMatcher.group(1));
        return QuestDatabase.STARTED;
      }
      Matcher artifactMatcher = RUFUS_ARTIFACT_PATTERN.matcher(details);
      if (artifactMatcher.find()) {
        Preferences.setString("rufusQuestType", "artifact");
        Preferences.setString("rufusQuestTarget", artifactMatcher.group(1));
        return QuestDatabase.STARTED;
      }
      Matcher itemsMatcher = RUFUS_ITEMS_PATTERN.matcher(details);
      if (itemsMatcher.find()) {
        Preferences.setString("rufusQuestType", "items");
        String items = itemsMatcher.group(1);
        // Convert to itemId and back to non-plural item name
        int itemId = ItemDatabase.getItemId(items, 3);
        String itemName = ItemDatabase.getItemName(itemId);
        Preferences.setString("rufusQuestTarget", itemName);
        return QuestDatabase.STARTED;
      }
      // This should not be possible
      return QuestDatabase.STARTED;
    }
    if (details.startsWith("Call Rufus")) {
      // You have done what Rufus wanted
      return "step1";
    }
    // This should not be possible
    return QuestDatabase.UNSTARTED;
  }
}
