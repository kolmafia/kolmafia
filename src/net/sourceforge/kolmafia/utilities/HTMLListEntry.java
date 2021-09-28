package net.sourceforge.kolmafia.utilities;

public class HTMLListEntry implements Comparable<HTMLListEntry> {
  private final String value;
  private String color;
  private String htmlText;

  public HTMLListEntry(String value, String color) {
    this.value = value;
    this.setColor(color);
  }

  public int compareTo(HTMLListEntry o) {
    String compareValue;

    compareValue = o.value;

    return this.value.compareTo(compareValue);
  }

  public String getValue() {
    return this.value;
  }

  public void updateColor() {}

  public void setColor(String color) {
    if (this.color != null && this.color.equals(color)) {
      return;
    }

    this.color = color;
    this.htmlText = "<html><font color=\"" + color + "\">" + value + "</font></html>";
  }

  @Override
  public String toString() {
    this.updateColor();

    return htmlText;
  }
}
