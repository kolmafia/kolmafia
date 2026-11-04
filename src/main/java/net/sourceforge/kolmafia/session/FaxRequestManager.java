package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.chat.ChatSender;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase.Monster;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest.Action;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.PauseObject;

/** Asks a faxbot for a monster and collects the fax from the clan VIP lounge fax machine. */
public class FaxRequestManager {
  private static final int DELAY = 200;

  private static String statusMessage;

  static {
    FaxBotDatabase.configure();
  }

  private FaxRequestManager() {}

  public static String getStatusMessage() {
    return FaxRequestManager.statusMessage;
  }

  public static boolean requestFax(final String botName, final Monster monster) {
    return FaxRequestManager.requestFax(botName, monster, true);
  }

  public static boolean requestFax(
      final String botName, final Monster monster, final boolean checkOnline) {
    // Validate ability to receive a fax
    if (!FaxRequestManager.canReceiveFax()) {
      KoLmafia.updateDisplay(FaxRequestManager.statusMessage);
      return false;
    }

    // Make sure FaxBot is online
    if (checkOnline && !FaxRequestManager.isBotOnline(botName)) {
      KoLmafia.updateDisplay(FaxRequestManager.statusMessage);
      return false;
    }

    // Do you already have a photocopied monster?
    if (InventoryManager.hasItem(ItemPool.PHOTOCOPIED_MONSTER)) {
      String current = Preferences.getString("photocopyMonster");
      if (current.equals("")) {
        current = "monster";
      }

      // Yes. Offer a chance to discard it right now
      if (!InputFieldUtilities.confirm(
          "You have a photocopied " + current + " in your inventory. Dump it?")) {
        FaxRequestManager.statusMessage =
            "You need to dispose of your photocopied " + current + " before you can receive a fax.";
        KoLmafia.updateDisplay(FaxRequestManager.statusMessage);
        return false;
      }

      ClanLoungeRequest request =
          new ClanLoungeRequest(Action.FAX_MACHINE, ClanLoungeRequest.SEND_FAX);
      RequestThread.postRequest(request);
    }

    String name = monster.getName();
    String command = monster.getCommand();

    // We can try several times...
    PauseObject pauser = new PauseObject();

    String message = "Asking " + botName + " to send a fax" + " of " + name + ": " + command;

    try {
      ChatManager.setFaxBot(botName);

      while (true) {
        KoLmafia.updateDisplay(message);

        // Clear last message, just in case.
        ChatManager.getLastFaxBotMessage();

        ChatSender.sendMessage(botName, command, false);

        String response = null;
        // Response is sent blue message. Can it fail?

        int limit = Preferences.getInteger("faxbotTimeout");
        int polls = limit * 1000 / DELAY;
        for (int i = 0; i < polls; ++i) {
          response = ChatManager.getLastFaxBotMessage();
          if (response != null) {
            break;
          }
          pauser.pause(DELAY);
        }

        if (response == null) {
          FaxRequestManager.statusMessage =
              "No response from " + botName + " after " + limit + " seconds.";

          // The reply is only seen while chat is open, so the fax may have arrived anyway
          KoLmafia.updateDisplay(FaxRequestManager.statusMessage + " Checking the fax machine.");
          if (FaxRequestManager.receiveRequestedFax(monster)) {
            KoLmafia.enableDisplay();
            Preferences.setString("lastSuccessfulFaxbot", botName);
            return true;
          }

          KoLmafia.updateDisplay(FaxRequestManager.statusMessage);
          return false;
        }

        // FaxBot just delivered a fax to your clan, please try again in 1 minute.
        if (response.contains("just delivered a fax")) {
          FaxRequestManager.statusMessage =
              botName + " recently delivered another fax. Retrying in one minute.";
          KoLmafia.updateDisplay(FaxRequestManager.statusMessage);
          KoLmafia.forceContinue();
          StaticEntity.executeCountdown("Countdown: ", 60);
          continue;
        }

        // parse FaxBot's response
        if (!FaxRequestManager.faxAvailable(botName, response)) {
          KoLmafia.updateDisplay(FaxRequestManager.statusMessage);
          return false;
        }

        // Success! No need to retry
        break;
      }
    } finally {
      ChatManager.setFaxBot(null);
    }

    // The monster is there! retrieve it.
    ClanLoungeRequest request =
        new ClanLoungeRequest(Action.FAX_MACHINE, ClanLoungeRequest.RECEIVE_FAX);
    RequestThread.postRequest(request);
    KoLmafia.enableDisplay();
    // Set the last faxbot the user successfully used
    Preferences.setString("lastSuccessfulFaxbot", botName);
    return true;
  }

  private static boolean receiveRequestedFax(final Monster monster) {
    RequestThread.postRequest(
        new ClanLoungeRequest(Action.FAX_MACHINE, ClanLoungeRequest.RECEIVE_FAX));

    if (!InventoryManager.hasItem(ItemPool.PHOTOCOPIED_MONSTER)) {
      return false;
    }

    MonsterData received = MonsterDatabase.findMonster(Preferences.getString("photocopyMonster"));
    if (received != null && received.getId() == monster.getMonster().getId()) {
      return true;
    }

    // Put it back, since a photocopy in inventory blocks the next request
    RequestThread.postRequest(
        new ClanLoungeRequest(Action.FAX_MACHINE, ClanLoungeRequest.SEND_FAX));
    if (InventoryManager.hasItem(ItemPool.PHOTOCOPIED_MONSTER)) {
      FaxRequestManager.statusMessage = "Could not put the photocopy back in the fax machine.";
    }
    return false;
  }

  private static boolean canReceiveFax() {
    // Are you allowed to use chat?
    if (!ChatManager.chatLiterate()) {
      FaxRequestManager.statusMessage = "You are not allowed to use chat.";
      return false;
    }

    // Do you have a VIP key?
    if (!InventoryManager.hasItem(ClanLoungeRequest.VIP_KEY)) {
      FaxRequestManager.statusMessage = "You don't have a VIP key.";
      return false;
    }

    // Are you Trendy?
    if (KoLCharacter.isTrendy() || KoLCharacter.getRestricted()) {
      FaxRequestManager.statusMessage = "Fax machines are out of style.";
      return false;
    }

    // Are you an Avatar of Boris?
    if (KoLCharacter.inAxecore()) {
      FaxRequestManager.statusMessage = "Boris sneered at technology.";
      return false;
    }

    // Are you an Avatar of Jarlsberg?
    if (KoLCharacter.isJarlsberg()) {
      FaxRequestManager.statusMessage = "Jarlsberg was more into magic than technology.";
      return false;
    }

    // Are you an Avatar of Sneaky Pete?
    if (KoLCharacter.isSneakyPete()) {
      FaxRequestManager.statusMessage =
          "Have you ever seen a cool person use a fax machine? I didn't think so.";
      return false;
    }

    // Try to visit the fax machine
    ClanLoungeRequest request = new ClanLoungeRequest(Action.FAX_MACHINE);
    RequestThread.postRequest(request);

    // Are you in a clan?
    String redirect = request.redirectLocation;
    if (redirect != null && redirect.equals("clan_signup.php")) {
      FaxRequestManager.statusMessage = "You are not in a clan.";
      return false;
    }

    // Does your clan have a fax machine?
    if (!request.responseText.contains("You approach the fax machine")) {
      FaxRequestManager.statusMessage = "Your clan does not have a fax machine.";
      return false;
    }
    return true;
  }

  private static boolean faxAvailable(final String botName, final String response) {
    // FaxBot has copied a Rockfish into your clan's Fax Machine.
    // Your monster has been delivered to your clan Fax Machine. Thank you for using FaustBot.
    // Your fax is ready.
    if (response.contains("into your clan's Fax Machine")
        || response.contains("delivered to your clan Fax Machine")
        || response.contains("Your fax is ready")) {
      return true;
    }

    // Sorry, it appears you requested an invalid monster.
    // I couldn't find that monster. Try sending "list" for a list of monster names.
    // I do not recognize that monster
    if (response.contains("I do not understand your request")
        || response.contains("you requested an invalid monster")
        || response.contains("I couldn't find that monster")
        || response.contains("I do not recognize that monster")) {
      FaxRequestManager.statusMessage = "Configuration error: unknown command sent to " + botName;
      return false;
    }

    // I am unable to whitelist to clan 'xxx', please verify faustbot (#2504770) is whitelisted.
    // Thank you.
    // I couldn't get into your clan
    // I am not whitelisted
    if (response.contains("could not whitelist")
        || response.contains("unable to whitelist")
        || response.contains("I couldn't get into your clan")
        || response.contains("I am not whitelisted")) {
      FaxRequestManager.statusMessage = botName + " is not on your clan's whitelist";
      return false;
    }

    // You are only allowed 20 fax requests per day. Please
    // try again tomorrow.

    FaxRequestManager.statusMessage = response;
    return false;
  }

  public static boolean isBotOnline(final String botName) {
    // Return false and set FaxRequestManager.statusMessage to an appropriate
    // message if the bot is NOT online.

    if (botName == null) {
      FaxRequestManager.statusMessage = "No faxbots configured.";
      return false;
    }

    if (!KoLmafia.isPlayerOnline(botName)) {
      FaxRequestManager.statusMessage = botName + " is probably not online.";
      return false;
    }

    // Do not bother allocating a message if bot is online
    return true;
  }
}
