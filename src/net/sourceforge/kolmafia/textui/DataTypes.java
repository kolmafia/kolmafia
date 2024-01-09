package net.sourceforge.kolmafia.textui;

import java.util.List;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.CoinmasterRegistry;
import net.sourceforge.kolmafia.EdServantData;
import net.sourceforge.kolmafia.EdServantData.Servant;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.PastaThrallData;
import net.sourceforge.kolmafia.VYKEACompanionData;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.Modifier;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.BountyDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.textui.parsetree.AggregateType;
import net.sourceforge.kolmafia.textui.parsetree.ArrayValue;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.TypeList;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.textui.parsetree.VarArgType;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class DataTypes {
  public enum TypeSpec {
    ANY,
    VOID,
    BOOLEAN,
    INT,
    FLOAT,
    STRING,
    BUFFER,
    MATCHER,

    ITEM,
    LOCATION,
    CLASS,
    STAT,
    SKILL,
    EFFECT,
    FAMILIAR,
    SLOT,
    MONSTER,
    ELEMENT,
    COINMASTER,
    PHYLUM,
    THRALL,
    BOUNTY,
    SERVANT,
    VYKEA,
    PATH,
    MODIFIER,

    STRICT_STRING,
    AGGREGATE,
    RECORD,
    TYPEDEF,
  }

  public static final Type ANY_TYPE = new Type(null, TypeSpec.ANY);
  public static final Type VOID_TYPE = new Type("void", TypeSpec.VOID);
  public static final Type BOOLEAN_TYPE = new Type("boolean", TypeSpec.BOOLEAN);
  public static final Type INT_TYPE = new Type("int", TypeSpec.INT);
  public static final Type FLOAT_TYPE = new Type("float", TypeSpec.FLOAT);
  public static final Type STRING_TYPE = new Type("string", TypeSpec.STRING);
  public static final Type BUFFER_TYPE = new Type("buffer", TypeSpec.BUFFER);
  public static final Type MATCHER_TYPE = new Type("matcher", TypeSpec.MATCHER);

  public static final Type ITEM_TYPE = new Type("item", TypeSpec.ITEM);
  public static final Type LOCATION_TYPE = new Type("location", TypeSpec.LOCATION);
  public static final Type CLASS_TYPE = new Type("class", TypeSpec.CLASS);
  public static final Type STAT_TYPE = new Type("stat", TypeSpec.STAT);
  public static final Type SKILL_TYPE = new Type("skill", TypeSpec.SKILL);
  public static final Type EFFECT_TYPE = new Type("effect", TypeSpec.EFFECT);
  public static final Type FAMILIAR_TYPE = new Type("familiar", TypeSpec.FAMILIAR);
  public static final Type SLOT_TYPE = new Type("slot", TypeSpec.SLOT);
  public static final Type MONSTER_TYPE = new Type("monster", TypeSpec.MONSTER);
  public static final Type ELEMENT_TYPE = new Type("element", TypeSpec.ELEMENT);
  public static final Type COINMASTER_TYPE = new Type("coinmaster", TypeSpec.COINMASTER);
  public static final Type PHYLUM_TYPE = new Type("phylum", TypeSpec.PHYLUM);
  public static final Type BOUNTY_TYPE = new Type("bounty", TypeSpec.BOUNTY);
  public static final Type THRALL_TYPE = new Type("thrall", TypeSpec.THRALL);
  public static final Type SERVANT_TYPE = new Type("servant", TypeSpec.SERVANT);
  public static final Type VYKEA_TYPE = new Type("vykea", TypeSpec.VYKEA);
  public static final Type PATH_TYPE = new Type("path", TypeSpec.PATH);
  public static final Type MODIFIER_TYPE = new Type("modifier", TypeSpec.MODIFIER);

  public static final Type STRICT_STRING_TYPE = new Type("strict_string", TypeSpec.STRICT_STRING);
  public static final Type AGGREGATE_TYPE = new Type("aggregate", TypeSpec.AGGREGATE);

  // Map from ITEM -> INT
  public static final AggregateType ITEM_TO_INT_TYPE =
      new AggregateType(DataTypes.INT_TYPE, DataTypes.ITEM_TYPE);

  // Map from ITEM -> FLOAT
  public static final AggregateType ITEM_TO_FLOAT_TYPE =
      new AggregateType(DataTypes.FLOAT_TYPE, DataTypes.ITEM_TYPE);

  // Map from INT -> ITEM
  public static final AggregateType INT_TO_ITEM_TYPE =
      new AggregateType(DataTypes.ITEM_TYPE, DataTypes.INT_TYPE);

  // Map from STRING -> BOOLEAN
  public static final AggregateType STRING_TO_BOOLEAN_TYPE =
      new AggregateType(DataTypes.BOOLEAN_TYPE, DataTypes.STRING_TYPE);

  // Map from STRING -> INT
  public static final AggregateType STRING_TO_INT_TYPE =
      new AggregateType(DataTypes.INT_TYPE, DataTypes.STRING_TYPE);

  // Map from STRING -> ITEM
  public static final AggregateType STRING_TO_ITEM_TYPE =
      new AggregateType(DataTypes.ITEM_TYPE, DataTypes.STRING_TYPE);

  // Map from INT -> STRING
  public static final AggregateType INT_TO_STRING_TYPE =
      new AggregateType(DataTypes.STRING_TYPE, DataTypes.INT_TYPE);

  // Map from STRING -> STRING
  public static final AggregateType STRING_TO_STRING_TYPE =
      new AggregateType(DataTypes.STRING_TYPE, DataTypes.STRING_TYPE);

  // Map from STRING -> LOCATION
  public static final AggregateType STRING_TO_LOCATION_TYPE =
      new AggregateType(DataTypes.LOCATION_TYPE, DataTypes.STRING_TYPE);

  public static final AggregateType REGEX_GROUP_TYPE =
      new AggregateType(
          new AggregateType(DataTypes.STRING_TYPE, DataTypes.INT_TYPE), DataTypes.INT_TYPE);

  public static final VarArgType VARARG_FLOAT_TYPE = new VarArgType(DataTypes.FLOAT_TYPE);
  public static final VarArgType VARARG_INT_TYPE = new VarArgType(DataTypes.INT_TYPE);

  // Common values

  public static final String[] BOOLEANS = {"true", "false"};

  public static final String[] STAT_ARRAY = new String[Stat.values().length];

  static {
    for (int i = 0; i < Stat.values().length; i++) {
      STAT_ARRAY[i] = Stat.values()[i].toString();
    }
  }

  public static final Value[] STAT_VALUES = {
    new Value(DataTypes.STAT_TYPE, Stat.MUSCLE.toString()),
    new Value(DataTypes.STAT_TYPE, Stat.MYSTICALITY.toString()),
    new Value(DataTypes.STAT_TYPE, Stat.MOXIE.toString()),
    new Value(DataTypes.STAT_TYPE, Stat.SUBMUSCLE.toString()),
    new Value(DataTypes.STAT_TYPE, Stat.SUBMYST.toString()),
    new Value(DataTypes.STAT_TYPE, Stat.SUBMOXIE.toString()),
  };

  public static final Value VOID_VALUE = new Value();
  public static final Value TRUE_VALUE = new Value(true);
  public static final Value FALSE_VALUE = new Value(false);
  public static final Value ZERO_VALUE = new Value(0);
  public static final Value ONE_VALUE = new Value(1);
  public static final Value ZERO_FLOAT_VALUE = new Value(0.0);
  public static final Value MUSCLE_VALUE = DataTypes.STAT_VALUES[0];
  public static final Value MYSTICALITY_VALUE = DataTypes.STAT_VALUES[1];
  public static final Value MOXIE_VALUE = DataTypes.STAT_VALUES[2];

  // Initial values for uninitialized variables

  // VOID_TYPE omitted since no variable can have that type
  public static final Value BOOLEAN_INIT = DataTypes.FALSE_VALUE;
  public static final Value INT_INIT = DataTypes.ZERO_VALUE;
  public static final Value FLOAT_INIT = DataTypes.ZERO_FLOAT_VALUE;
  public static final Value STRING_INIT = new Value("");

  public static final Value ITEM_INIT = new Value(DataTypes.ITEM_TYPE, -1, "none");
  public static final Value LOCATION_INIT = new Value(DataTypes.LOCATION_TYPE, "none", null);
  public static final Value CLASS_INIT = new Value(DataTypes.CLASS_TYPE, -1, "none", null);
  public static final Value STAT_INIT = new Value(DataTypes.STAT_TYPE, -1, "none");
  public static final Value SKILL_INIT = new Value(DataTypes.SKILL_TYPE, -1, "none");
  public static final Value EFFECT_INIT = new Value(DataTypes.EFFECT_TYPE, -1, "none");
  public static final Value FAMILIAR_INIT = new Value(DataTypes.FAMILIAR_TYPE, -1, "none");
  public static final Value SLOT_INIT = new Value(DataTypes.SLOT_TYPE, 0, "none");
  public static final Value MONSTER_INIT = new Value(DataTypes.MONSTER_TYPE, 0, "none", null);
  public static final Value ELEMENT_INIT = new Value(DataTypes.ELEMENT_TYPE, "none", Element.NONE);
  public static final Value COINMASTER_INIT = new Value(DataTypes.COINMASTER_TYPE, "none", null);
  public static final Value PHYLUM_INIT = new Value(DataTypes.PHYLUM_TYPE, "none", Phylum.NONE);
  public static final Value BOUNTY_INIT = new Value(DataTypes.BOUNTY_TYPE, "none", null);
  public static final Value THRALL_INIT = new Value(DataTypes.THRALL_TYPE, 0, "none", null);
  public static final Value SERVANT_INIT = new Value(DataTypes.SERVANT_TYPE, 0, "none", null);
  public static final Value VYKEA_INIT =
      new Value(DataTypes.VYKEA_TYPE, 0, "none", VYKEACompanionData.NO_COMPANION);
  public static final Value PATH_INIT = new Value(DataTypes.PATH_TYPE, -1, "none", Path.NONE);
  public static final Value MODIFIER_INIT = new Value(DataTypes.MODIFIER_TYPE, "none", null);

  public static final TypeList enumeratedTypes =
      TypeList.of(
          ITEM_TYPE,
          LOCATION_TYPE,
          CLASS_TYPE,
          STAT_TYPE,
          SKILL_TYPE,
          EFFECT_TYPE,
          FAMILIAR_TYPE,
          SLOT_TYPE,
          MONSTER_TYPE,
          ELEMENT_TYPE,
          COINMASTER_TYPE,
          PHYLUM_TYPE,
          BOUNTY_TYPE,
          THRALL_TYPE,
          SERVANT_TYPE,
          VYKEA_TYPE,
          PATH_TYPE,
          MODIFIER_TYPE);
  public static final TypeList simpleTypes =
      TypeList.of(
          VOID_TYPE,
          BOOLEAN_TYPE,
          INT_TYPE,
          FLOAT_TYPE,
          STRING_TYPE,
          BUFFER_TYPE,
          MATCHER_TYPE,
          AGGREGATE_TYPE);

  static {
    simpleTypes.addAll(enumeratedTypes);
  }

  private DataTypes() {}

  // For each simple data type X, we supply:
  // public static final ScriptValue parseXValue( String name );

  public static final Value parseBooleanValue(final String name, final boolean returnDefault) {
    if (name.equalsIgnoreCase("true")) {
      return DataTypes.TRUE_VALUE;
    }
    if (name.equalsIgnoreCase("false")) {
      return DataTypes.FALSE_VALUE;
    }

    if (returnDefault) {
      return makeBooleanValue(StringUtilities.parseInt(name));
    }

    return null;
  }

  public static final Value parseIntValue(final String name, final boolean returnDefault) {
    try {
      // Expected input is the string representation of an integer
      return new Value(StringUtilities.parseLong(name));
    } catch (NumberFormatException e) {
      // Allow names of booleans to represent the usual integer value
      if (name.equals("false")) {
        return ZERO_VALUE;
      }
      if (name.equals("true")) {
        return ONE_VALUE;
      }
      return returnDefault ? DataTypes.ZERO_VALUE : null;
    }
  }

  public static final Value parseFloatValue(final String name, final boolean returnDefault) {
    try {
      return new Value(StringUtilities.parseDouble(name));
    } catch (NumberFormatException e) {
      return returnDefault ? DataTypes.ZERO_FLOAT_VALUE : null;
    }
  }

  public static final Value parseStringValue(final String name) {
    return new Value(name);
  }

  public static final Value parseItemValue(String name, final boolean returnDefault) {
    return DataTypes.parseItemValue(name, returnDefault, false);
  }

  public static final Value parseItemValue(
      String name, final boolean returnDefault, final boolean resolveAliases) {
    if (name == null || name.trim().equals("")) {
      return returnDefault ? DataTypes.ITEM_INIT : null;
    }

    if (name.equalsIgnoreCase("none")) {
      return DataTypes.ITEM_INIT;
    }

    // Allow for an item number to be specified
    // inside of the "item" construct.

    if (StringUtilities.isNumeric(name)) {
      int itemId = StringUtilities.parseInt(name);
      name = ItemDatabase.getItemDataName(itemId);

      if (name == null) {
        return returnDefault ? DataTypes.ITEM_INIT : null;
      }

      return DataTypes.makeNormalizedItem(itemId, name);
    }

    // Otherwise, let ItemDatabase parse the name using fuzzy matching.
    int itemId = ItemDatabase.getItemId(name);

    if (itemId == -1 && resolveAliases) {
      AdventureResult item = new AdventureResult(name, itemId, 1, false);
      itemId = item.resolveBangPotion().getItemId();
    }

    // ItemDatabase parses as "[0]" as itemId 0, even though no such item exists,
    // to allow that to mean "no item" in concoctions
    if (itemId < 1) {
      return returnDefault ? DataTypes.ITEM_INIT : null;
    }

    name = ItemDatabase.getItemDataName(itemId);
    return DataTypes.makeNormalizedItem(itemId, name);
  }

  public static final Value parseLocationValue(final String name, final boolean returnDefault) {
    if (name == null || name.equals("")) {
      return returnDefault ? DataTypes.LOCATION_INIT : null;
    }

    if (name.equalsIgnoreCase("none")) {
      return DataTypes.LOCATION_INIT;
    }

    KoLAdventure content = AdventureDatabase.getAdventure(name);
    if (content == null) {
      return returnDefault ? DataTypes.LOCATION_INIT : null;
    }

    return DataTypes.makeLocationValue(content);
  }

  public static final Value parseLocationValue(final int adv, final boolean returnDefault) {
    if (adv <= 0) {
      return DataTypes.LOCATION_INIT;
    }

    KoLAdventure content = AdventureDatabase.getAdventureByURL("adventure.php?snarfblat=" + adv);
    if (content == null) {
      return returnDefault ? DataTypes.LOCATION_INIT : null;
    }

    return DataTypes.makeLocationValue(content);
  }

  public static final Value makeLocationValue(final KoLAdventure adventure) {
    return new Value(DataTypes.LOCATION_TYPE, adventure.getAdventureName(), adventure);
  }

  public static final Value parseClassValue(final String name, final boolean returnDefault) {
    if (name == null || name.equals("")) {
      return returnDefault ? DataTypes.CLASS_INIT : null;
    }

    if (name.equalsIgnoreCase("none")) {
      return DataTypes.CLASS_INIT;
    }

    AscensionClass ascensionClass = AscensionClass.find(name);

    if (ascensionClass == null || ascensionClass.getId() < 0) {
      return returnDefault ? DataTypes.CLASS_INIT : null;
    }

    return new Value(
        DataTypes.CLASS_TYPE, ascensionClass.getId(), ascensionClass.getName(), ascensionClass);
  }

  public static final Value parseStatValue(final String name, final boolean returnDefault) {
    if (name == null || name.equals("")) {
      return returnDefault ? DataTypes.STAT_INIT : null;
    }

    if (name.equalsIgnoreCase("none")) {
      return DataTypes.STAT_INIT;
    }

    for (int i = 0; i < DataTypes.STAT_VALUES.length; ++i) {
      if (name.equalsIgnoreCase(DataTypes.STAT_VALUES[i].toString())) {
        return STAT_VALUES[i];
      }
    }

    return returnDefault ? DataTypes.STAT_INIT : null;
  }

  public static final Value parseSkillValue(String name, final boolean returnDefault) {
    if (name == null || name.equals("")) {
      return returnDefault ? DataTypes.SKILL_INIT : null;
    }

    if (name.equalsIgnoreCase("none")) {
      return DataTypes.SKILL_INIT;
    }

    // Allow for a skill number to be specified
    // inside of the "skill" construct.

    int skillId;

    if (StringUtilities.isNumeric(name)) {
      skillId = StringUtilities.parseInt(name);
      name = SkillDatabase.getSkillName(skillId);

      if (name == null) {
        return returnDefault ? DataTypes.SKILL_INIT : null;
      }

      return DataTypes.makeNormalizedSkill(skillId, name);
    }

    skillId = SkillDatabase.getSkillId(name);

    if (skillId == -1) {
      return returnDefault ? DataTypes.SKILL_INIT : null;
    }

    name = SkillDatabase.getSkillName(skillId);
    return DataTypes.makeNormalizedSkill(skillId, name);
  }

  public static final Value parseSkillValue(
      String name, String typeName, final boolean returnDefault) {
    if (name == null || name.equals("")) {
      return returnDefault ? DataTypes.SKILL_INIT : null;
    }

    if (name.equalsIgnoreCase("none")) {
      return DataTypes.SKILL_INIT;
    }

    int skillId = SkillDatabase.getSkillId(name, typeName);

    if (skillId == -1) {
      return returnDefault ? DataTypes.SKILL_INIT : null;
    }

    name = SkillDatabase.getSkillName(skillId);
    return DataTypes.makeNormalizedSkill(skillId, name);
  }

  public static final Value parseEffectValue(String name, final boolean returnDefault) {
    if (name == null || name.equals("")) {
      return returnDefault ? DataTypes.EFFECT_INIT : null;
    }

    if (name.equalsIgnoreCase("none")) {
      return DataTypes.EFFECT_INIT;
    }

    // Allow for an effect number to be specified
    // inside of the "effect" construct.

    int effectId;

    if (StringUtilities.isNumeric(name)) {
      effectId = StringUtilities.parseInt(name);
      name = EffectDatabase.getEffectName(effectId);

      if (name == null) {
        return returnDefault ? DataTypes.EFFECT_INIT : null;
      }

      return DataTypes.makeNormalizedEffect(effectId, name);
    }

    effectId = EffectDatabase.getEffectId(name);

    if (effectId == -1) {
      return returnDefault ? DataTypes.EFFECT_INIT : null;
    }

    name = EffectDatabase.getEffectName(effectId);
    return DataTypes.makeNormalizedEffect(effectId, name);
  }

  public static final Value parseFamiliarValue(String name, final boolean returnDefault) {
    if (name == null || name.equals("")) {
      return returnDefault ? DataTypes.FAMILIAR_INIT : null;
    }

    if (name.equalsIgnoreCase("none")) {
      return DataTypes.FAMILIAR_INIT;
    }

    int num = FamiliarDatabase.getFamiliarId(name);
    if (num == -1) {
      return returnDefault ? DataTypes.FAMILIAR_INIT : null;
    }

    name = FamiliarDatabase.getFamiliarName(num);
    return new Value(DataTypes.FAMILIAR_TYPE, num, name);
  }

  public static final Value parseSlotValue(String name, final boolean returnDefault) {
    if (name == null || name.equals("")) {
      return returnDefault ? DataTypes.SLOT_INIT : null;
    }

    if (name.equalsIgnoreCase("none")) {
      return DataTypes.SLOT_INIT;
    }

    Slot num = EquipmentRequest.slotNumber(name);
    if (num == Slot.NONE) {
      return returnDefault ? DataTypes.SLOT_INIT : null;
    }

    name = num.name;
    return new Value(DataTypes.SLOT_TYPE, num.ordinal(), name);
  }

  public static final Value parseMonsterValue(final String name, final boolean returnDefault) {
    if (name == null || name.equals("")) {
      return returnDefault ? DataTypes.MONSTER_INIT : null;
    }

    if (name.equalsIgnoreCase("none")) {
      return DataTypes.MONSTER_INIT;
    }

    // Allow for an monster id to be specified
    // inside of the "monster" construct.

    if (StringUtilities.isNumeric(name)) {
      int monsterId = StringUtilities.parseInt(name);
      return DataTypes.makeMonsterValue(monsterId, returnDefault);
    }

    // Look for exact match
    MonsterData[] monsters = MonsterDatabase.findMonsters(name, false);
    if (monsters.length > 0) {
      // The name matches exactly for at least one monster.
      // Return the first one. It's up to the caller to disambiguate
      return DataTypes.makeMonsterValue(monsters[0]);
    }

    // Allow fuzzy matching. MonsterDatabase will not allow ambiguity
    MonsterData monster = MonsterDatabase.findMonster(name, true);
    if (monster == null) {
      return returnDefault ? DataTypes.MONSTER_INIT : null;
    }

    return DataTypes.makeMonsterValue(monster);
  }

  public static final Value parseElementValue(String name, final boolean returnDefault) {
    if (name == null || name.equals("")) {
      return returnDefault ? DataTypes.ELEMENT_INIT : null;
    }

    if (name.equalsIgnoreCase("none")) {
      return DataTypes.ELEMENT_INIT;
    }

    Element elem = MonsterDatabase.stringToElement(name);
    if (elem == Element.NONE) {
      return returnDefault ? DataTypes.ELEMENT_INIT : null;
    }

    name = elem.toString();
    return new Value(DataTypes.ELEMENT_TYPE, name, elem);
  }

  public static final Value parsePhylumValue(String name, final boolean returnDefault) {
    if (name == null || name.equals("")) {
      return returnDefault ? DataTypes.PHYLUM_INIT : null;
    }

    if (name.equalsIgnoreCase("none")) {
      return DataTypes.PHYLUM_INIT;
    }

    Phylum phylum = Phylum.find(name);
    if (phylum == Phylum.NONE) {
      return returnDefault ? DataTypes.PHYLUM_INIT : null;
    }

    name = phylum.toString();
    return new Value(DataTypes.PHYLUM_TYPE, name, phylum);
  }

  public static final Value parseThrallValue(String name, final boolean returnDefault) {
    if (name == null || name.equals("")) {
      return returnDefault ? DataTypes.THRALL_INIT : null;
    }

    if (name.equalsIgnoreCase("none")) {
      return DataTypes.THRALL_INIT;
    }

    var data = PastaThrallData.typeToData(name);
    if (data == null) {
      return returnDefault ? DataTypes.THRALL_INIT : null;
    }

    int id = PastaThrallData.dataToId(data);
    name = PastaThrallData.dataToType(data);
    return new Value(DataTypes.THRALL_TYPE, id, name, data);
  }

  public static final Value parseServantValue(String name, final boolean returnDefault) {
    if (name == null || name.equals("")) {
      return returnDefault ? DataTypes.SERVANT_INIT : null;
    }

    if (name.equalsIgnoreCase("none")) {
      return DataTypes.SERVANT_INIT;
    }

    Servant data = EdServantData.typeToData(name);
    if (data == null) {
      return returnDefault ? DataTypes.SERVANT_INIT : null;
    }

    int id = EdServantData.dataToId(data);
    name = EdServantData.dataToType(data);
    return new Value(DataTypes.SERVANT_TYPE, id, name, data);
  }

  public static final Value parseVykeaValue(String name, final boolean returnDefault) {
    if (name == null || name.equals("")) {
      return returnDefault ? DataTypes.VYKEA_INIT : null;
    }

    if (name.equalsIgnoreCase("none")) {
      return DataTypes.VYKEA_INIT;
    }

    VYKEACompanionData companion = VYKEACompanionData.fromString(name);

    if (companion == null) {
      return returnDefault ? DataTypes.VYKEA_INIT : null;
    }

    return new Value(DataTypes.VYKEA_TYPE, companion.getType().ordinal(), name, companion);
  }

  public static Value parsePathValue(final int id, final boolean returnDefault) {
    var path = AscensionPath.idToPath(id);
    if (path == Path.NONE) {
      return returnDefault ? DataTypes.PATH_INIT : null;
    }
    return new Value(path);
  }

  public static final Value parsePathValue(String name, final boolean returnDefault) {
    if (name == null || name.equals("")) {
      return returnDefault ? DataTypes.PATH_INIT : null;
    }

    if (name.equalsIgnoreCase("none")) {
      return DataTypes.PATH_INIT;
    }

    var path = AscensionPath.nameToPath(name);

    if (path == Path.NONE) {
      return returnDefault ? DataTypes.PATH_INIT : null;
    }

    return new Value(path);
  }

  public static final Value parseBountyValue(String name, final boolean returnDefault) {
    if (name == null || name.equals("")) {
      return returnDefault ? DataTypes.BOUNTY_INIT : null;
    }

    if (name.equals("none")) {
      return DataTypes.BOUNTY_INIT;
    }

    List<String> bounties = BountyDatabase.getMatchingNames(name);

    if (bounties.size() != 1) {
      return returnDefault ? DataTypes.BOUNTY_INIT : null;
    }

    String canonical = bounties.get(0);

    return new Value(DataTypes.BOUNTY_TYPE, BountyDatabase.canonicalToName(canonical));
  }

  public static final Value parseCoinmasterValue(String name, final boolean returnDefault) {
    if (name == null || name.equals("")) {
      return returnDefault ? DataTypes.COINMASTER_INIT : null;
    }

    if (name.equalsIgnoreCase("none")) {
      return DataTypes.COINMASTER_INIT;
    }

    CoinmasterData content = CoinmasterRegistry.findCoinmaster(name);
    if (content == null) {
      return returnDefault ? DataTypes.COINMASTER_INIT : null;
    }

    return new Value(DataTypes.COINMASTER_TYPE, content.getMaster(), content);
  }

  public static final Value makeCoinmasterValue(final CoinmasterData data) {
    if (data == null) {
      return DataTypes.COINMASTER_INIT;
    }

    return new Value(DataTypes.COINMASTER_TYPE, data.getMaster(), data);
  }

  public static final Value parseModifierValue(String name, final boolean returnDefault) {
    if (name == null || name.equals("")) {
      return returnDefault ? DataTypes.MODIFIER_INIT : null;
    }

    if (name.equalsIgnoreCase("none")) {
      return DataTypes.MODIFIER_INIT;
    }

    Modifier content = ModifierDatabase.byCaselessName(name);
    if (content == null) {
      return returnDefault ? DataTypes.MODIFIER_INIT : null;
    }

    return new Value(content);
  }

  public static final Value parseValue(
      final Type type, final String name, final boolean returnDefault) {
    return type.parseValue(name, returnDefault);
  }

  public static final Value coerceValue(
      final Type type, final Object object, final boolean returnDefault) {
    return type.coerceValue(object, returnDefault);
  }

  // For data types which map to integers, also supply:
  // public static final ScriptValue makeXValue( int num )

  public static final Value makeIntValue(final boolean val) {
    return val ? ONE_VALUE : ZERO_VALUE;
  }

  public static final Value makeIntValue(final long val) {
    return val == 0 ? ZERO_VALUE : val == 1 ? ONE_VALUE : new Value(val);
  }

  public static final Value makeFloatValue(final double val) {
    return val == 0.0 ? ZERO_FLOAT_VALUE : new Value(val);
  }

  public static final Value makeStringValue(final String val) {
    return val == null || val.equals("") ? STRING_INIT : new Value(val);
  }

  public static final Value makeBooleanValue(final int num) {
    return makeBooleanValue(num != 0);
  }

  public static final Value makeBooleanValue(final boolean value) {
    return value ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
  }

  private static Value makeNormalizedItem(final int num, String name) {
    if (num == -1) {
      return DataTypes.ITEM_INIT;
    }
    if (name == null) {
      name = "[" + num + "]";
    }
    int[] itemIds = ItemDatabase.getItemIds(name, 1, false);
    if (itemIds != null && itemIds.length > 1) {
      name = "[" + num + "]" + name;
    }
    return new Value(DataTypes.ITEM_TYPE, num, name);
  }

  public static final Value makeItemValue(final int num, final boolean returnDefault) {
    String name = ItemDatabase.getItemDataName(num);

    if (name == null) {
      return returnDefault ? DataTypes.ITEM_INIT : null;
    }

    return DataTypes.makeNormalizedItem(num, name);
  }

  public static final Value makeItemValue(String name) {
    if (name == null) {
      return DataTypes.ITEM_INIT;
    }

    int num = ItemDatabase.getItemId(name);

    if (num == -1) {
      return DataTypes.ITEM_INIT;
    }

    name = ItemDatabase.getItemDataName(num);
    return DataTypes.makeNormalizedItem(num, name);
  }

  public static final Value makeItemValue(final AdventureResult ar) {
    if (ar == null) return DataTypes.ITEM_INIT;
    int num = ar.getItemId();
    String name = ItemDatabase.getItemDataName(num);
    return DataTypes.makeNormalizedItem(num, name);
  }

  public static final Value makeClassValue(
      final AscensionClass ascensionClass, boolean returnDefault) {
    if (ascensionClass == null) {
      return returnDefault ? DataTypes.CLASS_INIT : null;
    }

    return new Value(
        DataTypes.CLASS_TYPE, ascensionClass.getId(), ascensionClass.getName(), ascensionClass);
  }

  public static final Value makeClassValue(final int id, boolean returnDefault) {
    return makeClassValue(AscensionClass.find(id), returnDefault);
  }

  private static Value makeNormalizedSkill(final int num, String name) {
    if (num == -1) {
      return DataTypes.SKILL_INIT;
    }
    if (name == null) {
      name = "[" + num + "]";
    }
    int[] skillIds = SkillDatabase.getSkillIds(name, false);
    if (skillIds != null && skillIds.length > 1) {
      name = "[" + num + "]" + name;
    }
    return new Value(DataTypes.SKILL_TYPE, num, name);
  }

  public static final Value makeSkillValue(final int num, final boolean returnDefault) {
    String name = SkillDatabase.getSkillName(num);
    if (name == null) {
      return returnDefault ? DataTypes.SKILL_INIT : null;
    }

    return DataTypes.makeNormalizedSkill(num, name);
  }

  private static Value makeNormalizedEffect(final int num, String name) {
    if (num == -1) {
      return DataTypes.EFFECT_INIT;
    }
    if (name == null) {
      name = "[" + num + "]";
    }
    int[] effectIds = EffectDatabase.getEffectIds(name, false);
    if (effectIds != null && effectIds.length > 1) {
      name = "[" + num + "]" + name;
    }
    return new Value(DataTypes.EFFECT_TYPE, num, name);
  }

  public static final Value makeEffectValue(final int num, final boolean returnDefault) {
    String name = EffectDatabase.getEffectName(num);
    if (name == null) {
      return returnDefault ? DataTypes.EFFECT_INIT : null;
    }
    return DataTypes.makeNormalizedEffect(num, name);
  }

  public static final Value makeFamiliarValue(final int num, final boolean returnDefault) {
    String name = FamiliarDatabase.getFamiliarName(num);
    if (name == null) {
      return returnDefault ? DataTypes.FAMILIAR_INIT : null;
    }
    return new Value(DataTypes.FAMILIAR_TYPE, num, name);
  }

  public static final Value makeMonsterValue(final int num, final boolean returnDefault) {
    MonsterData monster = MonsterDatabase.findMonsterById(num);
    if (monster == null) {
      return returnDefault ? DataTypes.MONSTER_INIT : null;
    }
    return makeMonsterValue(monster);
  }

  public static final Value makeSlotValue(final int num, final boolean returnDefault) {
    String name = Slot.byOrdinal(num).name;
    if (name == null) {
      return returnDefault ? DataTypes.SLOT_INIT : null;
    }
    return new Value(DataTypes.SLOT_TYPE, num, name);
  }

  public static final Value makeElementValue(Element elem) {
    return makeElementValue(elem, true);
  }

  public static final Value makeElementValue(Element elem, final boolean returnDefault) {
    if (elem == Element.NONE) {
      return returnDefault ? DataTypes.ELEMENT_INIT : null;
    }

    return new Value(DataTypes.ELEMENT_TYPE, elem.toString(), elem);
  }

  public static final Value makeThrallValue(
      final PastaThrallData thrall, final boolean returnDefault) {
    if (thrall == null || thrall == PastaThrallData.NO_THRALL) {
      return returnDefault ? DataTypes.THRALL_INIT : null;
    }
    return new Value(DataTypes.THRALL_TYPE, thrall.getId(), thrall.getType(), thrall.getData());
  }

  public static final Value makeThrallValue(final int num, final boolean returnDefault) {
    var data = PastaThrallData.idToData(num);
    if (data == null) {
      return returnDefault ? DataTypes.THRALL_INIT : null;
    }

    String name = PastaThrallData.dataToType(data);
    return new Value(DataTypes.THRALL_TYPE, num, name, data);
  }

  public static final Value makeServantValue(
      final EdServantData servant, final boolean returnDefault) {
    if (servant == null || servant == EdServantData.NO_SERVANT) {
      return returnDefault ? DataTypes.SERVANT_INIT : null;
    }
    return new Value(DataTypes.SERVANT_TYPE, servant.getId(), servant.getType(), servant.getData());
  }

  public static final Value makeServantValue(final int num, final boolean returnDefault) {
    Servant data = EdServantData.idToData(num);
    if (data == null) {
      return returnDefault ? DataTypes.SERVANT_INIT : null;
    }

    String name = EdServantData.dataToType(data);
    return new Value(DataTypes.SERVANT_TYPE, num, name, data);
  }

  public static final Value makeVykeaValue(
      final VYKEACompanionData companion, final boolean returnDefault) {
    if (companion == null || companion == VYKEACompanionData.NO_COMPANION) {
      return returnDefault ? DataTypes.VYKEA_INIT : null;
    }
    return new Value(
        DataTypes.VYKEA_TYPE, companion.getType().ordinal(), companion.toString(), companion);
  }

  public static final Value makePathValue(final Path path) {
    if (path == Path.NONE) return DataTypes.PATH_INIT;
    return new Value(DataTypes.PATH_TYPE, path.getId(), path.getName(), path);
  }

  public static final Value makePathValue(final int id, final boolean returnDefault) {
    var path = AscensionPath.idToPath(id);
    if (path == Path.NONE) {
      return returnDefault ? DataTypes.PATH_INIT : null;
    }
    return new Value(DataTypes.PATH_TYPE, id, path.getName(), path);
  }

  public static final Value makeMonsterValue(final MonsterData monster) {
    if (monster == null) {
      return DataTypes.MONSTER_INIT;
    }

    int id = monster.getId();
    String name = monster.getName();
    int[] monsterIds = MonsterDatabase.getMonsterIds(name, false);
    if (monsterIds != null && monsterIds.length > 1) {
      name = "[" + id + "]" + name;
    }

    return new Value(DataTypes.MONSTER_TYPE, id, name, monster);
  }

  public static final Value makeStringArrayValue(final List<String> list) {
    var length = list.size();
    AggregateType type = new AggregateType(DataTypes.STRING_TYPE, length);
    ArrayValue value = new ArrayValue(type);

    for (int i = 0; i < length; ++i) {
      value.aset(new Value(i), new Value(list.get(i)));
    }

    return value;
  }

  // Also supply:
  // public static final String promptForValue()

  public static String promptForValue(final Type type, final String name) {
    return DataTypes.promptForValue(type, "Please input a value for " + type + " " + name, name);
  }

  private static String promptForValue(final Type type, final String message, final String name) {
    switch (type.getType()) {
      case BOOLEAN -> {
        return InputFieldUtilities.input(message, DataTypes.BOOLEANS);
      }
      case LOCATION -> {
        LockableListModel<KoLAdventure> inputs = AdventureDatabase.getAsLockableListModel();
        KoLAdventure initial =
            AdventureDatabase.getAdventure(Preferences.getString("lastAdventure"));
        KoLAdventure value = InputFieldUtilities.input(message, inputs, initial);
        return value == null ? null : value.getAdventureName();
      }
      case SKILL -> {
        UseSkillRequest[] inputs =
            SkillDatabase.getCastableSkills().toArray(new UseSkillRequest[0]);
        UseSkillRequest value = InputFieldUtilities.input(message, inputs);
        return value == null ? null : value.getSkillName();
      }
      case FAMILIAR -> {
        FamiliarData[] inputs = KoLCharacter.usableFamiliars().toArray(new FamiliarData[0]);
        FamiliarData initial = KoLCharacter.getFamiliar();
        FamiliarData value = InputFieldUtilities.input(message, inputs, initial);
        return value == null ? null : value.getRace();
      }
      case SLOT -> {
        return InputFieldUtilities.input(message, SlotSet.NAMES);
      }
      case ELEMENT -> {
        return InputFieldUtilities.input(message, MonsterDatabase.ELEMENT_ARRAY);
      }
      case COINMASTER -> {
        return InputFieldUtilities.input(message, CoinmasterRegistry.MASTERS);
      }
      case PHYLUM -> {
        return InputFieldUtilities.input(message, MonsterDatabase.PHYLUM_ARRAY);
      }
      case THRALL -> {
        return InputFieldUtilities.input(message, PastaThrallData.THRALL_ARRAY);
      }
      case SERVANT -> {
        return InputFieldUtilities.input(message, EdServantData.SERVANT_ARRAY);
      }
      case VYKEA -> {
        return InputFieldUtilities.input(message, VYKEACompanionData.VYKEA);
      }
      case PATH -> {
        return InputFieldUtilities.input(message, Path.values()).toString();
      }
      case CLASS -> {
        return InputFieldUtilities.input(message, AscensionClass.values()).toString();
      }
      case STAT -> {
        return InputFieldUtilities.input(message, DataTypes.STAT_ARRAY);
      }
      case MODIFIER -> {
        return InputFieldUtilities.input(
                message, ModifierDatabase.allModifiers().toArray(new Modifier[0]))
            .toString();
      }
      case INT, FLOAT, STRING, ITEM, EFFECT, MONSTER -> {
        return InputFieldUtilities.input(message);
      }
      default -> throw new ScriptException("Internal error: Illegal type for main() parameter");
    }
  }
}
