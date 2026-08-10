package net.sourceforge.kolmafia.scripts.git;

import java.util.LinkedHashMap;
import java.util.Map;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;

public class GitUrlRemap implements Listener {
  static {
    PreferenceListenerRegistry.registerPreferenceListener("gitUrlRemaps", new GitUrlRemap());
  }

  private static volatile Map<String, String> urlRemaps = Map.of();

  private GitUrlRemap() {}

  public static String remapUrl(String url) {
    if (url == null || url.isBlank()) return url;
    var lowerUrl = url.toLowerCase();
    for (var entry : urlRemaps.entrySet()) {
      var key = entry.getKey();
      var lowerKey = key.toLowerCase();
      if (lowerUrl.startsWith(lowerKey)
          && (lowerUrl.length() == lowerKey.length()
              || lowerUrl.charAt(lowerKey.length()) == '/'
              || lowerUrl.charAt(lowerKey.length()) == '.')) {
        return entry.getValue() + url.substring(key.length());
      }
    }
    return url;
  }

  @Override
  public void update() {
    reload();
  }

  static void reload() {
    var value = Preferences.getString("gitUrlRemaps");
    if (value == null || value.isBlank()) {
      urlRemaps = Map.of();
      return;
    }
    var result = new LinkedHashMap<String, String>();
    for (var pair : value.split(";")) {
      var trimmed = pair.trim();
      if (trimmed.isBlank() || !trimmed.contains("|")) continue;
      var idx = trimmed.indexOf('|');
      result.put(trimmed.substring(0, idx).trim(), trimmed.substring(idx + 1).trim());
    }
    urlRemaps = result;
  }

  static Listener createListener() {
    reload();
    return new GitUrlRemap();
  }
}
