package net.sourceforge.kolmafia.request;

import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ConsequenceManager;
import net.sourceforge.kolmafia.session.InventoryManager;

public class QuestLogRequest extends GenericRequest {
  private static final Pattern HEADER_PATTERN =
      Pattern.compile("<b>([^<]*?[^>]*?)</b>(?:<p>|)<blockquote>", Pattern.DOTALL);
  private static final Pattern BODY_PATTERN =
      Pattern.compile(
          "(?<=<b>)(.*?[^<>]*?)</b><br>(.*?)(?=<p>$|<p><b>|<p></blockquote>)", Pattern.DOTALL);

  public QuestLogRequest() {
    super("questlog.php");
  }

  private static boolean finishedQuest(final String pref) {
    return Preferences.getString(pref).equals(QuestDatabase.FINISHED);
  }

  public static final boolean isDungeonOfDoomAvailable() {
    return Preferences.getInteger("lastPlusSignUnlock") == KoLCharacter.getAscensions()
        && !InventoryManager.hasItem(ItemPool.PLUS_SIGN);
  }

  public static final boolean isWhiteCitadelAvailable() {
    String pref = Preferences.getString(Quest.CITADEL.getPref());
    return pref.equals(QuestDatabase.FINISHED) || pref.equals("step5") || pref.equals("step6");
  }

  public static final boolean areFriarsAvailable() {
    return Preferences.getString(Quest.FRIAR.getPref()).equals(QuestDatabase.FINISHED);
  }

  public static final boolean isBlackMarketAvailable() {
    if (Preferences.getInteger("lastWuTangDefeated") == KoLCharacter.getAscensions()) {
      return false;
    }
    if (KoLCharacter.inNuclearAutumn()) {
      return false;
    }
    String pref = Preferences.getString(Quest.MACGUFFIN.getPref());

    return pref.equals(QuestDatabase.FINISHED) || pref.contains("step");
  }

  public static final boolean isHippyStoreAvailable() {
    return !Preferences.getString(Quest.ISLAND_WAR.getPref()).equals("step1");
  }

  @Override
  public void run() {
    KoLmafia.updateDisplay("Retrieving quest data...");
    // When KoL provides a link to the Quest log, it goes to the
    // section you visited last. Therefore, visit all sections but
    // end with page 1.

    this.addFormField("which", "3");
    super.run();

    this.addFormField("which", "2");
    super.run();

    this.addFormField("which", "1");
    super.run();
  }

  @Override
  protected boolean retryOnTimeout() {
    return true;
  }

  public static final void registerQuests(
      final boolean isExternal, final String urlString, final String responseText) {
    if (urlString.contains("which=1")
        || urlString.contains("which=7")
        || !urlString.contains("which")) {
      parseResponse(responseText, 1);
    } else if (urlString.contains("which=2")) {
      parseResponse(responseText, 2);
    } else if (urlString.contains("which=3")) {
      ConsequenceManager.parseAccomplishments(responseText);
      ChatManager.setChatLiteracy(Preferences.getBoolean("chatLiterate"));
    }
  }

  private static void parseResponse(final String responseText, final int source) {
    Matcher headers = QuestLogRequest.HEADER_PATTERN.matcher(responseText);
    HashMap<Integer, String> map = new HashMap<Integer, String>();

    while (headers.find()) {
      map.put(IntegerPool.get(headers.end()), headers.group(1));
    }

    if (!map.isEmpty() && source == 1) {
      // Some quests and quest information are known only by their absence as they do not appear in
      // completed quests
      if (!responseText.contains("Don't be Afraid of Any Ghost")) {
        Preferences.setString("ghostLocation", "");
      }
      if (!responseText.contains("New-You VIP Club")) {
        Preferences.setString("_newYouQuestMonster", "");
        Preferences.setString("_newYouQuestSkill", "");
        Preferences.setInteger("_newYouQuestSharpensDone", 0);
        Preferences.setInteger("_newYouQuestSharpensToDo", 0);
      }
      if (!responseText.contains("Doctor, Doctor")) {
        Preferences.setString("doctorBagQuestItem", "");
        Preferences.setString("doctorBagQuestLocation", "");
      }
      if (!responseText.contains("Toot!")) {
        QuestDatabase.setQuestProgress(Quest.TOOT, QuestDatabase.FINISHED);
      }
    }

    Iterator<Integer> it = map.keySet().iterator();
    while (it.hasNext()) {
      Integer key = it.next();
      String header = map.get(key);
      String cut = responseText.substring(key.intValue()).split("</blockquote>")[0];

      if (header.equals("Council Quests:")) {
        handleQuestText(cut);
      } else if (header.equals("Guild Quests:")) {
        handleQuestText(cut);
      }
      // First time I opened this today it said Miscellaneous quests, now says Other quests, so
      // check for both
      else if (header.equals("Other Quests:") || header.equals("Miscellaneous Quests:")) {
        handleQuestText(cut);
      } else {
        // encountered a section in questlog we don't know how to handle.
      }
    }

    // Some quests vanish when completed but can be inferred by the presence of a new one
    if (QuestDatabase.isQuestLaterThan(Quest.MANOR, QuestDatabase.STARTED)) {
      QuestDatabase.setQuestProgress(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.FINISHED);
    }
    if (QuestDatabase.isQuestLaterThan(Quest.SPOOKYRAVEN_BABIES, QuestDatabase.UNSTARTED)) {
      QuestDatabase.setQuestProgress(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.FINISHED);
    }
    if (QuestDatabase.isQuestLaterThan(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.UNSTARTED)) {
      QuestDatabase.setQuestProgress(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.FINISHED);
    }
    if (QuestDatabase.isQuestLaterThan(Quest.MACGUFFIN, "step1")) {
      QuestDatabase.setQuestProgress(Quest.BLACK, QuestDatabase.FINISHED);
    }
    if (QuestDatabase.isQuestLaterThan(Quest.WORSHIP, "step3")) {
      QuestDatabase.setQuestProgress(Quest.CURSES, QuestDatabase.FINISHED);
      QuestDatabase.setQuestProgress(Quest.DOCTOR, QuestDatabase.FINISHED);
      QuestDatabase.setQuestProgress(Quest.BUSINESS, QuestDatabase.FINISHED);
      QuestDatabase.setQuestProgress(Quest.SPARE, QuestDatabase.FINISHED);
    }
    if (QuestDatabase.isQuestLaterThan(Quest.PYRAMID, QuestDatabase.UNSTARTED)) {
      QuestDatabase.setQuestProgress(Quest.DESERT, QuestDatabase.FINISHED);
    }

    // Set (mostly historical) preferences we can set based on quest status
    if (QuestDatabase.isQuestLaterThan(Quest.MACGUFFIN, "step1")
        || QuestDatabase.isQuestFinished(Quest.MEATCAR)) {
      KoLCharacter.setDesertBeachAvailable();
    }
    if (QuestDatabase.isQuestLaterThan(Quest.PIRATE, QuestDatabase.UNSTARTED)
        || QuestDatabase.isQuestFinished(Quest.HIPPY)) {
      Preferences.setInteger("lastIslandUnlock", KoLCharacter.getAscensions());
    }
    if (QuestDatabase.isQuestFinished(Quest.SPOOKYRAVEN_NECKLACE)) {
      Preferences.setInteger("lastSecondFloorUnlock", KoLCharacter.getAscensions());
    }
    if (QuestDatabase.isQuestLaterThan(Quest.GARBAGE, "step7")) {
      Preferences.setInteger("lastCastleGroundUnlock", KoLCharacter.getAscensions());
    }
    if (QuestDatabase.isQuestLaterThan(Quest.GARBAGE, "step8")) {
      Preferences.setInteger("lastCastleTopUnlock", KoLCharacter.getAscensions());
    }
    if (QuestDatabase.isQuestLaterThan(Quest.WORSHIP, "step1")) {
      Preferences.setInteger("lastTempleButtonsUnlock", KoLCharacter.getAscensions());
      Preferences.setInteger("lastTempleUnlock", KoLCharacter.getAscensions());
    }
    Preferences.setBoolean(
        "middleChamberUnlock",
        QuestDatabase.isQuestLaterThan(Quest.PYRAMID, QuestDatabase.STARTED));
    Preferences.setBoolean(
        "lowerChamberUnlock", QuestDatabase.isQuestLaterThan(Quest.PYRAMID, "step1"));
    Preferences.setBoolean(
        "controlRoomUnlock", QuestDatabase.isQuestLaterThan(Quest.PYRAMID, "step2"));
    if (!Preferences.getBoolean("pyramidBombUsed")) {
      Preferences.setBoolean("pyramidBombUsed", QuestDatabase.isQuestFinished(Quest.PYRAMID));
    }
    Preferences.setBoolean(
        "bigBrotherRescued", QuestDatabase.isQuestLaterThan(Quest.SEA_MONKEES, "step1"));
    if (QuestDatabase.isQuestFinished(Quest.ISLAND_WAR)) {
      Preferences.setString("warProgress", "finished");
    } else if (QuestDatabase.isQuestLaterThan(Quest.ISLAND_WAR, QuestDatabase.STARTED)) {
      Preferences.setString("warProgress", "started");
    } else {
      Preferences.setString("warProgress", "unstarted");
    }
  }

  private static void handleQuestText(String response) {
    Matcher body = QuestLogRequest.BODY_PATTERN.matcher(response);
    // Form of.. a regex! group(1) now contains the quest title and group(2) has the details.
    while (body.find()) {
      String title = body.group(1);
      String details = body.group(2);
      String pref = QuestDatabase.titleToPref(title);
      String status = "";

      status = QuestDatabase.findQuestProgress(pref, details);

      // Debugging

      /*if ( !pref.equals( "" ) )
      {
        RequestLogger.printLine( pref + " (" + status + ")" );
      }
      else
      {
        RequestLogger.printLine( "unhandled: " + title );
      }*/

      // Once we've implemented everything, we can do some error checking to make sure we handled
      // everything
      // successfully.

      if (pref.equals("")) {
        /*KoLmafia.updateDisplay( KoLConstants.CONTINUE_STATE,
        "Unknown quest, or something went wrong while parsing questlog.php" );*/
        continue;
      }
      if (status.equals("")) {
        /*KoLmafia.updateDisplay( KoLConstants.CONTINUE_STATE,
        "Unknown quest status found while parsing questlog.php" );*/
        continue;
      }
      /*
       * if ( source == 2 && !status.equals( "finished" ) )
       * {
       * // Probably shouldn't happen. We were parsing the completed quests page but somehow didn't set a quest
       * to finished.  Possible exception happens during nemesis quest.
       * KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
       * "Something went wrong while parsing completed quests" );
       * return;
       * }
       */

      // We can't tell the difference between step1, step2 and step3 from the Quest Log, so if we
      // have some progress stored
      // from having previously examined the Bat Hole we should trust it.
      if (pref.equals(Quest.BAT.getPref())
          && (QuestDatabase.isQuestStep(Quest.BAT, "step2")
              || QuestDatabase.isQuestStep(Quest.BAT, "step3"))) {
        QuestDatabase.setQuestIfBetter(Quest.BAT, status);
        continue;
      }

      QuestDatabase.setQuestProgress(pref, status);
    }
  }

  public static boolean isTavernAvailable() {
    return QuestDatabase.isQuestLaterThan(Quest.RAT, QuestDatabase.STARTED);
  }
}
