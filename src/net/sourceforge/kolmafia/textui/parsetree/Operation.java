package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Parser;

public class Operation extends Expression {
  private final Operator oper;

  public Operation(final Evaluable lhs, final Evaluable rhs, final Operator oper) {
    super(
        rhs == null
            ? Parser.mergeLocations(oper.getLocation(), lhs.getLocation())
            : Parser.mergeLocations(lhs.getLocation(), rhs.getLocation()));
    this.lhs = lhs;
    this.rhs = rhs;
    this.oper = oper;
  }

  public Operation(final Evaluable lhs, final Operator oper) {
    this(lhs, null, oper);
  }

  Operator getOperator() {
    return this.oper;
  }

  @Override
  public Type getType() {
    Type leftType = this.lhs.getType();

    // Unary operators have no right hand side
    if (this.rhs == null) {
      return leftType;
    }

    Type rightType = this.rhs.getType();

    // String concatenation always yields a string
    if (this.oper.equals("+")
        && (leftType.equals(DataTypes.TYPE_STRING) || rightType.equals(DataTypes.TYPE_STRING))) {
      return DataTypes.STRING_TYPE;
    }

    // If it's an integer operator, must be integers
    if (this.oper.isInteger()) {
      return DataTypes.INT_TYPE;
    }

    // If it's a logical operator, must be both integers or both
    // booleans
    if (this.oper.isLogical()) {
      return leftType;
    }

    // If it's not arithmetic, it's boolean
    if (!this.oper.isArithmetic()) {
      return DataTypes.BOOLEAN_TYPE;
    }

    // Coerce int to float
    if (leftType.equals(DataTypes.TYPE_FLOAT)) {
      return DataTypes.FLOAT_TYPE;
    }

    // Otherwise result is whatever is on right
    return rightType;
  }

  @Override
  public Value execute(final AshRuntime interpreter) {
    return this.rhs == null
        ? this.oper.applyTo(interpreter, this.lhs)
        : this.oper.applyTo(interpreter, this.lhs, this.rhs);
  }

  @Override
  public String toString() {
    if (this.rhs == null) {
      return this.oper.toString() + " " + this.lhs.toQuotedString();
    }

    return "( "
        + this.lhs.toQuotedString()
        + " "
        + this.oper.toString()
        + " "
        + this.rhs.toQuotedString()
        + " )";
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    this.oper.print(stream, indent);
    super.print(stream, indent);
  }
}
