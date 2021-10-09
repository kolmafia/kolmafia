package net.sourceforge.kolmafia.request;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.Limitmode;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class CampAwayRequest extends PlaceRequest {
  private final String action;

  public static final String TENT = "campaway_tentclick";
  public static final String SKY = "campaway_sky";

  public CampAwayRequest() {
    super("campaway");
    this.action = null;
  }

  public CampAwayRequest(final String action) {
    super("campaway", action);
    this.action = action;
  }

  @Override
  public void run() {
    if (TENT.equals(this.action)) {
      // This will remove Curse effects
      // If on the Hidden Apartment Quest, and have a Curse, ask if you are sure you want to lose it
      // ?
      boolean cursed =
          KoLConstants.activeEffects.contains(EffectPool.CURSE1_EFFECT)
              || KoLConstants.activeEffects.contains(EffectPool.CURSE2_EFFECT)
              || KoLConstants.activeEffects.contains(EffectPool.CURSE3_EFFECT);
      if (cursed
          && Preferences.getInteger("hiddenApartmentProgress") < 7
          && !InputFieldUtilities.confirm("Are you sure, that will remove your Cursed effect?")) {
        return;
      }
    }

    super.run();
  }

  @Override
  public void processResults() {
    CampAwayRequest.parseResponse(this.getURLString(), this.responseText);
  }

  private static final Pattern EFFECT_PATTERN =
      Pattern.compile("You acquire an effect: <b>(.*?)</b>");

  public static final void parseResponse(final String urlString, final String responseText) {
    String action = GenericRequest.getAction(urlString);

    // Nothing more to do for a simple visit
    if (action == null) {
      return;
    }

    // There are two divs shown for the tent, each with a link
    //
    // When it is a free rest:
    //
    // place.php?whichplace=campaway&action=campaway_tent
    // place.php?whichplace=campaway&action=campaway_tentclick
    //
    // When it takes a turn:
    //
    // place.php?whichplace=campaway&action=campaway_tentturn
    // place.php?whichplace=campaway&action=campaway_tentclick

    if (action.startsWith("campaway_tent")) {
      Matcher m = EFFECT_PATTERN.matcher(responseText);
      if (m.find()) {
        String effect = m.group(1);
        if (effect.contains("Muscular")) {
          Preferences.setInteger("campAwayDecoration", 1);
        } else if (effect.contains("Mystical")) {
          Preferences.setInteger("campAwayDecoration", 2);
        } else if (effect.contains("Moxious")) {
          Preferences.setInteger("campAwayDecoration", 3);
        }
      } else {
        Preferences.setInteger("campAwayDecoration", 0);
      }
      Preferences.increment("timesRested");
    } else if (action.equals("campaway_sky")) {
      Matcher m = EFFECT_PATTERN.matcher(responseText);
      if (m.find()) {
        String effect = m.group(1);
        String prefix = "Smile of the ";
        int smileIndex = effect.indexOf(prefix);
        if (smileIndex != -1) {
          Preferences.setString(
              "_campAwaySmileBuffSign", effect.substring(smileIndex + prefix.length()));
          Preferences.increment("_campAwaySmileBuffs");
        } else if (effect.contains("Cloud-Talk")) {
          CampAwayRequest.parseCloudTalk(responseText);
          Preferences.increment("_campAwayCloudBuffs");
        }
      } else {
        Preferences.setInteger("_campAwayCloudBuffs", 1);
        Preferences.setInteger("_campAwaySmileBuffs", 3);
      }
    }
  }

  // <div class="msg">...</div>
  private static final Pattern CLOUD_TALK_MESSAGE_PATTERN =
      Pattern.compile("<div class=\"msg\">(.*?)</div>", Pattern.DOTALL);

  // <img
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/otherimages/smoke2/question.png"
  // class='float5'></div>
  private static final Pattern CLOUD_TALK_PATTERN =
      Pattern.compile("<img .*?otherimages/smoke2/([^\"]*)\"", Pattern.DOTALL);

  private static final Map<String, Character> cloudLetters = new HashMap<String, Character>();

  static {
    cloudLetters.put("a.png", 'A');
    cloudLetters.put("b.png", 'B');
    cloudLetters.put("c.png", 'C');
    cloudLetters.put("d.png", 'D');
    cloudLetters.put("e.png", 'E');
    cloudLetters.put("f.png", 'F');
    cloudLetters.put("g.png", 'G');
    cloudLetters.put("h.png", 'H');
    cloudLetters.put("i.png", 'I');
    cloudLetters.put("j.png", 'J');
    cloudLetters.put("k.png", 'K');
    cloudLetters.put("l.png", 'L');
    cloudLetters.put("m.png", 'M');
    cloudLetters.put("n.png", 'N');
    cloudLetters.put("o.png", 'O');
    cloudLetters.put("p.png", 'P');
    cloudLetters.put("q.png", 'Q');
    cloudLetters.put("r.png", 'R');
    cloudLetters.put("s.png", 'S');
    cloudLetters.put("t.png", 'T');
    cloudLetters.put("u.png", 'U');
    cloudLetters.put("v.png", 'V');
    cloudLetters.put("w.png", 'W');
    cloudLetters.put("x.png", 'X');
    cloudLetters.put("y.png", 'Y');
    cloudLetters.put("z.png", 'Z');
    cloudLetters.put("0.png", '0');
    cloudLetters.put("1.png", '1');
    cloudLetters.put("2.png", '2');
    cloudLetters.put("3.png", '3');
    cloudLetters.put("4.png", '4');
    cloudLetters.put("5.png", '5');
    cloudLetters.put("6.png", '6');
    cloudLetters.put("7.png", '7');
    cloudLetters.put("8.png", '8');
    cloudLetters.put("9.png", '9');
    cloudLetters.put("space.png", ' ');
    cloudLetters.put("comma.png", ',');
    cloudLetters.put("period.png", '.');
    cloudLetters.put("colon.png", ':');
    cloudLetters.put("semicolon.png", ';');
    cloudLetters.put("atsign.png", '@');
    cloudLetters.put("asterisk.png", '*');
    cloudLetters.put("hyphen.png", '-');
    cloudLetters.put("equals.png", '=');
    cloudLetters.put("underscore.png", '=');
    cloudLetters.put("slash.png", '/');
    cloudLetters.put("backslash.png", '\\');
    cloudLetters.put("ampersand.png", '&');
    cloudLetters.put("singlequote.png", '\'');
    cloudLetters.put("doublequote.png", '\"');
    cloudLetters.put("dollarsign.png", '$');
    cloudLetters.put("exclamation.png", '!');
    cloudLetters.put("leftparen.png", '(');
    cloudLetters.put("rightparen.png", ')');
    cloudLetters.put("lessthan.png", '<');
    cloudLetters.put("greaterthan.png", '>');
    cloudLetters.put("plussign.png", '+');
    cloudLetters.put("poundsign.png", '#');
    cloudLetters.put("question.png", '?');
  }

  // <small>Smoked by <a href="showplayer.php?who=550986">Croft</a></small>
  private static final Pattern CLOUD_TALKER_PATTERN =
      Pattern.compile("<small>Smoked by .*?who=(\\d+)\">(.*?)</a></small>");

  public static final void parseCloudTalk(final String responseText) {
    Matcher d = CLOUD_TALK_MESSAGE_PATTERN.matcher(responseText);
    if (!d.find()) {
      return;
    }

    StringBuilder buffer = new StringBuilder();
    Matcher m = CLOUD_TALK_PATTERN.matcher(d.group(1));
    while (m.find()) {
      String image = m.group(1);
      Character ch = cloudLetters.get(image);
      if (ch == null) {
        String printit = "Unknown cloud image: " + image;
        RequestLogger.printLine(printit);
        RequestLogger.updateSessionLog(printit);
        ch = 'x';
      }
      buffer.append(ch);
    }

    String message = buffer.toString();
    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);
    Preferences.setString("_cloudTalkMessage", message);

    Matcher w = CLOUD_TALKER_PATTERN.matcher(responseText);
    if (!w.find()) {
      return;
    }

    buffer.setLength(0);
    buffer.append("Smoked by ");
    buffer.append(w.group(2));
    buffer.append(" (");
    buffer.append(w.group(1));
    buffer.append(")");

    message = buffer.toString();
    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);
    Preferences.setString("_cloudTalkSmoker", message);
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("place.php") || !urlString.contains("whichplace=campaway")) {
      return false;
    }

    String action = GenericRequest.getAction(urlString);
    if (action == null) {
      // Nothing to log for simple visits
      return true;
    }

    String message = null;

    if (action.equals("campaway_sky")) {
      message = "Gazing at the Stars";
    } else if (action.startsWith("campaway_tent")) {
      message = "[" + KoLAdventure.getAdventureCount() + "] Rest in your campaway tent";
    }

    if (message == null) {
      // Log URL for anything else
      return false;
    }

    RequestLogger.printLine();
    RequestLogger.printLine(message);

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(message);

    return true;
  }

  public static boolean campAwayTentRestUsable() {
    return Preferences.getBoolean("restUsingCampAwayTent")
        && Preferences.getBoolean("getawayCampsiteUnlocked")
        && StandardRequest.isAllowed("Items", "Distant Woods Getaway Brochure")
        && !Limitmode.limitZone("Woods")
        && !KoLCharacter.inBadMoon();
  }
}
