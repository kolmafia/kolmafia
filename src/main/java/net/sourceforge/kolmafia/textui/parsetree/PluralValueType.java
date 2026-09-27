package net.sourceforge.kolmafia.textui.parsetree;

import net.sourceforge.kolmafia.textui.DataTypes;
import org.eclipse.lsp4j.Location;

public class PluralValueType extends AggregateType {
  public PluralValueType(final Type type) {
    this(type, null);
  }

  public PluralValueType(final Type type, final Location location) {
    super("pluralValue", DataTypes.BOOLEAN_TYPE, type, location);
  }

  @Override
  public void setSize(int size) {
    throw new RuntimeException("Cannot modify constant value");
  }

  @Override
  public PluralValueType reference(final Location location) {
    return new PluralValueTypeReference(this, location);
  }

  private class PluralValueTypeReference extends PluralValueType {
    private PluralValueTypeReference(
        final PluralValueType pluralValueType, final Location location) {
      super(pluralValueType.indexType, location);
    }

    @Override
    public Location getDefinitionLocation() {
      return PluralValueType.this.getDefinitionLocation();
    }
  }
}
