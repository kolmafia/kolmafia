package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class HeyDezeRequest extends GenericRequest {
  private final int effectId;
  private final String desc;

  private static final Pattern ID_PATTERN = Pattern.compile("whichbuff=(\\d+)");

  public HeyDezeRequest(final Stat stat) {
    super("heydeze.php");

    this.addFormField("action", "styxbuff");

    switch (stat) {
      case MUSCLE -> {
        // Hella Tough
        this.effectId = 446;
        this.desc = "tougher";
      }
      case MYSTICALITY -> {
        // Hella Smart
        this.effectId = 447;
        this.desc = "smarter";
      }
      case MOXIE -> {
        // Hella Smooth
        this.effectId = 448;
        this.desc = "smoother";
      }
      default -> {
        this.effectId = 0;
        this.desc = "";
        return;
      }
    }

    this.addFormField("whichbuff", String.valueOf(this.effectId));
  }

  @Override
  protected boolean retryOnTimeout() {
    return true;
  }

  @Override
  public void run() {
    if (!KoLCharacter.inBadMoon()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You can't find the Styx Pixie.");
      return;
    }

    if (this.effectId == 0) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Choose a stat to buff.");
      return;
    }

    KoLmafia.updateDisplay("Visiting the Styx Pixie...");
    super.run();
  }

  private static String styxStatString(final String urlString) {

    Matcher matcher = HeyDezeRequest.ID_PATTERN.matcher(urlString);

    if (!matcher.find()) {
      return null;
    }

    return switch (StringUtilities.parseInt(matcher.group(1))) {
      case 446 -> "muscle";
      case 447 -> "mysticality";
      case 448 -> "moxie";
      default -> null;
    };
  }

  @Override
  public void processResults() {
    HeyDezeRequest.parseResponse(this.getURLString(), this.responseText);

    if (this.responseText == null || this.responseText.equals("")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You can't find the Styx Pixie.");
      return;
    }

    // "You already got a buff today"
    if (this.responseText.indexOf("already got a buff today") != -1) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You can only visit the Styx Pixie once a day.");
      return;
    }

    KoLmafia.updateDisplay("You feel " + this.desc + ".");
  }

  // (cost: 1,600 Meat)
  private static final Pattern COST_PATTERN = Pattern.compile("\\(cost: ([\\d,]*) Meat\\)");

  public static void parseResponse(final String urlString, final String responseText) {
    if (urlString.indexOf("place=meansucker") != -1) {
      Matcher m = COST_PATTERN.matcher(responseText);
      if (m.find()) {
        int price = StringUtilities.parseInt(m.group(1));
        Preferences.setInteger("meansuckerPrice", price);
      }
      return;
    }

    if (urlString.indexOf("action=skillGET") != -1) {
      if (responseText.indexOf("You have learned a new skill") != -1) {
        int price = Preferences.getInteger("meansuckerPrice");
        ResultProcessor.processMeat(-price);
        Preferences.setInteger("meansuckerPrice", price * 2);
      }
      return;
    }
  }

  public static String locationName(final String urlString) {
    if (urlString.indexOf("place=styx") != -1) {
      return "The Styx Pixie";
    }
    if (urlString.indexOf("place=heartbreaker") != -1) {
      return "Heartbreaker's Hotel";
    }
    if (urlString.indexOf("place=meansucker") != -1) {
      return "Meansucker's House";
    }
    return null;
  }

  private static String visitLocation(final String urlString) {
    String name = HeyDezeRequest.locationName(urlString);
    if (name != null) {
      return "Visiting " + name + " in Hey Deze";
    }
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("heydeze.php")) {
      return false;
    }

    String action = GenericRequest.getAction(urlString);
    String message = null;

    // We want to log certain simple visits
    if (action == null) {
      message = HeyDezeRequest.visitLocation(urlString);
    }

    // Visit the Meansucker's House
    else if (action.equals("skillGET")) {
      return true;
    }

    // Visit the Styx Pixie
    else if (action.equals("styxbuff")) {
      String stat = HeyDezeRequest.styxStatString(urlString);
      if (stat == null) {
        return false;
      }

      message = "styx " + stat;
      Preferences.setBoolean("styxPixieVisited", true);
    }

    // Take the Hellevator
    else if (action.equals("elevator")) {
      message = "[" + KoLAdventure.getAdventureCount() + "] Heartbreaker's Hotel";
    }

    // Unknown action
    else {
      return false;
    }

    if (message == null) {
      return true;
    }

    RequestLogger.printLine();
    RequestLogger.updateSessionLog();
    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);

    return true;
  }
}
