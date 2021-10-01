package net.sourceforge.kolmafia;

public class RestoreExpression extends Expression {
  public RestoreExpression(String text, String name) {
    super(text, name);
  }

  public static RestoreExpression getInstance(String text, String name) {
    RestoreExpression expr = new RestoreExpression(text, name);
    String errors = expr.getExpressionErrors();
    if (errors != null) {
      KoLmafia.updateDisplay(errors);
    }
    return expr;
  }

  @Override
  protected String validBytecodes() {
    return super.validBytecodes() + "L";
  }

  @Override
  protected String function() {
    if (this.optional("class(")) {
      return this.literal(this.until(")").toLowerCase(), 'n');
    }
    if (this.optional("effect(")) {
      return this.literal(this.until(")").toLowerCase(), 'e');
    }
    if (this.optional("skill(")) {
      return this.literal(this.until(")").toLowerCase(), 'd');
    }
    if (this.optional("equipped(")) {
      return this.literal(this.until(")").toLowerCase(), 'g');
    }
    if (this.optional("path(")) {
      return this.literal(this.until(")"), '\u0092');
    }
    if (this.optional("HP")) {
      return "\u0085";
    }
    if (this.optional("MP")) {
      return "\u0091";
    }
    if (this.optional("CURHP")) {
      return "\u0095";
    }

    return null;
  }
}
