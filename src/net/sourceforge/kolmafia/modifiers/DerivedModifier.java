package net.sourceforge.kolmafia.modifiers;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public enum DerivedModifier implements Modifier {
  BUFFED_MUS("Buffed Muscle"),
  BUFFED_MYS("Buffed Mysticality"),
  BUFFED_MOX("Buffed Moxie"),
  BUFFED_HP("Buffed HP Maximum"),
  BUFFED_MP("Buffed MP Maximum");

  private final String name;

  DerivedModifier(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Pattern[] getDescPatterns() {
    return null;
  }

  @Override
  public Pattern getTagPattern() {
    return null;
  }

  @Override
  public String getTag() {
    return null;
  }

  public static final Set<DerivedModifier> DERIVED_MODIFIERS =
      Collections.unmodifiableSet(EnumSet.allOf(DerivedModifier.class));

  private static final Map<String, DerivedModifier> caselessNameToModifier =
      DERIVED_MODIFIERS.stream()
          .collect(Collectors.toMap(type -> type.name.toLowerCase(), Function.identity()));

  // equivalent to `Modifiers.findName`
  public static DerivedModifier byCaselessName(String name) {
    return caselessNameToModifier.get(name.toLowerCase());
  }
}
