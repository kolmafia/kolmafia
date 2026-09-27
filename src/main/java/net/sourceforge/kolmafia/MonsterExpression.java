package net.sourceforge.kolmafia;

public class MonsterExpression extends Expression {
  public MonsterExpression(String text, String name) {
    super(text, name);
  }

  public static MonsterExpression getInstance(String text, String name) {
    MonsterExpression expr = new MonsterExpression(text, name);
    String errors = expr.getExpressionErrors();
    if (errors != null) {
      KoLmafia.updateDisplay(errors);
    }
    return expr;
  }

  @Override
  protected String validBytecodes() {
    return super.validBytecodes() + "A";
  }

  @Override
  protected String function() {
    if (this.optional("MUS")) {
      return "\u0080";
    }
    if (this.optional("MYS")) {
      return "\u0081";
    }
    if (this.optional("MOX")) {
      return "\u0082";
    }
    if (this.optional("ML")) {
      return "\u0083";
    }
    if (this.optional("MCD")) {
      return "\u0084";
    }
    if (this.optional("HP")) {
      return "\u0085";
    }
    if (this.optional("BL")) {
      return "\u0086";
    }
    if (this.optional("KW")) {
      return "\u0087";
    }
    if (this.optional("KV")) {
      return "\u0088";
    }
    if (this.optional("KC")) {
      return "\u0089";
    }
    if (this.optional("STAT")) {
      return "\u0090";
    }
    if (this.optional("path(")) {
      return this.literal(this.until(")"), '\u0092');
    }
    if (this.optional("equipped(")) {
      return this.literal(this.until(")").toLowerCase(), 'g');
    }

    return null;
  }
}
