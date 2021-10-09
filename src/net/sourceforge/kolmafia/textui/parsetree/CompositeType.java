package net.sourceforge.kolmafia.textui.parsetree;

import org.eclipse.lsp4j.Location;

public abstract class CompositeType extends Type {
  public CompositeType(final String name, final int type, final Location location) {
    super(name, type, location);
    this.primitive = false;
  }

  /** Returns the Type of the keys used in this composite type. */
  public abstract Type getIndexType();

  /**
   * For {@link AggregateType}, returns the type of the values of this composite type.
   *
   * <p>For {@link RecordType}, use {@link CompositeType#getDataType(Object)}.
   */
  public abstract Type getDataType();

  /**
   * For {@link RecordType}, returns the type of the value associated with this key (if any).
   *
   * <p>For {@link AggregateType}, use {@link CompositeType#getDataType()}.
   */
  public abstract Type getDataType(final Object key);

  public abstract Value getKey(final Value key);

  @Override
  public Value initialValueExpression() {
    return new TypeInitializer(this);
  }
}
