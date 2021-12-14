package net.sourceforge.kolmafia.persistence;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import net.sourceforge.kolmafia.AreaCombatData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.session.CrystalBallManager;
import net.sourceforge.kolmafia.session.EncounterManager;
import net.sourceforge.kolmafia.utilities.RollingLinkedList;

/*
 * Instead of packing and unpacking a giant treemap into user preference files, this is a way of persisting a variable across sessions.
 * Uses the Java Serializable interface.
 */

public class AdventureQueueDatabase implements Serializable {
  private static final long serialVersionUID = -180241952508113931L;

  private static TreeMap<String, RollingLinkedList<String>> COMBAT_QUEUE =
      new TreeMap<String, RollingLinkedList<String>>();
  private static TreeMap<String, RollingLinkedList<String>> NONCOMBAT_QUEUE =
      new TreeMap<String, RollingLinkedList<String>>();

  // debugging tool
  public static void showQueue() {
    Set<String> keys = COMBAT_QUEUE.keySet();

    for (String key : keys) {
      RollingLinkedList<String> zoneQueue = COMBAT_QUEUE.get(key);

      StringBuilder builder = new StringBuilder(key + ": ");

      for (String it : zoneQueue) {
        if (it != null) {
          builder.append(it);
          builder.append(" | ");
        }
      }
      RequestLogger.printLine(builder.toString());
    }

    RequestLogger.printLine();
    RequestLogger.printLine("Noncombats:");

    keys = NONCOMBAT_QUEUE.keySet();

    for (String key : keys) {
      RollingLinkedList<String> zoneQueue = NONCOMBAT_QUEUE.get(key);

      StringBuilder builder = new StringBuilder(key + ": ");

      for (String it : zoneQueue) {
        if (it != null) {
          builder.append(it);
          builder.append(" | ");
        }
      }
      RequestLogger.printLine(builder.toString());
    }
  }

  public static void resetQueue() {
    resetQueue(true);
  }

  private static void resetQueue(boolean serializeAfterwards) {
    AdventureQueueDatabase.COMBAT_QUEUE = new TreeMap<String, RollingLinkedList<String>>();
    AdventureQueueDatabase.NONCOMBAT_QUEUE = new TreeMap<String, RollingLinkedList<String>>();

    List<KoLAdventure> list = AdventureDatabase.getAsLockableListModel();

    for (KoLAdventure adv : list) {
      AdventureQueueDatabase.COMBAT_QUEUE.put(
          adv.getAdventureName(), new RollingLinkedList<String>(5));
      AdventureQueueDatabase.NONCOMBAT_QUEUE.put(
          adv.getAdventureName(), new RollingLinkedList<String>(5));
    }

    if (serializeAfterwards) {
      AdventureQueueDatabase.serialize();
    }
  }

  private static boolean checkZones() {
    // See if any zones aren't in the TreeMap.  Add them if so.

    List<KoLAdventure> list = AdventureDatabase.getAsLockableListModel();
    Set<String> keys = COMBAT_QUEUE.keySet();

    boolean keyAdded = false;

    for (KoLAdventure adv : list) {
      if (!keys.contains(adv.getAdventureName())) {
        AdventureQueueDatabase.COMBAT_QUEUE.put(adv.getAdventureName(), new RollingLinkedList<>(5));
        keyAdded = true;
      }
    }

    keys = NONCOMBAT_QUEUE.keySet();

    for (KoLAdventure adv : list) {
      if (!keys.contains(adv.getAdventureName())) {
        AdventureQueueDatabase.NONCOMBAT_QUEUE.put(
            adv.getAdventureName(), new RollingLinkedList<>(5));
        keyAdded = true;
      }
    }

    return keyAdded;
  }

  public static void enqueue(KoLAdventure adv, String monster) {
    if (adv == null || monster == null) return;
    AdventureQueueDatabase.enqueue(adv.getAdventureName(), monster);
  }

  public static void enqueueNoncombat(KoLAdventure adv, String name) {
    if (adv == null || name == null) return;
    AdventureQueueDatabase.enqueueNoncombat(adv.getAdventureName(), name);
  }

  public static void enqueue(String adventureName, String monster) {
    if (adventureName == null || monster == null) return;

    RollingLinkedList<String> zoneQueue = COMBAT_QUEUE.get(adventureName);

    if (zoneQueue == null) return;

    MonsterData mon = MonsterDatabase.findMonster(monster);

    if (mon == null) {
      // We /should/ have canonicalized the string by now (and matching correctly failed), but just
      // in case see if stripping off "the" helps.
      // Other articles definitely should have been handled by now.
      if (monster.startsWith("the ") || monster.startsWith("The ")) {
        mon = MonsterDatabase.findMonster(monster.substring(4));
      }

      if (mon == null) return;
    }

    zoneQueue.add(mon.getName());
  }

  public static void enqueueNoncombat(String noncombatAdventureName, String name) {
    if (noncombatAdventureName == null) return;

    RollingLinkedList<String> zoneQueue = NONCOMBAT_QUEUE.get(noncombatAdventureName);

    if (zoneQueue == null) return;

    zoneQueue.add(name);
  }

  public static RollingLinkedList<String> getZoneQueue(KoLAdventure adv) {
    return AdventureQueueDatabase.getZoneQueue(adv.getAdventureName());
  }

  public static RollingLinkedList<String> getZoneQueue(String adv) {
    return COMBAT_QUEUE.get(adv);
  }

  public static RollingLinkedList<String> getZoneNoncombatQueue(KoLAdventure adv) {
    return AdventureQueueDatabase.getZoneNoncombatQueue(adv.getAdventureName());
  }

  public static RollingLinkedList<String> getZoneNoncombatQueue(String adv) {
    return NONCOMBAT_QUEUE.get(adv);
  }

  public static void serialize() {
    File file =
        new File(KoLConstants.DATA_LOCATION, KoLCharacter.baseUserName() + "_" + "queue.ser");

    try {
      FileOutputStream fileOut = new FileOutputStream(file);
      ObjectOutputStream out = new ObjectOutputStream(fileOut);

      // make a collection with combat queue first
      List<TreeMap<String, RollingLinkedList<String>>> queues =
          new ArrayList<TreeMap<String, RollingLinkedList<String>>>();
      queues.add(COMBAT_QUEUE);
      queues.add(NONCOMBAT_QUEUE);
      out.writeObject(queues);
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /*
   * Attempts to load saved adventure queue settings from <username>_queue.ser
   */
  @SuppressWarnings("unchecked")
  public static void deserialize() {
    File file =
        new File(KoLConstants.DATA_LOCATION, KoLCharacter.baseUserName() + "_" + "queue.ser");

    if (!file.exists()) {
      AdventureQueueDatabase.resetQueue(false);
      return;
    }
    try {
      FileInputStream fileIn = new FileInputStream(file);
      ObjectInputStream in = new ObjectInputStream(fileIn);

      List<TreeMap<String, RollingLinkedList<String>>> queues =
          (List<TreeMap<String, RollingLinkedList<String>>>) in.readObject();

      // Combat queue is first
      COMBAT_QUEUE = queues.get(0);
      NONCOMBAT_QUEUE = queues.get(1);

      in.close();

      // after successfully loading, check if there were new zones added that aren't yet in the
      // TreeMap.
      AdventureQueueDatabase.checkZones();
    } catch (FileNotFoundException e) {
      AdventureQueueDatabase.resetQueue(false);
      return;
    } catch (ClassNotFoundException e) {
      // Found the file, but the contents did not contain a properly-serialized treemap.
      // Wipe the bogus file.
      file.delete();
      AdventureQueueDatabase.resetQueue();
      return;
    } catch (ClassCastException e) {
      // Old version of the combat queue handling.  Sorry, have to delete your queue.
      file.delete();
      AdventureQueueDatabase.resetQueue();
      return;
    } catch (EOFException e) {
      // Malformed data. Wipe the bogus file.
      file.delete();
      AdventureQueueDatabase.resetQueue();
      return;
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static double applyQueueEffects(
      double numerator, MonsterData monster, AreaCombatData data) {
    String zone = data.getZone();
    RollingLinkedList<String> zoneQueue = getZoneQueue(zone);

    if (EncounterManager.isSaberForceZone(zone)) {
      return EncounterManager.isSaberForceMonster(monster, zone) ? 100.0 : 0.0;
    }

    if (CrystalBallManager.isCrystalBallZone(zone)) {
      return CrystalBallManager.isCrystalBallMonster(monster, zone) ? data.areaCombatPercent() : 0;
    }

    double denominator = data.totalWeighting();

    // without queue effects the result is just numerator/denominator.
    if (zoneQueue == null) {
      return numerator / denominator;
    }

    // rate for monster IN the queue is 1 / (4a - 3b) and rate for monster NOT IN the queue is 4 /
    // (4a - 3b) where
    // a = weight of monsters in the zone
    // b = weight of monsters in the queue

    HashSet<String> zoneSet = new HashSet<String>(zoneQueue); // just care about unique elements

    // Ignore monsters in the queue that aren't actually part of the zone's normal monster list
    // This includes monsters that have special conditions to find and wandering monsters
    // that are not part of the location at all
    // Ignore olfacted or long conned monsters, as these are never rejected
    int queueWeight = 0;
    for (String mon : zoneSet) {
      MonsterData queueMonster = MonsterDatabase.findMonster(mon);
      boolean olfacted =
          queueMonster != null
              && ((Preferences.getString("olfactedMonster").equals(queueMonster.getName())
                      && KoLConstants.activeEffects.contains(FightRequest.ONTHETRAIL))
                  || Preferences.getString("longConMonster").equals(queueMonster.getName()));
      if (queueMonster != null && data.getWeighting(queueMonster) > 0 && !olfacted) {
        queueWeight += data.getWeighting(queueMonster);
      }
    }

    boolean olfacted =
        (Preferences.getString("olfactedMonster").equals(monster.getName())
                && KoLConstants.activeEffects.contains(FightRequest.ONTHETRAIL))
            || Preferences.getString("longConMonster").equals(monster.getName());
    double newNumerator = numerator * (zoneQueue.contains(monster.getName()) && !olfacted ? 1 : 4);
    double newDenominator = (4 * denominator - 3 * queueWeight);

    return newNumerator / newDenominator;
  }
}
