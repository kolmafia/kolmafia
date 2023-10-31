package net.sourceforge.kolmafia.request;

import static net.sourceforge.kolmafia.utilities.Statics.DateTimeManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.session.LimitMode;

public class StandardRequest extends GenericRequest {
  // Types: "Items", "Bookshelf Books", "Skills", "Familiars", "Clan Items".

  private static final Map<RestrictedItemType, Set<String>> map = new HashMap<>();

  private static boolean running = false;

  private static boolean initialized = false;

  public static Map<RestrictedItemType, Set<String>> getRestrictionMap() {
    return map;
  }

  public static void reset() {
    StandardRequest.initialized = false;
    StandardRequest.map.clear();
  }

  public static void initialize(final boolean force) {
    // If we are not logged or are under a Limitmode, don't do this.
    if (GenericRequest.passwordHash.equals("") || KoLCharacter.getLimitMode() != LimitMode.NONE) {
      return;
    }

    if (force) {
      StandardRequest.reset();
    }

    if (!StandardRequest.initialized) {
      RequestThread.postRequest(new StandardRequest());
    }
  }

  public static boolean isNotRestricted(final RestrictedItemType type, final String key) {
    if (!KoLCharacter.getRestricted()) {
      return true;
    }
    StandardRequest.initialize(false);
    return !map.getOrDefault(type, Collections.emptySet()).contains(key.toLowerCase());
  }

  public static boolean isAllowed(RestrictedItemType type, final String key) {
    if (KoLCharacter.isTrendy() && !TrendyRequest.isTrendy(type, key)) {
      return false;
    }

    if (KoLCharacter.inQuantum() && type == RestrictedItemType.FAMILIARS) {
      return true;
    }

    if (!KoLCharacter.getRestricted()) {
      return true;
    }

    return StandardRequest.isAllowedInStandard(type, key);
  }

  public static boolean isAllowed(final FamiliarData familiar) {
    return isAllowed(RestrictedItemType.FAMILIARS, familiar.getRace());
  }

  public static boolean isAllowedInStandard(RestrictedItemType type, final String key) {
    if (type == RestrictedItemType.BOOKSHELF_BOOKS) {
      // Work around a KoL bug: most restricted books are
      // listed both under Bookshelf Books and Items, but
      // 3 are listed under only one or the other.
      return StandardRequest.isNotRestricted(RestrictedItemType.BOOKSHELF_BOOKS, key)
          && StandardRequest.isNotRestricted(RestrictedItemType.ITEMS, key);
    }

    return StandardRequest.isNotRestricted(type, key);
  }

  public StandardRequest() {
    super("standard.php");
    // Two years before current year
    int year = DateTimeManager.getRolloverDateTime().getYear();
    this.addFormField("date", (year - 2) + "-01-02");
    // Must use GET
    this.constructURLString(this.getFullURLString(), false);
  }

  @Override
  protected boolean retryOnTimeout() {
    return true;
  }

  @Override
  public void run() {
    if (StandardRequest.running) {
      return;
    }

    StandardRequest.running = true;
    KoLmafia.updateDisplay("Seeing what's still unrestricted today...");
    super.run();
    StandardRequest.running = false;
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
      StandardRequest.initialized = true;
      return;
    }

    StandardRequest.parseResponse(this.getURLString(), this.responseText);
    KoLmafia.updateDisplay("Done checking allowed items.");
  }

  // <b>Bookshelf Books</b><p><span class="i">, </span><span class="i">Gygaxian Libram, </span>
  // <span class="i">Libram of BRICKOs, </span><span class="i">Libram of Candy Heart Summoning,
  // </span>
  // <span class="i">Libram of Divine Favors, </span><span class="i">Libram of Love Songs, </span>
  // <span class="i">McPhee's Grimoire of Hilarious Object Summoning, </span><span class="i">Tome of
  // Clip Art, </span>
  // <span class="i">Tome of Snowcone Summoning, </span>
  // <span class="i">Tome of Sugar Shummoning</span><p>
  // <b>Skills</b>

  private static final Pattern STANDARD_PATTERN = Pattern.compile("<b>(.*?)</b><p>(.*?)<p>");
  private static final Pattern OBJECT_PATTERN =
      Pattern.compile("<span class=\"i\">(.*?)(, )?</span>");

  public static final void parseResponse(final String location, final String responseText) {
    StandardRequest.reset();

    Matcher matcher = StandardRequest.STANDARD_PATTERN.matcher(responseText);
    while (matcher.find()) {
      String type = matcher.group(1);
      RestrictedItemType itemType = RestrictedItemType.fromString(type);
      if (itemType == null) {
        continue;
      }

      Matcher objectMatcher = StandardRequest.OBJECT_PATTERN.matcher(matcher.group(2));
      while (objectMatcher.find()) {
        String object = objectMatcher.group(1).trim().toLowerCase();
        if (object.length() > 0) {
          map.computeIfAbsent(itemType, k -> new HashSet<>()).add(object);
        }
      }
    }

    StandardRequest.initialized = true;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("standard.php")) {
      return false;
    }

    // We don't need to register this in the gCLI or the session log
    return true;
  }
}
