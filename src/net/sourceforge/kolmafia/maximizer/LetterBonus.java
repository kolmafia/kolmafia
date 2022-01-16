package net.sourceforge.kolmafia.maximizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;

public class LetterBonus {
  static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]");

  static double letterBonus(AdventureResult item) {
    return item.getName().length();
  }

  static double letterBonus(AdventureResult item, String letter) {
    Pattern letterPattern = Pattern.compile(letter, Pattern.CASE_INSENSITIVE);

    int letters = 0;
    Matcher matcher = letterPattern.matcher(item.getName());
    while (matcher.find()) {
      letters++;
    }

    return letters;
  }

  static double numberBonus(AdventureResult item) {
    int numbers = 0;
    Matcher matcher = NUMBER_PATTERN.matcher(item.getName());
    while (matcher.find()) {
      numbers++;
    }

    return numbers;
  }
}
