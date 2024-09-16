package internal.matchers;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.maximizer.Boost;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class Maximizer {
  public static Matcher<Boost> recommends(String itemName) {
    return new TypeSafeMatcher<>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("item " + itemName);
      }

      @Override
      protected boolean matchesSafely(Boost boost) {
        if (!isItem(boost)) {
          return false;
        }
        return equalsItem(itemName, boost.getItem());
      }
    };
  }

  public static Matcher<Boost> recommends(int itemId) {
    return new TypeSafeMatcher<>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("item with id " + itemId);
      }

      @Override
      protected boolean matchesSafely(Boost boost) {
        if (!isItem(boost)) {
          return false;
        }
        return itemId == boost.getItem().getItemId();
      }
    };
  }

  public static Matcher<Boost> recommendsSlot(Slot slot) {
    return new TypeSafeMatcher<>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("slot " + slot);
      }

      @Override
      protected boolean matchesSafely(Boost boost) {
        if (!isItem(boost)) {
          return false;
        }
        return boost.getSlot() == slot;
      }
    };
  }

  public static Matcher<Boost> recommendsSlot(Slot slot, String itemName) {
    return new TypeSafeMatcher<>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("slot " + slot + " with item " + itemName);
      }

      @Override
      protected boolean matchesSafely(Boost boost) {
        if (boost.getSlot() != slot) {
          return false;
        }
        if (!isItem(boost)) {
          return false;
        }
        return equalsItem(itemName, boost.getItem());
      }
    };
  }

  public static boolean isItem(Boost boost) {
    if (!boost.isEquipment()) {
      return false;
    }
    return boost.getItem() != null;
  }

  public static boolean equalsItem(String testItem, AdventureResult boostItem) {
    var encoded = StringUtilities.getEntityEncode(testItem);
    if (encoded.equals(boostItem.getName())) {
      return true;
    }
    return AdventureResult.tallyItem(encoded).equals(boostItem);
  }
}
