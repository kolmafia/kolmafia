package net.sourceforge.kolmafia;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Class for supplying overrides to be used by the {@link Expression#eval() Expression evaluator}.
 * The specified overridden values will be used instead of using the current character state.
 *
 * <p>To add a new override for &lt;foo&gt;:
 *
 * <ol>
 *   <li>Add an appropriate nullable private field to store the overridden value, defaulted to
 *       {@code null}.
 *   <li>Add {@code ExpressionOverrides.setFoo()} to set the override. The method should also accept
 *       {@code null} to clear the override.
 *   <li>Add {@code ExpressionOverrides.overrideFoo()} which returns an {@link Optional} containing
 *       the overridden value, or an empty {@code Optional} if the value is not overridden.
 *   <li>Update the Expression evaluator to check for the overridden value.
 * </ol>
 */
public class ExpressionOverrides {
  // Ensure that nothing mutates this.
  public static final ExpressionOverrides NONE = new ExpressionOverrides();

  // region Overrides for user preferences
  private Map<String, String> prefOverrides = null;

  public void setPrefOverride(String pref, String value) {
    if (prefOverrides == null) {
      prefOverrides = new HashMap<>();
    }

    if (value == null) {
      prefOverrides.remove(pref);
    } else {
      prefOverrides.put(pref, value);
    }
  }

  public Optional<String> overridePref(String pref) {
    if (prefOverrides != null && prefOverrides.containsKey(pref)) {
      return Optional.of(prefOverrides.get(pref));
    } else {
      return Optional.empty();
    }
  }

  // endregion

  // region Override for unarmed
  private Boolean unarmedOverride = null;

  public void setUnarmedOverride(Boolean unarmed) {
    unarmedOverride = unarmed;
  }

  public Optional<Boolean> overrideUnarmed() {
    return Optional.ofNullable(unarmedOverride);
  }
  // endregion
}
