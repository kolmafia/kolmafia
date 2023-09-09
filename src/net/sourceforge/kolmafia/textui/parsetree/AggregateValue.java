package net.sourceforge.kolmafia.textui.parsetree;

public abstract class AggregateValue extends CompositeValue {
  public AggregateValue(final AggregateType type) {
    super(type);
  }

  // The only comparison we implement is equality; we define no
  // "natural order" for an aggregate value.
  //
  // Value implements equals and equalsIgnoreCase in terms of
  // equals(Value value, boolean ignoreCase)
  //
  // Therefore, that is the only method we need to implement here

  @Override
  protected boolean equals(final Object o, boolean ignoreCase) {
    // No object equals null
    if (o == null) {
      return false;
    }

    // If the objects are identical Objects, easy equality
    if (this == o) {
      return true;
    }

    // The Parser enforces this at compile time, but...
    if (!(o instanceof AggregateValue)) {
      return false;
    }

    AggregateValue av = (AggregateValue) o;

    if (!this.type.equals(av.getType())) {
      return false;
    }

    // Shallow equality: we don't actually look at the contents.
    if (this.count() == av.count()) {
      return true;
    }

    return false;
  }

  public Type getDataType() {
    return ((AggregateType) this.type).getDataType();
  }

  @Override
  public abstract int count();

  @Override
  public abstract boolean contains(final Value index);

  @Override
  public String toString() {
    return "aggregate " + this.type.toString();
  }
}
