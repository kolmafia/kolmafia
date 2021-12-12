package net.sourceforge.kolmafia.persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.request.FightRequest;

/*
 * Instead of packing and unpacking a giant map into user preference files,
 * this is a way of persisting a variable across sessions.
 * Uses the Java Serializable interface.
 */

public class AdventureSpentDatabase implements Serializable {
  private static final long serialVersionUID = -180241952508113933L;
  private static Map<String, Integer> TURNS = new TreeMap<String, Integer>();

  private static int lastTurnUpdated = -1;

  private static boolean noncombatEncountered = false;

  // debugging tool
  public static void showTurns() {
    Set<String> keys = TURNS.keySet();

    for (String key : keys) {
      int turns = TURNS.get(key);
      RequestLogger.printLine(key + ": " + turns);
    }
  }

  public static void resetTurns() {
    resetTurns(true);
  }

  private static void resetTurns(boolean serializeAfterwards) {
    AdventureSpentDatabase.TURNS = new TreeMap<String, Integer>();

    List<KoLAdventure> list = AdventureDatabase.getAsLockableListModel();

    for (KoLAdventure adv : list) {
      AdventureSpentDatabase.TURNS.put(adv.getAdventureName(), 0);
    }

    if (serializeAfterwards) {
      AdventureSpentDatabase.serialize();
    }
  }

  private static boolean checkZones() {
    // See if any zones aren't in the Map.  Add them if so.

    List<KoLAdventure> list = AdventureDatabase.getAsLockableListModel();
    Set<String> keys = TURNS.keySet();

    boolean keyAdded = false;

    for (KoLAdventure adv : list) {
      if (!keys.contains(adv.getAdventureName())) {
        AdventureSpentDatabase.TURNS.put(adv.getAdventureName(), 0);
        keyAdded = true;
      }
    }

    return keyAdded;
  }

  public static void addTurn(KoLAdventure adv) {
    if (FightRequest.edFightInProgress()) {
      return;
    }
    String name = adv.getAdventureName();
    AdventureSpentDatabase.addTurn(name);
  }

  public static void addTurn(final String loc) {
    if (FightRequest.edFightInProgress()) {
      return;
    }
    if (loc == null) {
      return;
    }
    if (!AdventureSpentDatabase.TURNS.containsKey(loc)) {
      // This is a new location
      AdventureSpentDatabase.TURNS.put(loc, 1);
      return;
    }
    int turns = AdventureSpentDatabase.TURNS.get(loc);
    AdventureSpentDatabase.TURNS.put(loc, turns + 1);
  }

  public static void setTurns(final String loc, final int turns) {
    // This function should rarely be needed
    if (loc == null) {
      return;
    }
    if (!AdventureSpentDatabase.TURNS.containsKey(loc)) {
      RequestLogger.printLine(loc + " is not a recognized location.");
      return;
    }
    AdventureSpentDatabase.TURNS.put(loc, turns);
  }

  public static int getTurns(KoLAdventure adv) {
    return AdventureSpentDatabase.getTurns(adv.getAdventureName(), false);
  }

  public static int getTurns(KoLAdventure adv, final boolean suppressPrint) {
    return AdventureSpentDatabase.getTurns(adv.getAdventureName(), suppressPrint);
  }

  public static int getTurns(final String loc, final boolean suppressPrint) {
    if (!AdventureSpentDatabase.TURNS.containsKey(loc)) {
      if (!suppressPrint) {
        RequestLogger.printLine(loc + " is not a recognized location.");
      }
      return -1;
    }
    return AdventureSpentDatabase.TURNS.get(loc);
  }

  public static int getTurns(final String loc) {
    return getTurns(loc, false);
  }

  public static void serialize() {
    File file =
        new File(KoLConstants.DATA_LOCATION, KoLCharacter.baseUserName() + "_" + "turns.ser");

    try {
      FileOutputStream fileOut = new FileOutputStream(file);
      ObjectOutputStream out = new ObjectOutputStream(fileOut);

      out.writeObject(AdventureSpentDatabase.TURNS);
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /*
   * Attempts to load saved adventure spent settings from <username>_turns.ser
   */
  public static void deserialize() {
    File file =
        new File(KoLConstants.DATA_LOCATION, KoLCharacter.baseUserName() + "_" + "turns.ser");

    if (!file.exists()) {
      AdventureSpentDatabase.resetTurns(false);
      return;
    }
    try {
      FileInputStream fileIn = new FileInputStream(file);
      ObjectInputStream in = new ObjectInputStream(fileIn);

      AdventureSpentDatabase.TURNS = (TreeMap<String, Integer>) in.readObject();

      in.close();

      // after successfully loading, check if there were new zones added that aren't yet in the
      // TreeMap.
      AdventureSpentDatabase.checkZones();
    } catch (FileNotFoundException e) {
      AdventureSpentDatabase.resetTurns(false);
      return;
    } catch (ClassNotFoundException e) {
      // Found the file, but the contents did not contain a properly-serialized treemap.
      // Wipe the bogus file.
      file.delete();
      AdventureSpentDatabase.resetTurns();
      return;
    } catch (ClassCastException e) {
      // Old version of the combat queue handling.  Sorry, have to delete your queue.
      file.delete();
      AdventureSpentDatabase.resetTurns();
      return;
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static final int getLastTurnUpdated() {
    return AdventureSpentDatabase.lastTurnUpdated;
  }

  public static final void setLastTurnUpdated(final int turnUpdated) {
    AdventureSpentDatabase.lastTurnUpdated = turnUpdated;
  }

  public static final boolean getNoncombatEncountered() {
    return AdventureSpentDatabase.noncombatEncountered;
  }

  public static final void setNoncombatEncountered(final boolean encountered) {
    if (encountered && FightRequest.edFightInProgress()) {
      return;
    }
    AdventureSpentDatabase.noncombatEncountered = encountered;
  }
}
