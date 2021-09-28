package net.sourceforge.kolmafia.webui;

import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.UseItemRequest;

public class UseItemDecorator {
  public static final void decorate(final String location, final StringBuffer buffer) {
    if (location.startsWith("inventory.php") && !location.contains("action=message")) {
      return;
    }

    // Saved when we executed inv_use.php, whether or not it
    // redirected to inventory.php
    int itemId = UseItemRequest.currentItemId();

    switch (itemId) {
      case ItemPool.BOO_CLUE:
      case ItemPool.GLUED_BOO_CLUE:
        UseItemDecorator.decorateBooClue(buffer);
        break;

      case ItemPool.PALINDROME_BOOK_1:
        UseItemDecorator.decorateVolume1(buffer);
        break;

      case ItemPool.PALINDROME_BOOK_2:
        UseItemDecorator.decorateVolume2(buffer);
        break;

      case ItemPool.POKE_GROW_FERTILIZER:
        UseItemDecorator.decorateFertilizer(buffer);
        break;
    }
  }

  private static void decorateItem(final StringBuffer buffer, final StringBuilder insert) {
    String search = "</blockquote></td></tr>";
    int index = buffer.indexOf(search);

    if (index == -1) {
      return;
    }

    // We will insert things before the end of the table
    index += search.length();

    String link = "<tr align=center><td>" + insert + "</td></tr>";
    buffer.insert(index, link);
  }

  // <table  width=95%  cellspacing=0 cellpadding=0><tr><td style="color: white;" align=center
  // bgcolor=blue><b>Results:</b></td></tr><tr><td style="padding: 5px; border: 1px solid
  // blue;"><center><table><tr><td><center><img
  // src="http://images.kingdomofloathing.com/itemimages/ratchet.gif" width=30
  // height=30><br></center><blockquote>TEXT</blockquote></td></tr></table>

  private static void decorateBooClue(final StringBuffer buffer) {
    if (buffer.indexOf("A-Boo Peak") == -1) {
      return;
    }

    // Add the link to adventure in A-Boo Peak
    StringBuilder link = new StringBuilder();
    link.append("<a href=\"adventure.php?snarfblat=296\">");
    link.append("[Adventure at A-Boo Peak]");
    link.append("</a>");

    UseItemDecorator.decorateItem(buffer, link);
  }

  private static void decorateVolume1(final StringBuffer buffer) {
    // Add the link
    StringBuilder link = new StringBuilder();
    link.append("<a href=\"place.php?whichplace=palindome&action=pal_droffice\">");
    link.append("[Place stuff on the shelves]");
    link.append("</a>");

    UseItemDecorator.decorateItem(buffer, link);
  }

  private static void decorateVolume2(final StringBuffer buffer) {
    // Add the link
    StringBuilder link = new StringBuilder();
    link.append("<a href=\"place.php?whichplace=palindome&action=pal_mroffice\">");
    link.append("[Talk to Mr. Alarm]");
    link.append("</a>");

    UseItemDecorator.decorateItem(buffer, link);
  }

  private static void decorateFertilizer(final StringBuffer buffer) {
    // Add link to campground
    StringBuilder link = new StringBuilder();
    link.append("<a href=\"campground.php\">");
    link.append("[Visit Campground]");
    link.append("</a>");

    UseItemDecorator.decorateItem(buffer, link);
  }
}
