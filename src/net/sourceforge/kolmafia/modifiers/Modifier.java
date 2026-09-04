package net.sourceforge.kolmafia.modifiers;

import java.util.regex.Pattern;

public interface Modifier {
  String getName();

  Pattern[] getDescPatterns();

  Pattern getTagPattern();

  String getTag();

  default boolean isMultiple() {
    return false;
  }

  ModifierValueType getType();

  /** Whether we are confident this modifier maps to a line of RPN internally. */
  default boolean isEnchantment() {
    return false;
  }
}
