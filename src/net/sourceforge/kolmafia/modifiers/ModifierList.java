package net.sourceforge.kolmafia.modifiers;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.sourceforge.kolmafia.ModifierExpression;
import net.sourceforge.kolmafia.modifiers.ModifierList.ModifierValue;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ModifierList implements Iterable<ModifierValue> {

  private final LinkedList<ModifierValue> list;

  public ModifierList() {
    this.list = new LinkedList<>();
  }

  @Override
  public Iterator<ModifierValue> iterator() {
    return this.list.iterator();
  }

  public Stream<ModifierValue> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  public void clear() {
    this.list.clear();
  }

  public int size() {
    return this.list.size();
  }

  public void addAll(final ModifierList list) {
    this.list.addAll(list.list);
  }

  public void addModifier(final ModifierValue modifier) {
    this.list.add(modifier);
  }

  public void addModifier(final String name, final String value) {
    this.list.add(new ModifierValue(name, value));
  }

  public void addToModifier(final ModifierValue modifier) {
    String name = modifier.getName();
    String current = this.getModifierValue(name);
    if (current == null) {
      this.list.add(modifier);
    } else {
      // We can only add to numeric values
      String value = modifier.getValue();
      if (StringUtilities.isNumeric(current) && StringUtilities.isNumeric(value)) {
        int newValue = Integer.parseInt(current) + Integer.parseInt(value);
        this.removeModifier(name);
        this.list.add(new ModifierValue(name, String.valueOf(newValue)));
      }
    }
  }

  public void addToModifier(final String name, final String value) {
    String current = this.getModifierValue(name);
    if (current == null) {
      this.list.add(new ModifierValue(name, value));
    } else {
      // We can only add to numeric values
      if (StringUtilities.isNumeric(current) && StringUtilities.isNumeric(value)) {
        int newValue = Integer.parseInt(current) + Integer.parseInt(value);
        this.removeModifier(name);
        this.list.add(new ModifierValue(name, String.valueOf(newValue)));
      }
    }
  }

  public boolean containsModifier(final String name) {
    for (ModifierValue modifier : this.list) {
      if (name.equals(modifier.name)) {
        return true;
      }
    }
    return false;
  }

  public String getModifierValue(final String name) {
    for (ModifierValue modifier : this.list) {
      if (name.equals(modifier.name)) {
        return modifier.value;
      }
    }
    return null;
  }

  public ModifierValue removeModifier(final String name) {
    Iterator<ModifierValue> iterator = this.iterator();
    while (iterator.hasNext()) {
      ModifierValue modifier = iterator.next();
      if (name.equals(modifier.name)) {
        iterator.remove();
        return modifier;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    for (ModifierValue modifier : this.list) {
      if (buffer.length() > 0) {
        buffer.append(", ");
      }

      modifier.toString(buffer);
    }
    return buffer.toString();
  }

  public static class ModifierValue {

    private final String name;
    private String value;

    public ModifierValue(final String name, final String value) {
      this.name = name;
      this.value = value;
    }

    public String getName() {
      return this.name;
    }

    public String getValue() {
      return this.value;
    }

    public void setValue(final String value) {
      this.value = value;
    }

    public void eval(final Lookup lookup) {
      if (this.value == null) {
        return;
      }

      int lb = this.value.indexOf("[");
      if (lb == -1) {
        return;
      }

      int rb = this.value.indexOf("]");
      if (rb == -1) {
        return;
      }

      ModifierExpression expr = new ModifierExpression(this.value.substring(lb + 1, rb), lookup);
      if (expr.hasErrors()) {
        return;
      }

      int val = (int) expr.eval();
      this.value = (val > 0 ? "+" : "") + val;
    }

    public void toString(final StringBuilder buffer) {
      buffer.append(name);
      if (value != null) {
        buffer.append(": ");
        buffer.append(value);
      }
    }

    @Override
    public String toString() {
      StringBuilder buffer = new StringBuilder();
      this.toString(buffer);
      return buffer.toString();
    }
  }
}
