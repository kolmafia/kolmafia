package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;

public class MonkeyPawRequest extends GenericRequest {
  private final String wish;

  public MonkeyPawRequest(final String wish) {
    super("choice.php");

    this.addFormField("whichchoice", "1501");
    this.addFormField("wish", wish);
    this.addFormField("option", "1");
    this.wish = wish.toLowerCase().trim();
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

    if (InventoryManager.getAccessibleCount(ItemPool.CURSED_MONKEY_PAW) == 0) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You do not have a cursed monkey paw.");
      return;
    }

    if (Preferences.getInteger("_monkeyPawWishesUsed") >= 5) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You have been cursed enough today.");
      return;
    }

    if (!KoLCharacter.hasEquipped(ItemPool.CURSED_MONKEY_PAW)) {
      InventoryManager.retrieveItem(ItemPool.CURSED_MONKEY_PAW, 1, true);
    }

    GenericRequest pawRequest = new GenericRequest("main.php?action=cmonk", false);
    pawRequest.run();

    // Redirects to choice.php?forceoption=0
    super.run();
  }

  // You look at your cursed monkey paw.  It has 5 fingers held up expectantly.
  // You look at your cursed monkey paw.  It has 4 fingers held up expectantly.
  // You look at your cursed monkey paw.  It has 3 fingers held up expectantly.
  // You look at your cursed monkey paw.  It has 2 fingers held up expectantly.
  // You look at your cursed monkey paw.  It has 1 finger held up expectantly.
  // You look at your cursed monkey paw.  It is closed in a tight withholding fist.
  private static final Pattern WISH_PATTERN =
      Pattern.compile("It has (\\d) fingers? held up expectantly\\.");

  public static void visitChoice(final String responseText) {
    if (responseText.contains("It is closed in a tight withholding fist.")) {
      Preferences.setInteger("_monkeyPawWishesUsed", 5);
      return;
    }

    Matcher matcher = MonkeyPawRequest.WISH_PATTERN.matcher(responseText);
    if (matcher.find()) {
      int wishesLeft = Integer.parseInt(matcher.group(1));
      Preferences.setInteger("_monkeyPawWishesUsed", 5 - wishesLeft);
    }
  }

  // You acquire an effect: <b>Cursed by a Monkey</b><br>(duration: 7 Adventures)
  private static final Pattern CURSE_PATTERN =
      Pattern.compile("<b>Cursed by a Monkey</b>.*?\\(duration: (\\d+) Adventures\\)");

  public static void postChoice(final String responseText, final String wish) {
    if (!responseText.contains("Wish granted.")) {
      // The wish failed.
      //
      // If we add code to predict what a wish will provide and thought
      // it would succeed, do something here.
      return;
    }

    // <table class="item" style="float: none"
    // rel="id=8691&s=0&q=0&d=0&g=0&t=1&n=1&m=1&p=0&u=u"><tr><td><img
    // src="https://d2uyhvukfffg5a.cloudfront.net/itemimages/bribebag.gif" alt="bag of foreign
    // bribes" title="bag of foreign bribes" class=hand onClick='descitem(597574332)' ></td><td
    // valign=center class=effect>You acquire an item: <b>bag of foreign
    // bribes</b></td></tr></table>

    // The wish succeeded.

    // If we add code to predict what a wish will provide, we could
    // parse acquired item or effect and see if we thought it agrees
    // with our expectation.

    Preferences.increment("_monkeyPawWishesUsed");

    Matcher matcher = MonkeyPawRequest.CURSE_PATTERN.matcher(responseText);
    if (matcher.find()) {
      int wishesUsed = Integer.parseInt(matcher.group(1)) / 7;
      Preferences.setInteger("_monkeyPawWishesUsed", wishesUsed);
    }
  }
}
