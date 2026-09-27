package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class EdBaseRequest extends PlaceRequest {
  public EdBaseRequest() {
    super("edbase");
  }

  public EdBaseRequest(final String action) {
    super("edbase", action);
  }

  public EdBaseRequest(final String action, final boolean followRedirects) {
    super("edbase", action, followRedirects);
  }

  public static final void parseResponse(final String urlString, final String responseText) {
    String action = GenericRequest.getAction(urlString);

    if (action == null) {
      return;
    }
  }

  private static final Pattern BOOK_PATTERN = Pattern.compile("You may memorize (\\d+) more pages");

  public static final void inspectBook(final String responseText) {
    int edPoints = Preferences.getInteger("edPoints");

    // If we know that we have enough edPoints to get all the
    // skills, don't bother checking.
    if (edPoints >= 20) {
      return;
    }

    // You read from the Book of the Undying.  You may memorize 21 more pages.
    Matcher matcher = EdBaseRequest.BOOK_PATTERN.matcher(responseText);
    if (matcher.find()) {
      // Assume that the displayed value includes one point
      // for your current run + any accumulated points + points gained by levelling
      int levelPoints = Math.min(15, KoLCharacter.getLevel());
      levelPoints = levelPoints - (levelPoints / 3);
      int skillsKnown = 0;
      for (int i = 17000; i <= 17020; i++) {
        if (KoLCharacter.hasSkill(i)) {
          skillsKnown++;
        }
      }
      int newEdPoints = StringUtilities.parseInt(matcher.group(1)) + skillsKnown - levelPoints;
      if (newEdPoints > edPoints) {
        Preferences.setInteger("edPoints", newEdPoints);
      }
    }
  }

  private static final Pattern WISDOM_PATTERN =
      Pattern.compile("Impart Wisdom unto Current Servant.*?(\\d+) remain");

  public static final void inspectServants(final String responseText) {
    int edPoints = Preferences.getInteger("edPoints");

    // If we know that we have enough edPoints to get all the
    // imbumentations, don't bother checking.
    if (edPoints >= 30) {
      return;
    }

    // "Impart Wisdom unto Current Servant (+100xp, 2 remain)"
    Matcher matcher = EdBaseRequest.WISDOM_PATTERN.matcher(responseText);
    if (matcher.find()) {
      int newEdPoints = StringUtilities.parseInt(matcher.group(1)) + 20;
      if (newEdPoints > edPoints) {
        Preferences.setInteger("edPoints", newEdPoints);
      }
    }
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("place.php") || !urlString.contains("edbase")) {
      return false;
    }

    String action = GenericRequest.getAction(urlString);

    // We have nothing special to do for other simple visits.
    if (action == null) {
      return true;
    }

    if (action.equals("edbase_book")) {
      RequestLogger.updateSessionLog("Visiting The Book of the Undying");
      return true;
    }

    if (action.equals("edbase_door")) {
      RequestLogger.updateSessionLog("Visiting The Servants' Quarters");
      return true;
    }

    return false;
  }
}
