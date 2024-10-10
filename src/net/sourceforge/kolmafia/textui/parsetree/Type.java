package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.CoinmasterRegistry;
import net.sourceforge.kolmafia.EdServantData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.PastaThrallData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.VYKEACompanionData;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.Modifier;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.BountyDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.DataTypes.TypeSpec;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.eclipse.lsp4j.Location;

@SuppressWarnings("incomplete-switch")
public class Type extends Symbol {
  public boolean primitive;
  private final TypeSpec type;
  private PluralValue allValues = null;

  public Type(final String name, final TypeSpec type) {
    this(name, type, null);
  }

  public Type(final String name, final TypeSpec type, final Location location) {
    super(name, location);
    this.primitive = true;
    this.type = type;
  }

  public TypeSpec getType() {
    return this.type;
  }

  public Type getBaseType() {
    return this;
  }

  public boolean isPrimitive() {
    return this.primitive;
  }

  public boolean equals(final Type type) {
    return this.type == type.type;
  }

  public boolean equals(final TypeSpec type) {
    return this.type == type;
  }

  public Type simpleType() {
    return this;
  }

  public Type asProxy() {
    if (this.equals(DataTypes.CLASS_TYPE)) {
      return ProxyRecordValue.ClassProxy._type;
    }
    if (this.equals(DataTypes.ITEM_TYPE)) {
      return ProxyRecordValue.ItemProxy._type;
    }
    if (this.equals(DataTypes.FAMILIAR_TYPE)) {
      return ProxyRecordValue.FamiliarProxy._type;
    }
    if (this.equals(DataTypes.SKILL_TYPE)) {
      return ProxyRecordValue.SkillProxy._type;
    }
    if (this.equals(DataTypes.EFFECT_TYPE)) {
      return ProxyRecordValue.EffectProxy._type;
    }
    if (this.equals(DataTypes.LOCATION_TYPE)) {
      return ProxyRecordValue.LocationProxy._type;
    }
    if (this.equals(DataTypes.MONSTER_TYPE)) {
      return ProxyRecordValue.MonsterProxy._type;
    }
    if (this.equals(DataTypes.COINMASTER_TYPE)) {
      return ProxyRecordValue.CoinmasterProxy._type;
    }
    if (this.equals(DataTypes.BOUNTY_TYPE)) {
      return ProxyRecordValue.BountyProxy._type;
    }
    if (this.equals(DataTypes.THRALL_TYPE)) {
      return ProxyRecordValue.ThrallProxy._type;
    }
    if (this.equals(DataTypes.SERVANT_TYPE)) {
      return ProxyRecordValue.ServantProxy._type;
    }
    if (this.equals(DataTypes.VYKEA_TYPE)) {
      return ProxyRecordValue.VykeaProxy._type;
    }
    if (this.equals(DataTypes.PATH_TYPE)) {
      return ProxyRecordValue.PathProxy._type;
    }
    if (this.equals(DataTypes.ELEMENT_TYPE)) {
      return ProxyRecordValue.ElementProxy._type;
    }
    if (this.equals(DataTypes.PHYLUM_TYPE)) {
      return ProxyRecordValue.PhylumProxy._type;
    }
    if (this.equals(DataTypes.SLOT_TYPE)) {
      return ProxyRecordValue.SlotProxy._type;
    }
    if (this.equals(DataTypes.STAT_TYPE)) {
      return ProxyRecordValue.StatProxy._type;
    }
    if (this.equals(DataTypes.MODIFIER_TYPE)) {
      return ProxyRecordValue.ModifierProxy._type;
    }
    return this;
  }

  public boolean isStringLike() {
    return switch (this.getType()) {
      case STRING,
          BUFFER,
          LOCATION,
          STAT,
          MONSTER,
          ELEMENT,
          COINMASTER,
          PHYLUM,
          BOUNTY,
          MODIFIER -> true;
      default -> false;
    };
  }

  public boolean isIntLike() {
    return switch (this.getType()) {
      case BOOLEAN,
          INT,
          FLOAT,
          STRING,
          ITEM,
          SKILL,
          EFFECT,
          FAMILIAR,
          MONSTER,
          THRALL,
          SERVANT,
          LOCATION,
          SLOT,
          PATH,
          CLASS -> true;
      default -> false;
    };
  }

  public Value initialValue() {
    return switch (this.type) {
      case VOID -> DataTypes.VOID_VALUE;
      case BOOLEAN -> DataTypes.BOOLEAN_INIT;
      case INT -> DataTypes.INT_INIT;
      case FLOAT -> DataTypes.FLOAT_INIT;
      case STRING -> DataTypes.STRING_INIT;
      case BUFFER -> new Value(DataTypes.BUFFER_TYPE, "", new StringBuffer());
      case MATCHER -> new Value(DataTypes.MATCHER_TYPE, "", Pattern.compile("").matcher(""));
      case ITEM -> DataTypes.ITEM_INIT;
      case LOCATION -> DataTypes.LOCATION_INIT;
      case CLASS -> DataTypes.CLASS_INIT;
      case STAT -> DataTypes.STAT_INIT;
      case SKILL -> DataTypes.SKILL_INIT;
      case EFFECT -> DataTypes.EFFECT_INIT;
      case FAMILIAR -> DataTypes.FAMILIAR_INIT;
      case SLOT -> DataTypes.SLOT_INIT;
      case MONSTER -> DataTypes.MONSTER_INIT;
      case ELEMENT -> DataTypes.ELEMENT_INIT;
      case COINMASTER -> DataTypes.COINMASTER_INIT;
      case PHYLUM -> DataTypes.PHYLUM_INIT;
      case BOUNTY -> DataTypes.BOUNTY_INIT;
      case THRALL -> DataTypes.THRALL_INIT;
      case SERVANT -> DataTypes.SERVANT_INIT;
      case VYKEA -> DataTypes.VYKEA_INIT;
      case PATH -> DataTypes.PATH_INIT;
      case MODIFIER -> DataTypes.MODIFIER_INIT;
      default -> null;
    };
  }

  public Value parseValue(final String name, final boolean returnDefault) {
    return switch (this.type) {
      case BOOLEAN -> DataTypes.parseBooleanValue(name, returnDefault);
      case INT -> DataTypes.parseIntValue(name, returnDefault);
      case FLOAT -> DataTypes.parseFloatValue(name, returnDefault);
      case STRING -> DataTypes.parseStringValue(name);
      case ITEM -> DataTypes.parseItemValue(name, returnDefault);
      case LOCATION -> DataTypes.parseLocationValue(name, returnDefault);
      case CLASS -> DataTypes.parseClassValue(name, returnDefault);
      case STAT -> DataTypes.parseStatValue(name, returnDefault);
      case SKILL -> DataTypes.parseSkillValue(name, returnDefault);
      case EFFECT -> DataTypes.parseEffectValue(name, returnDefault);
      case FAMILIAR -> DataTypes.parseFamiliarValue(name, returnDefault);
      case SLOT -> DataTypes.parseSlotValue(name, returnDefault);
      case MONSTER -> DataTypes.parseMonsterValue(name, returnDefault);
      case ELEMENT -> DataTypes.parseElementValue(name, returnDefault);
      case COINMASTER -> DataTypes.parseCoinmasterValue(name, returnDefault);
      case PHYLUM -> DataTypes.parsePhylumValue(name, returnDefault);
      case BOUNTY -> DataTypes.parseBountyValue(name, returnDefault);
      case THRALL -> DataTypes.parseThrallValue(name, returnDefault);
      case SERVANT -> DataTypes.parseServantValue(name, returnDefault);
      case VYKEA -> DataTypes.parseVykeaValue(name, returnDefault);
      case PATH -> DataTypes.parsePathValue(name, returnDefault);
      case MODIFIER -> DataTypes.parseModifierValue(name, returnDefault);
      default -> null;
    };
  }

  public Value makeValue(final Integer idval, final boolean returnDefault) {
    int id = idval;
    return switch (this.type) {
      case BOOLEAN -> DataTypes.makeBooleanValue(id);
      case INT -> DataTypes.makeIntValue(id);
      case FLOAT -> DataTypes.makeFloatValue(id);
      case STRING -> new Value(String.valueOf(id));
      case ITEM -> DataTypes.makeItemValue(id, returnDefault);
      case SKILL -> DataTypes.makeSkillValue(id, returnDefault);
      case EFFECT -> DataTypes.makeEffectValue(id, returnDefault);
      case FAMILIAR -> DataTypes.makeFamiliarValue(id, returnDefault);
      case MONSTER -> DataTypes.makeMonsterValue(id, returnDefault);
      case THRALL -> DataTypes.makeThrallValue(id, returnDefault);
      case SERVANT -> DataTypes.makeServantValue(id, returnDefault);
      case LOCATION -> DataTypes.parseLocationValue(id, returnDefault);
      case SLOT -> DataTypes.makeSlotValue(id, returnDefault);
      case PATH -> DataTypes.makePathValue(id, returnDefault);
      case CLASS -> DataTypes.makeClassValue(id, returnDefault);
        // The following don't have an integer -> object mapping
      case STAT -> DataTypes.STAT_INIT;
      case ELEMENT -> DataTypes.ELEMENT_INIT;
      case COINMASTER -> DataTypes.COINMASTER_INIT;
      case PHYLUM -> DataTypes.PHYLUM_INIT;
      case BOUNTY -> DataTypes.BOUNTY_INIT;
      case VYKEA -> DataTypes.VYKEA_INIT;
      case MODIFIER -> DataTypes.MODIFIER_INIT;
      default -> null;
    };
  }

  public List<String> getAmbiguousNames(String s1, Value value, boolean quote) {
    switch (this.type) {
      case ITEM, EFFECT, MONSTER, SKILL -> {
        String s2 = value.toString();
        if (s1.equalsIgnoreCase(s2)) {
          return null;
        }

        if (s1.startsWith("[")) {
          int bracket = s1.indexOf("]");
          if (bracket > 0 && StringUtilities.isNumeric(s1.substring(1, bracket))) {
            return null;
          }
        }

        if (StringUtilities.isNumeric(s1)) {
          // A number will have been unambiguously
          // interpreted as an item or effect id
          return null;
        }

        ArrayList<String> names = new ArrayList<>();
        int currentId = (int) value.contentLong;
        String name =
            this.type == TypeSpec.ITEM
                ? ItemDatabase.getItemName(currentId)
                : this.type == TypeSpec.EFFECT
                    ? EffectDatabase.getEffectName(currentId)
                    : this.type == TypeSpec.MONSTER
                        ? MonsterDatabase.getMonsterName(currentId)
                        : this.type == TypeSpec.SKILL ? SkillDatabase.getSkillName(currentId) : "";
        int[] ids =
            this.type == TypeSpec.ITEM
                ? ItemDatabase.getItemIds(name, 1, false)
                : this.type == TypeSpec.EFFECT
                    ? EffectDatabase.getEffectIds(name, false)
                    : this.type == TypeSpec.MONSTER
                        ? MonsterDatabase.getMonsterIds(name, false)
                        : this.type == TypeSpec.SKILL
                            ? SkillDatabase.getSkillIds(name, false)
                            : null;

        for (int id : ids) {
          String s3 = quote ? ("\"[" + id + "]" + name + "\"") : ("[" + id + "]" + name);
          names.add(s3);
        }
        return names;
      }
    }
    return null;
  }

  public void validateValue(final ScriptRuntime controller, String s1, Value value) {
    List<String> names = this.getAmbiguousNames(s1, value, true);
    if (names != null && names.size() > 1) {
      String s2 = value.toString();
      Exception ex =
          controller.runtimeException2(
              "Multiple matches for \"" + s1 + "\"; using \"" + s2 + "\".",
              "Clarify by using one of:");
      RequestLogger.printLine(ex.getMessage());
      for (String str : names) {
        RequestLogger.printLine(str);
      }
    }
  }

  public Value coerceValue(final Object object, final boolean returnDefault) {
    if (object instanceof String) {
      return this.parseValue((String) object, returnDefault);
    }
    if (object instanceof Integer) {
      int integer = (Integer) object;
      return switch (this.type) {
        case BOOLEAN -> DataTypes.makeBooleanValue(integer);
        case CLASS -> DataTypes.makeClassValue(integer, returnDefault);
        case INT -> DataTypes.makeIntValue(integer);
        case FLOAT -> DataTypes.makeFloatValue(integer);
        case STRING -> new Value(DataTypes.STRING_TYPE, String.valueOf(integer));
        case ITEM -> DataTypes.makeItemValue(integer, returnDefault);
        case SKILL -> DataTypes.makeSkillValue(integer, returnDefault);
        case EFFECT -> DataTypes.makeEffectValue(integer, returnDefault);
        case FAMILIAR -> DataTypes.makeFamiliarValue(integer, returnDefault);
        case MONSTER -> DataTypes.makeMonsterValue(integer, returnDefault);
        case THRALL -> DataTypes.makeThrallValue(integer, returnDefault);
        case SERVANT -> DataTypes.makeServantValue(integer, returnDefault);
        default -> null;
      };
    }
    if (object instanceof MonsterData monster) {
      return switch (this.type) {
        case INT -> DataTypes.makeIntValue(monster.getId());
        case STRING -> new Value(DataTypes.STRING_TYPE, monster.getName());
        case MONSTER -> DataTypes.makeMonsterValue(monster);
        default -> null;
      };
    }
    if (object instanceof AscensionClass ascensionClass) {
      return switch (this.type) {
        case INT -> DataTypes.makeIntValue(ascensionClass.getId());
        case STRING -> new Value(DataTypes.STRING_TYPE, ascensionClass.getName());
        case CLASS -> DataTypes.makeClassValue(ascensionClass, returnDefault);
        case PATH -> DataTypes.makePathValue(ascensionClass.getPath());
        default -> null;
      };
    }
    if (object instanceof Path path) {
      return switch (this.type) {
        case INT -> DataTypes.makeIntValue(path.getId());
        case STRING -> new Value(DataTypes.STRING_TYPE, path.getName());
        case PATH -> DataTypes.makePathValue(path);
        default -> null;
      };
    }
    if (object instanceof Modifier modifier) {
      if (this.type == TypeSpec.MODIFIER) {
        return new Value(modifier);
      }
      return null;
    }
    return null;
  }

  public PluralValue allValues() {
    if (this.allValues != null) return this.allValues;

    List<Value> list = new ArrayList<>();
    switch (this.type) {
      case BOOLEAN -> this.addValues(list, DataTypes.BOOLEANS);
      case ITEM -> {
        int limit = ItemDatabase.maxItemId();
        for (int i = 1; i <= limit; ++i) {
          if (i != 13 && ItemDatabase.getItemDataName(i) != null) {
            list.add(DataTypes.makeItemValue(i, true));
          }
        }
      }
      case LOCATION -> this.addValues(list, AdventureDatabase.getAsLockableListModel());
      case CLASS -> this.addValues(list, AscensionClass.allClasses());
      case STAT -> this.addValues(list, DataTypes.STAT_ARRAY, 0, 3);
      case SKILL -> this.addValues(list, SkillDatabase.entrySet());
      case EFFECT -> this.addValues(list, EffectDatabase.entrySet());
      case FAMILIAR -> this.addValues(list, FamiliarDatabase.entrySet());
      case SLOT -> this.addValues(list, SlotSet.NAMES);
      case MONSTER -> this.addValues(list, MonsterDatabase.valueSet());
      case ELEMENT -> this.addValues(list, MonsterDatabase.ELEMENT_ARRAY, 1, -1);
      case COINMASTER -> this.addValues(list, CoinmasterRegistry.MASTERS);
      case PHYLUM -> this.addValues(list, MonsterDatabase.PHYLUM_ARRAY, 1, -1);
      case BOUNTY -> this.addValues(list, BountyDatabase.entrySet());
      case THRALL -> this.addValues(list, PastaThrallData.THRALL_ARRAY);
      case SERVANT -> this.addValues(list, EdServantData.SERVANT_ARRAY);
      case VYKEA -> this.addValues(list, VYKEACompanionData.VYKEA);
      case PATH -> this.addValues(list, Path.allPaths());
      case MODIFIER -> this.addValues(list, ModifierDatabase.allModifiers());
      default -> {
        return null;
      }
    }
    this.allValues = new PluralValue(this, list);
    return this.allValues;
  }

  private void addValues(List<Value> results, String[] values) {
    this.addValues(results, values, 0, -1);
  }

  private void addValues(List<Value> results, String[] values, int start, int stop) {
    if (stop == -1) stop = values.length;
    for (int i = start; i < stop; ++i) {
      Value v = this.parseValue(values[i], false);
      if (v != null) results.add(v);
    }
  }

  private void addValues(List<Value> results, Collection<?> values) {
    for (Object o : values) {
      if (o instanceof Map.Entry<?, ?> e) { // Some of the database entrySet() methods return
        // Integer:String mappings, others String:<something>.
        // We prefer the former, but can handle either
        o = e.getKey();
      }
      if (o instanceof KoLAdventure adv) { // KoLAdventure.toString() returns "zone: location",
        // which isn't parseable as an ASH location.
        o = adv.getAdventureName();
      }
      Value v = this.coerceValue(o, false);
      if (v != null) results.add(v);
    }
  }

  public Value initialValueExpression() {
    return new TypeInitializer(this);
  }

  public int dataValues() {
    return 1;
  }

  @Override
  public Value execute(final AshRuntime interpreter) {
    return null;
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    AshRuntime.indentLine(stream, indent);
    stream.println("<TYPE " + this.name + ">");
  }

  /**
   * Creates a copy of the current Type with {@code location} as its Location.
   *
   * @param location the location of the reference
   */
  public Type reference(final Location location) {
    return new TypeReference(this, location);
  }

  private class TypeReference extends Type {
    private TypeReference(final Type type, final Location location) {
      super(type.name, type.type, location);
    }

    @Override
    public Location getDefinitionLocation() {
      return Type.this.getDefinitionLocation();
    }
  }

  public static class BadType extends Type implements BadNode {
    public BadType(final String name, final Location location) {
      super(name, TypeSpec.ANY, location);
    }
  }
}
