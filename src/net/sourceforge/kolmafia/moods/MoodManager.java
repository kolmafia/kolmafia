package net.sourceforge.kolmafia.moods;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class MoodManager {
  private static final AdventureResult[] AUTO_CLEAR = {
    EffectPool.get(EffectPool.BEATEN_UP),
    EffectPool.get(EffectPool.TETANUS),
    EffectPool.get(EffectPool.AMNESIA),
    EffectPool.get(EffectPool.CUNCTATITIS),
    EffectPool.get(EffectPool.HARDLY_POISONED),
    EffectPool.get(EffectPool.MAJORLY_POISONED),
    EffectPool.get(EffectPool.A_LITTLE_BIT_POISONED),
    EffectPool.get(EffectPool.SOMEWHAT_POISONED),
    EffectPool.get(EffectPool.REALLY_QUITE_POISONED),
  };

  public static final AdventureResult TURTLING_ROD = ItemPool.get(ItemPool.TURTLING_ROD, 1);
  public static final AdventureResult EAU_DE_TORTUE = EffectPool.get(EffectPool.EAU_DE_TORTUE);

  private static Mood currentMood = null;
  private static final SortedListModel<Mood> availableMoods = new SortedListModel<>();
  private static final SortedListModel<MoodTrigger> displayList = new SortedListModel<>();

  static boolean isExecuting = false;

  public static File getFile() {
    return new File(KoLConstants.SETTINGS_LOCATION, KoLCharacter.baseUserName() + "_moods.txt");
  }

  public static boolean isExecuting() {
    return MoodManager.isExecuting;
  }

  public static void updateFromPreferences() {
    MoodTrigger.clearKnownSources();
    MoodManager.availableMoods.clear();

    MoodManager.currentMood = null;
    MoodManager.displayList.clear();

    String currentMood = Preferences.getString("currentMood");
    MoodManager.loadSettings();

    MoodManager.setMood(currentMood);
    MoodManager.saveSettings();
  }

  public static LockableListModel<Mood> getAvailableMoods() {
    return MoodManager.availableMoods;
  }

  /**
   * Sets the current mood to be executed to the given mood. Also ensures that all defaults are
   * loaded for the given mood if no data exists.
   */
  public static void setMood(String newMoodName) {
    if (newMoodName == null || newMoodName.trim().equals("")) {
      newMoodName = "default";
    }

    if (newMoodName.equals("clear")
        || newMoodName.equals("autofill")
        || newMoodName.startsWith("exec")
        || newMoodName.startsWith("repeat")) {
      return;
    }

    Preferences.setString("currentMood", newMoodName);

    MoodManager.currentMood = null;
    Mood newMood = new Mood(newMoodName);

    for (Mood mood : MoodManager.availableMoods) {
      if (mood.equals(newMood)) {
        MoodManager.currentMood = mood;

        if (newMoodName.contains(" extends ") || newMoodName.contains(",")) {
          MoodManager.currentMood.setParentNames(newMood.getParentNames());
        }

        break;
      }
    }

    if (MoodManager.currentMood == null) {
      MoodManager.currentMood = newMood;
      MoodManager.availableMoods.remove(MoodManager.currentMood);
      MoodManager.availableMoods.add(MoodManager.currentMood);
    }

    MoodManager.displayList.clear();
    MoodManager.displayList.addAll(MoodManager.currentMood.getTriggers());

    MoodManager.availableMoods.setSelectedItem(MoodManager.currentMood);
  }

  /** Retrieves the model associated with the given mood. */
  public static LockableListModel<MoodTrigger> getTriggers() {
    return MoodManager.displayList;
  }

  public static List<MoodTrigger> getTriggers(String moodName) {
    if (moodName == null || moodName.length() == 0) {
      return Collections.emptyList();
    }

    for (Mood mood : MoodManager.availableMoods) {
      if (mood.getName().equals(moodName)) {
        return mood.getTriggers();
      }
    }

    return Collections.emptyList();
  }

  /** Adds a trigger to the temporary mood settings. */
  public static MoodTrigger addTrigger(final String type, final String name, final String action) {
    MoodTrigger trigger = MoodTrigger.constructNode(type + " " + name + " => " + action);

    if (MoodManager.currentMood.addTrigger(trigger)) {
      MoodManager.displayList.remove(trigger);
      MoodManager.displayList.add(trigger);
    }

    return trigger;
  }

  /** Removes all triggers from the current mood and displayList. */
  public static void removeTriggers(final Collection<MoodTrigger> triggers) {
    Iterator<MoodTrigger> it = triggers.iterator();
    while (it.hasNext()) {
      MoodTrigger trigger = it.next();
      if (MoodManager.currentMood.removeTrigger(trigger)) {
        if (triggers == MoodManager.displayList) {
          it.remove();
        } else {
          MoodManager.displayList.remove(trigger);
        }
      }
    }
  }

  public static void minimalSet() {
    String currentMood = Preferences.getString("currentMood");
    if (currentMood.equals("apathetic")) {
      return;
    }

    // If there's any effects the player currently has and there
    // is a known way to re-acquire it (internally known, anyway),
    // make sure to add those as well.

    AdventureResult[] effects = new AdventureResult[KoLConstants.activeEffects.size()];
    KoLConstants.activeEffects.toArray(effects);

    for (AdventureResult effect : effects) {
      String action = MoodManager.getDefaultAction("lose_effect", effect.getName());
      if (action != null && !action.equals("")) {
        MoodManager.addTrigger("lose_effect", effect.getName(), action);
      }
    }
  }

  /** Fills up the trigger list automatically. */
  private static final String[] hardcoreThiefBuffs =
      new String[] {
        "Fat Leon's Phat Loot Lyric",
        "The Moxious Madrigal",
        "Aloysius' Antiphon of Aptitude",
        "The Sonata of Sneakiness",
        "The Psalm of Pointiness",
        "Ur-Kel's Aria of Annoyance"
      };

  private static final String[] softcoreThiefBuffs =
      new String[] {
        "Fat Leon's Phat Loot Lyric",
        "Aloysius' Antiphon of Aptitude",
        "Ur-Kel's Aria of Annoyance",
        "The Sonata of Sneakiness",
        "Jackasses' Symphony of Destruction",
        "Cletus's Canticle of Celerity"
      };

  private static final String[] rankedBorisSongs =
      new String[] {
        "Song of Fortune",
        "Song of Accompaniment",
        // Can't actually pick the following, since it is in the same
        // skill tree as the preceding Songs
        "Song of Solitude",
        "Song of Cockiness",
      };

  public static void maximalSet() {
    String currentMood = Preferences.getString("currentMood");
    if (currentMood.equals("apathetic")) {
      return;
    }

    UseSkillRequest[] skills = new UseSkillRequest[KoLConstants.availableSkills.size()];
    KoLConstants.availableSkills.toArray(skills);

    ArrayList<String> thiefSkills = new ArrayList<>();
    ArrayList<String> borisSongs = new ArrayList<>();

    for (UseSkillRequest skill : skills) {
      int skillId = skill.getSkillId();

      if (skillId < 1000) {
        continue;
      }

      // Combat rate increasers are not handled by mood
      // autofill, since KoLmafia has a preference for
      // non-combats in the area below.
      // Musk of the Moose, Carlweather's Cantata of Confrontation,
      // Song of Battle

      if (skillId == 1019 || skillId == 6016 || skillId == 11019) {
        continue;
      }

      // Skip skills that aren't mood appropriate because they add effects
      // outside of battle.
      // Canticle of Carboloading, The Ode to Booze,
      // Inigo's Incantation of Inspiration, Song of the Glorious Lunch

      if (skillId == 3024 || skillId == 6014 || skillId == 6028 || skillId == 11023) {
        continue;
      }

      String skillName = skill.getSkillName();

      if (SkillDatabase.isAccordionThiefSong(skillId)) {
        thiefSkills.add(skillName);
        continue;
      }

      if (skillId >= 11000 && skillId < 12000) {
        if (SkillDatabase.isSong(skillId)) {
          borisSongs.add(skillName);
          continue;
        }
      }

      String effectName = UneffectRequest.skillToEffect(skillName);
      int effectId = EffectDatabase.getEffectId(effectName);
      if (EffectDatabase.contains(effectId)) {
        String action = MoodManager.getDefaultAction("lose_effect", effectName);
        MoodManager.addTrigger("lose_effect", effectName, action);
      }
    }

    // If we know Boris Songs, pick one
    if (!borisSongs.isEmpty()) {
      MoodManager.pickSkills(borisSongs, 1, MoodManager.rankedBorisSongs);
    }

    // If we know Accordion Thief Songs, pick some
    if (!thiefSkills.isEmpty()) {
      String[] rankedBuffs =
          KoLCharacter.isHardcore()
              ? MoodManager.hardcoreThiefBuffs
              : MoodManager.softcoreThiefBuffs;
      MoodManager.pickSkills(thiefSkills, UseSkillRequest.songLimit(), rankedBuffs);
    }

    // Now add in all the buffs from the minimal buff set, as those
    // are included here.

    MoodManager.minimalSet();
  }

  private static void pickSkills(
      final List<String> skills, final int limit, final String[] rankedBuffs) {
    if (skills.isEmpty()) {
      return;
    }

    int skillCount = skills.size();

    // If we know fewer skills than our capacity, add them all

    if (skillCount <= limit) {
      String[] skillNames = new String[skillCount];
      skills.toArray(skillNames);

      for (String skillName : skillNames) {
        String effectName = UneffectRequest.skillToEffect(skillName);
        MoodManager.addTrigger("lose_effect", effectName, "cast " + skillName);
      }

      return;
    }

    // Otherwise, pick from the ranked list of "useful" skills

    int foundSkillCount = 0;
    for (int i = 0; i < rankedBuffs.length && foundSkillCount < limit; ++i) {
      if (KoLCharacter.hasSkill(rankedBuffs[i])) {
        String effectName = UneffectRequest.skillToEffect(rankedBuffs[i]);
        MoodManager.addTrigger("lose_effect", effectName, "cast " + rankedBuffs[i]);
        ++foundSkillCount;
      }
    }
  }

  /** Deletes the current mood and sets the current mood to apathetic. */
  public static void deleteCurrentMood() {
    MoodManager.displayList.clear();

    Mood current = MoodManager.currentMood;
    if (current.getName().equals("default")) {
      MoodManager.removeTriggers(current.getTriggers());
      return;
    }

    MoodManager.availableMoods.remove(current);
    MoodManager.setMood("apathetic");
  }

  /** Duplicates the current trigger list into a new list */
  public static void copyTriggers(final String newMoodName) {
    // Copy displayList from current list, then
    // create and switch to new list

    Mood newMood = new Mood(newMoodName);
    newMood.copyFrom(MoodManager.currentMood);

    MoodManager.availableMoods.add(newMood);
    MoodManager.setMood(newMoodName);
  }

  /** Executes all the mood displayList for the current mood. */
  public static void execute() {
    MoodManager.execute(-1);
  }

  public static boolean effectInMood(final AdventureResult effect) {
    return MoodManager.currentMood.isTrigger(effect);
  }

  public static void checkpointedExecute(final int multiplicity) {
    try (Checkpoint checkpoint = new Checkpoint()) {
      MoodManager.execute(0);
    }
  }

  public static void execute(final int multiplicity) {
    if (KoLmafia.refusesContinue()) {
      return;
    }

    if (!MoodManager.willExecute(multiplicity)) {
      return;
    }

    // If in limitmode, eg. Spelunky, do not run moods
    if (KoLCharacter.getLimitMode().limitRecovery()) {
      return;
    }

    MoodManager.isExecuting = true;

    AdventureResult[] effects = new AdventureResult[KoLConstants.activeEffects.size()];
    KoLConstants.activeEffects.toArray(effects);

    // If you have too many accordion thief buffs to execute
    // your displayList, then shrug off your extra buffs, but
    // only if the user allows for this.

    // First we determine which buffs are already affecting the
    // character in question.

    ArrayList<AdventureResult> thiefBuffs = new ArrayList<>();
    for (AdventureResult adventureResult : effects) {
      String skillName = UneffectRequest.effectToSkill(adventureResult.getName());
      if (SkillDatabase.contains(skillName)) {
        int skillId = SkillDatabase.getSkillId(skillName);
        if (SkillDatabase.isAccordionThiefSong(skillId)) {
          thiefBuffs.add(adventureResult);
        }
      }
    }

    // Then, we determine the triggers which are thief skills, and
    // thereby would be cast at this time.

    ArrayList<AdventureResult> thiefKeep = new ArrayList<>();
    ArrayList<AdventureResult> thiefNeed = new ArrayList<>();

    List<MoodTrigger> triggers = MoodManager.currentMood.getTriggers();
    for (MoodTrigger trigger : triggers) {
      if (trigger.isThiefTrigger()) {
        AdventureResult effect = trigger.getEffect();

        if (thiefBuffs.remove(effect)) { // Already have this one
          thiefKeep.add(effect);
        } else { // New or completely expired buff - we may
          // need to shrug a buff to make room for it.
          thiefNeed.add(effect);
        }
      }
    }

    int buffsToRemove =
        thiefNeed.isEmpty()
            ? 0
            : thiefBuffs.size() + thiefKeep.size() + thiefNeed.size() - UseSkillRequest.songLimit();

    for (int i = 0; i < buffsToRemove && i < thiefBuffs.size(); ++i) {
      KoLmafiaCLI.DEFAULT_SHELL.executeLine("uneffect " + thiefBuffs.get(i).getName());
    }

    // Now that everything is prepared, go ahead and execute
    // the displayList which have been set.  First, start out
    // with any skill casting.

    for (MoodTrigger trigger : triggers) {
      if (KoLmafia.refusesContinue()) {
        break;
      }

      if (trigger.isSkill()) {
        trigger.execute(multiplicity);
      }
    }

    for (MoodTrigger trigger : triggers) {
      if (!trigger.isSkill()) {
        trigger.execute(multiplicity);
      }
    }

    MoodManager.isExecuting = false;
  }

  public static boolean willExecute(final int multiplicity) {
    if (!MoodManager.currentMood.isExecutable()) {
      return false;
    }

    boolean willExecute = false;

    List<MoodTrigger> triggers = MoodManager.currentMood.getTriggers();
    for (MoodTrigger trigger : triggers) {
      willExecute |= trigger.shouldExecute(multiplicity);
    }

    return willExecute;
  }

  public static List<AdventureResult> getMissingEffects() {
    List<MoodTrigger> triggers = MoodManager.currentMood.getTriggers();

    if (triggers.isEmpty()) {
      return Collections.emptyList();
    }

    ArrayList<AdventureResult> missing = new ArrayList<>();
    for (MoodTrigger trigger : triggers) {
      if (trigger.getType().equals("lose_effect") && !trigger.matches()) {
        missing.add(trigger.getEffect());
      }
    }

    // Special case: if the character has a turtling rod equipped,
    // assume the Eau de Tortue is a possibility

    if (KoLCharacter.hasEquipped(MoodManager.TURTLING_ROD, Slot.OFFHAND)
        && !KoLConstants.activeEffects.contains(MoodManager.EAU_DE_TORTUE)) {
      missing.add(MoodManager.EAU_DE_TORTUE);
    }

    return missing;
  }

  public static void removeMalignantEffects() {
    for (int i = 0; i < MoodManager.AUTO_CLEAR.length && KoLmafia.permitsContinue(); ++i) {
      AdventureResult effect = MoodManager.AUTO_CLEAR[i];

      // Don't bother removing in G-Lover if no G's, as it has no effect
      if (KoLCharacter.inGLover() && !KoLCharacter.hasGs(effect.getName())) {
        continue;
      }
      if (KoLConstants.activeEffects.contains(effect)) {
        RequestThread.postRequest(new UneffectRequest(effect));
      }
    }
  }

  public static long getMaintenanceCost() {
    List<MoodTrigger> triggers = MoodManager.currentMood.getTriggers();

    if (triggers.isEmpty()) {
      return 0;
    }

    long runningTally = 0;

    // Iterate over the entire list of applicable triggers,
    // locate the ones which involve spellcasting, and add
    // the MP cost for maintenance to the running tally.

    for (MoodTrigger trigger : triggers) {
      if (!trigger.getType().equals("lose_effect") || !trigger.shouldExecute(-1)) {
        continue;
      }

      String action = trigger.getAction();
      if (!action.startsWith("cast") && !action.startsWith("buff")) {
        continue;
      }

      int spaceIndex = action.indexOf(" ");
      if (spaceIndex == -1) {
        continue;
      }

      action = action.substring(spaceIndex + 1);

      int multiplier = 1;

      if (Character.isDigit(action.charAt(0))) {
        spaceIndex = action.indexOf(" ");
        multiplier = StringUtilities.parseInt(action.substring(0, spaceIndex));
        action = action.substring(spaceIndex + 1);
      }

      String skillName = SkillDatabase.getSkillName(action);
      if (skillName != null) {
        runningTally +=
            SkillDatabase.getMPConsumptionById(SkillDatabase.getSkillId(skillName)) * multiplier;
      }
    }

    // Running tally calculated, return the amount of
    // MP required to sustain this mood.

    return runningTally;
  }

  /**
   * Stores the settings maintained in this <code>MoodManager</code> object to disk for later
   * retrieval.
   */
  public static void saveSettings() {
    PrintStream writer = LogStream.openStream(getFile(), true);

    for (Mood mood : MoodManager.availableMoods) {
      writer.println(mood.toSettingString());
    }

    writer.close();
  }

  /**
   * Loads the settings located in the given file into this object. Note that all settings are
   * overridden; if the given file does not exist, the current global settings will also be
   * rewritten into the appropriate file.
   */
  public static void loadSettings() {
    loadSettings(FileUtilities.getReader(getFile()));
  }

  public static void loadSettings(final BufferedReader reader) {
    MoodManager.availableMoods.clear();

    Mood mood = new Mood("apathetic");
    MoodManager.availableMoods.add(mood);

    mood = new Mood("default");
    MoodManager.availableMoods.add(mood);

    if (reader != null) {

      try (reader) {
        String line;

        while ((line = reader.readLine()) != null) {
          line = line.trim();

          if (line.length() == 0) {
            continue;
          }

          if (!line.startsWith("[")) {
            mood.addTrigger(MoodTrigger.constructNode(line));
            continue;
          }

          int closeBracketIndex = line.indexOf("]");

          if (closeBracketIndex == -1) {
            continue;
          }

          String moodName = line.substring(1, closeBracketIndex);
          mood = new Mood(moodName);

          MoodManager.availableMoods.remove(mood);
          MoodManager.availableMoods.add(mood);
        }

        MoodManager.setMood(Preferences.getString("currentMood"));
      } catch (IOException e) {
        // This should not happen.  Therefore, print
        // a stack trace for debug purposes.

        StaticEntity.printStackTrace(e);
      }
    }
  }

  public static String getDefaultAction(final String type, final String name) {
    if (type == null || name == null) {
      return "";
    }

    // We can look at the displayList list to see if it matches
    // your current mood.  That way, the "default action" is
    // considered whatever your current mood says it is.

    String action = "";

    List<MoodTrigger> triggers =
        (MoodManager.currentMood == null)
            ? Collections.emptyList()
            : MoodManager.currentMood.getTriggers();

    for (MoodTrigger trigger : triggers) {
      if (trigger.getType().equals(type) && trigger.getName().equals(name)) {
        action = trigger.getAction();
        break;
      }
    }

    if (type.equals("unconditional")) {
      return action;
    } else if (type.equals("lose_effect")) {
      if (action.equals("")) {
        int effectId = EffectDatabase.getEffectId(name);
        action = EffectDatabase.getDefaultAction(effectId);

        if (action == null) {
          action = MoodTrigger.getKnownSources(name);
        }
      }

      return action;
    } else {
      if (action.equals("")) {
        int effectId = EffectDatabase.getEffectId(name);
        if (UneffectRequest.isRemovable(effectId)) {
          action = "uneffect " + name;
        }
      }

      return action;
    }
  }

  public static boolean currentlyExecutable(final AdventureResult effect, final String action) {
    // It's always OK to boost a stackable effect.
    // Otherwise, it's only OK if it's not active.

    return !MoodManager.unstackableAction(action) || !KoLConstants.activeEffects.contains(effect);
  }

  public static boolean unstackableAction(final String action) {
    return action.contains("absinthe")
        || action.contains("astral mushroom")
        || action.contains("oasis")
        || action.contains("turtle pheromones")
        || action.contains("gong");
  }

  public static boolean canMasterTrivia() {
    if (InventoryManager.canUseMall()) {
      return true;
    }
    return (InventoryManager.getAccessibleCount(ItemPool.WHAT_CARD) > 0
        && InventoryManager.getAccessibleCount(ItemPool.WHEN_CARD) > 0
        && InventoryManager.getAccessibleCount(ItemPool.WHERE_CARD) > 0
        && InventoryManager.getAccessibleCount(ItemPool.WHO_CARD) > 0);
  }
}
