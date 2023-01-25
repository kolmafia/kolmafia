package net.sourceforge.kolmafia.utilities;

public class Indexed<TIndex, TValue> {

  public TIndex index;
  public TValue value;

  public Indexed(TIndex index, TValue value) {
    this.index = index;
    this.value = value;
  }
}
