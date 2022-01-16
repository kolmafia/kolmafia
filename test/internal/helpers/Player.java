package internal.helpers;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.EquipmentRequirement;

public class Player {
  public static void equip(int slot, String item) {
    EquipmentManager.setEquipment(slot, AdventureResult.tallyItem(item));
  }

  public static void addItem(String item) {
    addItem(item, 1);
  }

  public static void addItem(String item, int count) {
    AdventureResult parsed = AdventureResult.tallyItem(item);
    for (int i = 0; i < count; i++) {
      AdventureResult.addResultToList(KoLConstants.inventory, parsed);
    }
  }

  public static void canUse(String item) {
    canUse(item, 1);
  }

  public static void canUse(String item, int count) {
    addItem(item, count);
    canEquip(item);
  }

  public static void hasFamiliar(int famId) {
    KoLCharacter.familiars.add(FamiliarData.registerFamiliar(famId, 0));
  }

  public static void addEffect(String effect) {
    KoLConstants.activeEffects.add(EffectPool.get(EffectDatabase.getEffectId(effect)));
  }

  public static void addSkill(String skill) {
    KoLCharacter.addAvailableSkill(skill);
  }

  public static void canEquip(String item) {
    int id = ItemDatabase.getItemId(item);
    String requirement = EquipmentDatabase.getEquipRequirement(id);
    EquipmentRequirement req = new EquipmentRequirement(requirement);

    setStats(
        Math.max(req.isMuscle() ? req.getAmount() : 0, KoLCharacter.getBaseMuscle()),
        Math.max(req.isMysticality() ? req.getAmount() : 0, KoLCharacter.getBaseMysticality()),
        Math.max(req.isMoxie() ? req.getAmount() : 0, KoLCharacter.getBaseMoxie()));
  }

  public static void setStats(int muscle, int mysticality, int moxie) {
    KoLCharacter.setStatPoints(
        muscle,
        (long) muscle * muscle,
        mysticality,
        (long) mysticality * mysticality,
        moxie,
        (long) moxie * moxie);
    KoLCharacter.recalculateAdjustments();
  }

  public static void inPath(Path path) {
    KoLCharacter.setPath(path);
  }
}
