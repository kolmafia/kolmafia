package net.sourceforge.kolmafia.webui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.Limitmode;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class TopMenuDecorator {
  public static final void decorate(final StringBuffer buffer, final String location) {
    if (KoLCharacter.getLimitmode() == Limitmode.BATMAN) {
      return;
    }

    switch (GenericRequest.topMenuStyle) {
      case GenericRequest.MENU_NORMAL:
        // "normal" (links) style of topmenu.php
        TopMenuDecorator.addScriptMenus(buffer, location);
        break;

      case GenericRequest.MENU_COMPACT:
        // "compact" (dropdowns) style of topmenu.php
        TopMenuDecorator.adjustCompactMenu(buffer);
        TopMenuDecorator.addScriptMenus(buffer, location);
        break;

      case GenericRequest.MENU_FANCY:
        // "fancy" (icons) style of topmenu.php
        TopMenuDecorator.addFancyScriptMenus(buffer, location);
        break;
    }

    // Send any logout link through KoLmafia's logout command so we clean up the GUI
    StringUtilities.singleStringReplace(
        buffer, "logout.php", "/KoLmafia/logout?pwd=" + GenericRequest.passwordHash);
  }

  private static void addFancyScriptMenus(final StringBuffer buffer, final String location) {
    int index = buffer.lastIndexOf("<div id=\"awesome\"");
    if (index == -1) {
      return;
    }

    StringBuilder menuBuffer = new StringBuilder();

    menuBuffer.append(
        "<div style='position: absolute; z-index: 5; top: 40px; right: 0px; border-width: 1px; color:#000000'>");
    menuBuffer.append("<table cellpadding=0 cellspacing=0>");

    // Build Relay Script menu
    TopMenuDecorator.addRelayScriptsMenu(menuBuffer, location);

    // Close the new row
    menuBuffer.append("</table>");
    menuBuffer.append("</div>");

    // Insert menus into topmenu
    buffer.insert(index, menuBuffer.toString());
  }

  private static void addScriptMenus(final StringBuffer buffer, final String location) {
    int index = buffer.lastIndexOf("</tr>");
    if (index == -1) {
      return;
    }

    StringBuilder menuBuffer = new StringBuilder();

    // Build a new element
    // <td valign=center align=center class=tiny><div id='menus' style='margin: 0px; padding: 0px;
    // display: inline'>
    menuBuffer.append("<td valign=center align=center class=tiny>");
    menuBuffer.append("<div id='kolmafia' style='margin: 0px; padding: 0px; display: inline'>");
    menuBuffer.append("<table cellpadding=0 cellspacing=0>");

    // Build Relay Script menu
    TopMenuDecorator.addRelayScriptsMenu(menuBuffer, location);

    // Close the new row
    menuBuffer.append("</table></div></td>");

    // Insert menus into topmenu
    buffer.insert(index, menuBuffer.toString());
  }

  private static void addRelayScriptsMenu(final StringBuilder buffer, final String location) {
    buffer.append("<tr>");

    buffer.append("<td align=left valign=center class=tiny>");
    buffer.append(KoLmafiaCLI.buildRelayScriptMenu());
    buffer.append("</td>");

    buffer.append("<td align=left valign=center>");
    buffer.append("[<a href=\"");
    buffer.append(location);
    buffer.append("\">re</a>]");
    buffer.append("</td>");

    buffer.append("</tr>");
  }

  private static void adjustCompactMenu(final StringBuffer buffer) {
    TopMenuDecorator.mafiatizeFunctionMenu(buffer);
    TopMenuDecorator.mafiatizeGotoMenu(buffer);

    // Kill off the weird focusing problems inherent in the
    // Javascript.

    StringUtilities.globalStringReplace(
        buffer,
        "selectedIndex=0;",
        "selectedIndex=0; if ( parent && parent.mainpane ) parent.mainpane.focus();");
  }

  private static final Pattern FUNCTION_MENU_PATTERN =
      Pattern.compile("(<select name=\"loc\".*?)</select>", Pattern.DOTALL);

  private static void mafiatizeFunctionMenu(final StringBuffer buffer) {
    Matcher menuMatcher = TopMenuDecorator.FUNCTION_MENU_PATTERN.matcher(buffer.toString());
    if (!menuMatcher.find()) {
      return;
    }

    StringBuffer functionMenu = new StringBuffer();
    functionMenu.append(menuMatcher.group());

    StringUtilities.singleStringReplace(
        functionMenu,
        "<option value=\"inventory.php\">Inventory</option>",
        "<option value=\"inventory.php?which=1\">Consumables</option><option value=\"inventory.php?which=2\">Equipment</option><option value=\"inventory.php?which=3\">Misc Items</option><option value=\"sellstuff.php\">Sell Stuff</option>");

    StringUtilities.singleStringReplace(buffer, menuMatcher.group(), functionMenu.toString());
  }

  private static final Pattern GOTO_MENU_PATTERN =
      Pattern.compile("(<select name=location.*?)</select>", Pattern.DOTALL);

  private static void mafiatizeGotoMenu(final StringBuffer buffer) {
    Matcher menuMatcher = TopMenuDecorator.GOTO_MENU_PATTERN.matcher(buffer.toString());
    if (!menuMatcher.find()) {
      return;
    }

    String originalMenu = menuMatcher.group(1);
    StringBuilder gotoMenu = new StringBuilder();
    gotoMenu.append(originalMenu);

    // Add special convenience areas not in normal menu
    for (int i = 0; i < KoLConstants.GOTO_MENU.length; ++i) {
      String tag = KoLConstants.GOTO_MENU[i][0];
      if (originalMenu.contains(tag)) {
        continue;
      }
      String url = KoLConstants.GOTO_MENU[i][1];
      gotoMenu.append("<option value=\"");
      gotoMenu.append(url);
      gotoMenu.append("\">");
      gotoMenu.append(tag);
      gotoMenu.append("</option>");
    }

    String[] bookmarkData = Preferences.getString("browserBookmarks").split("\\|");

    if (bookmarkData.length > 1) {
      gotoMenu.append("<option value=\"nothing\"> </option>");
      gotoMenu.append("<option value=\"nothing\">- Select -</option>");

      for (int i = 0; i < bookmarkData.length; i += 3) {
        gotoMenu.append("<option value=\"");
        gotoMenu.append(bookmarkData[i + 1]);
        gotoMenu.append("\">");
        gotoMenu.append(bookmarkData[i]);
        gotoMenu.append("</option>");
      }
    }

    gotoMenu.append("</select>");

    StringUtilities.singleStringReplace(buffer, menuMatcher.group(), gotoMenu.toString());
  }
}
