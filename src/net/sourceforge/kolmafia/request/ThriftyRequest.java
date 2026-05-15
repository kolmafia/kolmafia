package net.sourceforge.kolmafia.request;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.RestrictedItemType;

public class ThriftyRequest extends GenericRequest {
  // Types: "Items", "Skills", "Familiars"

  private static final Map<RestrictedItemType, Set<String>> map = new HashMap<>();

  private static final ThriftyRequest INSTANCE = new ThriftyRequest();
  private static boolean running = false;

  private static boolean initialized = false;

  public static void reset() {
    ThriftyRequest.initialized = false;
    ThriftyRequest.map.clear();
  }

  public static void initialize() {
    if (!ThriftyRequest.initialized) {
      RequestThread.postRequest(ThriftyRequest.INSTANCE);
    }
  }

  public static boolean isAllowed(final RestrictedItemType type, final String key) {
    ThriftyRequest.initialize();
    return map.getOrDefault(type, Collections.emptySet()).contains(key.toLowerCase());
  }

  public ThriftyRequest() {
    super("thrifty.php");
  }

  @Override
  protected boolean retryOnTimeout() {
    return true;
  }

  @Override
  public void run() {
    if (ThriftyRequest.running) {
      return;
    }

    ThriftyRequest.running = true;
    KoLmafia.updateDisplay("Seeing what's still thrifty today...");
    super.run();
    ThriftyRequest.running = false;
  }

  @Override
  protected boolean processOnFailure() {
    return true;
  }

  @Override
  public void processResults() {
    if (this.responseText.equals("")) {
      KoLmafia.updateDisplay("KoL returned a blank page. Giving up.");
      KoLmafia.forceContinue();
      ThriftyRequest.initialized = true;
      return;
    }

    ThriftyRequest.parseResponse(this.getURLString(), this.responseText);
    KoLmafia.updateDisplay("Done checking allowed items.");
  }

  private static final Pattern THRIFTY_PATTERN =
      Pattern.compile("<b>(.*?)</b><p>(.*?)(?:<p|</td>|</table>|</body>|</html>)");
  private static final Pattern OBJECT_PATTERN =
      Pattern.compile("<span class=\"i\">(.*?)(, )?</span>");

  public static final void parseResponse(final String location, final String responseText) {
    ThriftyRequest.reset();

    Matcher matcher = ThriftyRequest.THRIFTY_PATTERN.matcher(responseText);
    while (matcher.find()) {
      String type = matcher.group(1);
      RestrictedItemType itemType = RestrictedItemType.fromString(type);
      if (itemType == null) {
        continue;
      }

      Matcher objectMatcher = ThriftyRequest.OBJECT_PATTERN.matcher(matcher.group(2));
      while (objectMatcher.find()) {
        String object = objectMatcher.group(1).trim().toLowerCase();
        if (object.length() > 0) {
          map.computeIfAbsent(itemType, k -> new HashSet<>()).add(object);
        }
      }
    }

    ThriftyRequest.initialized = true;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("thrifty.php")) {
      return false;
    }

    // We don't need to register this in the gCLI or the session log
    return true;
  }
}
