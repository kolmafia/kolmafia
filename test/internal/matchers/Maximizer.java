package internal.matchers;

import net.sourceforge.kolmafia.maximizer.Boost;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class Maximizer {
  public static Matcher<Boost> recommends(String itemName) {
    return new TypeSafeMatcher<>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("item " + itemName + " to be recommended");
      }

      @Override
      protected boolean matchesSafely(Boost boost) {
        if (!boost.isEquipment()) {
          return false;
        }
        var item = boost.getItem();
        if (item == null) {
          return false;
        }
        return itemName.equals(item.getName());
      }
    };
  }

  public static Matcher<Boost> recommends(int itemId) {
    return new TypeSafeMatcher<>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("item with id " + itemId + " to be recommended");
      }

      @Override
      protected boolean matchesSafely(Boost boost) {
        if (!boost.isEquipment()) {
          return false;
        }
        var item = boost.getItem();
        if (item == null) {
          return false;
        }
        return itemId == item.getItemId();
      }
    };
  }
}
