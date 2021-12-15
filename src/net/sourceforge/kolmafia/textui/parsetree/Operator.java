package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import net.sourceforge.kolmafia.VYKEACompanionData;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import org.eclipse.lsp4j.Location;

public class Operator extends Command {
  final String operator;

  // For runtime error messages
  private final String fileName;
  private final int lineNumber;

  public Operator(final Location location, final String operator, final Parser parser) {
    super(location);
    this.operator = operator;
    this.fileName = parser.getShortFileName();
    this.lineNumber = parser.getLineNumber();
  }

  public boolean equals(final String op) {
    return this.operator.equals(op);
  }

  public boolean precedes(final Operator oper) {
    return this.operStrength() > oper.operStrength();
  }

  private int operStrength() {
    if (this.operator.equals(Parser.POST_INCREMENT)
        || this.operator.equals(Parser.POST_DECREMENT)) {
      return 14;
    }

    if (this.operator.equals("!")
        || this.operator.equals("~")
        || this.operator.equals("contains")
        || this.operator.equals("remove")
        || this.operator.equals(Parser.PRE_INCREMENT)
        || this.operator.equals(Parser.PRE_DECREMENT)) {
      return 13;
    }

    if (this.operator.equals("**")) {
      return 12;
    }

    if (this.operator.equals("*") || this.operator.equals("/") || this.operator.equals("%")) {
      return 11;
    }

    if (this.operator.equals("+") || this.operator.equals("-")) {
      return 10;
    }

    if (this.operator.equals("<<") || this.operator.equals(">>") || this.operator.equals(">>>")) {
      return 9;
    }

    if (this.operator.equals("<")
        || this.operator.equals(">")
        || this.operator.equals("<=")
        || this.operator.equals(">=")) {
      return 8;
    }

    if (this.operator.equals("==")
        || this.operator.equals(Parser.APPROX)
        || this.operator.equals("!=")) {
      return 7;
    }

    if (this.operator.equals("&")) {
      return 6;
    }

    if (this.operator.equals("^")) {
      return 5;
    }

    if (this.operator.equals("|")) {
      return 4;
    }

    if (this.operator.equals("&&")) {
      return 3;
    }

    if (this.operator.equals("||")) {
      return 2;
    }

    if (this.operator.equals("?") || this.operator.equals(":")) {
      return 1;
    }

    return -1;
  }

  public static boolean isStringLike(Type type) {
    return type.equals(DataTypes.TYPE_STRING)
        || type.equals(DataTypes.TYPE_BUFFER)
        || type.equals(DataTypes.TYPE_LOCATION)
        || type.equals(DataTypes.TYPE_STAT)
        || type.equals(DataTypes.TYPE_MONSTER)
        || type.equals(DataTypes.TYPE_ELEMENT)
        || type.equals(DataTypes.TYPE_COINMASTER)
        || type.equals(DataTypes.TYPE_PHYLUM)
        || type.equals(DataTypes.TYPE_BOUNTY);
  }

  public boolean isArithmetic() {
    return this.operator.equals("+")
        || this.operator.equals("-")
        || this.operator.equals("*")
        || this.operator.equals("/")
        || this.operator.equals("%")
        || this.operator.equals("**")
        || this.operator.equals(Parser.PRE_INCREMENT)
        || this.operator.equals(Parser.PRE_DECREMENT)
        || this.operator.equals(Parser.POST_INCREMENT)
        || this.operator.equals(Parser.POST_DECREMENT);
  }

  public boolean isBoolean() {
    return this.operator.equals("&&") || this.operator.equals("||");
  }

  public boolean isLogical() {
    return this.operator.equals("&")
        || this.operator.equals("|")
        || this.operator.equals("^")
        || this.operator.equals("~")
        || this.operator.equals("&=")
        || this.operator.equals("^=")
        || this.operator.equals("|=");
  }

  public boolean isInteger() {
    return this.operator.equals("<<")
        || this.operator.equals(">>")
        || this.operator.equals(">>>")
        || this.operator.equals("<<=")
        || this.operator.equals(">>=")
        || this.operator.equals(">>>=");
  }

  public boolean isComparison() {
    return this.operator.equals("==")
        || this.operator.equals(Parser.APPROX)
        || this.operator.equals("!=")
        || this.operator.equals("<")
        || this.operator.equals(">")
        || this.operator.equals("<=")
        || this.operator.equals(">=");
  }

  @Override
  public String toString() {
    return this.operator;
  }

  public boolean validCoercion(Type lhs, Type rhs) {
    int ltype = lhs.getBaseType().getType();
    int rtype = rhs.getBaseType().getType();

    if (lhs.isBad() || rhs.isBad()) {
      // BadNode's are only generated through errors, which
      // means one was already generated about this type.
      return true;
    }

    if (this.isInteger()) {
      return (ltype == DataTypes.TYPE_INT && rtype == DataTypes.TYPE_INT);
    }
    if (this.isBoolean()) {
      return ltype == rtype && (ltype == DataTypes.TYPE_BOOLEAN);
    }
    if (this.isLogical()) {
      return ltype == rtype && (ltype == DataTypes.TYPE_INT || ltype == DataTypes.TYPE_BOOLEAN);
    }
    return Operator.validCoercion(lhs, rhs, this.toString());
  }

  public static boolean validCoercion(Type lhs, Type rhs, final String oper) {
    // Resolve aliases
    lhs = lhs.getBaseType();
    rhs = rhs.getBaseType();

    if (lhs.isBad() || rhs.isBad()) {
      // BadNode's are only generated through errors, which
      // means one was already generated about this type.
      return true;
    }

    if (oper == null) {
      return lhs.getType() == rhs.getType();
    }

    // "oper" is either a standard operator or is a special name:
    //
    // "parameter" - value used as a function parameter
    //	lhs = parameter type, rhs = expression type
    //
    // "return" - value returned as function value
    //	lhs = function return type, rhs = expression type
    //
    // "assign" - value
    //	lhs = variable type, rhs = expression type

    // The "contains" operator requires an aggregate on the left
    // and the correct index type on the right.

    if (oper.equals("contains")) {
      return lhs.getType() == DataTypes.TYPE_AGGREGATE
          && ((AggregateType) lhs).getIndexType().getBaseType().equals(rhs);
    }

    // If the types are equal, no coercion is necessary
    if (lhs.equals(rhs)) {
      return true;
    }

    if (lhs.equals(DataTypes.ANY_TYPE)) {
      return true;
    }

    // Noncoercible strings only accept strings
    if (lhs.equals(DataTypes.STRICT_STRING_TYPE)) {
      return rhs.equals(DataTypes.TYPE_STRING) || rhs.equals(DataTypes.TYPE_BUFFER);
    }

    // Anything coerces to a string
    if (lhs.equals(DataTypes.TYPE_STRING)) {
      return true;
    }

    // Anything coerces to a string for concatenation
    if (oper.equals("+") && rhs.equals(DataTypes.TYPE_STRING)) {
      return true;
    }

    // Int coerces to float
    if (lhs.equals(DataTypes.TYPE_INT) && rhs.equals(DataTypes.TYPE_FLOAT)) {
      return true;
    }

    if (lhs.equals(DataTypes.TYPE_FLOAT) && rhs.equals(DataTypes.TYPE_INT)) {
      return true;
    }

    return false;
  }

  private Value compareValues(final AshRuntime interpreter, Value leftValue, Value rightValue) {
    Type ltype = leftValue.getType();
    Type rtype = rightValue.getType();
    boolean bool;

    // If either side is non-numeric, perform string comparison
    if (Operator.isStringLike(ltype) || Operator.isStringLike(rtype)) {
      String lstring = leftValue.toString();
      String rstring = rightValue.toString();
      int c =
          this.operator.equals(Parser.APPROX)
              ? lstring.compareToIgnoreCase(rstring)
              : lstring.compareTo(rstring);
      bool =
          (this.operator.equals("==") || this.operator.equals(Parser.APPROX))
              ? c == 0
              : this.operator.equals("!=")
                  ? c != 0
                  : this.operator.equals(">=")
                      ? c >= 0
                      : this.operator.equals("<=")
                          ? c <= 0
                          : this.operator.equals(">")
                              ? c > 0
                              : this.operator.equals("<") ? c < 0 : false;
    }

    // If either value is a float, coerce to float and compare.

    else if (ltype.equals(DataTypes.TYPE_FLOAT) || rtype.equals(DataTypes.TYPE_FLOAT)) {
      double lfloat = leftValue.toFloatValue().floatValue();
      double rfloat = rightValue.toFloatValue().floatValue();
      bool =
          (this.operator.equals("==") || this.operator.equals(Parser.APPROX))
              ? lfloat == rfloat
              : this.operator.equals("!=")
                  ? lfloat != rfloat
                  : this.operator.equals(">=")
                      ? lfloat >= rfloat
                      : this.operator.equals("<=")
                          ? lfloat <= rfloat
                          : this.operator.equals(">")
                              ? lfloat > rfloat
                              : this.operator.equals("<") ? lfloat < rfloat : false;
    }

    // VYKEA companions have a "name" component which should not be compared
    else if (ltype.equals(DataTypes.TYPE_VYKEA) || rtype.equals(DataTypes.TYPE_VYKEA)) {
      VYKEACompanionData v1 = (VYKEACompanionData) (leftValue.content);
      VYKEACompanionData v2 = (VYKEACompanionData) (rightValue.content);
      int c = v1.compareTo(v2);
      bool =
          (this.operator.equals("==") || this.operator.equals(Parser.APPROX))
              ? c == 0
              : this.operator.equals("!=")
                  ? c != 0
                  : this.operator.equals(">=")
                      ? c >= 0
                      : this.operator.equals("<=")
                          ? c <= 0
                          : this.operator.equals(">")
                              ? c > 0
                              : this.operator.equals("<") ? c < 0 : false;
    }

    // Otherwise, compare integers
    else {
      long lint = leftValue.intValue();
      long rint = rightValue.intValue();
      bool =
          (this.operator.equals("==") || this.operator.equals(Parser.APPROX))
              ? lint == rint
              : this.operator.equals("!=")
                  ? lint != rint
                  : this.operator.equals(">=")
                      ? lint >= rint
                      : this.operator.equals("<=")
                          ? lint <= rint
                          : this.operator.equals(">")
                              ? lint > rint
                              : this.operator.equals("<") ? lint < rint : false;
    }

    Value result = bool ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
    if (ScriptRuntime.isTracing()) {
      interpreter.trace("<- " + result);
    }
    interpreter.traceUnindent();
    return result;
  }

  private Value performArithmetic(final AshRuntime interpreter, Value leftValue, Value rightValue) {
    Type ltype = leftValue.getType();
    Type rtype = rightValue.getType();
    Value result;

    // If either side is non-numeric, perform string operations
    if (Operator.isStringLike(ltype) || Operator.isStringLike(rtype)) {
      // Since we only do string concatenation, we should
      // only get here if the operator is "+".
      if (!this.operator.equals("+")) {
        throw interpreter.runtimeException(
            "Operator '" + this.operator + "' applied to string operands",
            this.fileName,
            this.lineNumber);
      }

      String string = leftValue.toStringValue().toString() + rightValue.toStringValue().toString();
      result = new Value(string);
    }

    // If either value is a float, coerce to float

    else if (ltype.equals(DataTypes.TYPE_FLOAT) || rtype.equals(DataTypes.TYPE_FLOAT)) {
      double rfloat = rightValue.toFloatValue().floatValue();
      if ((this.operator.equals("/") || this.operator.equals("%")) && rfloat == 0.0) {
        throw interpreter.runtimeException("Division by zero", this.fileName, this.lineNumber);
      }

      double lfloat = leftValue.toFloatValue().floatValue();

      double val;

      if (this.operator.equals("**")) {
        val = Math.pow(lfloat, rfloat);
        if (Double.isNaN(val) || Double.isInfinite(val)) {
          throw interpreter.runtimeException(
              "Invalid exponentiation: cannot take " + lfloat + " ** " + rfloat,
              this.fileName,
              this.lineNumber);
        }
      } else {
        val =
            this.operator.equals("+")
                ? lfloat + rfloat
                : this.operator.equals("-")
                    ? lfloat - rfloat
                    : this.operator.equals("*")
                        ? lfloat * rfloat
                        : this.operator.equals("/")
                            ? lfloat / rfloat
                            : this.operator.equals("%") ? lfloat % rfloat : 0.0;
      }

      result = DataTypes.makeFloatValue(val);
    }

    // If this is a logical operator, return an int or boolean
    else if (this.isLogical()) {
      long lint = leftValue.intValue();
      long rint = rightValue.intValue();
      long val =
          this.operator.equals("&")
              ? lint & rint
              : this.operator.equals("^")
                  ? lint ^ rint
                  : this.operator.equals("|") ? lint | rint : 0;
      result =
          ltype.equals(DataTypes.TYPE_BOOLEAN)
              ? DataTypes.makeBooleanValue(val != 0)
              : DataTypes.makeIntValue(val);
    }

    // Otherwise, perform arithmetic on integers

    else {
      long rint = rightValue.intValue();
      if ((this.operator.equals("/") || this.operator.equals("%")) && rint == 0) {
        throw interpreter.runtimeException("Division by zero", this.fileName, this.lineNumber);
      }

      long lint = leftValue.intValue();
      long val =
          this.operator.equals("+")
              ? lint + rint
              : this.operator.equals("-")
                  ? lint - rint
                  : this.operator.equals("*")
                      ? lint * rint
                      : this.operator.equals("/")
                          ? lint / rint
                          : this.operator.equals("%")
                              ? lint % rint
                              : this.operator.equals("**")
                                  ? (long) Math.pow(lint, rint)
                                  : this.operator.equals("<<")
                                      ? lint << rint
                                      : this.operator.equals(">>")
                                          ? lint >> rint
                                          : this.operator.equals(">>>") ? lint >>> rint : 0;
      result = DataTypes.makeIntValue(val);
    }

    if (ScriptRuntime.isTracing()) {
      interpreter.trace("<- " + result);
    }
    interpreter.traceUnindent();
    return result;
  }

  public Value applyTo(final AshRuntime interpreter, final TypedNode lhs) {
    interpreter.traceIndent();
    if (ScriptRuntime.isTracing()) {
      interpreter.trace("Operator: " + this.operator);
    }

    // Unary operator with special evaluation of argument
    if (this.operator.equals("remove")) {
      CompositeReference operand = (CompositeReference) lhs;
      if (ScriptRuntime.isTracing()) {
        interpreter.traceIndent();
        interpreter.trace("Operand: " + operand);
        interpreter.traceUnindent();
      }
      Value result = operand.removeKey(interpreter);
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("<- " + result);
      }
      interpreter.traceUnindent();
      return result;
    }

    interpreter.traceIndent();
    if (ScriptRuntime.isTracing()) {
      interpreter.trace("Operand: " + lhs);
    }

    Value leftValue = lhs.execute(interpreter);
    interpreter.captureValue(leftValue);
    if (leftValue == null) {
      leftValue = DataTypes.VOID_VALUE;
    }

    if (ScriptRuntime.isTracing()) {
      interpreter.trace("[" + interpreter.getState() + "] <- " + leftValue.toQuotedString());
    }
    interpreter.traceUnindent();

    if (interpreter.getState().equals(ScriptRuntime.State.EXIT)) {
      interpreter.traceUnindent();
      return null;
    }

    Value result;

    // Unary Operators
    if (this.operator.equals("!")) {
      result = DataTypes.makeBooleanValue(leftValue.intValue() == 0);
    } else if (this.operator.equals("~")) {
      long val = leftValue.intValue();
      result =
          leftValue.getType().equals(DataTypes.TYPE_BOOLEAN)
              ? DataTypes.makeBooleanValue(val == 0)
              : DataTypes.makeIntValue(~val);
    } else if (this.operator.equals("-")) {
      if (lhs.getType().equals(DataTypes.TYPE_INT)) {
        result = DataTypes.makeIntValue(0 - leftValue.intValue());
      } else if (lhs.getType().equals(DataTypes.TYPE_FLOAT)) {
        result = DataTypes.makeFloatValue(0.0 - leftValue.floatValue());
      } else {
        throw interpreter.runtimeException(
            "Internal error: Unary minus can only be applied to numbers",
            this.fileName,
            this.lineNumber);
      }
    } else if (this.operator.equals(Parser.PRE_INCREMENT)
        || this.operator.equals(Parser.POST_INCREMENT)) {
      if (lhs.getType().equals(DataTypes.TYPE_INT)) {
        result = DataTypes.makeIntValue(leftValue.intValue() + 1);
      } else if (lhs.getType().equals(DataTypes.TYPE_FLOAT)) {
        result = DataTypes.makeFloatValue(leftValue.floatValue() + 1.0);
      } else {
        throw interpreter.runtimeException(
            "Internal error: pre/post increment can only be applied to numbers",
            this.fileName,
            this.lineNumber);
      }
    } else if (this.operator.equals(Parser.PRE_DECREMENT)
        || this.operator.equals(Parser.POST_DECREMENT)) {
      if (lhs.getType().equals(DataTypes.TYPE_INT)) {
        result = DataTypes.makeIntValue(leftValue.intValue() - 1);
      } else if (lhs.getType().equals(DataTypes.TYPE_FLOAT)) {
        result = DataTypes.makeFloatValue(leftValue.floatValue() - 1.0);
      } else {
        throw interpreter.runtimeException(
            "Internal error: pre/post increment can only be applied to numbers",
            this.fileName,
            this.lineNumber);
      }
    } else {
      throw interpreter.runtimeException(
          "Internal error: unknown unary operator \"" + this.operator + "\"",
          this.fileName,
          this.lineNumber);
    }

    if (ScriptRuntime.isTracing()) {
      interpreter.trace("<- " + result);
    }

    interpreter.traceUnindent();
    return result;
  }

  public Value applyTo(final AshRuntime interpreter, final TypedNode lhs, final TypedNode rhs) {
    interpreter.traceIndent();
    if (ScriptRuntime.isTracing()) {
      interpreter.trace("Operator: " + this.operator);
    }

    interpreter.traceIndent();
    if (ScriptRuntime.isTracing()) {
      interpreter.trace("Operand 1: " + lhs);
    }

    Value leftValue = lhs.execute(interpreter);
    interpreter.captureValue(leftValue);
    if (leftValue == null) {
      leftValue = DataTypes.VOID_VALUE;
    }
    if (ScriptRuntime.isTracing()) {
      interpreter.trace("[" + interpreter.getState() + "] <- " + leftValue.toQuotedString());
    }
    interpreter.traceUnindent();

    if (interpreter.getState().equals(ScriptRuntime.State.EXIT)) {
      interpreter.traceUnindent();
      return null;
    }

    // Unknown operator
    if (rhs == null) {
      throw interpreter.runtimeException(
          "Internal error: missing right operand.", this.fileName, this.lineNumber);
    }

    // Binary operators with optional right values
    if (this.operator.equals("||")) {
      if (leftValue.intValue() == 1) {
        if (ScriptRuntime.isTracing()) {
          interpreter.trace("<- " + DataTypes.TRUE_VALUE);
        }
        interpreter.traceUnindent();
        return DataTypes.TRUE_VALUE;
      }
      interpreter.traceIndent();
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("Operand 2: " + rhs);
      }
      Value rightValue = rhs.execute(interpreter);
      interpreter.captureValue(rightValue);
      if (rightValue == null) {
        rightValue = DataTypes.VOID_VALUE;
      }
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("[" + interpreter.getState() + "] <- " + rightValue.toQuotedString());
      }
      interpreter.traceUnindent();
      if (interpreter.getState().equals(ScriptRuntime.State.EXIT)) {
        interpreter.traceUnindent();
        return null;
      }
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("<- " + rightValue);
      }
      interpreter.traceUnindent();
      return rightValue;
    }

    if (this.operator.equals("&&")) {
      if (leftValue.intValue() == 0) {
        interpreter.traceUnindent();
        if (ScriptRuntime.isTracing()) {
          interpreter.trace("<- " + DataTypes.FALSE_VALUE);
        }
        return DataTypes.FALSE_VALUE;
      }
      interpreter.traceIndent();
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("Operand 2: " + rhs);
      }
      Value rightValue = rhs.execute(interpreter);
      interpreter.captureValue(rightValue);
      if (rightValue == null) {
        rightValue = DataTypes.VOID_VALUE;
      }
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("[" + interpreter.getState() + "] <- " + rightValue.toQuotedString());
      }
      interpreter.traceUnindent();
      if (interpreter.getState().equals(ScriptRuntime.State.EXIT)) {
        interpreter.traceUnindent();
        return null;
      }
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("<- " + rightValue);
      }
      interpreter.traceUnindent();
      return rightValue;
    }

    // Ensure type compatibility of operands
    if (!this.validCoercion(lhs.getType(), rhs.getType())) {
      throw interpreter.runtimeException(
          "Internal error: left hand side and right hand side do not correspond",
          this.fileName,
          this.lineNumber);
    }

    // Special binary operator: <aggref> contains <any>
    if (this.operator.equals("contains")) {
      interpreter.traceIndent();
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("Operand 2: " + rhs);
      }
      Value rightValue = rhs.execute(interpreter);
      interpreter.captureValue(rightValue);
      if (rightValue == null) {
        rightValue = DataTypes.VOID_VALUE;
      }
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("[" + interpreter.getState() + "] <- " + rightValue.toQuotedString());
      }
      interpreter.traceUnindent();
      if (interpreter.getState().equals(ScriptRuntime.State.EXIT)) {
        interpreter.traceUnindent();
        return null;
      }
      Value result = DataTypes.makeBooleanValue(leftValue.contains(rightValue));
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("<- " + result);
      }
      interpreter.traceUnindent();
      return result;
    }

    // Binary operators
    interpreter.traceIndent();
    if (ScriptRuntime.isTracing()) {
      interpreter.trace("Operand 2: " + rhs);
    }
    Value rightValue = rhs.execute(interpreter);
    interpreter.captureValue(rightValue);
    if (rightValue == null) {
      rightValue = DataTypes.VOID_VALUE;
    }
    if (ScriptRuntime.isTracing()) {
      interpreter.trace("[" + interpreter.getState() + "] <- " + rightValue.toQuotedString());
    }
    interpreter.traceUnindent();
    if (interpreter.getState().equals(ScriptRuntime.State.EXIT)) {
      interpreter.traceUnindent();
      return null;
    }

    // Comparison operators
    if (this.isComparison()) {
      return this.compareValues(interpreter, leftValue, rightValue);
    }

    // Arithmetic operators
    if (this.isArithmetic() || this.isLogical() || this.isInteger()) {
      return this.performArithmetic(interpreter, leftValue, rightValue);
    }

    // Unknown operator
    throw interpreter.runtimeException(
        "Internal error: unknown binary operator \"" + this.operator + "\"",
        this.fileName,
        this.lineNumber);
  }

  @Override
  public Value execute(final AshRuntime interpreter) {
    return null;
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    AshRuntime.indentLine(stream, indent);
    stream.println("<OPER " + this.operator + ">");
  }
}
