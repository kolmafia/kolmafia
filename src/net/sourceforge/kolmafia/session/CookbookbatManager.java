package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;

public class CookbookbatManager {
  private CookbookbatManager() {}

  private static Pattern[] COOKBOOKBAT_QUEST_PATTERNS = {
    Pattern.compile(
        "looks off in the distance before speaking, \"As I recall, (?<ingredient>.*) was common in (?<location>.*), back in my day.  Perhaps if you kill (?<monster>), you'll"),
    Pattern.compile(
        "My recollection is that (?<ingredient>.*) was often collected from (?<monster>.*)\\. If I recall correctly, you can hunt them in (?<location>.*)\\.\""),
    Pattern.compile(
        "If memory serves, (?<ingredient>.*) was very popular in (?<location>.*), during my time\\. Perhaps if you find (?<monster>.*), you'll collect one,"),
  };

  public static void wipeQuest() {
    Preferences.resetToDefault("cookbookbatQuestIngredient");
    Preferences.resetToDefault("cookbookbatQuestLocation");
    Preferences.resetToDefault("cookbookbatQuestMonster");
    Preferences.resetToDefault("cookbookbatQuestTurns");
  }

  private static void updateQuestData(String ingredient, String location, String monster) {
    Preferences.setString("cookbookbatQuestIngredient", ingredient);
    Preferences.setString("cookbookbatQuestLocation", location);
    Preferences.setString("cookbookbatQuestMonster", monster);
    Preferences.resetToDefault("cookbookbatQuestTurns");
  }

  public static void parseResponse(String responseText) {
    Matcher questCompletedMatcher =
        Pattern.compile("As I recall, this is where I told you to look").matcher(responseText);
    if (questCompletedMatcher.find()) {
      wipeQuest();
      return;
    }

    for (Pattern pattern : COOKBOOKBAT_QUEST_PATTERNS) {
      Matcher matcher = pattern.matcher(responseText);
      if (matcher.find()) {
        String monsterWithArticle = matcher.group("monster");
        String monster = MonsterDatabase.findMonster(monsterWithArticle).getName();
        updateQuestData(matcher.group("ingredient"), matcher.group("location"), monster);
      }
    }
  }
}
