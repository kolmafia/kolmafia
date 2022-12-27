package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.TavernRequest;
import net.sourceforge.kolmafia.textui.command.CouncilCommand;

public class TavernManager {
  /** Completes the infamous tavern quest. */

  //  1<--2<--3<--4<--5
  //  v
  //  6   7-->8   9->10
  //  v   ^   v   ^   v
  // 11  12  13  14  15
  //  v       v   ^   v
  // 16  17  18->19  20
  //  v   ^ \
  // 21->22  23->24->25

  private static final Integer[] searchOrder = {
    // 5,
    4, 3, 2,
    1, 6, 11,
    16, 21, 22,
    17, 23, 24,
    25, 12, 7,
    8, 13, 18,
    19, 14, 9,
    10, 15, 20,
  };

  private static final int EXPLORE = 1;
  private static final int FAUCET = 2;
  private static final int BARON = 3;
  private static final int FIGHT_BARON = 4;

  private static int overrideSquare = -1;

  private TavernManager() {}

  public static int exploreTavern() {
    return TavernManager.exploreTavern(TavernManager.EXPLORE);
  }

  public static int locateTavernFaucet() {
    return TavernManager.exploreTavern(TavernManager.FAUCET);
  }

  public static int locateBaron() {
    return TavernManager.exploreTavern(TavernManager.BARON);
  }

  public static int fightBaron() {
    return TavernManager.exploreTavern(TavernManager.FIGHT_BARON);
  }

  public static String faucetSquare() {
    String layout = TavernRequest.tavernLayout();
    int faucet = layout.indexOf("3");
    return faucet == -1 ? "" : String.valueOf(faucet + 1);
  }

  public static String baronSquare() {
    String layout = TavernRequest.tavernLayout();
    int baron = layout.indexOf("4");
    return baron == -1 ? "" : String.valueOf(baron + 1);
  }

  private static int exploreTavern(final int goal) {
    if ((KoLCharacter.getLevel() < 3) && !KoLCharacter.isKingdomOfExploathing()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You need to level up first.");
      return -1;
    }

    // See if we've already found our goal within KoLmafia
    String layout = TavernRequest.tavernLayout();

    int faucet = layout.indexOf("3");
    if (goal == TavernManager.FAUCET && faucet != -1) {
      TavernManager.logFaucetSquare(faucet);
      return faucet + 1;
    }

    int baron = layout.indexOf("4");
    if (goal == TavernManager.BARON && baron != -1) {
      TavernManager.logBaronSquare(baron);
      return baron + 1;
    }

    int mansion = layout.indexOf("6");
    if ((goal == TavernManager.BARON || goal == TavernManager.FIGHT_BARON) && mansion != -1) {
      TavernManager.logMansionSquare(mansion);
      KoLmafia.updateDisplay("You already defeated Baron von Ratsworth.");
      return mansion + 1;
    }

    int unexplored = layout.indexOf("0");
    if (goal == TavernManager.EXPLORE && unexplored == -1) {
      TavernManager.logFaucetSquare(faucet);
      TavernManager.logBaronSquare(baron);
      TavernManager.logMansionSquare(mansion);
      KoLmafia.updateDisplay("Entire cellar explored");
      return 0;
    }

    // No. Go look for it.
    String message =
        goal == TavernManager.FAUCET
            ? "Searching for faucet..."
            : goal == TavernManager.BARON
                ? "Searching for Baron von Ratsworth..."
                : goal == TavernManager.FIGHT_BARON
                    ? "Going after Baron von Ratsworth..."
                    : "Exploring rest of cellar...";

    KoLmafia.updateDisplay(message);

    // Make sure we have the quest from the council
    RequestThread.postRequest(CouncilCommand.COUNCIL_VISIT);

    // Make sure Bart Ender has given us access to the cellar
    GenericRequest barkeep = new GenericRequest("tavern.php?place=barkeep");
    RequestThread.postRequest(barkeep);
    // *** Should look at response and make sure we got there

    // Visit the tavern cellar to update the layout
    RequestThread.postRequest(new GenericRequest("cellar.php"));

    // Refetch the current layout
    layout = TavernRequest.tavernLayout();

    // See if we've already found the goal outside KoLmafia
    faucet = layout.indexOf("3");
    if (goal == TavernManager.FAUCET && faucet != -1) {
      TavernManager.logFaucetSquare(faucet);
      return faucet + 1;
    }

    baron = layout.indexOf("4");
    if (goal == TavernManager.BARON && baron != -1) {
      TavernManager.logBaronSquare(baron);
      return baron + 1;
    }

    mansion = layout.indexOf("6");
    if ((goal == TavernManager.BARON || goal == TavernManager.FIGHT_BARON) && mansion != -1) {
      TavernManager.logMansionSquare(mansion);
      KoLmafia.updateDisplay("You already defeated Baron von Ratsworth.");
      return mansion + 1;
    }

    unexplored = layout.indexOf("0");
    if (goal == TavernManager.EXPLORE && unexplored == -1) {
      TavernManager.logFaucetSquare(faucet);
      TavernManager.logBaronSquare(baron);
      TavernManager.logMansionSquare(mansion);
      KoLmafia.updateDisplay("Entire cellar explored");
      return 0;
    }

    // If the goal has not yet been found, then explore
    KoLAdventure adventure = AdventureDatabase.getAdventure("The Typical Tavern Cellar");

    // Remember if we have already found the faucet
    boolean hadFaucet = faucet != -1;

    // Reset Baron's choice to automatically skip him the first time we find him
    int oldBaronSetting = Preferences.getInteger("choiceAdventure511");
    if (oldBaronSetting != 2) {
      Preferences.setInteger("choiceAdventure511", 2);
    }

    // Set preferences to automatically adjust the faucet
    Preferences.setInteger("choiceAdventure509", 1);
    Preferences.setInteger("choiceAdventure1000", 1);

    while ((goal == TavernManager.FAUCET && faucet == -1
            || goal == TavernManager.BARON && baron == -1
            || goal == TavernManager.FIGHT_BARON && baron == -1
            || goal == TavernManager.EXPLORE && unexplored != -1)
        && KoLmafia.permitsContinue()
        && KoLCharacter.getCurrentHP() > 0
        && KoLCharacter.getAdventuresLeft() > 0) {
      // Tell TavernRequest to visit the next unexplored square
      TavernManager.overrideSquare = TavernManager.nextUnexploredSquare() - 1;
      RequestThread.postRequest(adventure);
      while (FightRequest.inMultiFight && KoLmafia.permitsContinue()) {
        FightRequest.INSTANCE.run();
      }
      TavernManager.overrideSquare = -1;

      // See what we discovered
      layout = TavernRequest.tavernLayout();
      faucet = layout.indexOf("3");
      baron = layout.indexOf("4");
      unexplored = layout.indexOf("0");

      // If we just found the faucet for the first time, visit Bart Ender to claim reward
      if (!hadFaucet && faucet != -1) {
        RequestThread.postRequest(barkeep);
        hadFaucet = true;
      }
    }

    // Restore Baron choice option setting.
    if (oldBaronSetting != 2) {
      Preferences.setInteger("choiceAdventure511", oldBaronSetting);
    }

    if (goal == TavernManager.FAUCET) {
      if (faucet == -1) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Unable to find faucet.");
        return -1;
      }

      // Notify the user that the faucet has been found.
      TavernManager.logFaucetSquare(faucet);
      return faucet + 1;
    }

    if (goal == TavernManager.BARON) {
      if (baron == -1) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Unable to find baron.");
        return -1;
      }

      // Notify the user that the baron has been found.
      TavernManager.logBaronSquare(baron);
      return baron + 1;
    }

    if (goal == TavernManager.FIGHT_BARON) {
      if (baron == -1) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Unable to find baron.");
        return -1;
      }

      // Go fight Baron von Ratsworth
      if (oldBaronSetting != 1) {
        Preferences.setInteger("choiceAdventure511", 1);
      }

      TavernManager.logBaronSquare(baron);
      TavernManager.overrideSquare = baron;
      RequestThread.postRequest(adventure);
      TavernManager.overrideSquare = -1;

      if (oldBaronSetting != 1) {
        Preferences.setInteger("choiceAdventure511", oldBaronSetting);
      }

      layout = TavernRequest.tavernLayout();
      mansion = layout.indexOf("6");

      KoLmafia.updateDisplay("Baron " + (mansion == -1 ? "fought" : "defeated") + ".");
      return baron + 1;
    }

    // Otherwise, we are exploring the rest of the tavern
    if (goal == TavernManager.EXPLORE) {
      if (unexplored != -1) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Unable to finish exploring cellar.");
        return -1;
      }

      // Notify the user that we are done
      TavernManager.logFaucetSquare(faucet);
      TavernManager.logBaronSquare(baron);
      KoLmafia.updateDisplay("Done exploring.");
      return 0;
    }

    // What were we doing?
    return -1;
  }

  private static void logFaucetSquare(final int faucet) {
    if (faucet != -1) {
      int faucetRow = faucet / 5 + 1;
      int faucetColumn = faucet % 5 + 1;

      KoLmafia.updateDisplay("Faucet found in row " + faucetRow + ", column " + faucetColumn);
    }
  }

  private static void logBaronSquare(final int baron) {
    if (baron != -1) {
      int baronRow = baron / 5 + 1;
      int baronColumn = baron % 5 + 1;

      KoLmafia.updateDisplay("Baron found in row " + baronRow + ", column " + baronColumn);
    }
  }

  private static void logMansionSquare(final int mansion) {
    if (mansion != -1) {
      int mansionRow = mansion / 5 + 1;
      int mansionColumn = mansion % 5 + 1;

      KoLmafia.updateDisplay(
          "Baron's empty mansion found in row " + mansionRow + ", column " + mansionColumn);
    }
  }

  private static ArrayList<Integer> getSearchList(final String layout) {
    ArrayList<Integer> searchList = new ArrayList<>();

    for (int i = 0; i < TavernManager.searchOrder.length; ++i) {
      searchList.add(TavernManager.searchOrder[i]);
    }

    for (int i = layout.length() - 1; i >= 0; --i) {
      if (layout.charAt(i) == '0') {
        continue;
      }

      // Remove explored square from searchlist
      int index = searchList.indexOf(i + 1);
      if (index != -1) {
        searchList.remove(index);
      }
    }

    return searchList;
  }

  public static boolean shouldAutoFaucet() {
    if (TavernManager.overrideSquare >= 0) {
      return false;
    }

    // If we know where the faucet is, go there.
    return TavernRequest.tavernLayout().indexOf("3") >= 0;
  }

  public static int recommendSquare() {
    if (TavernManager.overrideSquare >= 0) {
      return TavernManager.overrideSquare + 1;
    }

    if (KoLCharacter.getLevel() < 3) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You need to level up first.");
      return 0;
    }

    // See if we know the faucet square
    String layout = TavernRequest.tavernLayout();
    int faucet = layout.indexOf("3");
    if (faucet >= 0) {
      return faucet + 1;
    }

    // Otherwise, return the next unexplored square
    return TavernManager.nextUnexploredSquare();
  }

  public static int nextUnexploredSquare() {
    // Get the tavern layout
    String layout = TavernRequest.tavernLayout();

    // See if any squares are unexplored
    if (layout.indexOf("0") == -1) {
      return 0;
    }

    // Some squares remain to be visited. Get a list of them in order.
    ArrayList<Integer> searchList = TavernManager.getSearchList(layout);
    if (searchList.size() == 0) {
      // Should never happen
      return 0;
    }

    // Take the first square off of the list
    Integer searchIndex = searchList.remove(0);

    // That's the square we will visit.
    return searchIndex.intValue();
  }

  public static void handleTavernChange(String responseText) {
    if (responseText.contains("have a few drinks on the house")
        || responseText.contains("something that wasn't booze")
        || responseText.contains("a round on the house")) {
      QuestDatabase.setQuestProgress(Quest.RAT, QuestDatabase.FINISHED);
    }
  }
}
