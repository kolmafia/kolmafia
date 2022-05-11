package net.sourceforge.kolmafia.maximizer;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;

public class LetterBonus {
  private LetterBonus() {}

  static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]");

  static double letterBonus(AdventureResult item) {
    if (item == null || item.getItemId() < 0) return 0;
    return item.getName().length();
  }

  static double letterBonus(AdventureResult item, String letter) {
    if (item == null || item.getItemId() < 0) return 0;

    Pattern letterPattern = Pattern.compile(letter, Pattern.CASE_INSENSITIVE);

    return letterPattern.matcher(item.getName()).results().count();
  }

  static double numberBonus(AdventureResult item) {
    if (item == null || item.getItemId() < 0) return 0;

    return NUMBER_PATTERN.matcher(item.getName()).results().count();
  }
}
