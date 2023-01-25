package net.sourceforge.kolmafia.modifiers;

import java.util.EnumSet;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.utilities.IntOrString;

public class Lookup {

  public ModifierType type;
  private IntOrString key; // int for Skill, Item, Effect; String otherwise.

  public Lookup(ModifierType type, IntOrString key) {
    this.type = type;
    this.key = key;
  }

  public Lookup(ModifierType type, int key) {
    this.type = type;
    this.key = new IntOrString(key);
  }

  public Lookup(ModifierType type, String name) {
    this.type = type;
    this.key =
        switch (type) {
          case ITEM -> new IntOrString(ItemDatabase.getExactItemId(name));
          case EFFECT -> new IntOrString(EffectDatabase.getEffectId(name, true));
          case SKILL -> new IntOrString(SkillDatabase.getSkillId(name, true));
          default -> new IntOrString(name);
        };
    if (EnumSet.of(ModifierType.ITEM, ModifierType.EFFECT, ModifierType.SKILL).contains(type)
        && this.key.getIntValue() == -1) {
      this.type = ModifierType.fromString("PSEUDO_" + type.name());
      this.key = new IntOrString(name);
    }
  }

  @Override
  public String toString() {
    return switch (this.type) {
      case ITEM, EFFECT, SKILL -> this.type.pascalCaseName() + ":[" + this.key + "]";
      default -> this.type.pascalCaseName() + ":" + this.key;
    };
  }

  public IntOrString getKey() {
    return this.key;
  }

  public int getIntKey() {
    return this.key.getIntValue();
  }

  public String getStringKey() {
    return this.key.getStringValue();
  }

  public String getName() {
    return switch (type) {
      case ITEM -> getIntKey() < -1
          ? ClanLoungeRequest.hotdogIdToName(getIntKey())
          : ItemDatabase.getItemName(getIntKey());
      case EFFECT -> EffectDatabase.getEffectName(getIntKey());
      case SKILL -> SkillDatabase.getSkillName(getIntKey());
      default -> getStringKey();
    };
  }
}
