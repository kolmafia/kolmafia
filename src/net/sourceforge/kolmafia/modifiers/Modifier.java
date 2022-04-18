package net.sourceforge.kolmafia.modifiers;

import java.util.regex.Pattern;

public interface Modifier {
  String getName();
  Pattern[] getDescPatterns();
  Pattern getTagPattern();
  String getTag();
}
