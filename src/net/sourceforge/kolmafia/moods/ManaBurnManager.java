package net.sourceforge.kolmafia.moods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.maximizer.Evaluator;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.session.BreakfastManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ManaBurnManager {
  private ManaBurnManager() {}

  public static final void burnExtraMana(final boolean isManualInvocation) {
    if (KoLmafia.refusesContinue()
        || KoLCharacter.inZombiecore()
        || KoLCharacter.getLimitMode().limitRecovery()) {
      return;
    }

    String nextBurnCast;

    float manaBurnTrigger = Preferences.getFloat("manaBurningTrigger");
    if (!isManualInvocation
        && KoLCharacter.getCurrentMP() < (int) (manaBurnTrigger * KoLCharacter.getMaximumMP())) {
      return;
    }

    boolean was = MoodManager.isExecuting;
    MoodManager.isExecuting = true;

    long currentMP = -1;

    while (currentMP != KoLCharacter.getCurrentMP()
        && (nextBurnCast = ManaBurnManager.getNextBurnCast()) != null) {
      currentMP = KoLCharacter.getCurrentMP();
      KoLmafiaCLI.DEFAULT_SHELL.executeLine(nextBurnCast);
    }

    MoodManager.isExecuting = was;
  }

  public static final void burnMana(long minimum) {
    if (KoLCharacter.inZombiecore()) {
      return;
    }

    String nextBurnCast;

    boolean was = MoodManager.isExecuting;
    MoodManager.isExecuting = true;

    minimum = Math.max(0, minimum);
    long currentMP = -1;

    while (currentMP != KoLCharacter.getCurrentMP()
        && (nextBurnCast = ManaBurnManager.getNextBurnCast(minimum)) != null) {
      currentMP = KoLCharacter.getCurrentMP();
      KoLmafiaCLI.DEFAULT_SHELL.executeLine(nextBurnCast);
    }

    MoodManager.isExecuting = was;
  }

  public static final String getNextBurnCast() {
    // Punt immediately if mana burning is disabled

    float manaBurnPreference = Preferences.getFloat("manaBurningThreshold");
    if (manaBurnPreference < 0.0f) {
      return null;
    }

    float manaRecoverPreference = Preferences.getFloat("mpAutoRecovery");
    int minimum =
        (int)
                (Math.max(manaBurnPreference, manaRecoverPreference)
                    * (float) KoLCharacter.getMaximumMP())
            + 1;
    return ManaBurnManager.getNextBurnCast(minimum);
  }

  private static String getNextBurnCast(final long minimum) {
    // Punt immediately if already burned enough or must recover MP

    long allowedMP = KoLCharacter.getCurrentMP() - minimum;
    if (allowedMP <= 0) {
      return null;
    }

    // Pre-calculate possible breakfast/libram skill

    boolean onlyMood = !Preferences.getBoolean("allowNonMoodBurning");
    String breakfast =
        Preferences.getBoolean("allowSummonBurning")
            ? ManaBurnManager.considerBreakfastSkill(minimum)
            : null;
    int summonThreshold = Preferences.getInteger("manaBurnSummonThreshold");
    int durationLimit = Preferences.getInteger("maxManaBurn") + KoLCharacter.getAdventuresLeft();
    ManaBurn chosen = null;
    ArrayList<ManaBurn> burns = new ArrayList<>();

    // Rather than maintain mood-related buffs only, maintain any
    // active effect that the character can auto-cast. Examine all
    // active effects in order from lowest duration to highest.

    for (int i = 0; i < KoLConstants.activeEffects.size() && KoLmafia.permitsContinue(); ++i) {
      AdventureResult currentEffect = KoLConstants.activeEffects.get(i);
      String effectName = currentEffect.getName();
      String skillName = UneffectRequest.effectToSkill(effectName);

      // Only cast if the player knows the skill

      if (!KoLCharacter.hasSkill(skillName)) {
        continue;
      }

      // Only cast if the MP cost is non-zero, since otherwise you'd
      // be in an infinite loop

      int skillId = SkillDatabase.getSkillId(skillName);
      long mpCost = SkillDatabase.getMPConsumptionById(skillId);

      if (mpCost <= 0) {
        continue;
      }

      // Don't cast if you are restricted by your current class/skills
      if (Evaluator.checkEffectConstraints(currentEffect.getEffectId())) {
        continue;
      }

      // Don't cast if the the skill has not currently castable
      UseSkillRequest skill = UseSkillRequest.getInstance(skillName);
      long maximumCast = skill.getMaximumCast();
      if (maximumCast == 0) {
        continue;
      }

      int priority = Preferences.getInteger("skillBurn" + skillId) + 100;
      // skillBurnXXXX values offset by 100 so that missing prefs read
      // as 100% by default.
      // All skills that were previously hard-coded as unextendable are
      // now indicated by skillBurnXXXX = -100 in defaults.txt, so they
      // can be overridden if desired.

      int currentDuration = currentEffect.getCount();
      int currentLimit = durationLimit * Math.min(100, priority) / 100;

      // If we already have 1000 turns more than the number
      // of turns the player has available, that's enough.
      // Also, if we have more than twice the turns of some
      // more expensive buff, save up for that one instead
      // of extending this one.

      if (currentDuration >= currentLimit) {
        continue;
      }

      // If the player wants to burn mana on summoning
      // skills, only do so if all potential effects have at
      // least 10 turns remaining.

      if (breakfast != null && currentDuration >= summonThreshold) {
        return breakfast;
      }

      // If the player only wants to cast buffs related to
      // their mood, then skip the buff if it's not in the
      // any of the player's moods.

      if (onlyMood && !MoodManager.effectInMood(currentEffect)) {
        continue;
      }

      // If we don't have enough MP for this skill, consider
      // extending some cheaper effect - but only up to twice
      // the turns of this effect, so that a slow but steady
      // MP gain won't be used exclusively on the cheaper effect.

      if (mpCost > allowedMP) {
        durationLimit = Math.max(10, Math.min(currentDuration * 2, durationLimit));
        continue;
      }

      ManaBurn b = new ManaBurn(skillId, skillName, currentDuration, currentLimit);
      if (chosen == null) {
        chosen = b;
      }

      burns.add(b);
      breakfast = null; // we're definitely extending an effect
    }

    if (chosen == null) {
      // No buff found. Return possible breakfast/libram skill
      if (breakfast != null || allowedMP < Preferences.getInteger("lastChanceThreshold")) {
        return breakfast;
      }

      // TODO: find the known but currently unused skill with the
      // highest skillBurnXXXX value (>0), and cast it.

      // Last chance: let the user specify something to do with this
      // MP that we can't find any other use for. Don't allow burn command
      // as there is no burn command that'll work without changing the amount
      // of MP spent
      String cmd = Preferences.getString("lastChanceBurn");
      if (cmd.length() == 0 || cmd.startsWith("burn ")) {
        return null;
      }

      return StringUtilities.globalStringReplace(cmd, "#", String.valueOf(allowedMP));
    }

    // Simulate casting all of the extendable skills in a balanced
    // manner, to determine the final count of the chosen skill -
    // rather than making multiple server requests.
    Iterator<ManaBurn> i = burns.iterator();
    while (i.hasNext()) {
      ManaBurn b = i.next();

      if (!b.isCastable(allowedMP)) {
        i.remove();
        continue;
      }

      allowedMP -= b.simulateCast();
      Collections.sort(burns);
      i = burns.iterator();
    }

    return chosen.toString();
  }

  private static String considerBreakfastSkill(final long minimum) {
    for (int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i) {
      if (!KoLCharacter.hasSkill(UseSkillRequest.BREAKFAST_SKILLS[i])) {
        continue;
      }
      if (UseSkillRequest.BREAKFAST_SKILLS[i].equals("Pastamastery") && !KoLCharacter.canEat()) {
        continue;
      }
      if (UseSkillRequest.BREAKFAST_SKILLS[i].equals("Advanced Cocktailcrafting")
          && !KoLCharacter.canDrink()) {
        continue;
      }

      UseSkillRequest skill = UseSkillRequest.getInstance(UseSkillRequest.BREAKFAST_SKILLS[i]);

      long maximumCast = skill.getMaximumCast();

      if (maximumCast == 0) {
        continue;
      }

      long availableMP = KoLCharacter.getCurrentMP() - minimum;
      long mpPerUse = SkillDatabase.getMPConsumptionById(skill.getSkillId());
      long castCount = Math.min(maximumCast, availableMP / mpPerUse);

      if (castCount > 0) {
        return "cast " + castCount + " " + UseSkillRequest.BREAKFAST_SKILLS[i];
      }
    }

    return ManaBurnManager.considerLibramSummon(minimum);
  }

  private static String considerLibramSummon(final long minimum) {
    long castCount = SkillDatabase.libramSkillCasts(KoLCharacter.getCurrentMP() - minimum);
    if (castCount <= 0) {
      return null;
    }

    List<String> castable = BreakfastManager.getBreakfastLibramSkills();
    int skillCount = castable.size();

    if (skillCount == 0) {
      return null;
    }

    int nextCast = Preferences.getInteger("libramSummons");
    StringBuilder buf = new StringBuilder();
    for (int i = 0; i < skillCount; ++i) {
      long thisCast = (castCount + skillCount - 1 - i) / skillCount;
      if (thisCast <= 0) continue;
      buf.append("cast ");
      buf.append(thisCast);
      buf.append(" ");
      buf.append(castable.get((i + nextCast) % skillCount));
      buf.append(";");
    }

    return buf.toString();
  }
}
