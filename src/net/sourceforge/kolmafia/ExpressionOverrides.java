package net.sourceforge.kolmafia;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/*
 * Class for supplying overrides to be used by the Expression evaluator. If an instance of this
 * class is provided to Expression.eval(), the specified overridden values will be used instead of
 * using the current character state.
 *
 * To add a new override for <foo>:
 * 1. Add an appropriate nullable private field to store the overridden value, defaulted to null.
 * 2. Add ExpressionOverrides.setFoo() to set the override. The method should also accept null to
 *    clear the override.
 * 3. Add ExpressionOverrides.foo() which returns an Optional containing the overridden value, or an
 *    empty Optional if the value is not overridden.
 * 4. Update the Expression evaluator to check for the overridden value.
 */
public class ExpressionOverrides {
  // Ensure that nothing mutates this.
  public static final ExpressionOverrides NONE = new ExpressionOverrides();

  // region Overrides for user preferences
  private Map<String, String> pref = null;

  public void setPref(String pref, String value) {
    if (this.pref == null) {
      this.pref = new HashMap<>();
    }

    if (value == null) {
      this.pref.remove(pref);
    } else {
      this.pref.put(pref, value);
    }
  }

  public Optional<String> pref(String pref) {
    if (this.pref != null && this.pref.containsKey(pref)) {
      return Optional.of(this.pref.get(pref));
    } else {
      return Optional.empty();
    }
  }

  // endregion

  // region Override for unarmed
  private Boolean unarmed = null;

  public void setUnarmed(Boolean unarmed) {
    this.unarmed = unarmed;
  }

  public Optional<Boolean> unarmed() {
    return Optional.ofNullable(unarmed);
  }
  // endregion
}
