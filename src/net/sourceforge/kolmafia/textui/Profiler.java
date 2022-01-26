package net.sourceforge.kolmafia.textui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.sourceforge.kolmafia.KoLConstants;

public class Profiler {
  public String name;
  public int count;
  public long total; // milliseconds, added to all functions in call stack
  public long net; // milliseconds, added to current function only
  public long net0; // starting point for current net time accumulation

  private Profiler next; // linked list
  private static Profiler freeList;
  private static final Map<String, Profiler> totals = new HashMap<>();

  private Profiler() {}

  public static Profiler create(String name) {
    Profiler rv = Profiler.freeList;
    if (rv == null) {
      rv = new Profiler();
    } else {
      Profiler.freeList = rv.next;
    }
    rv.name = name;
    rv.total = 0L;
    rv.net = 0L;
    return rv;
  }

  public void finish() {
    Profiler existing = Profiler.totals.get(this.name);
    if (existing != null) {
      ++existing.count;
      existing.total += this.total;
      existing.net += this.net;
      this.next = Profiler.freeList;
      Profiler.freeList = this;
    } else {
      this.count = 1;
      Profiler.totals.put(this.name, this);
    }
  }

  public static String summary() {
    StringBuffer buff = new StringBuffer();
    ArrayList<Profiler> list = new ArrayList<Profiler>();
    list.addAll(Profiler.totals.values());
    Profiler.totals.clear();

    buff.append("<br>");

    list.sort(
        new Comparator<Profiler>() {
          @Override
          public int compare(Profiler left, Profiler right) {
            return (int) Math.signum(right.total - left.total);
          }
        });
    Profiler.addTable(buff, list, "(sorted by total time)");

    buff.append("<br>");

    list.sort(
        new Comparator<Profiler>() {
          @Override
          public int compare(Profiler left, Profiler right) {
            return (int) Math.signum(right.net - left.net);
          }
        });
    Profiler.addTable(buff, list, "(sorted by net time)");

    buff.append("<br>");
    return buff.toString();
  }

  private static void addTable(StringBuffer buff, ArrayList<Profiler> list, String title) {
    buff.append("<table border=0><tr><td>Count</td><td>Total</td>");
    buff.append("<td>Net</td><td>Name ");
    buff.append(title);
    buff.append("</td></tr>");
    Iterator<Profiler> i = list.iterator();
    while (i.hasNext()) {
      Profiler p = i.next();
      buff.append("<tr><td>");
      buff.append(p.count);
      buff.append("</td><td>");
      buff.append(KoLConstants.NONSCIENTIFIC_FORMAT.format(p.total / 1e9d));
      buff.append("</td><td>");
      buff.append(KoLConstants.NONSCIENTIFIC_FORMAT.format(p.net / 1e9d));
      buff.append("</td><td>");
      buff.append(p.name);
      buff.append("</td></tr>");
    }
    buff.append("</table>");
  }
}
