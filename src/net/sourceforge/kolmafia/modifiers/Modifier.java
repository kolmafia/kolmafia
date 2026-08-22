package net.sourceforge.kolmafia.modifiers;

import java.util.regex.Pattern;

public interface Modifier {
  String getName();

  Pattern[] getDescPatterns();

  Pattern getTagPattern();

  String getTag();

  ModifierValueType getType();

  /** Whether TCRS re-rolls this modifier as an item enchantment. */
  default boolean isEnchantment() {
    return false;
  }
}
