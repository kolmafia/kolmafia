package net.sourceforge.kolmafia.utilities;

public class IntOrString {

  private int intValue = -1;
  private String stringValue = null;

  public IntOrString(String stringValue) {
    this.stringValue = stringValue;
  }

  public IntOrString(int intValue) {
    this.intValue = intValue;
  }

  public boolean isInt() {
    return stringValue == null;
  }

  public boolean isString() {
    return stringValue != null;
  }

  public int getIntValue() {
    return intValue;
  }

  public String getStringValue() {
    return stringValue;
  }

  @Override
  public int hashCode() {
    return isInt() ? this.intValue : this.stringValue.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof IntOrString other)) {
      return false;
    }
    return isInt() && other.isInt()
        ? this.intValue == other.intValue
        : this.stringValue.equals(other.stringValue);
  }

  @Override
  public String toString() {
    return isInt() ? Integer.toString(this.intValue) : this.stringValue;
  }
}
