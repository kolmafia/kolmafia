package net.sourceforge.kolmafia.session;

import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.preferences.Preferences;

public class AutumnatonManager {
  private static final Pattern UPGRADE = Pattern.compile("autumnaton/(?!base)(.*?)\\.png");

  public static void parseChoice(final String responseText) {
    var upgrades =
        UPGRADE
            .matcher(responseText)
            .results()
            .map(m -> m.group(1))
            .sorted()
            .collect(Collectors.joining(","));
    Preferences.setString("autumnatonUpgrades", upgrades);
  }
}
