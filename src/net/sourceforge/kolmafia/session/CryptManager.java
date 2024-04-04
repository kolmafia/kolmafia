package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.*;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CryptManager {

  // CYRPT("questL07Cyrptic")	unstarted -> started -> step1 -> finished

  // cyrptAlcoveEvilness	0 -> 50 -> 0
  // cyrptCrannyEvilness	0 -> 50 -> 0
  // cyrptNicheEvilness		0 -> 50 -> 0
  // cyrptNookEvilness		0 -> 50 -> 0
  // cyrptTotalEvilness		0 -> 200 -> 999 -> 0

  public static void acquireEvilometer() {
    QuestDatabase.setQuestProgress(Quest.CYRPT, QuestDatabase.STARTED);
    Preferences.setInteger("cyrptTotalEvilness", 200);
    Preferences.setInteger("cyrptAlcoveEvilness", 50);
    Preferences.setInteger("cyrptCrannyEvilness", 50);
    Preferences.setInteger("cyrptNicheEvilness", 50);
    Preferences.setInteger("cyrptNookEvilness", 50);
  }

  // <center>Total evil: <b>200</b><p>Alcove: <b>50</b><br>Cranny: <b>50</b><br>Niche:
  // <b>50</b><br>Nook: <b>50</b></center>

  // <center>Total evil: <b>999</b><p>Haert: <b>999</b></center>

  private static final Pattern EVILOMETER_PATTERN1 =
      Pattern.compile("<center>Total evil: <b>(\\d+)</b>");

  private static final Pattern EVILOMETER_PATTERN2 =
      Pattern.compile(
          "<p>Alcove: <b>(\\d+)</b><br>Cranny: <b>(\\d+)</b><br>Niche: <b>(\\d+)</b><br>Nook: <b>(\\d+)</b>");

  public static void examineEvilometer(final String responseText) {
    int total = 0;
    int alcove = 0;
    int cranny = 0;
    int niche = 0;
    int nook = 0;

    Matcher matcher1 = EVILOMETER_PATTERN1.matcher(responseText);
    if (matcher1.find()) {
      total = StringUtilities.parseInt(matcher1.group(1));
    }

    Matcher matcher2 = EVILOMETER_PATTERN2.matcher(responseText);
    if (matcher2.find()) {
      alcove = StringUtilities.parseInt(matcher2.group(1));
      cranny = StringUtilities.parseInt(matcher2.group(2));
      niche = StringUtilities.parseInt(matcher2.group(3));
      nook = StringUtilities.parseInt(matcher2.group(4));
    }

    Preferences.setInteger("cyrptTotalEvilness", total);
    Preferences.setInteger("cyrptAlcoveEvilness", alcove);
    Preferences.setInteger("cyrptCrannyEvilness", cranny);
    Preferences.setInteger("cyrptNicheEvilness", niche);
    Preferences.setInteger("cyrptNookEvilness", nook);

    if (responseText.contains("give it a proper burial")) {
      ResultProcessor.removeItem(ItemPool.EVILOMETER);
    }
  }

  public static void visitCrypt(final String responseText) {
    // otherimages/cyrpt/ul.gif
    // otherimages/cyrpt/ul_clear.gif
    int nook =
        responseText.contains("cyrpt/ul.gif") ? Preferences.getInteger("cyrptNookEvilness") : 0;

    // otherimages/cyrpt/ur.gif
    // otherimages/cyrpt/ur_clear.gif
    int niche =
        responseText.contains("cyrpt/ur.gif") ? Preferences.getInteger("cyrptNicheEvilness") : 0;

    // otherimages/cyrpt/ll.gif
    // otherimages/cyrpt/ll_clear.gif
    int cranny =
        responseText.contains("cyrpt/ll.gif") ? Preferences.getInteger("cyrptCrannyEvilness") : 0;

    // otherimages/cyrpt/lr.gif
    // otherimages/cyrpt/lr_clear.gif
    int alcove =
        responseText.contains("cyrpt/lr.gif") ? Preferences.getInteger("cyrptAlcoveEvilness") : 0;

    int total = nook + niche + cranny + alcove;

    // otherimages/cyrpt/thecrypt_heart.gif
    if (responseText.contains("cyrpt/thecrypt_heart.gif")) {
      nook = 0;
      niche = 0;
      cranny = 0;
      alcove = 0;
      total = 999;
    }

    Preferences.setInteger("cyrptAlcoveEvilness", alcove);
    Preferences.setInteger("cyrptCrannyEvilness", cranny);
    Preferences.setInteger("cyrptNicheEvilness", niche);
    Preferences.setInteger("cyrptNookEvilness", nook);
    Preferences.setInteger("cyrptTotalEvilness", total);
  }

  public static void decorateCrypt(final StringBuffer buffer) {
    int evilness = Preferences.getInteger("cyrptTotalEvilness");
    if (evilness == 0 || evilness == 999) {
      return;
    }

    // <A href=place.php?whichplace=plains>Back to the Misspelled Cemetary</a>
    // I expect that will change to "whichplace=cemetery" eventually.
    int table = buffer.indexOf("</tr></table><p><center><A href=place.php");
    if (table == -1) {
      // There is no table of corners and hence no need for the Evilometer
      return;
    }

    int nookEvil = Preferences.getInteger("cyrptNookEvilness");
    int nicheEvil = Preferences.getInteger("cyrptNicheEvilness");
    int crannyEvil = Preferences.getInteger("cyrptCrannyEvilness");
    int alcoveEvil = Preferences.getInteger("cyrptAlcoveEvilness");

    String nookColor = nookEvil > 13 ? "000000" : "FF0000";
    String nookHint = nookEvil > 13 ? "Item Drop" : "<b>BOSS</b>";
    String nicheColor = nicheEvil > 13 ? "000000" : "FF0000";
    String nicheHint = nicheEvil > 13 ? "Sniff Dirty Lihc" : "<b>BOSS</b>";
    String crannyColor = crannyEvil > 13 ? "000000" : "FF0000";
    String crannyHint = crannyEvil > 13 ? "ML & Noncombat" : "<b>BOSS</b>";
    String alcoveColor = alcoveEvil > 13 ? "000000" : "FF0000";
    String alcoveHint = alcoveEvil > 13 ? "Initiative" : "<b>BOSS</b>";

    StringBuilder evilometer = new StringBuilder();

    evilometer.append("<table cellpadding=0 cellspacing=0><tr><td colspan=3>");
    evilometer.append("<img src=\"");
    evilometer.append(KoLmafia.imageServerPath());
    evilometer.append("otherimages/cyrpt/eo_top.gif\">");
    evilometer.append("<tr><td><img src=\"");
    evilometer.append(KoLmafia.imageServerPath());
    evilometer.append("otherimages/cyrpt/eo_left.gif\">");
    evilometer.append("<td width=150><center>");

    if (nookEvil > 0) {
      evilometer.append("<font size=2 color=\"#");
      evilometer.append(nookColor);
      evilometer.append("\"><b>Nook</b> - ");
      evilometer.append(nookEvil);
      evilometer.append("<br><font size=1>");
      evilometer.append(nookHint);
      evilometer.append("<br></font></font>");
    }

    if (nicheEvil > 0) {
      evilometer.append("<font size=2 color=\"#");
      evilometer.append(nicheColor);
      evilometer.append("\"><b>Niche</b> - ");
      evilometer.append(nicheEvil);
      evilometer.append("<br><font size=1>");
      evilometer.append(nicheHint);
      evilometer.append("<br></font></font>");
    }

    if (crannyEvil > 0) {
      evilometer.append("<font size=2 color=\"#");
      evilometer.append(crannyColor);
      evilometer.append("\"><b>Cranny</b> - ");
      evilometer.append(crannyEvil);
      evilometer.append("<br><font size=1>");
      evilometer.append(crannyHint);
      evilometer.append("<br></font></font>");
    }

    if (alcoveEvil > 0) {
      evilometer.append("<font size=2 color=\"#");
      evilometer.append(alcoveColor);
      evilometer.append("\"><b>Alcove</b> - ");
      evilometer.append(alcoveEvil);
      evilometer.append("<br><font size=1>");
      evilometer.append(alcoveHint);
      evilometer.append("<br></font></font>");
    }

    evilometer.append("<td><img src=\"");
    evilometer.append(KoLmafia.imageServerPath());
    evilometer.append("otherimages/cyrpt/eo_right.gif\"><tr><td colspan=3>");
    evilometer.append("<img src=\"");
    evilometer.append(KoLmafia.imageServerPath());
    evilometer.append("otherimages/cyrpt/eo_bottom.gif\"></table>");

    evilometer.insert(0, "</table><td>");
    buffer.insert(table + 5, evilometer.toString());

    String selector = "</map><table";
    int index = buffer.indexOf(selector);
    buffer.insert(index + selector.length(), "><tr><td><table");
  }
}
