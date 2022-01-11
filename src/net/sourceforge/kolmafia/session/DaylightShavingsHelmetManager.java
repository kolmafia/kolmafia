package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;

public class DaylightShavingsHelmetManager {
  public static final String[] MESSAGES = {
    "Your helmet shoots some lasers at your face.  You smell burning hair.",
    "A pair of scissors emerges from somewhere in your helmet and adjusts your facial hair.",
    "A nozzle emerges from your helmet and sprays a pattern of depilatory foam on your face.",
    "A couple of straight razors snake out of a panel on the side of your helmet and quickly give you a shave.",
    "A clippers-tipped robotic arm emerges from your helmet and gives you a quick face trim.",
  };

  public static final Pattern EFFECT_PATTERN = Pattern.compile("onClick='eff\\(\"(.*?)\"\\);'");

  public static void updatePreference(String responseText) {
    for (String msg : MESSAGES) {
      int position = responseText.indexOf(msg);
      if (position > -1) {
        Matcher matcher = EFFECT_PATTERN.matcher(responseText);
        if (matcher.find(position)) {
          int effect = EffectDatabase.getEffectIdFromDescription(matcher.group(1));
          if (effect >= 2666 && effect <= 2676) {
            Preferences.setInteger("lastBeardBuff", effect);
          }
        }
        return;
      }
    }
  }
}
