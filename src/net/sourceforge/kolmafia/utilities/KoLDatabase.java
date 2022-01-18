package net.sourceforge.kolmafia.utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.sourceforge.kolmafia.KoLConstants;

public class KoLDatabase {
  private static class ItemCounter implements Comparable<ItemCounter> {
    private final int count;
    private final String name;

    public ItemCounter(final String name, final int count) {
      this.name = name;
      this.count = count;
    }

    @Override
    public int compareTo(final ItemCounter o) {

      if (this.count != o.count) {
        return o.count - this.count;
      }

      return this.name.compareToIgnoreCase(o.name);
    }

    @Override
    public String toString() {
      return this.name + ": " + this.count;
    }
  }

  public static String getBreakdown(final List<String> items) {
    if ((items == null) || (items.isEmpty())) {
      return KoLConstants.LINE_BREAK;
    }

    StringBuilder strbuf = new StringBuilder();
    strbuf.append(KoLConstants.LINE_BREAK);

    Object[] itemArray = new Object[items.size()];
    items.toArray(itemArray);

    int currentCount = 1;

    ArrayList<ItemCounter> itemList = new ArrayList<>();

    for (int i = 1; i < itemArray.length; ++i) {
      if (itemArray[i - 1] == null) {
        continue;
      }

      if (itemArray[i] != null && !itemArray[i - 1].equals(itemArray[i])) {
        itemList.add(new ItemCounter(itemArray[i - 1].toString(), currentCount));
        currentCount = 0;
      }

      ++currentCount;
    }

    if (itemArray[itemArray.length - 1] != null) {
      itemList.add(new ItemCounter(itemArray[itemArray.length - 1].toString(), currentCount));
    }

    strbuf.append("<ul>");
    Collections.sort(itemList);

    for (ItemCounter itemCounter : itemList) {
      strbuf.append("<li><nobr>").append(itemCounter).append("</nobr></li>");
      strbuf.append(KoLConstants.LINE_BREAK);
    }

    strbuf.append("</ul>");
    strbuf.append(KoLConstants.LINE_BREAK);

    return strbuf.toString();
  }

  /**
   * Calculates the sum of all the integers in the given list. Note that the list must consist
   * entirely of Integer objects and the only usages of this pass a list of Integers
   */
  public static long calculateTotal(final List<Integer> values) {
    long total = 0;
    for (Integer obj : values) {
      if (obj != null) {
        total += obj;
      }
    }

    return total;
  }

  /**
   * Calculates the average of all the integers in the given list. Note that the list must consist
   * entirely of Integer objects.
   */
  public static float calculateAverage(final List<Integer> values) {
    return (float) KoLDatabase.calculateTotal(values) / (float) values.size();
  }
}
