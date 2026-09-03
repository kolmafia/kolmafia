package net.sourceforge.kolmafia.maximizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.utilities.StringUtilities;

final class MaximizerExpressionParser {
  private static final Pattern TERM_PATTERN =
      Pattern.compile(
          "\\G\\s*(\\+|-|)([\\d.]*)\\s*(\"[^\"]+\"|(?:[^-+,0-9]|(?<! )[-+0-9])+),?\\s*");

  private MaximizerExpressionParser() {}

  static void parse(String expression, MaximizerTermRegistry terms) {
    String normalized = expression.trim().toLowerCase();
    Matcher matcher = TERM_PATTERN.matcher(normalized);
    int position = 0;

    while (position < normalized.length()) {
      if (!matcher.find()) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "Unable to interpret: " + normalized.substring(position));
        return;
      }
      position = matcher.end();
      double weight =
          StringUtilities.parseDouble(
              matcher.end(2) == matcher.start(2)
                  ? matcher.group(1) + "1"
                  : matcher.group(1) + matcher.group(2));
      String keyword = matcher.group(3).trim();
      if (keyword.startsWith("\"") && keyword.endsWith("\"")) {
        keyword = keyword.substring(1, keyword.length() - 1).trim();
      }

      var term =
          new MaximizerTermRegistry.ParsedTerm(keyword, weight, matcher.end(2) != matcher.start(2));
      if (!terms.apply(term)) {
        return;
      }
    }

    terms.finish();
  }
}
