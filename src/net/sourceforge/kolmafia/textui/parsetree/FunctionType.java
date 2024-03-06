package net.sourceforge.kolmafia.textui.parsetree;

import java.util.Arrays;
import java.util.List;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.DataTypes.TypeSpec;
import net.sourceforge.kolmafia.textui.parsetree.Function.MatchType;
import net.sourceforge.kolmafia.textui.parsetree.Type.BadType;
import org.eclipse.lsp4j.Location;

public class FunctionType extends Type {
  private final Type returnType;
  private final Type[] parameterTypes;
  private List<Type> parameterTypeList;
  private String typeString = null;

  public FunctionType(final Type returnType, final List<Type> parameterTypes) {
    this(returnType, parameterTypes.toArray(new Type[0]), null);
    this.parameterTypeList = parameterTypes;
  }

  public FunctionType(final Type returnType, final Type[] parameterTypes) {
    this(returnType, parameterTypes, null);
  }

  public FunctionType(final Type returnType, final Type[] parameterTypes, final Location location) {
    super("function", TypeSpec.FUNCTION, location);

    this.returnType = returnType;
    this.parameterTypes = parameterTypes;
    this.primitive = false;
  }

  public Type getReturnType() {
    return this.returnType;
  }

  public Type[] getParameterTypes() {
    return this.parameterTypes;
  }

  public List<Type> getParameterTypeList() {
    if (this.parameterTypeList == null) {
      this.parameterTypeList = Arrays.asList(this.parameterTypes);
    }
    return this.parameterTypeList;
  }

  public boolean paramsMatch(
      final List<? extends TypedNode> params, MatchType match, boolean vararg) {
    return Function.paramsMatch(params, this.getParameterTypeList(), match, vararg);
  }

  @Override
  public Value initialValue() {
    ArrowFunction function = new ArrowFunction(this);
    FunctionValue functionValue = new FunctionValue(function);
    Type returnType = this.returnType;
    if (!returnType.equals(DataTypes.VOID_TYPE)) {
      Scope scope = function.getScope();
      Evaluable initialValue = Value.locate(null, returnType.initialValueExpression());
      Command returnCommand = new FunctionReturn(null, initialValue, returnType);
      scope.addCommand(returnCommand);
    }
    return functionValue;
  }

  @Override
  public boolean equals(final Type o) {
    if (o instanceof FunctionType fo) {
      // The return type must match
      if (!this.returnType.equals(fo.returnType)) {
        return false;
      }
      // The parameter types must match
      int parameterCount = this.parameterTypes.length;
      if (parameterCount != fo.parameterTypes.length) {
        return false;
      }
      for (int i = 0; i < parameterCount; ++i) {
        if (!this.parameterTypes[i].equals(fo.parameterTypes[i])) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public String toString() {
    if (typeString == null) {
      StringBuilder buffer = new StringBuilder("function ");
      buffer.append(this.returnType);
      buffer.append(" (");
      for (int i = 0; i < this.parameterTypes.length; ++i) {
        if (i > 0) {
          buffer.append(" ,");
        }
        buffer.append(this.parameterTypes[i]);
      }
      buffer.append(")");
      this.typeString = buffer.toString();
    }
    return this.typeString;
  }

  public static class BadFunctionType extends FunctionType implements BadNode {
    public BadFunctionType(final Location location) {
      super(new BadType(null, location), new Type[0], location);
    }
  }
}
