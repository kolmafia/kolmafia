package internal.helpers;

import static org.mockito.Mockito.mockStatic;

import java.util.Calendar;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.EquipmentRequirement;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.mockito.Mockito;

public class Player {
  public static Cleanups equip(int slot, String item) {
    var cleanups = new Cleanups();
    EquipmentManager.setEquipment(slot, AdventureResult.tallyItem(item));
    cleanups.add(() -> EquipmentManager.setEquipment(slot, EquipmentRequest.UNEQUIP));
    return cleanups;
  }

  public static Cleanups addItem(String item) {
    return addItem(item, 1);
  }

  public static Cleanups addItem(String item, int count) {
    var cleanups = new Cleanups();
    AdventureResult parsed = AdventureResult.tallyItem(item);
    for (int i = 0; i < count; i++) {
      AdventureResult.addResultToList(KoLConstants.inventory, parsed);
      cleanups.add(() -> AdventureResult.removeResultFromList(KoLConstants.inventory, parsed));
    }
    return cleanups;
  }

  public static void addItem(int itemId) {
    addItem(itemId, 1);
  }

  public static void addItem(int itemId, int count) {
    AdventureResult.addResultToList(KoLConstants.inventory, ItemPool.get(itemId));
  }

  public static int countItem(int itemId) {
    return InventoryManager.getCount(itemId);
  }

  public static int countItem(String item) {
    AdventureResult parsed = AdventureResult.tallyItem(item);
    return InventoryManager.getCount(parsed);
  }

  public static Cleanups canUse(String item) {
    return canUse(item, 1);
  }

  public static Cleanups canUse(String item, int count) {
    var cleanups = new Cleanups();
    cleanups.add(addItem(item, count));
    canEquip(item);
    cleanups.add(() -> setStats(0, 0, 0));
    return cleanups;
  }

  public static Cleanups hasFamiliar(int famId) {
    var familiar = FamiliarData.registerFamiliar(famId, 0);
    KoLCharacter.familiars.add(familiar);
    return new Cleanups(() -> KoLCharacter.familiars.remove(familiar));
  }

  public static Cleanups setFamiliar(int famId) {
    var cleanups = new Cleanups();
    KoLCharacter.setFamiliar(FamiliarData.registerFamiliar(famId, 0));
    cleanups.add(() -> KoLCharacter.setFamiliar(FamiliarData.NO_FAMILIAR));
    return cleanups;
  }

  public static Cleanups addEffect(String effectName, int turns) {
    var effect = EffectPool.get(EffectDatabase.getEffectId(effectName), turns);
    KoLConstants.activeEffects.add(effect);
    return new Cleanups(() -> KoLConstants.activeEffects.remove(effect));
  }

  public static Cleanups addEffect(String effect) {
    return addEffect(effect, 1);
  }

  public static void addIntrinsic(String effect) {
    addEffect(effect, Integer.MAX_VALUE);
  }

  public static Cleanups addSkill(String skill) {
    KoLCharacter.addAvailableSkill(skill);
    return new Cleanups(() -> KoLCharacter.removeAvailableSkill(skill));
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

  public static void isClass(AscensionClass ascensionClass) {
    KoLCharacter.setAscensionClass(ascensionClass);
  }

  public static Cleanups isSign(String sign) {
    KoLCharacter.setSign(sign);
    return new Cleanups(() -> isSign(ZodiacSign.NONE));
  }

  public static Cleanups isSign(ZodiacSign sign) {
    KoLCharacter.setSign(sign);
    return new Cleanups(() -> isSign(ZodiacSign.NONE));
  }

  public static Cleanups inPath(Path path) {
    KoLCharacter.setPath(path);
    return new Cleanups(() -> inPath(Path.NONE));
  }

  public static Cleanups inLocation(String location) {
    Modifiers.setLocation(AdventureDatabase.getAdventure(location));
    return new Cleanups(() -> inLocation(null));
  }

  public static Cleanups isDay(Calendar cal) {
    var mocked = mockStatic(HolidayDatabase.class, Mockito.CALLS_REAL_METHODS);
    mocked.when(HolidayDatabase::getDate).thenReturn(cal.getTime());
    return new Cleanups(mocked::close);
  }

  public static Cleanups usedAbsorbs(int absorbs) {
    KoLCharacter.setAbsorbs(absorbs);
    return new Cleanups(() -> usedAbsorbs(0));
  }
}
