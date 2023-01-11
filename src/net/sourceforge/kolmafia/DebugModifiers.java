package net.sourceforge.kolmafia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

public class DebugModifiers extends Modifiers {
  private static HashMap<Integer, String> wanted, adjustments;
  private static String currentType;
  private static String currentName;
  private static StringBuilder buffer;

  public static int setup(String parameters) {
    DebugModifiers.wanted = new HashMap<>();
    DebugModifiers.adjustments = new HashMap<>();
    for (int i = 0; i < Modifiers.DOUBLE_MODIFIERS; ++i) {
      String name = Modifiers.getModifierName(i);
      if (name.toLowerCase().contains(parameters)) {
        DebugModifiers.wanted.put(i, "<td colspan=3>" + name + "</td>");
        DebugModifiers.adjustments.put(i, "<td colspan=2>" + name + "</td>");
      }
    }
    DebugModifiers.currentType = "type";
    DebugModifiers.currentName = "source";
    DebugModifiers.buffer = new StringBuilder("<table border=2>");
    return DebugModifiers.wanted.size();
  }

  private static String getDesc() {
    return switch (DebugModifiers.currentType) {
      case "Item" -> ItemDatabase.getItemDisplayName(DebugModifiers.currentName);
      case "Effect" -> EffectDatabase.getEffectDisplayName(DebugModifiers.currentName);
      case "Skill" -> SkillDatabase.getSkillDisplayName(DebugModifiers.currentName);
      default -> DebugModifiers.currentName;
    };
  }

  private static void flushRow() {
    DebugModifiers.buffer.append("<tr><td>");
    DebugModifiers.buffer.append(DebugModifiers.currentType);
    DebugModifiers.buffer.append("</td><td>");
    DebugModifiers.buffer.append(DebugModifiers.getDesc());
    DebugModifiers.buffer.append("</td>");
    for (Integer key : DebugModifiers.wanted.keySet()) {
      String item = DebugModifiers.adjustments.get(key);
      DebugModifiers.buffer.append(Objects.requireNonNullElse(item, "<td></td><td></td>"));
    }
    DebugModifiers.buffer.append("</tr>");
    DebugModifiers.adjustments.clear();
  }

  @Override
  protected void addDouble(
      final int index, final double mod, final ModifierType type, final IntOrString key) {
    if (index < 0 || index >= Modifiers.DOUBLE_MODIFIERS || mod == 0.0) {
      return;
    }

    Lookup lookup = new Lookup(type, key);

    super.addDouble(index, mod, type, key);

    if (!DebugModifiers.wanted.containsKey(index)) {
      return;
    }

    String name = lookup.getName();
    if (!name.equals(DebugModifiers.currentName) || DebugModifiers.adjustments.containsKey(index)) {
      DebugModifiers.flushRow();
    }
    DebugModifiers.currentType = type.wordsName();
    DebugModifiers.currentName = name;
    DebugModifiers.adjustments.put(
        index,
        "<td>"
            + KoLConstants.ROUNDED_MODIFIER_FORMAT.format(mod)
            + "</td><td>=&nbsp;"
            + KoLConstants.ROUNDED_MODIFIER_FORMAT.format(this.get(index))
            + "</td>");
  }

  public static void finish() {
    DebugModifiers.flushRow();
    DebugModifiers.buffer.append("</table><br>");
    RequestLogger.printLine(DebugModifiers.buffer.toString());
    RequestLogger.printLine();
    DebugModifiers.buffer = null;
  }

  public static void allModifiers() {
    DebugModifiers.buffer.append("<tr>");
    HashMap<Integer, Iterator<Change>> modifiersChangers = new HashMap<>();
    for (Integer key : DebugModifiers.wanted.keySet()) {
      String modifier = DebugModifiers.wanted.get(key);
      DebugModifiers.buffer.append(modifier);
      ArrayList<Change> modChangers = new ArrayList<>();
      for (Lookup lookup : Modifiers.getAllModifiers()) {
        Modifiers mods = Modifiers.getModifiers(lookup);
        if (mods == null) {
          continue;
        }
        double value = mods.get(key);
        if (value != 0.0) {
          ModifierType type = lookup.type;
          String name = lookup.getName();
          modChangers.add(new Change(type.wordsName(), name, value, mods.variable));
        }
      }
      if (modChangers.size() > 0) {
        Collections.sort(modChangers);
        modifiersChangers.put(key, modChangers.iterator());
      }
    }
    DebugModifiers.buffer.append("</tr>");
    while (modifiersChangers.size() > 0) {
      DebugModifiers.buffer.append("<tr>");
      for (Integer key : DebugModifiers.wanted.keySet()) {
        Iterator<Change> li = modifiersChangers.get(key);
        if (li == null) {
          DebugModifiers.buffer.append("<td colspan=3></td>");
        } else {
          Change c = li.next();
          DebugModifiers.buffer.append(c.toString());

          if (!li.hasNext()) {
            modifiersChangers.remove(key);
          }
        }
      }
      DebugModifiers.buffer.append("</tr>");
    }

    DebugModifiers.buffer.append("</table><br>");
    RequestLogger.printLine(DebugModifiers.buffer.toString());
    DebugModifiers.buffer = null;
  }

  private static class Change implements Comparable<Change> {
    String type;
    String name;
    double value;
    boolean variable;

    public Change(String type, String name, double value, boolean variable) {
      this.type = type;
      this.name = name;
      this.value = value;
      this.variable = variable;
    }

    @Override
    public String toString() {
      if (this.type.equals("Item")) {
        return "<td>Item</td><td>"
            + ItemDatabase.getItemDisplayName(this.name)
            + "</td><td>"
            + KoLConstants.ROUNDED_MODIFIER_FORMAT.format(this.value)
            + (this.variable ? "v" : "")
            + "</td>";
      }
      if (this.type.equals("Effect")) {
        return "<td>Effect</td><td>"
            + EffectDatabase.getEffectDisplayName(this.name)
            + "</td><td>"
            + KoLConstants.ROUNDED_MODIFIER_FORMAT.format(this.value)
            + (this.variable ? "v" : "")
            + "</td>";
      }
      return "<td>"
          + this.type
          + "</td><td>"
          + this.name
          + "</td><td>"
          + KoLConstants.ROUNDED_MODIFIER_FORMAT.format(this.value)
          + (this.variable ? "v" : "")
          + "</td>";
    }

    @Override
    public int compareTo(Change o) {
      if (this.value < o.value) return 1;
      if (this.value > o.value) return -1;
      return this.name.compareTo(o.name);
    }
  }
}
