package net.sourceforge.kolmafia.request;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.RestrictedItemType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

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

  public static boolean isAllowed(final FamiliarData familiar) {
    return isAllowed(RestrictedItemType.FAMILIARS, familiar.getRace());
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

  public static final void parseResponse(final String location, final String responseText) {
    ThriftyRequest.reset();

    var document = Jsoup.parse(responseText);
    for (Element header : document.select("p > b")) {
      String type = header.text().trim();
      RestrictedItemType itemType = RestrictedItemType.fromString(type);
      if (itemType == null) {
        continue;
      }

      Element data = header.parent().nextElementSibling();
      if (data == null || !"p".equals(data.tagName())) {
        continue;
      }

      for (Element objectElement : data.select("span.i")) {
        String object = objectElement.text().replaceFirst(",\\s*$", "").trim().toLowerCase();
        if (!object.isEmpty()) {
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
