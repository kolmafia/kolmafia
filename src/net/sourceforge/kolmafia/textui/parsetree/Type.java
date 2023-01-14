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
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.BountyDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.DataTypes.TypeSpec;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.eclipse.lsp4j.Location;

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
    return this;
  }

  public boolean isStringLike() {
    return switch (this.getType()) {
      case TYPE_STRING,
          TYPE_BUFFER,
          TYPE_LOCATION,
          TYPE_STAT,
          TYPE_MONSTER,
          TYPE_ELEMENT,
          TYPE_COINMASTER,
          TYPE_PHYLUM,
          TYPE_BOUNTY -> true;
      default -> false;
    };
  }

  public Value initialValue() {
    return switch (this.type) {
      case TYPE_VOID -> DataTypes.VOID_VALUE;
      case TYPE_BOOLEAN -> DataTypes.BOOLEAN_INIT;
      case TYPE_INT -> DataTypes.INT_INIT;
      case TYPE_FLOAT -> DataTypes.FLOAT_INIT;
      case TYPE_STRING -> DataTypes.STRING_INIT;
      case TYPE_BUFFER -> new Value(DataTypes.BUFFER_TYPE, "", new StringBuffer());
      case TYPE_MATCHER -> new Value(DataTypes.MATCHER_TYPE, "", Pattern.compile("").matcher(""));
      case TYPE_ITEM -> DataTypes.ITEM_INIT;
      case TYPE_LOCATION -> DataTypes.LOCATION_INIT;
      case TYPE_CLASS -> DataTypes.CLASS_INIT;
      case TYPE_STAT -> DataTypes.STAT_INIT;
      case TYPE_SKILL -> DataTypes.SKILL_INIT;
      case TYPE_EFFECT -> DataTypes.EFFECT_INIT;
      case TYPE_FAMILIAR -> DataTypes.FAMILIAR_INIT;
      case TYPE_SLOT -> DataTypes.SLOT_INIT;
      case TYPE_MONSTER -> DataTypes.MONSTER_INIT;
      case TYPE_ELEMENT -> DataTypes.ELEMENT_INIT;
      case TYPE_COINMASTER -> DataTypes.COINMASTER_INIT;
      case TYPE_PHYLUM -> DataTypes.PHYLUM_INIT;
      case TYPE_BOUNTY -> DataTypes.BOUNTY_INIT;
      case TYPE_THRALL -> DataTypes.THRALL_INIT;
      case TYPE_SERVANT -> DataTypes.SERVANT_INIT;
      case TYPE_VYKEA -> DataTypes.VYKEA_INIT;
      case TYPE_PATH -> DataTypes.PATH_INIT;
      default -> null;
    };
  }

  public Value parseValue(final String name, final boolean returnDefault) {
    return switch (this.type) {
      case TYPE_BOOLEAN -> DataTypes.parseBooleanValue(name, returnDefault);
      case TYPE_INT -> DataTypes.parseIntValue(name, returnDefault);
      case TYPE_FLOAT -> DataTypes.parseFloatValue(name, returnDefault);
      case TYPE_STRING -> DataTypes.parseStringValue(name);
      case TYPE_ITEM -> DataTypes.parseItemValue(name, returnDefault);
      case TYPE_LOCATION -> DataTypes.parseLocationValue(name, returnDefault);
      case TYPE_CLASS -> DataTypes.parseClassValue(name, returnDefault);
      case TYPE_STAT -> DataTypes.parseStatValue(name, returnDefault);
      case TYPE_SKILL -> DataTypes.parseSkillValue(name, returnDefault);
      case TYPE_EFFECT -> DataTypes.parseEffectValue(name, returnDefault);
      case TYPE_FAMILIAR -> DataTypes.parseFamiliarValue(name, returnDefault);
      case TYPE_SLOT -> DataTypes.parseSlotValue(name, returnDefault);
      case TYPE_MONSTER -> DataTypes.parseMonsterValue(name, returnDefault);
      case TYPE_ELEMENT -> DataTypes.parseElementValue(name, returnDefault);
      case TYPE_COINMASTER -> DataTypes.parseCoinmasterValue(name, returnDefault);
      case TYPE_PHYLUM -> DataTypes.parsePhylumValue(name, returnDefault);
      case TYPE_BOUNTY -> DataTypes.parseBountyValue(name, returnDefault);
      case TYPE_THRALL -> DataTypes.parseThrallValue(name, returnDefault);
      case TYPE_SERVANT -> DataTypes.parseServantValue(name, returnDefault);
      case TYPE_VYKEA -> DataTypes.parseVykeaValue(name, returnDefault);
      case TYPE_PATH -> DataTypes.parsePathValue(name, returnDefault);
      default -> null;
    };
  }

  public Value makeValue(final Integer idval, final boolean returnDefault) {
    int id = idval;
    return switch (this.type) {
      case TYPE_BOOLEAN -> DataTypes.makeBooleanValue(id);
      case TYPE_INT -> DataTypes.makeIntValue(id);
      case TYPE_FLOAT -> DataTypes.makeFloatValue(id);
      case TYPE_STRING -> new Value(String.valueOf(id));
      case TYPE_ITEM -> DataTypes.makeItemValue(id, returnDefault);
      case TYPE_SKILL -> DataTypes.makeSkillValue(id, returnDefault);
      case TYPE_EFFECT -> DataTypes.makeEffectValue(id, returnDefault);
      case TYPE_FAMILIAR -> DataTypes.makeFamiliarValue(id, returnDefault);
      case TYPE_MONSTER -> DataTypes.makeMonsterValue(id, returnDefault);
      case TYPE_THRALL -> DataTypes.makeThrallValue(id, returnDefault);
      case TYPE_SERVANT -> DataTypes.makeServantValue(id, returnDefault);
      case TYPE_LOCATION -> DataTypes.parseLocationValue(id, returnDefault);
      case TYPE_SLOT -> DataTypes.makeSlotValue(id, returnDefault);
      case TYPE_PATH -> DataTypes.makePathValue(id, returnDefault);
      case TYPE_CLASS -> DataTypes.makeClassValue(id, returnDefault);
        // The following don't have an integer -> object mapping
      case TYPE_STAT -> DataTypes.STAT_INIT;
      case TYPE_ELEMENT -> DataTypes.ELEMENT_INIT;
      case TYPE_COINMASTER -> DataTypes.COINMASTER_INIT;
      case TYPE_PHYLUM -> DataTypes.PHYLUM_INIT;
      case TYPE_BOUNTY -> DataTypes.BOUNTY_INIT;
      case TYPE_VYKEA -> DataTypes.VYKEA_INIT;
      default -> null;
    };
  }

  public List<String> getAmbiguousNames(String s1, Value value, boolean quote) {
    switch (this.type) {
      case TYPE_ITEM, TYPE_EFFECT, TYPE_MONSTER, TYPE_SKILL -> {
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
            this.type == TypeSpec.TYPE_ITEM
                ? ItemDatabase.getItemName(currentId)
                : this.type == TypeSpec.TYPE_EFFECT
                    ? EffectDatabase.getEffectName(currentId)
                    : this.type == TypeSpec.TYPE_MONSTER
                        ? MonsterDatabase.getMonsterName(currentId)
                        : this.type == TypeSpec.TYPE_SKILL
                            ? SkillDatabase.getSkillName(currentId)
                            : "";
        int[] ids =
            this.type == TypeSpec.TYPE_ITEM
                ? ItemDatabase.getItemIds(name, 1, false)
                : this.type == TypeSpec.TYPE_EFFECT
                    ? EffectDatabase.getEffectIds(name, false)
                    : this.type == TypeSpec.TYPE_MONSTER
                        ? MonsterDatabase.getMonsterIds(name, false)
                        : this.type == TypeSpec.TYPE_SKILL
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
        case TYPE_BOOLEAN -> DataTypes.makeBooleanValue(integer);
        case TYPE_CLASS -> DataTypes.makeClassValue(integer, returnDefault);
        case TYPE_INT -> DataTypes.makeIntValue(integer);
        case TYPE_FLOAT -> DataTypes.makeFloatValue(integer);
        case TYPE_STRING -> new Value(DataTypes.STRING_TYPE, String.valueOf(integer));
        case TYPE_ITEM -> DataTypes.makeItemValue(integer, returnDefault);
        case TYPE_SKILL -> DataTypes.makeSkillValue(integer, returnDefault);
        case TYPE_EFFECT -> DataTypes.makeEffectValue(integer, returnDefault);
        case TYPE_FAMILIAR -> DataTypes.makeFamiliarValue(integer, returnDefault);
        case TYPE_MONSTER -> DataTypes.makeMonsterValue(integer, returnDefault);
        case TYPE_THRALL -> DataTypes.makeThrallValue(integer, returnDefault);
        case TYPE_SERVANT -> DataTypes.makeServantValue(integer, returnDefault);
        default -> null;
      };
    }
    if (object instanceof MonsterData monster) {
      return switch (this.type) {
        case TYPE_INT -> DataTypes.makeIntValue(monster.getId());
        case TYPE_STRING -> new Value(DataTypes.STRING_TYPE, monster.getName());
        case TYPE_MONSTER -> DataTypes.makeMonsterValue(monster);
        default -> null;
      };
    }
    if (object instanceof AscensionClass ascensionClass) {
      return switch (this.type) {
        case TYPE_INT -> DataTypes.makeIntValue(ascensionClass.getId());
        case TYPE_STRING -> new Value(DataTypes.STRING_TYPE, ascensionClass.getName());
        case TYPE_CLASS -> DataTypes.makeClassValue(ascensionClass, returnDefault);
        case TYPE_PATH -> DataTypes.makePathValue(ascensionClass.getPath());
        default -> null;
      };
    }
    if (object instanceof Path path) {
      return switch (this.type) {
        case TYPE_INT -> DataTypes.makeIntValue(path.getId());
        case TYPE_STRING -> new Value(DataTypes.STRING_TYPE, path.getName());
        case TYPE_PATH -> DataTypes.makePathValue(path);
        default -> null;
      };
    }
    return null;
  }

  public PluralValue allValues() {
    if (this.allValues != null) return this.allValues;

    List<Value> list = new ArrayList<>();
    switch (this.type) {
      case TYPE_BOOLEAN:
        this.addValues(list, DataTypes.BOOLEANS);
        break;
      case TYPE_ITEM:
        int limit = ItemDatabase.maxItemId();
        for (int i = 1; i <= limit; ++i) {
          if (i != 13 && ItemDatabase.getItemDataName(i) != null) {
            list.add(DataTypes.makeItemValue(i, true));
          }
        }
        break;
      case TYPE_LOCATION:
        this.addValues(list, AdventureDatabase.getAsLockableListModel());
        break;
      case TYPE_CLASS:
        this.addValues(list, AscensionClass.allClasses());
        break;
      case TYPE_STAT:
        this.addValues(list, DataTypes.STAT_ARRAY, 0, 3);
        break;
      case TYPE_SKILL:
        this.addValues(list, SkillDatabase.entrySet());
        break;
      case TYPE_EFFECT:
        this.addValues(list, EffectDatabase.entrySet());
        break;
      case TYPE_FAMILIAR:
        this.addValues(list, FamiliarDatabase.entrySet());
        break;
      case TYPE_SLOT:
        this.addValues(list, EquipmentRequest.slotNames);
        break;
      case TYPE_MONSTER:
        this.addValues(list, MonsterDatabase.valueSet());
        break;
      case TYPE_ELEMENT:
        this.addValues(list, MonsterDatabase.ELEMENT_ARRAY, 1, -1);
        break;
      case TYPE_COINMASTER:
        this.addValues(list, CoinmasterRegistry.MASTERS);
        break;
      case TYPE_PHYLUM:
        this.addValues(list, MonsterDatabase.PHYLUM_ARRAY, 1, -1);
        break;
      case TYPE_BOUNTY:
        this.addValues(list, BountyDatabase.entrySet());
        break;
      case TYPE_THRALL:
        this.addValues(list, PastaThrallData.THRALL_ARRAY);
        break;
      case TYPE_SERVANT:
        this.addValues(list, EdServantData.SERVANT_ARRAY);
        break;
      case TYPE_VYKEA:
        this.addValues(list, VYKEACompanionData.VYKEA);
        break;
      case TYPE_PATH:
        this.addValues(list, Path.allPaths());
        break;
      default:
        return null;
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
      super(name, TypeSpec.TYPE_ANY, location);
    }
  }
}
