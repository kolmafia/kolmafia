package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AfterLifeRequest extends GenericRequest {
  private static final Pattern ITEM_PATTERN =
      Pattern.compile(
          "<span onclick='descitem\\(([\\d]+)\\)'>([^<]*)<.*?name=whichitem value=([\\d]+)>",
          Pattern.DOTALL);
  private static final Pattern KARMA_PATTERN =
      Pattern.compile("You gain ([0123456789,]+) Karma", Pattern.DOTALL);

  private AfterLifeRequest() {
    super("afterlife.php");
  }

  @Override
  public void processResults() {
    AfterLifeRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static boolean parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("afterlife.php")) {
      return false;
    }

    // If this is our first visit to the afterlife - we are outside
    // the pearly gates - refresh the charpane
    if (urlString.equals("afterlife.php")) {
      return true;
    }

    // Learn new astral items simply by visiting an astral vendor

    // afterlife.php?place=permery
    // afterlife.php?place=deli
    // afterlife.php?place=armory
    // afterlife.php?place=reincarnate

    Matcher matcher = ITEM_PATTERN.matcher(responseText);
    while (matcher.find()) {
      String descId = matcher.group(1);
      String itemName = matcher.group(2);
      int itemId = StringUtilities.parseInt(matcher.group(3));

      String data = ItemDatabase.getItemDataName(itemId);
      if (data == null || !data.equals(itemName)) {
        ItemDatabase.registerItem(itemId, itemName, descId);
      }
    }

    String action = GenericRequest.getAction(urlString);

    // No need to refresh if simply visiting a vendor
    if (action == null) {
      return false;
    }

    if (action.equals("pearlygates")) {
      int karma = Preferences.getInteger("bankedKarma");
      RequestLogger.updateSessionLog("You have " + karma + " banked Karma.");
      // <td valign=center>You gain 311 Karma</td>
      matcher = KARMA_PATTERN.matcher(responseText);
      while (matcher.find()) {
        int delta = StringUtilities.parseInt(matcher.group(1));
        RequestLogger.updateSessionLog("You gain " + delta + " Karma");
        karma += delta;
      }
      RequestLogger.updateSessionLog("Your new Karma balance is " + karma);
      Preferences.setInteger("bankedKarma", karma);
      return true;
    }

    int delta = 0;
    if (action.equals("scperm")) {
      // afterlife.php?action=scperm&whichskill=6027
      // <td valign=center>You spend 100 Karma</td>
      if (responseText.contains("don't have enough Karma for that")) {
        RequestLogger.updateSessionLog("You don't have enough Karma to perm that skill");
      } else {
        delta = -100;
      }
    } else if (action.equals("hcperm")) {
      // afterlife.php?action=hcperm&whichskill=6027
      // <td valign=center>You spend 200 Karma</td>
      if (responseText.contains("don't have enough Karma for that")) {
        RequestLogger.updateSessionLog("You don't have enough Karma to perm that skill");
      } else {
        delta = -200;
      }
    } else if (action.equals("returnskill")) {
      // afterlife.php?action=returnskill&classid=6&skillid=27&hc=1
      // <td>Skill permanence returned.</td>
      delta = !urlString.contains("hc=1") ? 100 : 200;
    } else if (action.equals("buydeli")) {
      // afterlife.php?action=buydeli&whichitem=5045
      // <td valign=center>You spend 1 Karma</td>
      delta = -1;
    } else if (action.equals("delireturn")) {
      // afterlife.php?action=delireturn&whichitem=5045
      // <td valign=center>You gain 1 Karma</td>
      delta = 1;
    } else if (action.equals("buyarmory")) {
      // afterlife.php?action=buyarmory&whichitem=5041
      // <td valign=center>You spend 10 Karma</td>
      delta = -10;
    } else if (action.equals("armoryreturn")) {
      // afterlife.php?action=armoryreturn&whichitem=5041
      // <td valign=center>You gain 10 Karma</td>
      delta = 10;
    }

    if (delta != 0) {
      Preferences.increment("bankedKarma", delta);
      String message =
          (delta < 0) ? ("You spend " + (-delta) + " Karma") : ("You gain " + delta + " Karma");
      RequestLogger.updateSessionLog(message);
    }

    return true;
  }

  public static final Pattern SKILL_PATTERN = Pattern.compile("whichskill=([^&]*)");
  public static final Pattern CLASSID_PATTERN = Pattern.compile("classid=([^&]*)");
  public static final Pattern SKILLID_PATTERN = Pattern.compile("skillid=([^&]*)");
  public static final Pattern HC_PATTERN = Pattern.compile("hc=([^&]*)");
  public static final Pattern SIGN_PATTERN = Pattern.compile("whichsign=([^&]*)");
  public static final Pattern GENDER_PATTERN = Pattern.compile("gender=([^&]*)");
  public static final Pattern CLASS_PATTERN = Pattern.compile("whichclass=([^&]*)");
  public static final Pattern PATH_PATTERN = Pattern.compile("whichpath=([^&]*)");
  public static final Pattern TYPE_PATTERN = Pattern.compile("asctype=([^&]*)");

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("afterlife.php")) {
      return false;
    }

    String action = GenericRequest.getAction(urlString);

    // Visiting the Permery
    // afterlife.php?place=permery
    // Visiting the Deli
    // afterlife.php?place=deli
    // Visiting the Armory
    // afterlife.php?place=armory
    // Visiting the Bureau of Reincarnation
    // afterlife.php?place=reincarnate

    // No need to refresh if simply visiting a vendor
    if (action == null) {
      return true;
    }

    String message = null;
    int karma = Preferences.getInteger("bankedKarma");

    // Walking through the Pearly Gates
    // afterlife.php?action=pearlygates
    if (action.equals("pearlygates")) {
      message = "Welcome to Valhalla!";
    }

    // Perming a skill
    // afterlife.php?action=scperm&whichskill=6027
    // afterlife.php?action=hcperm&whichskill=6027
    else if (action.equals("scperm") || action.equals("hcperm")) {
      Matcher m = SKILL_PATTERN.matcher(urlString);
      if (!m.find()) {
        return true;
      }

      int skillId = StringUtilities.parseInt(m.group(1));
      boolean hc = action.startsWith("hc");
      String skill = SkillDatabase.getSkillName(skillId);

      String type = hc ? "Hard" : "Soft";
      String cost = hc ? "200" : "100";
      String name = (skill != null) ? skill : ("Skill #" + skillId);
      message =
          type + "core perm " + name + " for " + cost + " Karma (initial balance = " + karma + ")";
    }

    // Returning a skill
    // afterlife.php?action=returnskill&classid=6&skillid=27&hc=1
    else if (action.equals("returnskill")) {
      Matcher m = CLASSID_PATTERN.matcher(urlString);
      if (!m.find()) {
        return true;
      }
      int classId = StringUtilities.parseInt(m.group(1));

      m = SKILLID_PATTERN.matcher(urlString);
      if (!m.find()) {
        return true;
      }
      int skillId = StringUtilities.parseInt(m.group(1));

      m = HC_PATTERN.matcher(urlString);
      if (!m.find()) {
        return true;
      }
      boolean hc = m.group(1).equals("1");

      int id = (classId * 1000) + skillId;
      String skill = SkillDatabase.getSkillName(id);

      String type = hc ? "Hard" : "Soft";
      String cost = hc ? "200" : "100";
      String name = (skill != null) ? skill : ("Skill #" + id);
      message =
          "Return "
              + type
              + "core Skill "
              + name
              + " for "
              + cost
              + " Karma (initial balance = "
              + karma
              + ")";
    }

    // Buying from the Deli
    // afterlife.php?action=buydeli&whichitem=5045
    // Buying an item
    // afterlife.php?action=buyarmory&whichitem=5041
    else if (action.equals("buydeli") || action.equals("buyarmory")) {
      Matcher m = GenericRequest.WHICHITEM_PATTERN.matcher(urlString);
      if (!m.find()) {
        return true;
      }
      int itemId = StringUtilities.parseInt(m.group(1));
      String itemName = ItemDatabase.getItemName(itemId);
      String cost = action.equals("buydeli") ? "1" : "10";
      message = "Buy " + itemName + " for " + cost + " Karma (initial balance = " + karma + ")";
    }

    // Returning an item to the Deli
    // afterlife.php?action=delireturn&whichitem=5045
    // Returning an item
    // afterlife.php?action=armoryreturn&whichitem=5041
    else if (action.equals("delireturn") || action.equals("armoryreturn")) {
      Matcher m = GenericRequest.WHICHITEM_PATTERN.matcher(urlString);
      if (!m.find()) {
        return true;
      }
      int itemId = StringUtilities.parseInt(m.group(1));
      String itemName = ItemDatabase.getItemName(itemId);
      String cost = action.startsWith("deli") ? "1" : "10";
      message = "Return " + itemName + " for " + cost + " Karma (initial balance = " + karma + ")";
    }

    // Ascending
    // afterlife.php?action=ascend&asctype=3&whichclass=4&gender=2&whichpath=4&whichsign=2
    // Confirming Ascension
    // afterlife.php?action=ascend&confirmascend=1&whichsign=2&gender=2&whichclass=4&whichpath=4&asctype=3
    else if (action.equals("ascend")) {
      if (!urlString.contains("confirmascend=1")) {
        return true;
      }

      Matcher m = TYPE_PATTERN.matcher(urlString);
      if (!m.find()) {
        return true;
      }
      int type = StringUtilities.parseInt(m.group(1));

      StringBuilder builder = new StringBuilder();
      builder.append("Ascend as a ");

      switch (type) {
        case 1:
          builder.append("Casual");
          break;
        case 2:
          builder.append("Normal");
          break;
        case 3:
          builder.append("Hardcore");
          break;
        default:
          builder.append("(Type ");
          builder.append(type);
          builder.append(")");
          break;
      }

      m = GENDER_PATTERN.matcher(urlString);
      if (!m.find()) {
        return true;
      }
      int gender = StringUtilities.parseInt(m.group(1));

      builder.append(" ");

      switch (gender) {
        case 1:
          builder.append("Male");
          break;
        case 2:
          builder.append("Female");
          break;
        default:
          builder.append("(Gender ");
          builder.append(gender);
          builder.append(")");
          break;
      }

      m = CLASS_PATTERN.matcher(urlString);
      if (!m.find()) {
        return true;
      }
      int pclass = StringUtilities.parseInt(m.group(1));

      builder.append(" ");

      switch (pclass) {
        case 1:
          builder.append("Seal Clubber");
          break;
        case 2:
          builder.append("Turtle Tamer");
          break;
        case 3:
          builder.append("Pastamancer");
          break;
        case 4:
          builder.append("Sauceror");
          break;
        case 5:
          builder.append("Disco Bandit");
          break;
        case 6:
          builder.append("Accordion Thief");
          break;
        case 11:
          builder.append("Avatar of Boris");
          break;
        case 12:
          builder.append("Zombie Master");
          break;
        case 14:
          builder.append("Avatar of Jarlsberg");
          break;
        case 15:
          builder.append("Avatar of Sneaky Pete");
          break;
        case 17:
          builder.append("Ed the Undying");
          break;
        case 18:
          builder.append("Cow Puncher");
          break;
        case 19:
          builder.append("Beanslinger");
          break;
        case 20:
          builder.append("Snake Oiler");
          break;
        case 23:
          builder.append("Gelatinous Noob");
          break;
        case 24:
          builder.append("Vampyre");
          break;
        case 25:
          builder.append("Plumber");
          break;
        default:
          builder.append("(Class ");
          builder.append(pclass);
          builder.append(")");
          break;
      }

      m = SIGN_PATTERN.matcher(urlString);
      if (!m.find()) {
        return true;
      }

      builder.append(" under the ");

      int sign = StringUtilities.parseInt(m.group(1));
      ZodiacSign zSign = ZodiacSign.find(sign);
      if (zSign != ZodiacSign.NONE) {
        builder.append(zSign);
      } else {
        builder.append("(Sign ");
        builder.append(sign);
        builder.append(")");
      }

      builder.append(" sign");

      m = PATH_PATTERN.matcher(urlString);
      if (!m.find()) {
        return true;
      }

      int pathId = StringUtilities.parseInt(m.group(1));
      Path path = AscensionPath.idToPath(pathId);

      builder.append(" on ");
      builder.append(path.description());
      builder.append(",");

      builder.append(" banking ");
      builder.append(karma);
      builder.append(" Karma.");

      message = builder.toString();
    }

    if (message == null) {
      // Something New!
      return false;
    }

    KoLmafia.updateDisplay("");
    RequestLogger.updateSessionLog("");

    KoLmafia.updateDisplay(message);
    RequestLogger.updateSessionLog(message);

    return true;
  }
}
