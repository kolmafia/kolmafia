package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.textui.AshRuntime;
import org.eclipse.lsp4j.Location;

public abstract class Function extends Symbol {
  protected FunctionType functionType;
  protected Type type;
  protected List<VariableReference> variableReferences;
  protected List<Type> parameterTypes;
  private String signature;

  public Function(
      final String name,
      final Type type,
      final List<VariableReference> variableReferences,
      final Location location) {
    super(name, location);
    this.type = type;
    this.variableReferences = variableReferences;
  }

  public Function(final String name, final Type type) {
    this(name, type, new ArrayList<>(), null);
  }

  public Function(
      final FunctionType type,
      final List<VariableReference> variableReferences,
      final Location location) {
    this("function", type.getReturnType(), variableReferences, location);
    this.functionType = functionType;
  }

  public List<Type> getParameterTypes() {
    if (this.parameterTypes == null && this.variableReferences != null) {
      this.parameterTypes = new ArrayList<>();
      for (VariableReference variableReference : variableReferences) {
        this.parameterTypes.add(variableReference.getRawType());
      }
    }
    return this.parameterTypes;
  }

  public FunctionType getFunctionType() {
    if (this.functionType == null && this.variableReferences != null) {
      this.functionType = new FunctionType(this.type, this.getParameterTypes());
    }
    return this.functionType;
  }

  public Type getType() {
    return this.type;
  }

  public List<VariableReference> getVariableReferences() {
    return this.variableReferences;
  }

  public void setVariableReferences(final List<VariableReference> variableReferences) {
    this.variableReferences = variableReferences;
    this.functionType = null;
  }

  public String getSignature() {
    if (this.signature == null) {
      StringBuffer buf = new StringBuffer();
      // Since you can't usefully have multiple overloads with the
      // same parameter types but different return types, including
      // the return type in the signature isn't very useful.
      // buf.append( this.type );
      // buf.append( " " );
      buf.append(this.name);
      buf.append("(");

      String sep = "";
      for (VariableReference current : this.variableReferences) {
        buf.append(sep);
        sep = ", ";
        Type paramType = current.getType();
        buf.append(paramType);
      }

      buf.append(")");
      this.signature = buf.toString();
    }
    return this.signature;
  }

  public enum MatchType {
    ANY,
    EXACT,
    BASE,
    COERCE
  }

  public boolean paramsMatch(final Function that, final boolean base) {
    Iterator<VariableReference> it1 = this.variableReferences.iterator();
    Iterator<VariableReference> it2 = that.variableReferences.iterator();

    while (it1.hasNext() && it2.hasNext()) {
      VariableReference val1 = it1.next();
      VariableReference val2 = it2.next();
      Type paramType1 = base ? val1.getType() : val1.getRawType();
      Type paramType2 = base ? val2.getType() : val2.getRawType();
      if (!paramType1.equals(paramType2)) {
        return false;
      }
    }

    // There must be the same number of parameters

    if (it1.hasNext() || it2.hasNext()) {
      return false;
    }

    return true;
  }

  public boolean varargsClash(final Function that) {
    Iterator<VariableReference> thisIterator = this.getVariableReferences().iterator();
    Iterator<VariableReference> thatIterator = that.getVariableReferences().iterator();
    VariableReference thisVararg = null;
    VariableReference thatVararg = null;

    while (thisIterator.hasNext() || thatIterator.hasNext()) {
      VariableReference thisParam;
      if (!thisIterator.hasNext()) {
        // If this function ran out of arguments without seeing a vararg, no clash
        if (thisVararg == null) {
          return false;
        }
        thisParam = thisVararg;
      } else {
        thisParam = thisIterator.next();
      }

      Type thisParamType = thisParam.getType();
      if (thisParamType instanceof VarArgType vat) {
        thisVararg = thisParam;
        thisParamType = vat.getDataType();
      }

      VariableReference thatParam;
      if (!thatIterator.hasNext()) {
        // If that function ran out of arguments without seeing a vararg, no clash
        if (thatVararg == null) {
          return false;
        }
        thatParam = thatVararg;
      } else {
        thatParam = thatIterator.next();
      }

      Type thatParamType = thatParam.getType();
      if (thatParamType instanceof VarArgType vat) {
        thatVararg = thatParam;
        thatParamType = vat.getDataType();
      }

      // If types don't agree, nothing more to look at
      if (!thisParamType.equals(thatParamType)) {
        return false;
      }

      // If we have seen two varargs, an exact match is a clash
      if (thisVararg != null && thatVararg != null) {
        return true;
      }

      // Otherwise, continue looking for varargs
    }

    return false;
  }

  public boolean paramsMatch(
      final List<? extends TypedNode> params, MatchType match, boolean vararg) {
    return paramsMatch(params, this.getParameterTypes(), match, vararg);
  }

  public static boolean paramsMatch(
      final List<? extends TypedNode> params,
      final List<Type> paramTypes,
      MatchType match,
      boolean vararg) {
    return (vararg)
        ? paramsMatchVararg(params, paramTypes, match)
        : paramsMatchNoVararg(params, paramTypes, match);
  }

  private static boolean paramsMatchNoVararg(
      final List<? extends TypedNode> params, List<Type> paramTypes, MatchType match) {
    Iterator<Type> typeIterator = paramTypes.iterator();
    Iterator<? extends TypedNode> valIterator = params.iterator();
    boolean matched = true;

    while (matched && typeIterator.hasNext() && valIterator.hasNext()) {
      Type paramType = typeIterator.next();

      if (paramType == null || paramType instanceof VarArgType) {
        matched = false;
        break;
      }

      TypedNode currentValue = valIterator.next();
      Type valueType = currentValue.getType();

      switch (match) {
        case EXACT:
          if (!paramType.equals(currentValue.getRawType())) {
            matched = false;
          }
          break;

        case BASE:
          if (!paramType.getBaseType().equals(currentValue.getType())) {
            matched = false;
          }
          break;

        case COERCE:
          if (!Operator.validCoercion(paramType, currentValue.getType(), "parameter")) {
            matched = false;
          }
          break;
      }
    }

    if (matched && !typeIterator.hasNext() && !valIterator.hasNext()) {
      return true;
    }
    return false;
  }

  private static boolean paramsMatchVararg(
      final List<? extends TypedNode> params, List<Type> paramTypes, MatchType match) {
    Iterator<Type> typeIterator = paramTypes.iterator();
    Iterator<? extends TypedNode> valIterator = params.iterator();
    boolean matched = true;
    Type vararg = null;
    VarArgType varargType = null;

    while (matched && (vararg != null || typeIterator.hasNext()) && valIterator.hasNext()) {
      // A VarArg parameter will consume all remaining values
      Type paramType = (vararg != null) ? vararg : typeIterator.next();
      TypedNode currentValue = valIterator.next();
      Type valueType = currentValue.getType();

      // If have found the vararg, remember it.
      if (vararg == null && paramType instanceof VarArgType vat) {
        vararg = paramType;
        varargType = vat;
      }

      // Only one vararg is allowed. It must be at the end.
      if (vararg != null && typeIterator.hasNext()) {
        matched = false;
        break;
      }

      switch (match) {
        case EXACT:
          if (!paramType.equals(valueType)) {
            matched = false;
          }
          break;

        case BASE:
          if (vararg != null) {
            paramType = varargType.getDataType();
          }
          if (!paramType.equals(valueType)) {
            matched = false;
          }
          break;

        case COERCE:
          if (vararg != null) {
            paramType = varargType.getDataType();
          }
          if (!Operator.validCoercion(paramType, valueType, "parameter")) {
            matched = false;
          }
          break;
      }
    }

    if (typeIterator.hasNext()) {
      // If the next parameter is a vararg, this is
      // allowed if we ran out of parameters.
      Type currentParam = typeIterator.next();
      Type paramType = currentParam;

      if (paramType instanceof VarArgType) {
        vararg = currentParam;
        matched = true;
      }
    }

    if (matched && vararg != null && !typeIterator.hasNext() && !valIterator.hasNext()) {
      return true;
    }

    return false;
  }

  public void printDisabledMessage(AshRuntime interpreter) {
    try {
      StringBuffer message = new StringBuffer("Called disabled function: ");
      message.append(this.getName());

      message.append("(");

      String sep = "";
      for (VariableReference current : this.variableReferences) {
        message.append(sep);
        sep = ",";
        message.append(' ');
        message.append(current.getValue(interpreter).toStringValue().toString());
      }

      message.append(" )");
      RequestLogger.printLine(message.toString());
    } catch (Exception e) {
      // If it fails, don't print the disabled message.
      // Which means, exiting here is okay.
    }
  }

  public Object[] bindVariableReferences(AshRuntime interpreter, Object[] values) {
    List<Object> newValues = new ArrayList<>();

    // This is the interpreter.
    newValues.add(values[0]);

    int paramCount = 1;
    int valueCount = values.length;
    for (VariableReference paramVarRef : this.variableReferences) {
      Value value = null;

      Type paramType = paramVarRef.getType();
      if (paramType instanceof VarArgType vat) {
        // If this is a vararg, it consumes all remaining values
        if (paramCount >= valueCount) {
          value = new ArrayValue(vat, Collections.emptyList());
        } else {
          value = (Value) values[paramCount];
          if (!paramType.equals(value.getType())) {
            // Collect the values
            List<Value> varValues = new ArrayList<>();
            for (int index = paramCount; index < values.length; ++index) {
              varValues.add((Value) values[index]);
            }
            // Put them into an Array value
            value = new ArrayValue(vat, varValues);
          } else {
            // User explicitly passed us an array
          }
        }
      } else {
        value = (Value) values[paramCount];
      }

      paramCount++;

      if (interpreter != null) {
        // Bind parameter to new value
        paramVarRef.setValue(interpreter, value);
      }

      // Add to new values list
      newValues.add(value);
    }

    // If this function has a VarArg parameter, we've collapsed
    // multiple values into an array.
    return newValues.toArray(new Object[newValues.size()]);
  }

  @Override
  public Value execute(final AshRuntime interpreter) {
    // Dereference variables and pass Values to function
    Object[] values = new Object[this.variableReferences.size() + 1];
    values[0] = interpreter;

    int index = 1;
    for (VariableReference current : this.variableReferences) {
      values[index++] = current.getValue(interpreter);
    }

    return this.execute(interpreter, values);
  }

  public abstract Value execute(final AshRuntime interpreter, Object[] values);

  @Override
  public void print(final PrintStream stream, final int indent) {
    AshRuntime.indentLine(stream, indent);
    stream.println("<FUNC " + this.type + " " + this.getName() + ">");

    for (VariableReference current : this.variableReferences) {
      current.print(stream, indent + 1);
    }
  }

  public static class BadFunction extends UserDefinedFunction implements BadNode {
    public BadFunction(final String name) {
      super(name, new Type.BadType(null, null), new ArrayList<>(), null);
    }
  }
}
