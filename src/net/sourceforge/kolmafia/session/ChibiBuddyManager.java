package net.sourceforge.kolmafia.session;

import java.util.Random;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ChibiBuddyManager {
  private static final Pattern CHIBI_NAME =
      Pattern.compile("&quot;I am (.*?), and I am sure we will be the best of friends!&quot;");

  private static final Pattern CHIBI_STATS =
      Pattern.compile("<td height=25>(.*?): </td><td><img.*?title=\"(\\d+) dots\"></td>");

  private static final Pattern CHIBI_AGE = Pattern.compile("</s><center>.*? is (\\d) days old.<p>");

  private ChibiBuddyManager() {}

  private static String randomName() {
    var contacts =
        ContactManager.getMailContacts().stream().filter(Predicate.not(String::isEmpty)).toList();
    var rng = new Random();
    var player =
        (contacts.size() == 0)
            ? KoLCharacter.getUserName()
            : contacts.get(rng.nextInt(contacts.size()));
    return "Li'l " + player;
  }

  public static void ensureLiveChibi() {
    if (!haveChibiBuddy()) {
      return;
    }

    if (haveChibiBuddyOn()) {
      InventoryManager.retrieveItem(ItemPool.CHIBIBUDDY_ON);
      RequestThread.postRequest(
          new GenericRequest("inv_use.php?whichitem=" + ItemPool.CHIBIBUDDY_ON));
      RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=627&option=7"));
    }

    if (!haveChibiBuddyOn()) {
      InventoryManager.retrieveItem(ItemPool.CHIBIBUDDY_OFF);
      var req = new GenericRequest("inv_use.php?whichitem=" + ItemPool.CHIBIBUDDY_OFF);
      RequestThread.postRequest(req);
      if (ChoiceManager.currentChoice() == 633) {
        // Users can provide their own names using the choice adventure prefs.
        // Otherwise we'll pick a Li'l Friend for them
        var query = Preferences.getString("choiceAdventure633");
        if (query.isEmpty()) query = "1&chibiname=" + randomName();

        req.constructURLString("choice.php?whichchoice=633&option=" + query);
        RequestThread.postRequest(req);
      }
      if (ChoiceManager.currentChoice() == 627) {
        req.constructURLString("choice.php?whichchoice=627&option=7");
        RequestThread.postRequest(req);
      }
    }
  }

  public static void chat() {
    if (!haveChibiBuddyOn()) return;
    RequestThread.postRequest(
        new GenericRequest("inv_use.php?whichitem=" + ItemPool.CHIBIBUDDY_ON));
    RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=627&option=5"));
  }

  public static boolean haveChibiBuddyOn() {
    return InventoryManager.getAccessibleCount(ItemPool.CHIBIBUDDY_ON) > 0;
  }

  public static boolean haveChibiBuddyOff() {
    return InventoryManager.getAccessibleCount(ItemPool.CHIBIBUDDY_OFF) > 0;
  }

  public static boolean haveChibiBuddy() {
    return haveChibiBuddyOn() || haveChibiBuddyOff();
  }

  public static int getDaysSinceLastVisit() {
    var currentDays = KoLCharacter.getCurrentDays();
    var lastVisited = Preferences.getInteger("chibiLastVisit");
    if (lastVisited < 0) return -1;
    return currentDays - lastVisited;
  }

  public static int getAge() {
    var currentDays = KoLCharacter.getCurrentDays();
    var birthday = Preferences.getInteger("chibiBirthday");
    if (birthday < 0) return -1;
    return (currentDays - birthday) + 1;
  }

  public static void visit(final int choice, final String text) {
    if (text.contains("<b>Oh no!</b>") || text.contains("but the batteries are dead")) {
      if (Preferences.getInteger("chibiBirthday") >= 0) {
        var message =
            "Oh no! Your ChibiBuddy&trade; "
                + Preferences.getString("chibiName")
                + " died aged "
                + getAge();
        RequestLogger.printLine(message);
        RequestLogger.updateSessionLog(message);
      }

      // Adventures are reset but ChibiChanged is not
      Preferences.resetToDefault(
          "_chibiAdventures",
          "chibiAlignment",
          "chibiBirthday",
          "chibiFitness",
          "chibiIntelligence",
          "chibiLastVisit",
          "chibiName",
          "chibiSocialization");
      ResultProcessor.processItem(ItemPool.CHIBIBUDDY_ON, -1);
      ResultProcessor.processItem(ItemPool.CHIBIBUDDY_OFF, 1);
      return;
    }

    var daycount = KoLCharacter.getCurrentDays();
    Preferences.setInteger("chibiLastVisit", daycount);

    var ageMatcher = CHIBI_AGE.matcher(text);
    if (ageMatcher.find()) {
      Preferences.setInteger(
          "chibiBirthday", daycount - StringUtilities.parseInt(ageMatcher.group(1)));
    }

    if (text.contains("value=\"Put your ChibiBuddy&trade; away\"")) {
      Preferences.setBoolean("_chibiChanged", !text.contains("value=\"Have a ChibiChat&trade;\">"));
    }

    var statMatcher = CHIBI_STATS.matcher(text);

    while (statMatcher.find()) {
      var stat = statMatcher.group(1);
      var value = StringUtilities.parseInt(statMatcher.group(2));
      Preferences.setInteger("chibi" + stat, value);
    }
  }

  public static void postChoice(final int choice, final int decision, final String text) {
    switch (choice) {
      case 627:
        if (decision == 5) {
          var message = "Having a ChibiChat&trade;";
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
          Preferences.setBoolean("_chibiChanged", true);
        }
        break;
      case 628:
      case 629:
      case 630:
      case 631:
        if (!text.contains("Results:")) return;
        if (decision == 1 || decision == 2) {
          var message =
              "["
                  + KoLAdventure.getAdventureCount()
                  + "] "
                  + Preferences.getString("lastEncounter");
          RequestLogger.printLine();
          RequestLogger.updateSessionLog();
          RequestLogger.printLine(message);
          RequestLogger.updateSessionLog(message);
          Preferences.increment("_chibiAdventures", 1, 5, false);
        }
        break;
      case 633:
        if (decision == 1) {
          ResultProcessor.processItem(ItemPool.CHIBIBUDDY_OFF, -1);
          ResultProcessor.processItem(ItemPool.CHIBIBUDDY_ON, 1);

          var matcher = CHIBI_NAME.matcher(text);
          if (matcher.find()) {
            var name = matcher.group(1);
            Preferences.setString("chibiName", name);

            var message =
                "Welcome to the world "
                    + name
                    + ", your new ChibiBuddy&trade; who will surely never die";
            RequestLogger.printLine(message);
            RequestLogger.updateSessionLog(message);
          }

          Preferences.setInteger("chibiBirthday", KoLCharacter.getCurrentDays());
          Preferences.setInteger("chibiLastVisit", KoLCharacter.getCurrentDays());
        }
        break;
    }
  }
}
