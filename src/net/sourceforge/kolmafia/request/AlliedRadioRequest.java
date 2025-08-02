package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

public class AlliedRadioRequest extends GenericRequest {
  public AlliedRadioRequest(final String request) {
    super("choice.php");

    this.addFormField("option", "1");
    this.addFormField("request", request);
  }

  @Override
  protected boolean shouldFollowRedirect() {
    return true;
  }

  @Override
  public void run() {
    if (GenericRequest.abortIfInFightOrChoice()) {
      return;
    }

    boolean handheld;
    // preferentially use the backpack
    if (InventoryManager.equippedOrInInventory(ItemPool.ALLIED_RADIO_BACKPACK)
        && Preferences.getInteger("_alliedRadioDropsUsed") < 3) {
      handheld = false;
      GenericRequest radioReq =
          new GenericRequest(
              "inventory.php?action=requestdrop&pwd=" + GenericRequest.passwordHash, false);
      radioReq.run();
    } else if (InventoryManager.getCount(ItemPool.HANDHELD_ALLIED_RADIO) > 0) {
      handheld = true;
      GenericRequest useRequest = new GenericRequest("inv_use.php");
      useRequest.addFormField("whichitem", String.valueOf(ItemPool.HANDHELD_ALLIED_RADIO));
      useRequest.run();
    } else {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "You do not have a backpack or handheld radio to use.");
      return;
    }

    this.addFormField("whichchoice", handheld ? "1563" : "1561");

    // Redirects to choice.php?forceoption=0
    super.run();
  }

  // Looks like you have enough battery left to make 2 calls today.
  private static final Pattern BATTERY_PATTERN =
      Pattern.compile("Looks like you have enough battery left to make (\\d) calls? today\\.");

  public static void visitChoice(final String responseText) {
    Matcher matcher = AlliedRadioRequest.BATTERY_PATTERN.matcher(responseText);
    if (matcher.find()) {
      int batteryLeft = Integer.parseInt(matcher.group(1));
      Preferences.setInteger("_alliedRadioDropsUsed", 3 - batteryLeft);
    }
  }

  // The radio emits a plaintive honk, followed by a woman's voice saying <b>&quot;1654...
  // S...&quot;</b>
  private static final Pattern NUMBER_LETTER_PATTERN =
      Pattern.compile(
          "voice saying <b>(?:&quot;|\")(\\d+)\\.\\.\\. ([A-Z])\\.\\.\\.(?:&quot;|\")</b>");

  // You try to radio back a thank you, but you’re getting some kind of strange interference.  You
  // can hear chanting and otherworldly howls amongst the static.  &quot;Ssss… click  <thrumming
  // noise> …'h grea'… pop ssss… …<i style='color: #999'>ulH</i>… crackle ssss… 'ur cal'…
  private static final Pattern GREY_TEXT_PATTERN =
      Pattern.compile("<i style='color: #999'>([^<]+)</i>");

  public static void postChoice(final String responseText, final boolean handheld, String req) {
    req = req.toLowerCase();
    if (req.equals("sniper support")) {
      Preferences.setBoolean("noncombatForcerActive", true);
    }

    if (req.equals("materiel intel")) {
      Preferences.setBoolean("_alliedRadioMaterielIntel", true);
    }

    if (req.equals("wildsun boon")) {
      Preferences.setBoolean("_alliedRadioWildsunBoon", true);
    }

    Matcher matcher = AlliedRadioRequest.NUMBER_LETTER_PATTERN.matcher(responseText);
    if (matcher.find()) {
      String number = matcher.group(1);
      String letter = matcher.group(2);
      String message = "Radio number / letter pattern received: " + number + " - " + letter;
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
    }

    matcher = AlliedRadioRequest.GREY_TEXT_PATTERN.matcher(responseText);
    if (matcher.find()) {
      String text = matcher.group(1);
      String message = "Radio grey text received: " + text;
      RequestLogger.printLine(message);
      RequestLogger.updateSessionLog(message);
    }

    if (handheld) {
      ResultProcessor.removeItem(ItemPool.HANDHELD_ALLIED_RADIO);
    } else {
      if (responseText.contains("Please request something else")) {
        return;
      }

      Preferences.increment("_alliedRadioDropsUsed");
    }
  }
}
