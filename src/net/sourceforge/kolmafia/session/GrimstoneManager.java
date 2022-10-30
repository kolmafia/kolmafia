package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class GrimstoneManager {

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
  // cinderellaMinutesToMidnight
  // cinderellaScore
  // rumpelstiltskinTurnsUsed
  // rumpelstiltskinKidsRescued

  private static final Pattern CINDERELLA_SCORE_PATTERN =
      Pattern.compile("score (?:is now|was) <b>(\\d+)</b>");

  public static void visitChoice(String text) {
    switch (ChoiceManager.lastChoice) {
      case 822:
      case 823:
      case 824:
      case 825:
      case 826:
      case 827:
        // stepmother
        parseCinderellaTime();
        Preferences.setString("grimstoneMaskPath", "stepmother");
        break;

      case 844:
      case 845:
      case 846:
      case 847:
      case 848:
      case 849:
      case 850:
        // gnome
        RumpleManager.visitChoice(text);
        break;
    }
  }

  public static void postChoice2(String text) {
    switch (ChoiceManager.lastChoice) {
      case 822:
      case 823:
      case 824:
      case 825:
      case 826:
      case 827:
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
            Preferences.setInteger("cinderellaMinutesToMidnight", 0);
            Preferences.setString("grimstoneMaskPath", "");
          }
          break;
        }

      case 829:
        // We all wear masks
        if (ChoiceManager.lastDecision != 6) {
          ResultProcessor.processItem(ItemPool.GRIMSTONE_MASK, -1);
          Preferences.setInteger("cinderellaMinutesToMidnight", 0);
        }
        if (ChoiceManager.lastDecision == 1) {
          Preferences.setInteger("cinderellaMinutesToMidnight", 30);
          Preferences.setInteger("cinderellaScore", 0);
          Preferences.setString("grimstoneMaskPath", "stepmother");
        } else if (ChoiceManager.lastDecision == 2) {
          Preferences.setString("grimstoneMaskPath", "wolf");
        } else if (ChoiceManager.lastDecision == 3) {
          Preferences.setString("grimstoneMaskPath", "witch");
        } else if (ChoiceManager.lastDecision == 4) {
          Preferences.setString("grimstoneMaskPath", "gnome");
        } else if (ChoiceManager.lastDecision == 5) {
          Preferences.setString("grimstoneMaskPath", "hare");
        }
        RumpleManager.reset(ChoiceManager.lastDecision);
        break;

      case 844:
      case 845:
      case 846:
      case 847:
      case 848:
      case 849:
      case 850:
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
