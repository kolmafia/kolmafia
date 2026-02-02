package net.sourceforge.kolmafia.modifiers;

import java.util.List;

public class StringOrList extends ListOrT<String> {

  public StringOrList(String tValue) {
    super(tValue);
  }

  public StringOrList(List<String> listValue) {
    super(listValue);
  }

  public boolean isString() {
    return isT();
  }

  @Override
  public String defaultT() {
    return "";
  }

  public String getStringValue() {
    return getTValue();
  }

  public StringOrList append(StringOrList newValue) {
    return super.append(newValue, StringOrList::new);
  }
}
