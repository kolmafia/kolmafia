package internal.matchers;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public class Item {
  public static <T> Matcher<T> isInInventory(Matcher<? super Integer> inventoryMatcher) {
    return new FeatureMatcher<T, Integer>(
        inventoryMatcher, "count in inventory to be", "inventory count") {
      @Override
      protected Integer featureValueOf(T actual) {
        if (actual instanceof AdventureResult) {
          return InventoryManager.getCount(((AdventureResult) actual).getItemId());
        }
        int id =
            (actual instanceof Integer) ? (int) actual : ItemDatabase.getItemId(actual.toString());
        return InventoryManager.getCount(id);
      }
    };
  }

  public static <T> Matcher<T> isInInventory(int count) {
    return isInInventory(equalTo(count));
  }

  public static <T> Matcher<T> isInInventory() {
    return isInInventory(greaterThan(0));
  }
}
