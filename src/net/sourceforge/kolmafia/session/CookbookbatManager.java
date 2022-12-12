package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;

public class CookbookbatManager {
  private CookbookbatManager() {}

  private record QuestPattern(
      Pattern pattern, Boolean hasIngredient, Boolean hasMonster, Boolean hasLocation) {}
  ;

  private static QuestPattern[] COOKBOOKBAT_QUEST_PATTERNS = {
    new QuestPattern(
        Pattern.compile(
            "looks off in the distance before speaking, \"As I recall, (?<ingredient>.*) was common in (?<location>.*), back in my day.  Perhaps if you kill (?<monster>), you'll"),
        true,
        true,
        true),
    new QuestPattern(
        Pattern.compile(
            "My recollection is that (?<ingredient>.*) was often collected from (?<monster>.*)\\. If I recall correctly, you can hunt them in (?<location>.*)\\.\""),
        true,
        true,
        true),
    new QuestPattern(
        Pattern.compile(
            "If memory serves, (?<ingredient>.*) was very popular in (?<location>.*), during my time\\. Perhaps if you find (?<monster>.*), you'll collect one,"),
        true,
        true,
        true),
    new QuestPattern(
        Pattern.compile(
            "rattles, \"If my ancient memory serves, I suggested looking in (?<location>.*)\\."),
        false,
        false,
        true),
    new QuestPattern(
        Pattern.compile("croaks, \"If I recall, I suggested that you look for (?<monster>.*)\\.\""),
        false,
        true,
        false),
    new QuestPattern(
        Pattern.compile(
            "According to my memories, (?<monster>.*) at (?<location>.*) may have what"),
        false,
        true,
        true)
  };

  private static void wipeQuest() {
    Preferences.resetToDefault("cookbookbatQuestIngredient");
    Preferences.resetToDefault("cookbookbatQuestLocation");
    Preferences.resetToDefault("cookbookbatQuestMonster");
  }

  private static void updateQuestData(String ingredient, String location, String monster) {
    String currentIngredient = Preferences.getString("cookbookbatQuestIngredient");
    String currentLocation = Preferences.getString("cookbookbatQuestLocation");
    String currentMonster = Preferences.getString("cookbookbatQuestMonster");
    if ((ingredient != null && currentIngredient != "" && ingredient != currentIngredient)
        || (location != null && currentLocation != "" && location != currentLocation)
        || (monster != null && currentMonster != "" && monster != currentMonster)) {
      CookbookbatManager.wipeQuest();
      Preferences.resetToDefault("cookbookbatQuestTurns");
    }

    if (ingredient != null) Preferences.setString("cookbookbatQuestIngredient", ingredient);
    if (location != null) Preferences.setString("cookbookbatQuestLocation", location);
    if (monster != null) Preferences.setString("cookbookbatQuestMonster", monster);
  }

  public static void parseResponse(String responseText) {

    Matcher questCompletedMatcher =
        Pattern.compile("As I recall, this is where I told you to look").matcher(responseText);
    if (questCompletedMatcher.find()) {
      wipeQuest();
      return;
    }

    for (QuestPattern questPattern : COOKBOOKBAT_QUEST_PATTERNS) {
      Matcher matcher = questPattern.pattern.matcher(responseText);
      if (matcher.find()) {
        String ingredient = null;
        String location = null;
        String monster = null;

        if (questPattern.hasIngredient) ingredient = matcher.group("ingredient");
        if (questPattern.hasLocation) location = matcher.group("location");
        if (questPattern.hasMonster)
          monster = MonsterDatabase.findMonster(matcher.group("monster")).getName();

        updateQuestData(ingredient, location, monster);
      }
    }
  }
}
