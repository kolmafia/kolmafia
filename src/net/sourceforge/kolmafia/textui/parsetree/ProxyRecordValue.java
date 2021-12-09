package net.sourceforge.kolmafia.textui.parsetree;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AreaCombatData;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.CoinmasterRegistry;
import net.sourceforge.kolmafia.EdServantData;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.PastaThrallData;
import net.sourceforge.kolmafia.PokefamData;
import net.sourceforge.kolmafia.VYKEACompanionData;
import net.sourceforge.kolmafia.persistence.*;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.WildfireCampRequest;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;

public class ProxyRecordValue extends RecordValue {
  public ProxyRecordValue(final RecordType type, final Value obj) {
    super(type);

    this.contentLong = obj.contentLong;
    this.contentString = obj.contentString;
    this.content = obj.content;
  }

  @Override
  public Value aref(final Value key, final AshRuntime interpreter) {
    int index = ((RecordType) this.type).indexOf(key);
    if (index < 0) {
      throw interpreter.runtimeException("Internal error: field index out of bounds");
    }
    return this.aref(index, interpreter);
  }

  @Override
  public Value aref(final int index, final AshRuntime interpreter) {
    RecordType type = (RecordType) this.type;
    int size = type.fieldCount();
    if (index < 0 || index >= size) {
      throw interpreter.runtimeException("Internal error: field index out of bounds");
    }

    Object rv;
    try {
      rv = this.getClass().getMethod("get_" + type.getFieldNames()[index]).invoke(this);
    } catch (InvocationTargetException e) {
      throw interpreter.runtimeException("Unable to invoke attribute getter: " + e.getCause());
    } catch (Exception e) {
      throw interpreter.runtimeException("Unable to invoke attribute getter: " + e);
    }

    if (rv == null) {
      return type.getFieldTypes()[index].initialValue();
    }

    if (rv instanceof Value) {
      return (Value) rv;
    }

    if (rv instanceof Integer) {
      return DataTypes.makeIntValue(((Integer) rv).intValue());
    }

    if (rv instanceof Long) {
      return DataTypes.makeIntValue(((Long) rv).longValue());
    }

    if (rv instanceof Float) {
      return DataTypes.makeFloatValue(((Float) rv).floatValue());
    }

    if (rv instanceof Double) {
      return DataTypes.makeFloatValue(((Double) rv).doubleValue());
    }

    if (rv instanceof String) {
      return new Value(rv.toString());
    }

    if (rv instanceof Boolean) {
      return DataTypes.makeBooleanValue(((Boolean) rv).booleanValue());
    }

    if (rv instanceof CoinmasterData) {
      return DataTypes.makeCoinmasterValue((CoinmasterData) rv);
    }

    throw interpreter.runtimeException(
        "Unable to convert attribute value of type: " + rv.getClass());
  }

  @Override
  public void aset(final Value key, final Value val, final AshRuntime interpreter) {
    throw interpreter.runtimeException("Cannot assign to a proxy record field");
  }

  @Override
  public void aset(final int index, final Value val, final AshRuntime interpreter) {
    throw interpreter.runtimeException("Cannot assign to a proxy record field");
  }

  @Override
  public Value remove(final Value key, final AshRuntime interpreter) {
    throw interpreter.runtimeException("Cannot assign to a proxy record field");
  }

  @Override
  public void clear() {}

  /* Helper for building parallel arrays of field names & types */
  private static class RecordBuilder {
    private final ArrayList<String> names;
    private final ArrayList<Type> types;

    public RecordBuilder() {
      names = new ArrayList<String>();
      types = new ArrayList<Type>();
    }

    public RecordBuilder add(String name, Type type) {
      this.names.add(name.toLowerCase());
      this.types.add(type);
      return this;
    }

    public RecordType finish(String name) {
      int len = this.names.size();
      return new RecordType(
          name, this.names.toArray(new String[len]), this.types.toArray(new Type[len]));
    }
  }

  public static class ClassProxy extends ProxyRecordValue {
    public static RecordType _type =
        new RecordBuilder().add("primestat", DataTypes.STAT_TYPE).finish("class proxy");

    public ClassProxy(Value obj) {
      super(_type, obj);
    }

    public Value get_primestat() {
      int primeIndex = ((AscensionClass) this.content).getPrimeStatIndex();

      String name = null;
      if (primeIndex > -1 && primeIndex < AdventureResult.STAT_NAMES.length) {
        name = AdventureResult.STAT_NAMES[primeIndex];
      }

      return DataTypes.parseStatValue(name, true);
    }
  }

  public static class ItemProxy extends ProxyRecordValue {
    public static RecordType _type =
        new RecordBuilder()
            .add("name", DataTypes.STRING_TYPE)
            .add("plural", DataTypes.STRING_TYPE)
            .add("descid", DataTypes.STRING_TYPE)
            .add("image", DataTypes.STRING_TYPE)
            .add("smallimage", DataTypes.STRING_TYPE)
            .add("levelreq", DataTypes.INT_TYPE)
            .add("quality", DataTypes.STRING_TYPE)
            .add("adventures", DataTypes.STRING_TYPE)
            .add("muscle", DataTypes.STRING_TYPE)
            .add("mysticality", DataTypes.STRING_TYPE)
            .add("moxie", DataTypes.STRING_TYPE)
            .add("fullness", DataTypes.INT_TYPE)
            .add("inebriety", DataTypes.INT_TYPE)
            .add("spleen", DataTypes.INT_TYPE)
            .add("minhp", DataTypes.INT_TYPE)
            .add("maxhp", DataTypes.INT_TYPE)
            .add("minmp", DataTypes.INT_TYPE)
            .add("maxmp", DataTypes.INT_TYPE)
            .add("dailyusesleft", DataTypes.INT_TYPE)
            .add("notes", DataTypes.STRING_TYPE)
            .add("quest", DataTypes.BOOLEAN_TYPE)
            .add("gift", DataTypes.BOOLEAN_TYPE)
            .add("tradeable", DataTypes.BOOLEAN_TYPE)
            .add("discardable", DataTypes.BOOLEAN_TYPE)
            .add("combat", DataTypes.BOOLEAN_TYPE)
            .add("combat_reusable", DataTypes.BOOLEAN_TYPE)
            .add("usable", DataTypes.BOOLEAN_TYPE)
            .add("reusable", DataTypes.BOOLEAN_TYPE)
            .add("multi", DataTypes.BOOLEAN_TYPE)
            .add("fancy", DataTypes.BOOLEAN_TYPE)
            .add("pasteable", DataTypes.BOOLEAN_TYPE)
            .add("smithable", DataTypes.BOOLEAN_TYPE)
            .add("cookable", DataTypes.BOOLEAN_TYPE)
            .add("mixable", DataTypes.BOOLEAN_TYPE)
            .add("candy", DataTypes.BOOLEAN_TYPE)
            .add("candy_type", DataTypes.STRING_TYPE)
            .add("chocolate", DataTypes.BOOLEAN_TYPE)
            .add("seller", DataTypes.COINMASTER_TYPE)
            .add("buyer", DataTypes.COINMASTER_TYPE)
            .add("name_length", DataTypes.INT_TYPE)
            .add("noob_skill", DataTypes.SKILL_TYPE)
            .add("tcrs_name", DataTypes.STRING_TYPE)
            .finish("item proxy");

    public ItemProxy(Value obj) {
      super(_type, obj);
    }

    /**
     * Returns the name of the Item.
     *
     * @return The name
     */
    public String get_name() {
      return ItemDatabase.getDataName((int) this.contentLong);
    }

    /**
     * Returns the name of the Item as it appears in your current Two Crazy Random Summer run. If
     * you are not in a TCRS run, the regular Item name is returned.
     *
     * @return The TCRS name
     */
    public String get_tcrs_name() {
      return TCRSDatabase.getTCRSName((int) this.contentLong);
    }

    /**
     * Returns the plural of the Item. If the official plural is not known, returns the name of the
     * Item with an "s" appended.
     *
     * @return The plural
     */
    public String get_plural() {
      return ItemDatabase.getPluralName((int) this.contentLong);
    }

    /**
     * Returns the descid of the Item. This is the identifier used to see the description of the
     * Item.
     *
     * @return The descid
     */
    public String get_descid() {
      return ItemDatabase.getDescriptionId((int) this.contentLong);
    }

    /**
     * Returns the filename of the image associated with the Item.
     *
     * @return The filename of the image
     */
    public String get_image() {
      return ItemDatabase.getImage((int) this.contentLong);
    }

    /**
     * Returns the filename of the small image associated with the Item. For items with an image
     * that is usually larger than 30x30, returns their 30x30 equivalent.
     *
     * <p>For example, "folders" from the "over-the-shoulder Folder Holder" will normally return a
     * 100x100 image but a 30x30 image here.
     *
     * @return The filename of the small image
     */
    public String get_smallimage() {
      return ItemDatabase.getSmallImage((int) this.contentLong);
    }

    /**
     * Returns the level requirement for consuming or equipping the Item.
     *
     * @return The level requirement
     */
    public Integer get_levelreq() {
      return ConsumablesDatabase.getLevelReqByName(this.contentString);
    }

    /**
     * Returns the quality of the Item if it is a consumable, or blank otherwise. Quality can be one
     * of "decent", "crappy", "good", "awesome" or "EPIC".
     *
     * @return The quality
     */
    public String get_quality() {
      return ConsumablesDatabase.getQuality(this.contentString);
    }

    /**
     * Returns the range of adventures gained from consuming the Item. The string will either
     * contain the adventures for invariant gains, or a hyphen-separated minimum and maximum for
     * variant gains.
     *
     * @return The range of adventures gained
     */
    public String get_adventures() {
      return ConsumablesDatabase.getAdvRangeByName(this.contentString);
    }

    /**
     * Returns the range of muscle substats gained from consuming the Item. The string will either
     * contain the substats for invariant gains, or a hyphen-separated minimum and maximum for
     * variant gains. Note that substat gains can be negative.
     *
     * @return The range of muscle substats gained
     */
    public String get_muscle() {
      return ConsumablesDatabase.getMuscleByName(this.contentString);
    }

    /**
     * Returns the range of mysticality substats gained from consuming the Item. The string will
     * either contain the substats for invariant gains, or a hyphen-separated minimum and maximum
     * for variant gains. Note that substat gains can be negative.
     *
     * @return The range of mysticality substats gained
     */
    public String get_mysticality() {
      return ConsumablesDatabase.getMysticalityByName(this.contentString);
    }

    /**
     * Returns the range of moxie substats gained from consuming the Item. The string will either
     * contain the substats for invariant gains, or a hyphen-separated minimum and maximum for
     * variant gains. Note that substat gains can be negative.
     *
     * @return The range of moxie substats gained
     */
    public String get_moxie() {
      return ConsumablesDatabase.getMoxieByName(this.contentString);
    }

    /**
     * Returns the stomach size of Item. If the Item is not edible, returns 0.
     *
     * @return The stomach size
     */
    public int get_fullness() {
      return ConsumablesDatabase.getFullness(this.contentString);
    }

    /**
     * Returns the liver size of Item. If the Item is not drinkable, returns 0.
     *
     * @return The liver size
     */
    public int get_inebriety() {
      return ConsumablesDatabase.getInebriety(this.contentString);
    }

    /**
     * Returns the spleen size of Item. If the Item is not chewable, returns 0.
     *
     * @return The spleen size
     */
    public int get_spleen() {
      return ConsumablesDatabase.getSpleenHit(this.contentString);
    }

    /**
     * Returns the minimum HP restored by consuming this Item.
     *
     * @return The minimum HP restored
     */
    public long get_minhp() {
      return RestoresDatabase.getHPMin(this.contentString);
    }

    /**
     * Returns the maximum HP restored by consuming this Item.
     *
     * @return The maximum HP restored
     */
    public long get_maxhp() {
      return RestoresDatabase.getHPMax(this.contentString);
    }

    /**
     * Returns the minimum MP restored by consuming this Item.
     *
     * @return The minimum MP restored
     */
    public long get_minmp() {
      return RestoresDatabase.getMPMin(this.contentString);
    }

    /**
     * Returns the maximum MP restored by consuming this Item.
     *
     * @return The maximum MP restored
     */
    public long get_maxmp() {
      return RestoresDatabase.getMPMax(this.contentString);
    }

    /**
     * Returns the number of daily uses remaining for this Item.
     *
     * @return The number of daily uses left
     */
    public int get_dailyusesleft() {
      return UseItemRequest.maximumUses((int) this.contentLong);
    }

    /**
     * Returns any notes that exist for the Item. Examples of (comma-separated) contents are:
     *
     * <ul>
     *   <li>The name and duration of any effects granted by consumption, if applicable.
     *   <li>Items dropped when the item is consumed, if applicable.
     *   <li>Tags relevant to game mechanics (such as "MARTINI", "BEER" and "SAUCY")
     *   <li>"Unspaded"
     * </ul>
     *
     * @return The notes
     */
    public String get_notes() {
      return ConsumablesDatabase.getNotes(this.contentString);
    }

    /**
     * Returns `true` if the Item is a quest item, else `false`.
     *
     * @return Whether the Item is a quest item
     */
    public boolean get_quest() {
      return ItemDatabase.isQuestItem((int) this.contentLong);
    }

    /**
     * Returns `true` if the Item is a gift item, else `false`.
     *
     * @return Whether the Item is a gift item
     */
    public boolean get_gift() {
      return ItemDatabase.isGiftItem((int) this.contentLong);
    }

    /**
     * Returns `true` if the Item is tradeable, else `false`.
     *
     * @return Whether the Item is tradeable
     */
    public boolean get_tradeable() {
      return ItemDatabase.isTradeable((int) this.contentLong);
    }

    /**
     * Returns `true` if the Item is discardable, else `false`.
     *
     * @return Whether the Item is a discardable
     */
    public boolean get_discardable() {
      return ItemDatabase.isDiscardable((int) this.contentLong);
    }

    /**
     * Returns `true` if the Item usable in combat, else `false`. This returns `true` whether the
     * item is consumed by being used or not Items.
     *
     * @return Whether the Item is usable in combat
     */
    public boolean get_combat() {
      return ItemDatabase.getAttribute(
          (int) this.contentLong, ItemDatabase.ATTR_COMBAT | ItemDatabase.ATTR_COMBAT_REUSABLE);
    }

    /**
     * Returns `true` if the Item is usable in combat and is not consumed when doing so, else
     * `false`.
     *
     * @return Whether the Item is combat reusable
     */
    public boolean get_combat_reusable() {
      return ItemDatabase.getAttribute((int) this.contentLong, ItemDatabase.ATTR_COMBAT_REUSABLE);
    }

    /**
     * Returns `true` if the Item is usable, else `false`. This returns `true` whether the Item is
     * consumed by being used or not.
     *
     * @return Whether the Item is usable
     */
    public boolean get_usable() {
      return ItemDatabase.isUsable((int) this.contentLong);
    }

    /**
     * Returns `true` if the Item is usable and is not consumed when doing so, else `false`.
     *
     * @return Whether the Item is reusable
     */
    public boolean get_reusable() {
      int id = (int) this.contentLong;
      return ItemDatabase.getConsumptionType(id) == KoLConstants.INFINITE_USES
          || ItemDatabase.getAttribute(id, ItemDatabase.ATTR_REUSABLE);
    }

    /**
     * Returns `true` if the Item is multiusable, else `false`.
     *
     * @return Whether the Item is multiusable
     */
    public boolean get_multi() {
      return ItemDatabase.isMultiUsable((int) this.contentLong);
    }

    /**
     * Returns `true` if the Item is a "fancy" ingredient, else `false`.
     *
     * @return Whether the Item is a "fancy" ingredient
     */
    public boolean get_fancy() {
      return ItemDatabase.isFancyItem((int) this.contentLong);
    }

    /**
     * Returns `true` if the Item is a meatpasting ingredient, else `false`.
     *
     * @return Whether the Item is a meatpasting ingredient
     */
    public boolean get_pasteable() {
      return ItemDatabase.isPasteable((int) this.contentLong);
    }

    /**
     * Returns `true` if the Item is a meatsmithing ingredient, else `false`.
     *
     * @return Whether the Item is a meatsmithing ingredient
     */
    public boolean get_smithable() {
      return ItemDatabase.isSmithable((int) this.contentLong);
    }

    /**
     * Returns `true` if the Item is a cooking ingredient, else `false`.
     *
     * @return Whether the Item is a cooking ingredient
     */
    public boolean get_cookable() {
      return ItemDatabase.isCookable((int) this.contentLong);
    }

    /**
     * Returns `true` if the Item is a cocktailcrafting ingredient, else `false`.
     *
     * @return Whether the Item is a cocktailcrafting ingredient
     */
    public boolean get_mixable() {
      return ItemDatabase.isMixable((int) this.contentLong);
    }

    /**
     * Returns `true` if the Item is a candy, else `false`.
     *
     * @return Whether the Item is a candy
     */
    public boolean get_candy() {
      return ItemDatabase.isCandyItem((int) this.contentLong);
    }

    /**
     * Returns the candy type of the Item if it is a candy, or blank otherwise. Candy type can be
     * one of "simple", "complex" or "unspaded".
     *
     * @return The candy type
     */
    public String get_candy_type() {
      return CandyDatabase.getCandyType((int) this.contentLong);
    }

    /**
     * Returns `true` if the Item is a chocolate, else `false`.
     *
     * @return Whether the Item is a chocolate
     */
    public boolean get_chocolate() {
      return ItemDatabase.isChocolateItem((int) this.contentLong);
    }

    /**
     * Returns which Coinmaster sells this Item, if any.
     *
     * @return The coinmaster who sells this Item
     */
    public Value get_seller() {
      return DataTypes.makeCoinmasterValue(CoinmasterRegistry.findSeller((int) this.contentLong));
    }

    /**
     * Returns which Coinmaster buys this Item, if any.
     *
     * @return The Coinmaster who buys this Item
     */
    public Value get_buyer() {
      return DataTypes.makeCoinmasterValue(CoinmasterRegistry.findBuyer((int) this.contentLong));
    }

    /**
     * Returns the length of the Item's display name
     *
     * @return The length of the display name
     */
    public int get_name_length() {
      return ItemDatabase.getNameLength((int) this.contentLong);
    }

    /**
     * Returns the noob Skill granted by absorbing this Item.
     *
     * @return The noob Skill granted
     */
    public Value get_noob_skill() {
      return DataTypes.makeSkillValue(ItemDatabase.getNoobSkillId((int) this.contentLong), true);
    }
  }

  public static class FamiliarProxy extends ProxyRecordValue {
    public static RecordType _type =
        new RecordBuilder()
            .add("hatchling", DataTypes.ITEM_TYPE)
            .add("image", DataTypes.STRING_TYPE)
            .add("name", DataTypes.STRING_TYPE)
            .add("experience", DataTypes.INT_TYPE)
            .add("charges", DataTypes.INT_TYPE)
            .add("drop_name", DataTypes.STRING_TYPE)
            .add("drop_item", DataTypes.ITEM_TYPE)
            .add("drops_today", DataTypes.INT_TYPE)
            .add("drops_limit", DataTypes.INT_TYPE)
            .add("fights_today", DataTypes.INT_TYPE)
            .add("fights_limit", DataTypes.INT_TYPE)
            .add("combat", DataTypes.BOOLEAN_TYPE)
            .add("physical_damage", DataTypes.BOOLEAN_TYPE)
            .add("elemental_damage", DataTypes.BOOLEAN_TYPE)
            .add("block", DataTypes.BOOLEAN_TYPE)
            .add("delevel", DataTypes.BOOLEAN_TYPE)
            .add("hp_during_combat", DataTypes.BOOLEAN_TYPE)
            .add("mp_during_combat", DataTypes.BOOLEAN_TYPE)
            .add("other_action_during_combat", DataTypes.BOOLEAN_TYPE)
            .add("hp_after_combat", DataTypes.BOOLEAN_TYPE)
            .add("mp_after_combat", DataTypes.BOOLEAN_TYPE)
            .add("other_action_after_combat", DataTypes.BOOLEAN_TYPE)
            .add("passive", DataTypes.BOOLEAN_TYPE)
            .add("underwater", DataTypes.BOOLEAN_TYPE)
            .add("variable", DataTypes.BOOLEAN_TYPE)
            .add("attributes", DataTypes.STRING_TYPE)
            .add("poke_level", DataTypes.INT_TYPE)
            .add("poke_level_2_power", DataTypes.INT_TYPE)
            .add("poke_level_2_hp", DataTypes.INT_TYPE)
            .add("poke_level_3_power", DataTypes.INT_TYPE)
            .add("poke_level_3_hp", DataTypes.INT_TYPE)
            .add("poke_level_4_power", DataTypes.INT_TYPE)
            .add("poke_level_4_hp", DataTypes.INT_TYPE)
            .add("poke_move_1", DataTypes.STRING_TYPE)
            .add("poke_move_2", DataTypes.STRING_TYPE)
            .add("poke_move_3", DataTypes.STRING_TYPE)
            .add("poke_attribute", DataTypes.STRING_TYPE)
            .finish("familiar proxy");

    public FamiliarProxy(Value obj) {
      super(_type, obj);
    }

    public Value get_hatchling() {
      return DataTypes.makeItemValue(
          FamiliarDatabase.getFamiliarLarva((int) this.contentLong), true);
    }

    public String get_image() {
      return FamiliarDatabase.getFamiliarImageLocation((int) this.contentLong);
    }

    public String get_name() {
      FamiliarData fam = KoLCharacter.findFamiliar(this.contentString);
      return fam == null ? "" : fam.getName();
    }

    public int get_experience() {
      FamiliarData fam = KoLCharacter.findFamiliar(this.contentString);
      return fam == null ? 0 : fam.getTotalExperience();
    }

    public int get_charges() {
      FamiliarData fam = KoLCharacter.findFamiliar(this.contentString);
      return fam == null ? 0 : fam.getCharges();
    }

    public String get_drop_name() {
      String dropName = FamiliarData.dropName((int) this.contentLong);
      return dropName == null ? "" : dropName;
    }

    public Value get_drop_item() {
      AdventureResult item = FamiliarData.dropItem((int) this.contentLong);
      return DataTypes.makeItemValue(item == null ? -1 : item.getItemId(), true);
    }

    public int get_drops_today() {
      return FamiliarData.dropsToday((int) this.contentLong);
    }

    public int get_drops_limit() {
      return FamiliarData.dropDailyCap((int) this.contentLong);
    }

    public int get_fights_today() {
      return FamiliarData.fightsToday((int) this.contentLong);
    }

    public int get_fights_limit() {
      return FamiliarData.fightDailyCap((int) this.contentLong);
    }

    public boolean get_combat() {
      return FamiliarDatabase.isCombatType((int) this.contentLong);
    }

    public boolean get_physical_damage() {
      return FamiliarDatabase.isCombat0Type((int) this.contentLong);
    }

    public boolean get_elemental_damage() {
      return FamiliarDatabase.isCombat1Type((int) this.contentLong);
    }

    public boolean get_block() {
      return FamiliarDatabase.isBlockType((int) this.contentLong);
    }

    public boolean get_delevel() {
      return FamiliarDatabase.isDelevelType((int) this.contentLong);
    }

    public boolean get_hp_during_combat() {
      return FamiliarDatabase.isHp0Type((int) this.contentLong);
    }

    public boolean get_mp_during_combat() {
      return FamiliarDatabase.isMp0Type((int) this.contentLong);
    }

    public boolean get_other_action_during_combat() {
      return FamiliarDatabase.isOther0Type((int) this.contentLong);
    }

    public boolean get_hp_after_combat() {
      return FamiliarDatabase.isHp1Type((int) this.contentLong);
    }

    public boolean get_mp_after_combat() {
      return FamiliarDatabase.isMp1Type((int) this.contentLong);
    }

    public boolean get_other_action_after_combat() {
      return FamiliarDatabase.isOther1Type((int) this.contentLong);
    }

    public boolean get_passive() {
      return FamiliarDatabase.isPassiveType((int) this.contentLong);
    }

    public boolean get_underwater() {
      return FamiliarDatabase.isUnderwaterType((int) this.contentLong);
    }

    public boolean get_variable() {
      return FamiliarDatabase.isVariableType((int) this.contentLong);
    }

    public String get_attributes() {
      List<String> attrs = FamiliarDatabase.getFamiliarAttributes((int) this.contentLong);
      if (attrs == null) {
        return "";
      }
      StringBuilder builder = new StringBuilder();
      for (String attr : attrs) {
        if (builder.length() != 0) {
          builder.append("; ");
        }
        builder.append(attr);
      }
      return builder.toString();
    }

    public int get_poke_level() {
      FamiliarData fam = KoLCharacter.findFamiliar(this.contentString);
      return fam == null ? 0 : fam.getPokeLevel();
    }

    public int get_poke_level_2_power() {
      PokefamData data = FamiliarDatabase.getPokeDataById((int) this.contentLong);
      return data == null ? 0 : data.getPower2();
    }

    public int get_poke_level_2_hp() {
      PokefamData data = FamiliarDatabase.getPokeDataById((int) this.contentLong);
      return data == null ? 0 : data.getHP2();
    }

    public int get_poke_level_3_power() {
      PokefamData data = FamiliarDatabase.getPokeDataById((int) this.contentLong);
      return data == null ? 0 : data.getPower3();
    }

    public int get_poke_level_3_hp() {
      PokefamData data = FamiliarDatabase.getPokeDataById((int) this.contentLong);
      return data == null ? 0 : data.getHP3();
    }

    public int get_poke_level_4_power() {
      PokefamData data = FamiliarDatabase.getPokeDataById((int) this.contentLong);
      return data == null ? 0 : data.getPower4();
    }

    public int get_poke_level_4_hp() {
      PokefamData data = FamiliarDatabase.getPokeDataById((int) this.contentLong);
      return data == null ? 0 : data.getHP4();
    }

    public String get_poke_move_1() {
      PokefamData data = FamiliarDatabase.getPokeDataById((int) this.contentLong);
      return data == null ? "" : data.getMove1();
    }

    public String get_poke_move_2() {
      PokefamData data = FamiliarDatabase.getPokeDataById((int) this.contentLong);
      return data == null ? "" : data.getMove2();
    }

    public String get_poke_move_3() {
      PokefamData data = FamiliarDatabase.getPokeDataById((int) this.contentLong);
      return data == null ? "" : data.getMove3();
    }

    public String get_poke_attribute() {
      PokefamData data = FamiliarDatabase.getPokeDataById((int) this.contentLong);
      return data == null ? "" : data.getAttribute();
    }
  }

  public static class BountyProxy extends ProxyRecordValue {
    public static RecordType _type =
        new RecordBuilder()
            .add("plural", DataTypes.STRING_TYPE)
            .add("type", DataTypes.STRING_TYPE)
            .add("kol_internal_type", DataTypes.STRING_TYPE)
            .add("number", DataTypes.INT_TYPE)
            .add("image", DataTypes.STRING_TYPE)
            .add("monster", DataTypes.MONSTER_TYPE)
            .add("location", DataTypes.LOCATION_TYPE)
            .finish("bounty proxy");

    public BountyProxy(Value obj) {
      super(_type, obj);
    }

    public String get_plural() {
      String plural = BountyDatabase.getPlural(this.contentString);
      return plural == null ? "" : plural;
    }

    public String get_type() {
      String type = BountyDatabase.getType(this.contentString);
      return type == null ? "" : type;
    }

    public String get_kol_internal_type() {
      String type = BountyDatabase.getType(this.contentString);
      return type == null
          ? ""
          : type.equals("easy")
              ? "low"
              : type.equals("hard") ? "high" : type.equals("special") ? "special" : null;
    }

    public int get_number() {
      int number = BountyDatabase.getNumber(this.contentString);
      return number;
    }

    public String get_image() {
      String image = BountyDatabase.getImage(this.contentString);
      return image == null ? "" : image;
    }

    public Value get_monster() {
      String monster = BountyDatabase.getMonster(this.contentString);
      return DataTypes.parseMonsterValue(monster == null ? "" : monster, true);
    }

    public Value get_location() {
      String location = BountyDatabase.getLocation(this.contentString);
      return DataTypes.parseLocationValue(location == null ? "" : location, true);
    }
  }

  public static class ThrallProxy extends ProxyRecordValue {
    public static RecordType _type =
        new RecordBuilder()
            .add("id", DataTypes.INT_TYPE)
            .add("name", DataTypes.STRING_TYPE)
            .add("level", DataTypes.INT_TYPE)
            .add("image", DataTypes.STRING_TYPE)
            .add("tinyimage", DataTypes.STRING_TYPE)
            .add("skill", DataTypes.SKILL_TYPE)
            .add("current_modifiers", DataTypes.STRING_TYPE)
            .finish("thrall proxy");

    public ThrallProxy(Value obj) {
      super(_type, obj);
    }

    public int get_id() {
      Object[] data = (Object[]) this.content;
      return data == null ? 0 : PastaThrallData.dataToId(data);
    }

    public String get_name() {
      PastaThrallData thrall = KoLCharacter.findPastaThrall(this.contentString);
      return thrall == null ? "" : thrall.getName();
    }

    public int get_level() {
      PastaThrallData thrall = KoLCharacter.findPastaThrall(this.contentString);
      return thrall == null ? 0 : thrall.getLevel();
    }

    public String get_image() {
      Object[] data = (Object[]) this.content;
      return data == null ? "" : PastaThrallData.dataToImage(data);
    }

    public String get_tinyimage() {
      Object[] data = (Object[]) this.content;
      return data == null ? "" : PastaThrallData.dataToTinyImage(data);
    }

    public Value get_skill() {
      Object[] data = (Object[]) this.content;
      return DataTypes.makeSkillValue(data == null ? 0 : PastaThrallData.dataToSkillId(data), true);
    }

    public String get_current_modifiers() {
      PastaThrallData thrall = KoLCharacter.findPastaThrall(this.contentString);
      return thrall == null ? "" : thrall.getCurrentModifiers();
    }
  }

  public static class ServantProxy extends ProxyRecordValue {
    public static RecordType _type =
        new RecordBuilder()
            .add("id", DataTypes.INT_TYPE)
            .add("name", DataTypes.STRING_TYPE)
            .add("level", DataTypes.INT_TYPE)
            .add("experience", DataTypes.INT_TYPE)
            .add("image", DataTypes.STRING_TYPE)
            .add("level1_ability", DataTypes.STRING_TYPE)
            .add("level7_ability", DataTypes.STRING_TYPE)
            .add("level14_ability", DataTypes.STRING_TYPE)
            .add("level21_ability", DataTypes.STRING_TYPE)
            .finish("servant proxy");

    public ServantProxy(Value obj) {
      super(_type, obj);
    }

    public int get_id() {
      Object[] data = (Object[]) this.content;
      return data == null ? 0 : EdServantData.dataToId(data);
    }

    public String get_name() {
      EdServantData servant = EdServantData.findEdServant(this.contentString);
      return servant == null ? "" : servant.getName();
    }

    public int get_level() {
      EdServantData servant = EdServantData.findEdServant(this.contentString);
      return servant == null ? 0 : servant.getLevel();
    }

    public int get_experience() {
      EdServantData servant = EdServantData.findEdServant(this.contentString);
      return servant == null ? 0 : servant.getExperience();
    }

    public String get_image() {
      Object[] data = (Object[]) this.content;
      return data == null ? "" : EdServantData.dataToImage(data);
    }

    public String get_level1_ability() {
      Object[] data = (Object[]) this.content;
      return data == null ? "" : EdServantData.dataToLevel1Ability(data);
    }

    public String get_level7_ability() {
      Object[] data = (Object[]) this.content;
      return data == null ? "" : EdServantData.dataToLevel7Ability(data);
    }

    public String get_level14_ability() {
      Object[] data = (Object[]) this.content;
      return data == null ? "" : EdServantData.dataToLevel14Ability(data);
    }

    public String get_level21_ability() {
      Object[] data = (Object[]) this.content;
      return data == null ? "" : EdServantData.dataToLevel21Ability(data);
    }
  }

  public static class VykeaProxy extends ProxyRecordValue {
    public static RecordType _type =
        new RecordBuilder()
            .add("id", DataTypes.INT_TYPE)
            .add("name", DataTypes.STRING_TYPE)
            .add("type", DataTypes.INT_TYPE)
            .add("rune", DataTypes.ITEM_TYPE)
            .add("level", DataTypes.INT_TYPE)
            .add("image", DataTypes.STRING_TYPE)
            .add("modifiers", DataTypes.STRING_TYPE)
            .add("attack_element", DataTypes.ELEMENT_TYPE)
            .finish("vykea proxy");

    public VykeaProxy(Value obj) {
      super(_type, obj);
    }

    public int get_id() {
      return (int) this.contentLong;
    }

    public String get_name() {
      VYKEACompanionData companion = (VYKEACompanionData) this.content;
      return companion == null ? "" : companion.getName();
    }

    public String get_type() {
      VYKEACompanionData companion = (VYKEACompanionData) this.content;
      return companion == null ? "" : companion.typeToString();
    }

    public Value get_rune() {
      VYKEACompanionData companion = (VYKEACompanionData) this.content;
      return companion == null
          ? DataTypes.ITEM_INIT
          : DataTypes.makeItemValue(companion.getRune().getItemId(), true);
    }

    public int get_level() {
      VYKEACompanionData companion = (VYKEACompanionData) this.content;
      return companion == null ? 0 : companion.getLevel();
    }

    public String get_image() {
      VYKEACompanionData companion = (VYKEACompanionData) this.content;
      return companion == null ? "" : companion.getImage();
    }

    public String get_modifiers() {
      VYKEACompanionData companion = (VYKEACompanionData) this.content;
      return companion == null ? "" : companion.getModifiers();
    }

    public Value get_attack_element() {
      VYKEACompanionData companion = (VYKEACompanionData) this.content;
      return companion == null
          ? DataTypes.ELEMENT_INIT
          : DataTypes.makeElementValue(companion.getAttackElement(), true);
    }
  }

  public static class SkillProxy extends ProxyRecordValue {
    public static RecordType _type =
        new RecordBuilder()
            .add("name", DataTypes.STRING_TYPE)
            .add("type", DataTypes.STRING_TYPE)
            .add("level", DataTypes.INT_TYPE)
            .add("image", DataTypes.STRING_TYPE)
            .add("traincost", DataTypes.INT_TYPE)
            .add("class", DataTypes.CLASS_TYPE)
            .add("libram", DataTypes.BOOLEAN_TYPE)
            .add("passive", DataTypes.BOOLEAN_TYPE)
            .add("buff", DataTypes.BOOLEAN_TYPE)
            .add("combat", DataTypes.BOOLEAN_TYPE)
            .add("song", DataTypes.BOOLEAN_TYPE)
            .add("expression", DataTypes.BOOLEAN_TYPE)
            .add("walk", DataTypes.BOOLEAN_TYPE)
            .add("summon", DataTypes.BOOLEAN_TYPE)
            .add("permable", DataTypes.BOOLEAN_TYPE)
            .add("dailylimit", DataTypes.INT_TYPE)
            .add("timescast", DataTypes.INT_TYPE)
            .finish("skill proxy");

    public SkillProxy(Value obj) {
      super(_type, obj);
    }

    public String get_name() {
      return SkillDatabase.getSkillName((int) this.contentLong);
    }

    public String get_type() {
      return SkillDatabase.getSkillTypeName((int) this.contentLong);
    }

    public int get_level() {
      return SkillDatabase.getSkillLevel((int) this.contentLong);
    }

    public String get_image() {
      return SkillDatabase.getSkillImage((int) this.contentLong);
    }

    public int get_traincost() {
      return SkillDatabase.getSkillPurchaseCost((int) this.contentLong);
    }

    public Value get_class() {
      return DataTypes.parseClassValue(
          SkillDatabase.getSkillCategory((int) this.contentLong), true);
    }

    public boolean get_libram() {
      return SkillDatabase.isLibramSkill((int) this.contentLong);
    }

    public boolean get_passive() {
      return SkillDatabase.isPassive((int) this.contentLong);
    }

    public boolean get_buff() {
      return SkillDatabase.isBuff((int) this.contentLong);
    }

    public boolean get_combat() {
      return SkillDatabase.isCombat((int) this.contentLong);
    }

    public boolean get_song() {
      return SkillDatabase.isSong((int) this.contentLong);
    }

    public boolean get_expression() {
      return SkillDatabase.isExpression((int) this.contentLong);
    }

    public boolean get_walk() {
      return SkillDatabase.isWalk((int) this.contentLong);
    }

    public boolean get_summon() {
      return SkillDatabase.isSummon((int) this.contentLong);
    }

    public boolean get_permable() {
      return SkillDatabase.isPermable((int) this.contentLong);
    }

    public long get_dailylimit() {
      return SkillDatabase.getMaxCasts((int) this.contentLong);
    }

    public int get_timescast() {
      return SkillDatabase.getCasts((int) this.contentLong);
    }
  }

  public static class EffectProxy extends ProxyRecordValue {
    public static RecordType _type =
        new RecordBuilder()
            .add("name", DataTypes.STRING_TYPE)
            .add("default", DataTypes.STRING_TYPE)
            .add("note", DataTypes.STRING_TYPE)
            .add("all", new AggregateType(DataTypes.BOOLEAN_TYPE, DataTypes.STRING_TYPE))
            .add("image", DataTypes.STRING_TYPE)
            .add("descid", DataTypes.STRING_TYPE)
            .add("candy_tier", DataTypes.INT_TYPE)
            .add("quality", DataTypes.STRING_TYPE)
            .add("attributes", DataTypes.STRING_TYPE)
            .add("song", DataTypes.BOOLEAN_TYPE)
            .finish("effect proxy");

    public EffectProxy(Value obj) {
      super(_type, obj);
    }

    public String get_name() {
      return EffectDatabase.getEffectName((int) this.contentLong);
    }

    public String get_default() {
      return EffectDatabase.getDefaultAction((int) this.contentLong);
    }

    public String get_quality() {
      return EffectDatabase.getQualityDescription((int) this.contentLong);
    }

    public String get_attributes() {
      List<String> attrs = EffectDatabase.getEffectAttributes((int) this.contentLong);
      return (attrs == null) ? "" : String.join(",", attrs);
    }

    public String get_note() {
      return EffectDatabase.getActionNote((int) this.contentLong);
    }

    public Value get_all() {
      ArrayList<Value> rv = new ArrayList<Value>();
      Iterator<String> i = EffectDatabase.getAllActions((int) this.contentLong);
      while (i.hasNext()) {
        rv.add(new Value(i.next()));
      }
      return new PluralValue(DataTypes.STRING_TYPE, rv);
    }

    public String get_image() {
      return EffectDatabase.getImageName((int) this.contentLong);
    }

    public String get_descid() {
      return EffectDatabase.getDescriptionId((int) this.contentLong);
    }

    public int get_candy_tier() {
      return CandyDatabase.getEffectTier((int) this.contentLong);
    }

    public boolean get_song() {
      return EffectDatabase.isSong((int) this.contentLong);
    }
  }

  public static class LocationProxy extends ProxyRecordValue {
    public static RecordType _type =
        new RecordBuilder()
            .add("id", DataTypes.BOOLEAN_TYPE)
            .add("nocombats", DataTypes.BOOLEAN_TYPE)
            .add("combat_percent", DataTypes.FLOAT_TYPE)
            .add("zone", DataTypes.STRING_TYPE)
            .add("parent", DataTypes.STRING_TYPE)
            .add("parentdesc", DataTypes.STRING_TYPE)
            .add("environment", DataTypes.STRING_TYPE)
            .add("fire_level", DataTypes.INT_TYPE)
            .add("bounty", DataTypes.BOUNTY_TYPE)
            .add("combat_queue", DataTypes.STRING_TYPE)
            .add("noncombat_queue", DataTypes.STRING_TYPE)
            .add("turns_spent", DataTypes.INT_TYPE)
            .add("kisses", DataTypes.INT_TYPE)
            .add("recommended_stat", DataTypes.INT_TYPE)
            .add("water_level", DataTypes.INT_TYPE)
            .add("wanderers", DataTypes.BOOLEAN_TYPE)
            .finish("location proxy");

    public LocationProxy(Value obj) {
      super(_type, obj);
    }

    public int get_id() {
      return this.content != null ? ((KoLAdventure) this.content).getSnarfblat() : -1;
    }

    public boolean get_nocombats() {
      return this.content != null ? ((KoLAdventure) this.content).isNonCombatsOnly() : false;
    }

    public double get_combat_percent() {
      if (this.content == null) {
        return 0;
      }

      AreaCombatData area = ((KoLAdventure) this.content).getAreaSummary();

      if (area == null) {
        return 0;
      }

      return area.areaCombatPercent();
    }

    public String get_zone() {
      return this.content != null ? ((KoLAdventure) this.content).getZone() : "";
    }

    public String get_parent() {
      return this.content != null ? ((KoLAdventure) this.content).getParentZone() : "";
    }

    public String get_parentdesc() {
      return this.content != null ? ((KoLAdventure) this.content).getParentZoneDescription() : "";
    }

    public String get_environment() {
      return this.content != null ? ((KoLAdventure) this.content).getEnvironment() : "";
    }

    public Value get_bounty() {
      if (this.content == null) {
        return DataTypes.BOUNTY_INIT;
      }
      AdventureResult bounty = AdventureDatabase.getBounty((KoLAdventure) this.content);
      return bounty == null
          ? DataTypes.BOUNTY_INIT
          : DataTypes.parseBountyValue(bounty.getName(), true);
    }

    public String get_combat_queue() {
      if (this.content == null) {
        return "";
      }

      List<?> zoneQueue = AdventureQueueDatabase.getZoneQueue((KoLAdventure) this.content);
      if (zoneQueue == null) {
        return "";
      }

      StringBuilder builder = new StringBuilder();
      for (Object ob : zoneQueue) {
        if (ob == null) continue;

        if (builder.length() > 0) builder.append("; ");

        builder.append(ob.toString());
      }

      return builder.toString();
    }

    public String get_noncombat_queue() {
      if (this.content == null) {
        return "";
      }

      List<?> zoneQueue = AdventureQueueDatabase.getZoneNoncombatQueue((KoLAdventure) this.content);
      if (zoneQueue == null) {
        return "";
      }

      StringBuilder builder = new StringBuilder();
      for (Object ob : zoneQueue) {
        if (ob == null) continue;

        if (builder.length() > 0) builder.append("; ");

        builder.append(ob.toString());
      }

      return builder.toString();
    }

    public int get_turns_spent() {
      return this.content != null
          ? AdventureSpentDatabase.getTurns((KoLAdventure) this.content, true)
          : 0;
    }

    public int get_kisses() {
      return this.content != null ? FightRequest.dreadKisses((KoLAdventure) this.content) : 0;
    }

    public int get_recommended_stat() {
      return this.content != null ? ((KoLAdventure) this.content).getRecommendedStat() : 0;
    }

    public int get_water_level() {
      return this.content == null
          ? KoLCharacter.inRaincore() ? ((KoLAdventure) this.content).getWaterLevel() : 0
          : 0;
    }

    public boolean get_wanderers() {
      return this.content != null ? ((KoLAdventure) this.content).hasWanderers() : false;
    }

    public int get_fire_level() {
      if (this.content == null || !KoLCharacter.inFirecore()) {
        return 0;
      }

      return WildfireCampRequest.getFireLevel((KoLAdventure) this.content);
    }
  }

  public static class MonsterProxy extends ProxyRecordValue {
    public static RecordType _type =
        new RecordBuilder()
            .add("name", DataTypes.STRING_TYPE)
            .add("id", DataTypes.INT_TYPE)
            .add("base_hp", DataTypes.INT_TYPE)
            .add("base_attack", DataTypes.INT_TYPE)
            .add("base_defense", DataTypes.INT_TYPE)
            .add("raw_hp", DataTypes.INT_TYPE)
            .add("raw_attack", DataTypes.INT_TYPE)
            .add("raw_defense", DataTypes.INT_TYPE)
            .add("base_initiative", DataTypes.INT_TYPE)
            .add("raw_initiative", DataTypes.INT_TYPE)
            .add("attack_element", DataTypes.ELEMENT_TYPE)
            .add("defense_element", DataTypes.ELEMENT_TYPE)
            .add("physical_resistance", DataTypes.INT_TYPE)
            .add("min_meat", DataTypes.INT_TYPE)
            .add("max_meat", DataTypes.INT_TYPE)
            .add("min_sprinkles", DataTypes.INT_TYPE)
            .add("max_sprinkles", DataTypes.INT_TYPE)
            .add("base_mainstat_exp", DataTypes.FLOAT_TYPE)
            .add("phylum", DataTypes.PHYLUM_TYPE)
            .add("poison", DataTypes.EFFECT_TYPE)
            .add("boss", DataTypes.BOOLEAN_TYPE)
            .add("copyable", DataTypes.BOOLEAN_TYPE)
            .add("image", DataTypes.STRING_TYPE)
            .add("images", new AggregateType(DataTypes.BOOLEAN_TYPE, DataTypes.STRING_TYPE))
            .add("sub_types", new AggregateType(DataTypes.BOOLEAN_TYPE, DataTypes.STRING_TYPE))
            .add(
                "random_modifiers",
                new AggregateType(DataTypes.BOOLEAN_TYPE, DataTypes.STRING_TYPE))
            .add("manuel_name", DataTypes.STRING_TYPE)
            .add("wiki_name", DataTypes.STRING_TYPE)
            .add("attributes", DataTypes.STRING_TYPE)
            .finish("monster proxy");

    public MonsterProxy(Value obj) {
      super(_type, obj);
    }

    public String get_name() {
      return this.content != null ? MonsterDatabase.getMonsterName((int) this.contentLong) : "";
    }

    public int get_id() {
      return this.content != null ? ((MonsterData) this.content).getId() : 0;
    }

    public int get_base_hp() {
      return this.content != null ? ((MonsterData) this.content).getHP() : 0;
    }

    public int get_base_attack() {
      return this.content != null ? ((MonsterData) this.content).getAttack() : 0;
    }

    public int get_raw_hp() {
      return this.content != null ? ((MonsterData) this.content).getRawHP() : 0;
    }

    public int get_raw_attack() {
      return this.content != null ? ((MonsterData) this.content).getRawAttack() : 0;
    }

    public int get_raw_defense() {
      return this.content != null ? ((MonsterData) this.content).getRawDefense() : 0;
    }

    public int get_base_defense() {
      return this.content != null ? ((MonsterData) this.content).getDefense() : 0;
    }

    public int get_base_initiative() {
      return this.content != null ? ((MonsterData) this.content).getInitiative() : 0;
    }

    public int get_raw_initiative() {
      return this.content != null ? ((MonsterData) this.content).getRawInitiative() : 0;
    }

    public Value get_attack_element() {
      return this.content != null
          ? DataTypes.parseElementValue(
              ((MonsterData) this.content).getAttackElement().toString(), true)
          : DataTypes.ELEMENT_INIT;
    }

    public Value get_defense_element() {
      return this.content != null
          ? DataTypes.parseElementValue(
              ((MonsterData) this.content).getDefenseElement().toString(), true)
          : DataTypes.ELEMENT_INIT;
    }

    public int get_physical_resistance() {
      return this.content != null ? ((MonsterData) this.content).getPhysicalResistance() : 0;
    }

    public int get_min_meat() {
      return this.content != null ? ((MonsterData) this.content).getMinMeat() : 0;
    }

    public int get_max_meat() {
      return this.content != null ? ((MonsterData) this.content).getMaxMeat() : 0;
    }

    public int get_min_sprinkles() {
      return this.content != null ? ((MonsterData) this.content).getMinSprinkles() : 0;
    }

    public int get_max_sprinkles() {
      return this.content != null ? ((MonsterData) this.content).getMaxSprinkles() : 0;
    }

    public double get_base_mainstat_exp() {
      return this.content != null ? ((MonsterData) this.content).getExperience() : 0;
    }

    public Value get_phylum() {
      return this.content != null
          ? DataTypes.parsePhylumValue(((MonsterData) this.content).getPhylum().toString(), true)
          : DataTypes.PHYLUM_INIT;
    }

    public Value get_poison() {
      if (this.content == null) {
        return DataTypes.EFFECT_INIT;
      }
      int poisonLevel = ((MonsterData) this.content).getPoison();
      String poisonName =
          poisonLevel == Integer.MAX_VALUE
              ? "none"
              : EffectDatabase.getEffectName(EffectDatabase.POISON_ID[poisonLevel]);
      return DataTypes.parseEffectValue(poisonName, true);
    }

    public boolean get_boss() {
      return this.content != null ? ((MonsterData) this.content).isBoss() : false;
    }

    public boolean get_copyable() {
      return this.content != null ? !(((MonsterData) this.content).isNoCopy()) : false;
    }

    public String get_image() {
      return this.content != null ? ((MonsterData) this.content).getImage() : "";
    }

    public Value get_images() {
      if (this.content == null) {
        return new PluralValue(DataTypes.STRING_TYPE, new ArrayList<>());
      }
      ArrayList<Value> rv = new ArrayList<Value>();
      for (String image : ((MonsterData) this.content).getImages()) {
        rv.add(new Value(image));
      }
      return new PluralValue(DataTypes.STRING_TYPE, rv);
    }

    public Value get_random_modifiers() {
      if (this.content == null) {
        return new PluralValue(DataTypes.STRING_TYPE, new ArrayList<>());
      }
      ArrayList<Value> rv = new ArrayList<Value>();
      for (String attribute : ((MonsterData) this.content).getRandomModifiers()) {
        rv.add(new Value(attribute));
      }
      return new PluralValue(DataTypes.STRING_TYPE, rv);
    }

    public Value get_sub_types() {
      if (this.content == null) {
        return new PluralValue(DataTypes.STRING_TYPE, new ArrayList<>());
      }
      ArrayList<Value> rv = new ArrayList<Value>();
      for (String attribute : ((MonsterData) this.content).getSubTypes()) {
        rv.add(new Value(attribute));
      }
      return new PluralValue(DataTypes.STRING_TYPE, rv);
    }

    public String get_manuel_name() {
      return this.content != null ? ((MonsterData) this.content).getManuelName() : "";
    }

    public String get_wiki_name() {
      return this.content != null ? ((MonsterData) this.content).getWikiName() : "";
    }

    public String get_attributes() {
      return this.content != null ? ((MonsterData) this.content).getAttributes() : "";
    }
  }

  public static class CoinmasterProxy extends ProxyRecordValue {
    public static RecordType _type =
        new RecordBuilder()
            .add("token", DataTypes.STRING_TYPE)
            .add("item", DataTypes.ITEM_TYPE)
            .add("property", DataTypes.STRING_TYPE)
            .add("available_tokens", DataTypes.INT_TYPE)
            .add("buys", DataTypes.BOOLEAN_TYPE)
            .add("sells", DataTypes.BOOLEAN_TYPE)
            .add("nickname", DataTypes.STRING_TYPE)
            .finish("coinmaster proxy");

    public CoinmasterProxy(Value obj) {
      super(_type, obj);
    }

    public String get_token() {
      return this.content != null ? ((CoinmasterData) this.content).getToken() : "";
    }

    public Value get_item() {
      if (this.content == null) {
        return DataTypes.ITEM_INIT;
      }
      CoinmasterData data = ((CoinmasterData) this.content);
      AdventureResult item = data.getItem();
      return item == null ? DataTypes.ITEM_INIT : DataTypes.makeItemValue(item.getItemId(), true);
    }

    public String get_property() {
      return this.content != null ? ((CoinmasterData) this.content).getProperty() : "";
    }

    public int get_available_tokens() {
      return this.content != null ? ((CoinmasterData) this.content).availableTokens() : 0;
    }

    public boolean get_buys() {
      return this.content != null ? ((CoinmasterData) this.content).getSellAction() != null : false;
    }

    public boolean get_sells() {
      return this.content != null ? ((CoinmasterData) this.content).getBuyAction() != null : false;
    }

    public String get_nickname() {
      return this.content != null ? ((CoinmasterData) this.content).getNickname() : "";
    }
  }

  public static class ElementProxy extends ProxyRecordValue {
    public static RecordType _type =
        new RecordBuilder().add("image", DataTypes.STRING_TYPE).finish("element proxy");

    public ElementProxy(Value obj) {
      super(_type, obj);
    }

    public String get_image() {
      switch ((Element) this.content) {
        case NONE:
          return "circle.gif";
        case COLD:
          return "snowflake.gif";
        case HOT:
          return "fire.gif";
        case SLEAZE:
          return "wink.gif";
        case SPOOKY:
          return "skull.gif";
        case STENCH:
          return "stench.gif";
          // No image for Slime or Supercold in Manuel
        case SLIME:
          return "circle.gif";
        case SUPERCOLD:
          return "circle.gif";
      }
      return "";
    }
  }

  public static class PhylumProxy extends ProxyRecordValue {
    public static RecordType _type =
        new RecordBuilder().add("image", DataTypes.STRING_TYPE).finish("phylum proxy");

    public PhylumProxy(Value obj) {
      super(_type, obj);
    }

    public String get_image() {
      Phylum phylum = (Phylum) this.content;
      if (phylum == null || phylum.equals(Phylum.NONE)) {
        return "";
      }

      return phylum.getImage();
    }
  }

  public static class StatProxy extends ProxyRecordValue {
    public static RecordType _type = new RecordBuilder().finish("stat proxy");

    public StatProxy(Value obj) {
      super(_type, obj);
    }
  }

  public static class SlotProxy extends ProxyRecordValue {
    public static RecordType _type = new RecordBuilder().finish("slot proxy");

    public SlotProxy(Value obj) {
      super(_type, obj);
    }
  }
}
