package net.sourceforge.kolmafia.textui.parsetree;

import net.sourceforge.kolmafia.textui.DataTypes;
import org.eclipse.lsp4j.Location;

public class TypeDef extends Type {
  private final Type base;

  public TypeDef(final String name, final Type base, final Location location) {
    super(name, DataTypes.TYPE_TYPEDEF, location);
    this.primitive = base.isPrimitive();
    this.base = base;
  }

  @Override
  public Type getBaseType() {
    return this.base.getBaseType();
  }

  @Override
  public Value initialValue() {
    return this.base.initialValue();
  }

  @Override
  public Value parseValue(final String name, final boolean returnDefault) {
    return this.base.parseValue(name, returnDefault);
  }

  @Override
  public Value initialValueExpression() {
    return new TypeInitializer(this.base.getBaseType());
  }

  @Override
  public boolean equals(final Type o) {
    return o instanceof TypeDef && this.name.equals(o.name);
  }

  @Override
  public TypeDef reference(final Location location) {
    return new TypeDefReference(this, location);
  }

  private class TypeDefReference extends TypeDef {
    private TypeDefReference(final TypeDef typeDef, final Location location) {
      super(typeDef.name, typeDef.base, location);
    }

    @Override
    public Location getDefinitionLocation() {
      return TypeDef.this.getDefinitionLocation();
    }
  }

  @Override
  public boolean isBad() {
    return this.base.isBad();
  }
}
