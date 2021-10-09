package net.sourceforge.kolmafia;

import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;

public class ModifierExpression extends Expression {
  public ModifierExpression(String text, String name) {
    super(text, name);
  }

  public static ModifierExpression getInstance(String text, String name) {
    ModifierExpression expr = new ModifierExpression(text, name);
    String errors = expr.getExpressionErrors();
    if (errors != null) {
      KoLmafia.updateDisplay(errors);
    }
    return expr;
  }

  @Override
  protected void initialize() {
    // The first check also matches "[zone(The Slime Tube)]"
    // Hence the second check.
    String type = Modifiers.getTypeFromLookup(this.name);
    if (type.equals("Effect") && text.contains("T")) {
      int effectId = EffectDatabase.getEffectId(Modifiers.getNameFromLookup(this.name));
      if (effectId != -1) {
        this.effect = EffectPool.get(effectId, 0);
      }
    }
  }

  @Override
  protected String validBytecodes() {
    return super.validBytecodes() + "ABCDEFGHIJKLMNPRSTUWXY";
  }

  @Override
  protected String function() {
    if (this.optional("loc(")) {
      return this.literal(this.until(")"), 'l');
    }
    if (this.optional("zone(")) {
      return this.literal(this.until(")"), 'z');
    }
    if (this.optional("event(")) {
      return this.literal(this.until(")"), 'v');
    }
    if (this.optional("fam(")) {
      return this.literal(this.until(")"), 'w');
    }
    if (this.optional("famattr(")) {
      return this.literal(this.until(")"), 'i');
    }
    if (this.optional("mainhand(")) {
      return this.literal(this.until(")"), 'h');
    }
    if (this.optional("equipped(")) {
      return this.literal(this.until(")").toLowerCase(), 'g');
    }
    if (this.optional("effect(")) {
      return this.literal(this.until(")"), 'e');
    }
    if (this.optional("res(")) {
      return this.literal(this.until(")"), 'b');
    }
    if (this.optional("class(")) {
      return this.literal(this.until(")"), 'n');
    }
    if (this.optional("skill(")) {
      return this.literal(this.until(")"), 'd');
    }
    if (this.optional("env(")) {
      return this.literal(this.until(")"), 'j');
    }
    if (this.optional("path(")) {
      return this.literal(this.until(")"), '\u0092');
    }
    if (this.optional("mod(")) {
      return this.literal(this.until(")"), '\u0093');
    }
    if (this.optional("interact(")) {
      return this.literal(this.until(")"), '\u0094');
    }
    if (this.optional("mus")) {
      return "\u0080";
    }
    if (this.optional("mys")) {
      return "\u0081";
    }
    if (this.optional("mox")) {
      return "\u0082";
    }

    return null;
  }
}
