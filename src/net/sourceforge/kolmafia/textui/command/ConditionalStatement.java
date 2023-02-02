package net.sourceforge.kolmafia.textui.command;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI.ParameterHandling;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class ConditionalStatement extends AbstractCommand {
  private static final Pattern STATDAY_PATTERN = Pattern.compile("(today|tomorrow) is (.*?) day");

  {
    this.flags = ParameterHandling.FLOW_CONTROL;
  }

  /**
   * Utility method which tests if the given condition is true. Note that this only examines level,
   * health, mana, items, meat and status effects.
   */
  public static final boolean test(final String parameters) {
    if (!KoLmafia.permitsContinue()) {
      return false;
    }
    if (parameters.equals("")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "No condition specified.");
      return false;
    }

    // Allow checking for moon signs for stat days
    // only.  Allow test for today and tomorrow.

    Matcher dayMatcher = ConditionalStatement.STATDAY_PATTERN.matcher(parameters);
    if (dayMatcher.find()) {
      String statDayToday = HolidayDatabase.getMoonEffect().toLowerCase();
      String statDayTest = dayMatcher.group(2).substring(0, 3).toLowerCase();

      return statDayToday.indexOf(statDayTest) != -1
          && statDayToday.indexOf("bonus") != -1
          && statDayToday.indexOf("not " + dayMatcher.group(1)) == -1;
    }

    // Check if the person is looking for whether or
    // not they are a certain class.

    if (parameters.startsWith("class is not ")) {
      String className = parameters.substring(13).trim().toLowerCase();
      String actualClassName = KoLCharacter.getAscensionClassName().toLowerCase();
      return actualClassName.indexOf(className) == -1;
    }

    if (parameters.startsWith("class is ")) {
      String className = parameters.substring(9).trim().toLowerCase();
      String actualClassName = KoLCharacter.getAscensionClassName().toLowerCase();
      return actualClassName.indexOf(className) != -1;
    }

    // Check if the person has a specific skill
    // in their available skills list.

    if (parameters.startsWith("skill list lacks ")) {
      return !KoLCharacter.hasSkill(
          SkillDatabase.getSkillName(parameters.substring(17).trim().toLowerCase()));
    }

    if (parameters.startsWith("skill list contains ")) {
      return KoLCharacter.hasSkill(
          SkillDatabase.getSkillName(parameters.substring(20).trim().toLowerCase()));
    }

    // Generic tests for numerical comparisons
    // involving left and right values.

    String operator =
        parameters.indexOf("==") != -1
            ? "=="
            : parameters.indexOf("!=") != -1
                ? "!="
                : parameters.indexOf(">=") != -1
                    ? ">="
                    : parameters.indexOf("<=") != -1
                        ? "<="
                        : parameters.indexOf("=") != -1
                            ? "=="
                            : parameters.indexOf("<>") != -1
                                ? "!="
                                : parameters.indexOf(">") != -1
                                    ? ">"
                                    : parameters.indexOf("<") != -1 ? "<" : null;

    if (operator == null) {
      KoLmafia.updateDisplay(MafiaState.ERROR, parameters + " contains no comparison operator.");
      return false;
    }

    String[] tokens = parameters.split("[\\!<>=]");

    String left = tokens[0].trim();
    String right = tokens[tokens.length - 1].trim();

    long leftValue;
    long rightValue;

    try {
      leftValue = ConditionalStatement.lvalue(left);
      rightValue = ConditionalStatement.rvalue(left, right);
    } catch (Exception e) {
      // This should not happen.  Therefore, print
      // a stack trace for debug purposes.

      KoLmafia.updateDisplay(MafiaState.ERROR, parameters + " is not a valid construct.");
      return false;
    }

    return operator.equals("==")
        ? leftValue == rightValue
        : operator.equals("!=")
            ? leftValue != rightValue
            : operator.equals(">=")
                ? leftValue >= rightValue
                : operator.equals(">")
                    ? leftValue > rightValue
                    : operator.equals("<=")
                        ? leftValue <= rightValue
                        : operator.equals("<") ? leftValue < rightValue : false;
  }

  static final long lvalue(final String left) {
    if (StringUtilities.isNumeric(left)) {
      return StringUtilities.parseLong(left);
    }

    if (left.equals("level")) {
      return KoLCharacter.getLevel();
    }

    if (left.equals("health")) {
      return KoLCharacter.getCurrentHP();
    }

    if (left.equals("mana")) {
      return KoLCharacter.getCurrentMP();
    }

    if (left.equals("meat")) {
      return KoLCharacter.getAvailableMeat();
    }

    if (left.equals("adventures")) {
      return KoLCharacter.getAdventuresLeft();
    }

    if (left.equals("inebriety") || left.equals("drunkenness") || left.equals("drunkness")) {
      return KoLCharacter.getInebriety();
    }

    if (left.equals("muscle")) {
      return KoLCharacter.getBaseMuscle();
    }

    if (left.equals("mysticality")) {
      return KoLCharacter.getBaseMysticality();
    }

    if (left.equals("moxie")) {
      return KoLCharacter.getBaseMoxie();
    }

    if (left.equals("worthless item")) {
      return HermitRequest.getWorthlessItemCount();
    }

    if (left.equals("stickers")) {
      return Arrays.stream(EquipmentManager.STICKER_SLOTS)
          .mapToObj(EquipmentManager::getEquipment)
          .filter(Predicate.not(EquipmentRequest.UNEQUIP::equals))
          .count();
    }

    AdventureResult item = AbstractCommand.itemParameter(left);
    AdventureResult effect = AbstractCommand.effectParameter(left);

    // If there is no question you're looking for one or
    // the other, then return the appropriate match.

    if (item != null && effect == null) {
      return item.getCount(KoLConstants.inventory);
    }

    if (item == null && effect != null) {
      return effect.getCount(KoLConstants.activeEffects);
    }

    // This breaks away from fuzzy matching so that a
    // substring match is preferred over a fuzzy match.
    // Items first for one reason: Knob Goblin perfume.

    if (item != null && item.getName().toLowerCase().indexOf(left.toLowerCase()) != -1) {
      return item.getCount(KoLConstants.inventory);
    }

    if (effect != null && effect.getName().toLowerCase().indexOf(left.toLowerCase()) != -1) {
      return effect.getCount(KoLConstants.activeEffects);
    }

    // Now, allow fuzzy match results to return a value.
    // Again, following the previous precident, items are
    // preferred over effects.

    if (item != null) {
      return item.getCount(KoLConstants.inventory);
    }

    if (effect != null) {
      return effect.getCount(KoLConstants.activeEffects);
    }

    // No match.  The value is zero by default.

    return 0;
  }

  static final int rvalue(final String left, String right) {
    if (right.endsWith("%")) {
      right = right.substring(0, right.length() - 1);
      int value = StringUtilities.parseInt(right);

      if (left.equals("health")) {
        return (int) ((float) value * (float) KoLCharacter.getMaximumHP() / 100.0f);
      }

      if (left.equals("mana")) {
        return (int) ((float) value * (float) KoLCharacter.getMaximumMP() / 100.0f);
      }

      return value;
    }

    for (int i = 0; i < right.length(); ++i) {
      if (!Character.isDigit(right.charAt(i))) {
        // Items first for one reason: Knob Goblin perfume
        // Determine which item is being matched.

        AdventureResult item = AbstractCommand.itemParameter(right);

        if (item != null) {
          return item.getCount(KoLConstants.inventory);
        }

        AdventureResult effect = AbstractCommand.effectParameter(right);

        if (effect != null) {
          return effect.getCount(KoLConstants.activeEffects);
        }

        // If it is neither an item nor an effect, report
        // the exception.

        if (i == 0 && right.charAt(0) == '-') {
          continue;
        }

        KoLmafia.updateDisplay(
            MafiaState.ERROR, "Invalid operand [" + right + "] on right side of operator");
      }
    }

    // If it gets this far, then it must be numeric,
    // so parse the number and return it.

    return StringUtilities.parseInt(right);
  }
}
