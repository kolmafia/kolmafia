package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CurseRequest extends GenericRequest {
  private static final Pattern ITEM_PATTERN = Pattern.compile("whichitem=(\\d+)");
  private static final Pattern PLAYER_PATTERN =
      Pattern.compile("(?=.*action=use).*targetplayer=([^&]*)");
  private static final Pattern QTY_PATTERN =
      Pattern.compile("You have ([\\d,]+) more |You don't have any more ");

  private final AdventureResult itemUsed;

  private static final AdventureResult MARSHMALLLOW = ItemPool.get(ItemPool.MARSHMALLOW, 1);

  public CurseRequest(final AdventureResult item) {
    this(item, KoLCharacter.getPlayerId(), "");
  }

  public CurseRequest(final AdventureResult item, final String target, final String message) {
    super("curse.php");
    this.itemUsed = item;
    this.addFormField("action", "use");
    this.addFormField("whichitem", String.valueOf(item.getItemId()));
    this.addFormField("targetplayer", target);
    this.addMessage(message);
  }

  @Override
  public void run() {
    AdventureResult item = this.itemUsed;
    String action;

    switch (item.getItemId()) {
      case ItemPool.CRIMBO_TRAINING_MANUAL -> {
        // Usable once per day
        if (Preferences.getBoolean("_crimboTraining")) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "You've already trained somebody today.");
          return;
        }
        item = item.getInstance(1);
        action = "Training ";
      }
      case ItemPool.PING_PONG_TABLE -> {
        // Usable once per day
        if (Preferences.getBoolean("_pingPongGame")) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "You've already played ping-pong today.");
          return;
        }
        item = item.getInstance(1);
        action = "Playing ping-pong with ";
      }
      default -> {
        action = "Throwing " + this.itemUsed.getName() + " at ";
      }
    }

    // Ensure we have enough of the item in inventory
    if (!InventoryManager.retrieveItem(item)) {
      return;
    }

    String message = action + this.getFormField("targetplayer") + "...";

    for (int i = item.getCount(); KoLmafia.permitsContinue() && i > 0; --i) {
      KoLmafia.updateDisplay(message);
      super.run();
    }
  }

  @Override
  public void processResults() {
    CurseRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static final void parseResponse(final String location, final String responseText) {
    if (!location.startsWith("curse.php")) {
      return;
    }

    Matcher playerMatcher = CurseRequest.PLAYER_PATTERN.matcher(location);
    if (!playerMatcher.find()) {
      return;
    }
    String player = playerMatcher.group(1);

    if (responseText.contains("That player could not be found")) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR,
          player + " evaded your thrown item by the unusual strategy of being nonexistent.");
      return;
    }

    if (responseText.contains("try again later")
        || responseText.contains("cannot be used")
        || responseText.contains("can't use this item")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Can't use the item on that player at the moment.");
      return;
    }

    if (responseText.contains("You can't fire")
        || responseText.contains("That player has already been hit")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You cannot arrow that person.");
      return;
    }

    Matcher itemMatcher = CurseRequest.ITEM_PATTERN.matcher(location);
    if (!itemMatcher.find()) {
      return;
    }
    AdventureResult item = ItemPool.get(StringUtilities.parseInt(itemMatcher.group(1)), 1);

    // Special items require special processing
    switch (item.getItemId()) {
      case ItemPool.SMORE_GUN -> {
        // When you "throw" a s'more gun at someone, marshmallows get used up
        item = CurseRequest.MARSHMALLLOW;
      }
      case ItemPool.CRIMBO_TRAINING_MANUAL -> {
        // Usable once per day on another player

        if (responseText.contains("They already know that skill.")) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "They already know that skill.");
          return;
        }

        if (responseText.contains("You've already trained somebody today.")) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "You've already trained somebody today.");
          Preferences.setBoolean("_crimboTraining", true);
          return;
        }

        String message = "Training " + player;

        // You train Veracity.
        if (responseText.contains("You train")) {
          RequestLogger.updateSessionLog(message);
          RequestLogger.updateSessionLog("You train " + player + ".");
          Preferences.setBoolean("_crimboTraining", true);
          return;
        }

        // No count and is not consumed.
        return;
      }
      case ItemPool.PING_PONG_TABLE -> {
        // Usable once per day on another player

        String message = "Playing ping-pong with " + player;

        // You aren't up for another ping-pong game today.
        if (responseText.contains("You aren't up for another ping-pong game today.")) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "You've already played ping-pong today.");
          Preferences.setBoolean("_pingPongGame", true);
          return;
        }

        // You're able to use your ping-pong skill to defeat Blippy Bloppy. Yay!
        // You're unable to use your ping-pong skill to defeat Veracity. Boo.
        if (responseText.contains("use your ping-pong skill")) {
          String resultMessage =
              "You " + (responseText.contains("unable") ? "lost" : "won") + " the ping-pong game.";
          KoLmafia.updateDisplay(resultMessage);
          RequestLogger.updateSessionLog(message);
          RequestLogger.updateSessionLog(resultMessage);
          Preferences.setBoolean("_pingPongGame", true);
          return;
        }

        // No count and is not consumed.
        return;
      }
    }

    Matcher quantityMatcher = CurseRequest.QTY_PATTERN.matcher(responseText);
    if (!quantityMatcher.find()) {
      return;
    }
    int qty =
        quantityMatcher.group(1) == null ? 0 : StringUtilities.parseInt(quantityMatcher.group(1));
    qty = item.getCount(KoLConstants.inventory) - qty;
    if (qty != 0) {
      item = item.getInstance(qty);
      ResultProcessor.processResult(item.getNegation());
    }

    if (responseText.contains("You don't have that item")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Have not, throw not.");
      return;
    }

    if (responseText.contains("No message?") || responseText.contains("no message")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "That item requires a message.");
      return;
    }

    if (responseText.contains("FAA regulations prevent")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You've already used a Warbear Gyrocopter today.");
      Preferences.setBoolean("_warbearGyrocopterUsed", true);
      return;
    }

    if (responseText.contains("You input the address")) {
      Preferences.setBoolean("_warbearGyrocopterUsed", true);
    }

    RequestLogger.updateSessionLog("throw " + item + " at " + player);
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("curse.php")) {
      return false;
    }

    return true;
  }

  private void addMessage(final String message) {
    if (message.length() == 0) {
      return;
    }

    if (this.itemUsed.equals(ItemPool.get(ItemPool.BRICK, 1))) {
      this.addFormField("message", message);
      return;
    }

    String[] msg = message.split("\\s*\\|\\s*");
    for (int i = 0; i < msg.length; ++i) {
      this.addFormField("text" + ((char) (i + 'a')), msg[i]);
    }
  }
}
