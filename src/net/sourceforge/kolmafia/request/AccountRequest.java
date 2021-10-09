package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.ZodiacType;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.AscensionSnapshot;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.json.JSONException;
import org.json.JSONObject;

public class AccountRequest extends PasswordHashRequest {
  private static final Pattern SELECTED_PATTERN =
      Pattern.compile("selected=\"selected\" value=\"?(\\d+)\"?>");

  public enum Tab {
    ALL(null),
    INTERFACE("interface"),
    INVENTORY("inventory"),
    CHAT("chat"),
    COMBAT("combat"),
    ACCOUNT("account"),
    PROFILE("profile"),
    PRIVACY("privacy"),
    NONE(null);

    private final String name;

    Tab(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  private final Tab tab;

  public AccountRequest() {
    this(Tab.ALL);
  }

  public AccountRequest(final Tab tab) {
    super("account.php");
    this.tab = tab;

    String field = tab.toString();
    if (field != null) {
      this.addFormField("tab", field);
    }
  }

  private static final Pattern TAB_PATTERN = Pattern.compile("tab=([^&]*)");
  private static final Pattern LOADTAB_PATTERN = Pattern.compile("action=loadtab&value=([^&]*)");

  private static Tab getTab(final String urlString) {
    if (urlString.equals("account.php")) {
      return Tab.INTERFACE;
    }

    Matcher m = TAB_PATTERN.matcher(urlString);
    if (!m.find()) {
      m = LOADTAB_PATTERN.matcher(urlString);
      if (!m.find()) {
        return Tab.NONE;
      }
    }

    String tabName = m.group(1);
    for (Tab tab : Tab.values()) {
      if (tabName.equals(tab.toString())) {
        return tab;
      }
    }

    return Tab.NONE;
  }

  @Override
  protected boolean retryOnTimeout() {
    return true;
  }

  @Override
  public void run() {
    if (this.tab == Tab.ALL) {
      RequestThread.postRequest(new AccountRequest(Tab.INTERFACE));
      RequestThread.postRequest(new AccountRequest(Tab.INVENTORY));
      // RequestThread.postRequest( new AccountRequest ( Tab.CHAT ) );
      RequestThread.postRequest(new AccountRequest(Tab.COMBAT));
      RequestThread.postRequest(new AccountRequest(Tab.ACCOUNT));
      // RequestThread.postRequest( new AccountRequest ( Tab.PROFILE ) );
      // RequestThread.postRequest( new AccountRequest ( Tab.PRIVACY ) );
    } else {
      super.run();
    }
  }

  @Override
  public void processResults() {
    AccountRequest.parseAccountData(this.getURLString(), this.responseText);
  }

  public static final void parseAccountData(final String location, final String responseText) {
    if (location.contains("action=")) {
      AccountRequest.parseAction(location, responseText);
      return;
    }

    // Visiting a tab on the Options page
    PasswordHashRequest.updatePasswordHash(responseText);
    AccountRequest.parseOptionTab(location, responseText);
  }

  private static void parseOptionTab(final String location, final String responseText) {
    switch (AccountRequest.getTab(location)) {
      case INTERFACE:
        AccountRequest.parseInterfaceOptions(responseText);
        return;
      case INVENTORY:
        AccountRequest.parseInventoryOptions(responseText);
        return;
      case CHAT:
        AccountRequest.parseChatOptions(responseText);
        return;
      case COMBAT:
        AccountRequest.parseCombatOptions(responseText);
        return;
      case ACCOUNT:
        AccountRequest.parseAccountOptions(responseText);
        return;
      case PROFILE:
        AccountRequest.parseProfileOptions(responseText);
        return;
      case PRIVACY:
        AccountRequest.parsePrivacyOptions(responseText);
        return;
    }
  }

  private static boolean getCheckbox(final String flag, final String responseText) {
    String test = "checked=\"checked\"  name=\"" + flag + "\"";
    return responseText.contains(test);
  }

  private static final String fancyMenuStyle =
      "<input type=\"radio\" value=\"fancy\" checked=\"checked\"  name=\"menu\"/>Icons";
  private static final String compactMenuStyle =
      "<input type=\"radio\" value=\"compact\" checked=\"checked\"  name=\"menu\"/>Drop-Downs";

  private static void parseInterfaceOptions(final String responseText) {
    // Top Menu Style
    GenericRequest.topMenuStyle =
        responseText.contains(fancyMenuStyle)
            ? GenericRequest.MENU_FANCY
            : responseText.contains(compactMenuStyle)
                ? GenericRequest.MENU_COMPACT
                : GenericRequest.MENU_NORMAL;

    boolean checked;
    checked = AccountRequest.getCheckbox("flag_compactchar", responseText);
    CharPaneRequest.compactCharacterPane = checked;
    checked = AccountRequest.getCheckbox("flag_swapfam", responseText);
    CharPaneRequest.familiarBelowEffects = checked;
  }

  private static void parseInventoryOptions(final String responseText) {
    boolean checked;
    checked = AccountRequest.getCheckbox("flag_sellstuffugly", responseText);
    KoLCharacter.setAutosellMode(checked ? "compact" : "detailed");
    checked = AccountRequest.getCheckbox("flag_lazyinventory", responseText);
    KoLCharacter.setLazyInventory(checked);
    checked = AccountRequest.getCheckbox("flag_unfamequip", responseText);
    KoLCharacter.setUnequipFamiliar(checked);
  }

  private static void parseChatOptions(final String responseText) {}

  private static final Pattern AUTOATTACK_PATTERN =
      Pattern.compile("<select name=\"autoattack\">.*?</select>", Pattern.DOTALL);

  private static void parseCombatOptions(final String responseText) {
    // Disable stationary buttons to avoid conflicts when
    // the action bar is enabled.

    boolean checked = AccountRequest.getCheckbox("flag_wowbar", responseText);
    Preferences.setBoolean("serverAddsCustomCombat", checked);

    int autoAttackAction = 0;

    Matcher selectMatcher = AccountRequest.AUTOATTACK_PATTERN.matcher(responseText);
    if (selectMatcher.find()) {
      Matcher optionMatcher = AccountRequest.SELECTED_PATTERN.matcher(selectMatcher.group());
      if (optionMatcher.find()) {
        String autoAttackActionString = optionMatcher.group(1);
        autoAttackAction = Integer.parseInt(autoAttackActionString);
      }
    }

    KoLCharacter.setAutoAttackAction(autoAttackAction);
  }

  private static void parseAccountOptions(final String responseText) {
    // Whether or not a player is currently in Bad Moon or hardcore
    // is also found here through the presence of buttons.

    // <input class=button name="action" type=submit value="Drop Hardcore">
    boolean isHardcore =
        responseText.contains(
            "<input class=button name=\"action\" type=submit value=\"Drop Hardcore\">");
    KoLCharacter.setHardcore(isHardcore);

    // <input class=button name="action" type=submit value="Drop Bad Moon">

    if (responseText.contains(
        "<input class=button name=\"action\" type=submit value=\"Drop Bad Moon\">")) {
      KoLCharacter.setHardcore(true);
      KoLCharacter.setSign("Bad Moon");
    } else if (KoLCharacter.getSignStat() == ZodiacType.BAD_MOON) {
      KoLCharacter.setSign("None");
    }

    // Your skills have been recalled if you have freed the king
    // and don't have a "Recall Skills" button in your account menu

    // <input class=button name="action" type="submit" value="Recall Skills">

    boolean recalled =
        KoLCharacter.kingLiberated()
            && !responseText.contains(
                "<input class=button name=\"action\" type=\"submit\" value=\"Recall Skills\">");
    KoLCharacter.setSkillsRecalled(recalled);
  }

  private static void parseProfileOptions(final String responseText) {}

  private static void parsePrivacyOptions(final String responseText) {}

  private static final Pattern ACTION_PATTERN = Pattern.compile("action=([^&]*)");
  private static final Pattern VALUE_PATTERN = Pattern.compile("value=([^&]*)");

  private static void parseAction(final String location, final String responseText) {
    Matcher actionMatcher = AccountRequest.ACTION_PATTERN.matcher(location);
    if (!actionMatcher.find()) {
      return;
    }

    String action = actionMatcher.group(1);

    if (action.equals("loadtab")) {
      AccountRequest.parseOptionTab(location, responseText);
      return;
    }

    Matcher valueMatcher = AccountRequest.VALUE_PATTERN.matcher(location);
    String valueString = valueMatcher.find() ? valueMatcher.group(1) : "";

    // Interface options

    if (action.equals("menu")) {
      // account.php?pwd&action=menu&value=fancy&ajax=1
      // account.php?pwd&action=menu&value=compact&ajax=1
      // account.php?pwd&action=menu&value=normal&ajax=1
      GenericRequest.topMenuStyle =
          valueString.equals("fancy")
              ? GenericRequest.MENU_FANCY
              : valueString.equals("compact")
                  ? GenericRequest.MENU_COMPACT
                  : GenericRequest.MENU_NORMAL;
      return;
    }

    if (action.equals("flag_compactchar")) {
      boolean checked = valueString.equals("1");
      CharPaneRequest.compactCharacterPane = checked;
      return;
    }

    if (action.equals("flag_swapfam")) {
      boolean checked = valueString.equals("1");
      CharPaneRequest.familiarBelowEffects = checked;
      return;
    }

    // Inventory options

    if (action.equals("flag_sellstuffugly")) {
      boolean checked = valueString.equals("1");
      KoLCharacter.setAutosellMode(checked ? "compact" : "detailed");
      return;
    }

    if (action.equals("flag_lazyinventory")) {
      boolean checked = valueString.equals("1");
      KoLCharacter.setLazyInventory(checked);
      return;
    }

    if (action.equals("flag_unfamequip")) {
      boolean checked = valueString.equals("1");
      KoLCharacter.setUnequipFamiliar(checked);
      return;
    }

    // Combat options

    if (action.equals("flag_wowbar")) {
      boolean checked = valueString.equals("1");
      Preferences.setBoolean("serverAddsCustomCombat", checked);
      return;
    }

    if (action.equals("autoattack")) {
      int value = Integer.parseInt(valueString);
      KoLCharacter.setAutoAttackAction(value);
      return;
    }

    // Account options

    // <form method="post" action="account.php">

    // account.php?actions[]=recallskills&action=Recall+Skills&tab=account&pwd
    // -->
    // You must confirm the confirmation box.
    //
    // account.php?actions[]=recallskills&action=Recall+Skills&recallconfirm=1&tab=account&pwd
    // -->
    // Your ancient memories return in a flood!  You feel more
    // skilled!  You remember some old familiar familiars!

    if (action.equals("Recall+Skills")) {
      if (location.contains("recallconfirm=1")) {
        // Recalling skills
        RequestLogger.updateSessionLog();
        RequestLogger.updateSessionLog("Recalled ancient memories. Yowza!");
        RequestLogger.updateSessionLog();
        KoLCharacter.setSkillsRecalled(true);
      }
      return;
    }

    // Check for failure to drop path before checking to see if a path was dropped
    // For Boris, "You must abandon the Avatar of Boris before forsaking ronin."
    if (responseText.contains("You must abandon")) {
      return;
    }

    // <input type=hidden name="actions[]" value="unronin">
    // <input class=button name="action" type=submit value="Forsake Ronin">
    // <input type="checkbox" class="confirm" name="unroninconfirm" value="1">

    // account.php?actions[]=unronin&action=Forsake+Ronin&unroninconfirm=1&tab=account&pwd

    if (action.equals("Forsake+Ronin")) {
      if (location.contains("unroninconfirm=1")) {
        // Dropping from Softcore to Casual.
        KoLCharacter.setRonin(false);
        CharPaneRequest.setInteraction(true);
        RequestLogger.updateSessionLog();
        RequestLogger.updateSessionLog("Dropped into Casual. Slacker.");
        RequestLogger.updateSessionLog();
      }
      return;
    }

    // <input type=hidden name="actions[]" value="unpath">
    // <input class=button name="action" type="submit" value="Drop Oxygenarian">
    // <input type="checkbox" class="confirm" name="unpathconfirm" value="1">

    // account.php?actions[]=unpath&action=Drop+Teetotaler&unpathconfirm=1&tab=account&pwd
    // account.php?actions[]=unpath&action=Drop+Boozetafarian&unpathconfirm=1&tab=account&pwd
    // account.php?actions[]=unpath&action=Drop+Oxygenarian&unpathconfirm=1&tab=account&pwd
    if (action.equals("Drop+Teetotaler")
        || action.equals("Drop+Boozetafarian")
        || action.equals("Drop+Oxygenarian")) {
      if (location.contains("unpathconfirm=1")) {
        // Dropping consumption restrictions
        KoLCharacter.setConsumptionRestriction(AscensionSnapshot.NOPATH);
        RequestLogger.updateSessionLog();
        RequestLogger.updateSessionLog("Dropped consumption restrictions.");
        RequestLogger.updateSessionLog();
      }
      return;
    }

    // <input type=hidden name="actions[]" value="unhardcore">
    // <input class=button name="action" type=submit value="Drop Hardcore">
    // <input type="checkbox" class="confirm" name="unhardcoreconfirm" value="1">

    // account.php?actions[]=unhardcore&action=Drop+Hardcore&unhardcoreconfirm=1&tab=account&pwd
    if (action.equals("Drop+Hardcore")) {
      if (location.contains("unhardcoreconfirm=1")) {
        // Dropping Hardcore
        boolean inRonin = KoLCharacter.roninLeft() > 0;
        KoLCharacter.setHardcore(false);
        KoLCharacter.setRonin(inRonin);
        CharPaneRequest.setInteraction(!inRonin);
        RequestLogger.updateSessionLog();
        RequestLogger.updateSessionLog("Dropped Hardcore. Wimp.");
        RequestLogger.updateSessionLog();
      }
      return;
    }

    // <input type=hidden name="actions[]" value="unbadmoon">
    // <input class=button name="action" type=submit value="Drop Bad Moon">
    // <input type="checkbox" class="confirm" name="unbadmoonconfirm" value="1">

    // account.php?actions[]=unbadmoon&action=Drop+Bad+Moon&unbadmoonconfirm=1&tab=account&pwd
    if (action.equals("Drop+Bad+Moon")) {
      if (location.contains("unbadmoonconfirm=1")) {
        // Dropping Bad Moon
        KoLCharacter.setSign("None");
        RequestLogger.updateSessionLog();
        RequestLogger.updateSessionLog("Dropped Bad Moon. You fool!");
        RequestLogger.updateSessionLog();
      }
      return;
    }

    // Anything not covered above is dropping a challenge path
    // account.php?actions[]=unpath&action=Drop+Bees+Hate+You&unpathconfirm=1&tab=account&pwd
    // account.php?actions[]=unpath&action=Drop+Way+of+the+Surprising+Fist&unpathconfirm=1&tab=account&pwd
    // account.php?actions[]=unpath&action=Drop+Trendy&unpathconfirm=1&tab=account&pwd
    // account.php?actions[]=unpath&action=Drop+Avatar+of+Boris&unpathconfirm=1&tab=account&pwd
    // account.php?actions[]=unpath&action=Drop+Bugbear+Invasion&unpathconfirm=1&tab=account&pwd
    if (action.startsWith("Drop+")) {
      if (location.contains("unpathconfirm=1")) {
        // Dropping challenge path
        Path oldPath = KoLCharacter.getPath();
        KoLCharacter.setPath(Path.NONE);
        RequestLogger.updateSessionLog();
        RequestLogger.updateSessionLog("Dropped " + oldPath);
        RequestLogger.updateSessionLog();

        // If we were in Beecore, we need to check the Telescope again
        // Ditto for Bugbear Invasion
        if (oldPath == Path.BEES_HATE_YOU || oldPath == Path.BUGBEAR_INVASION) {
          Preferences.setInteger("lastTelescopeReset", -1);
          KoLCharacter.checkTelescope();
        }

        // If we drop Avatar of Boris, do we get here,
        // or are we redirected to the "End of the
        // Boris Road" choice adventure?
      }
      return;
    }

    if (action.equals("whichpenpal")) {
      // There's no way of telling whether this was successful so just update our status and let
      // that resolve it
      ApiRequest.updateStatus();
    }
  }

  public static final void parseStatus(final JSONObject JSON) throws JSONException {
    JSONObject flags = JSON.getJSONObject("flag_config");

    boolean checked;

    // Interface options

    /*
    // This is currently busted
    int topmenu = flags.getInt( "topmenu" );
    GenericRequest.topMenuStyle =
      (topmenu == 2 ) ?
      GenericRequest.MENU_FANCY :
      (topmenu == 1 ) ?
      GenericRequest.MENU_COMPACT :
      GenericRequest.MENU_NORMAL;
    */

    checked = flags.getInt("compactchar") == 1;
    CharPaneRequest.compactCharacterPane = checked;

    checked = flags.getInt("swapfam") == 1;
    CharPaneRequest.familiarBelowEffects = checked;

    checked = flags.getInt("ignorezonewarnings") == 1;
    KoLCharacter.setIgnoreZoneWarnings(checked);

    // Inventory options

    checked = flags.getInt("sellstuffugly") == 1;
    KoLCharacter.setAutosellMode(checked ? "compact" : "detailed");

    checked = flags.getInt("lazyinventory") == 1;
    KoLCharacter.setLazyInventory(checked);

    checked = flags.getInt("unfamequip") == 1;
    KoLCharacter.setUnequipFamiliar(checked);

    // Combat options

    checked = flags.getInt("wowbar") == 1;
    Preferences.setBoolean("serverAddsCustomCombat", checked);

    int autoAttackAction = flags.getInt("autoattack");
    KoLCharacter.setAutoAttackAction(autoAttackAction);

    // Account options

    String sign = JSON.getString("sign");
    KoLCharacter.setSign(sign);

    int pathId = JSON.getInt("path");
    Path path = AscensionPath.idToPath(pathId);
    KoLCharacter.setPath(path);

    boolean hardcore = JSON.getInt("hardcore") == 1 || sign.equals("Bad Moon");
    KoLCharacter.setHardcore(hardcore);

    boolean casual = JSON.getInt("casual") == 1;
    KoLCharacter.setCasual(casual);

    // This isn't safe in Ed after defeating adventurer, but if we're Ed we haven't freed ralph!
    if (path != Path.ACTUALLY_ED_THE_UNDYING) {
      boolean liberated = JSON.getInt("freedralph") == 1;
      KoLCharacter.setKingLiberated(liberated);
    } else {
      KoLCharacter.setKingLiberated(false);
    }

    boolean recalled = JSON.getInt("recalledskills") == 1;
    KoLCharacter.setSkillsRecalled(recalled);

    int eudora = flags.getInt("whichpenpal");
    KoLCharacter.setEudora(eudora);
  }
}
