package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.preferences.Preferences;

public class DaylightShavingsHelmetManager {
    public static final Pattern[] DAYLIGHT_SHAVINGS_PATTERNS = {
        Pattern.compile("Your helmet shoots some lasers at your face\\. You smell burning hair\\.<center><table><tr><td><img class=hand src=\"\\/images\\/itemimages\\/dsstache\\d+\\\\.gif\" onClick='eff\\(\"[^\"]+\"\\);' width=30 height=30 alt=\"([^\"]+)\""),
        Pattern.compile("A pair of scissors emerges from somewhere in your helmet and adjusts your facial hair\\.<center><table><tr><td><img class=hand src=\"\\/images\\/itemimages\\/dsstache\\d+\\\\.gif\" onClick='eff\\(\"[^\"]+\"\\);' width=30 height=30 alt=\"([^\"]+)\""),
        Pattern.compile("A nozzle emerges from your helmet and sprays a pattern of depilatory foam on your face\\.<center><table><tr><td><img class=hand src=\"\\/images\\/itemimages\\/dsstache\\d+\\\\.gif\" onClick='eff\\(\"[^\"]+\"\\);' width=30 height=30 alt=\"([^\"]+)\""),
        Pattern.compile("A couple of straight razors snake out of a panel on the side of your helmet and quickly give you a shave\\.<center><table><tr><td><img class=hand src=\"\\/images\\/itemimages\\/dsstache\\d+\\\\.gif\" onClick='eff\\(\"[^\"]+\"\\);' width=30 height=30 alt=\"([^\"]+)\""),
        Pattern.compile("A clippers-tipped robotic arm emerges from your helmet and gives you a quick face trim\\.<center><table><tr><td><img class=hand src=\"\\/images\\/itemimages\\/dsstache\\d+\\\\.gif\" onClick='eff\\(\"[^\"]+\"\\);' width=30 height=30 alt=\"([^\"]+)\""),
    }; 
    public static void updatePreference(String responseText) {
        for (Pattern p : DAYLIGHT_SHAVINGS_PATTERNS) {
            Matcher matcher = p.matcher(responseText);
            if (matcher.find()) {
                Preferences.setString("lastBeardBuff", matcher.group(1));
                return;
            }
        }
    }
}
