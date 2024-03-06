package net.sourceforge.kolmafia.textui.parsetree;

public class FunctionValue extends Value {

  private final Function function;

  public FunctionValue(final Function function) {
    this(function.getFunctionType(), function);
  }

  public FunctionValue(final Type type, final Function function) {
    super(type);
    this.function = function;
  }

  public Function getFunction() {
    return this.function;
  }
}
