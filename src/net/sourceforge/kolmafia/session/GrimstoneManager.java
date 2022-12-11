package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class GrimstoneManager {

  private GrimstoneManager() {}

  public static boolean isGrimstoneAdventure(KoLAdventure adventure) {
    if (adventure == null) {
      return false;
    }

    switch (adventure.getAdventureNumber()) {
      case -1 -> {
        return adventure.getAdventureId().equals("ioty2014_wolf");
      }
      case AdventurePool.YE_OLDE_MEDIEVALE_VILLAGEE,
          AdventurePool.A_DESERTED_STRETCH_OF_I_911,
          AdventurePool.INNER_WOLF_GYM,
          AdventurePool.SWEET_ADE_LAKE,
          AdventurePool.EAGER_RICE_BURROWS,
          AdventurePool.GUMDROP_FOREST,
          AdventurePool.PRINCES_RESTROOM,
          AdventurePool.PRINCES_DANCE_FLOOR,
          AdventurePool.PRINCES_KITCHEN,
          AdventurePool.PRINCES_BALCONY,
          AdventurePool.PRINCES_LOUNGE,
          AdventurePool.PRINCES_CANAPES_TABLE -> {
        return true;
      }
    }
    return false;
  }

  public static void incrementFights(int adventureId) {
    switch (adventureId) {
      case AdventurePool.YE_OLDE_MEDIEVALE_VILLAGEE -> {
        Preferences.increment("rumpelstiltskinTurnsUsed", 1);
      }
      case AdventurePool.INNER_WOLF_GYM -> {
        Preferences.increment("wolfTurnsUsed", 1);
      }
      case AdventurePool.A_DESERTED_STRETCH_OF_I_911 -> {
        Preferences.increment("hareTurnsUsed", 1);
      }
      case AdventurePool.SWEET_ADE_LAKE,
          AdventurePool.EAGER_RICE_BURROWS,
          AdventurePool.GUMDROP_FOREST -> {
        Preferences.increment("candyWitchTurnsUsed", 1);
      }
    }
  }

  // Choice Adventures:
  //
  // Grimstone Mask (pick a tale) - 829
  //
  // stepmother - 822 - 827
  // wolf -  830, 832, 833, 834
  // witch - 831, 837, 838, 839, 840, 841, 842
  // gnome - 844-850
  // hare - (none)

  // Preferences:
  //
  // grimstoneMaskPath
  // candyWitchTurnsUsed
  // candyWitchCandyTotal
  // cinderellaMinutesToMidnight
  // cinderellaScore
  // hareMillisecondsSaved
  // rumpelstiltskinKidsRescued
  // rumpelstiltskinTurnsUsed
  // wolfPigsEvicted
  // wolfTurnsUsed

  private static final Pattern CINDERELLA_SCORE_PATTERN =
      Pattern.compile("score (?:is now|was) <b>(\\d+)</b>");

  // Your final candy total is: <b>684!</b>
  private static final Pattern FINAL_CANDY_PATTERN =
      Pattern.compile("Your final candy total is: <b>(\\d+)!</b>");

  private static final Pattern LOSE_CANDY_PATTERN = Pattern.compile("<b>-(\\d+) Candy</b>");

  public static void visitChoice(String text) {
    switch (ChoiceManager.lastChoice) {
      case 822: // The Prince's Ball (In the Restroom)
      case 823: // The Prince's Ball (On the Dance Floor)
      case 824: // The Prince's Ball (The Kitchen)
      case 825: // The Prince's Ball (On the Balcony)
      case 826: // The Prince's Ball (The Lounge)
      case 827: // The Prince's Ball (At the Canapés Table)
        // stepmother
        Preferences.setString("grimstoneMaskPath", "stepmother");
        parseCinderellaTime();
        break;

      case 829: // We all Wear Masks
        break;

      case 830: // Cooldown
      case 832: // Shower Power
      case 833: // Vendi, Vidi, Vici
      case 834: // Back Room Dealings
        // wolf
        Preferences.setString("grimstoneMaskPath", "wolf");
        break;

      case 831: // Intrusion
      case 837: // On Purple Pond
      case 838: // General Mill
      case 839: // The Sounds of the Undergrounds
      case 840: // Hop on Rock Pops
      case 841: // Building, Structure, Edifice
      case 842: // The Gingerbread Warehouse
        // witch
        Preferences.setString("grimstoneMaskPath", "witch");
        break;

      case 844: // The Portal to Horrible Parents
      case 845: // Rumpelstiltskin's Workshop
      case 846: // Bartering for the Future of Innocent Children
      case 847: // Pick Your Poison
      case 848: // Where the Magic Happens
      case 849: // The Practice
      case 850: // World of Bartercraft
        // gnome
        Preferences.setString("grimstoneMaskPath", "gnome");
        RumpleManager.visitChoice(text);
        break;
    }
  }

  public static void postChoice2(String text) {
    switch (ChoiceManager.lastChoice) {
      case 822: // The Prince's Ball (In the Restroom)
      case 823: // The Prince's Ball (On the Dance Floor)
      case 824: // The Prince's Ball (The Kitchen)
      case 825: // The Prince's Ball (On the Balcony)
      case 826: // The Prince's Ball (The Lounge)
      case 827: // The Prince's Ball (At the Canapés Table)
        {
          // The Prince's Ball
          if (parseCinderellaTime() == false) {
            Preferences.decrement("cinderellaMinutesToMidnight");
          }
          Matcher matcher = CINDERELLA_SCORE_PATTERN.matcher(text);
          if (matcher.find()) {
            int score = StringUtilities.parseInt(matcher.group(1));
            if (score != -1) {
              Preferences.setInteger("cinderellaScore", score);
            }
          }
          if (text.contains("Your final score was")) {
            if (text.contains("reduced to <b>0</b> due to failure")) {
              Preferences.setInteger("cinderellaScore", 0);
            }
            Preferences.setInteger("cinderellaMinutesToMidnight", 0);
            Preferences.setString("grimstoneMaskPath", "");
          }
          break;
        }

      case 829: // We all wear masks
        if (ChoiceManager.lastDecision != 6) {
          ResultProcessor.processItem(ItemPool.GRIMSTONE_MASK, -1);
          Preferences.setInteger("cinderellaMinutesToMidnight", 0);
          // Reset preferences (if starting new gnome path).
          // In any case, destroy unused crafting materials
          RumpleManager.reset(ChoiceManager.lastDecision);
        }
        switch (ChoiceManager.lastDecision) {
          case 1 -> {
            Preferences.setInteger("cinderellaMinutesToMidnight", 30);
            Preferences.setInteger("cinderellaScore", 0);
            Preferences.setString("grimstoneMaskPath", "stepmother");
          }
          case 2 -> {
            Preferences.setInteger("wolfPigsEvicted", 0);
            Preferences.setInteger("wolfTurnsUsed", 0);
            Preferences.setString("grimstoneMaskPath", "wolf");
          }
          case 3 -> {
            Preferences.setInteger("candyWitchCandyTotal", 0);
            Preferences.setInteger("candyWitchTurnsUsed", 0);
            Preferences.setString("grimstoneMaskPath", "witch");
          }
          case 4 -> {
            // Preferences reset above
            Preferences.setString("grimstoneMaskPath", "gnome");
          }
          case 5 -> {
            Preferences.setInteger("hareMillisecondsSaved", 0);
            Preferences.setInteger("hareTurnsUsed", 0);
            Preferences.setString("grimstoneMaskPath", "hare");
          }
        }
        break;

      case 830: // Cooldown
        break;
      case 832: // Shower Power
      case 833: // Vendi, Vidi, Vici
      case 834: // Back Room Dealings
        // wolf
        Preferences.increment("wolfTurnsUsed", 1);
        break;

      case 831: // Intrusion
        // witch
        if (ChoiceManager.lastDecision == 1) {
          // The only choice

          // After 10 turns of preparation, the army of greedy children attack in
          // three waves. Each wave diminishes your candy pile.

          Matcher matcher = LOSE_CANDY_PATTERN.matcher(text);
          while (matcher.find()) {
            int candy = StringUtilities.parseInt(matcher.group(1));
            Preferences.decrement("candyWitchCandyTotal", candy);
          }

          // The game is over after the third such intrusion and your remaining
          // candy total is your final score.

          if (text.contains("Your final candy total is")) {
            matcher = FINAL_CANDY_PATTERN.matcher(text);
            if (matcher.find()) {
              int candy = StringUtilities.parseInt(matcher.group(1));
              Preferences.setInteger("candyWitchCandyTotal", candy);
            }
            Preferences.setString("grimstoneMaskPath", "");
          }
        }
        break;

      case 837: // On Purple Pond
      case 838: // General Mill
      case 839: // The Sounds of the Undergrounds
      case 840: // Hop on Rock Pops
      case 841: // Building, Structure, Edifice
      case 842: // The Gingerbread Warehouse
        // witch
        Preferences.increment("candyWitchTurnsUsed", 1);
        break;

      case 844: // The Portal to Horrible Parents
      case 845: // Rumpelstiltskin's Workshop
      case 846: // Bartering for the Future of Innocent Children
      case 847: // Pick Your Poison
      case 848: // Where the Magic Happens
      case 849: // The Practice
      case 850: // World of Bartercraft
        // gnome
        RumpleManager.postChoice2(text);
        break;
    }
  }

  private static final Pattern CINDERELLA_TIME_PATTERN =
      Pattern.compile("<i>It is (\\d+) minute(?:s) to midnight.</i>");

  private static boolean parseCinderellaTime() {
    Matcher matcher = CINDERELLA_TIME_PATTERN.matcher(ChoiceManager.lastResponseText);
    while (matcher.find()) {
      int time = StringUtilities.parseInt(matcher.group(1));
      if (time != -1) {
        Preferences.setInteger("cinderellaMinutesToMidnight", time);
        return true;
      }
    }
    return false;
  }
}
