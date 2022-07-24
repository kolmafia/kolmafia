package internal.helpers;

import static org.mockito.Mockito.mockStatic;

import internal.network.FakeHttpClientBuilder;
import java.util.Calendar;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.BasementRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.ChoiceControl;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.EquipmentRequirement;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.HttpUtilities;
import org.mockito.Mockito;

public class Player {
  public static Cleanups equip(int slot, String item) {
    var cleanups = new Cleanups();
    EquipmentManager.setEquipment(
        slot, item == null ? EquipmentRequest.UNEQUIP : AdventureResult.tallyItem(item));
    cleanups.add(() -> EquipmentManager.setEquipment(slot, EquipmentRequest.UNEQUIP));
    return cleanups;
  }

  public static Cleanups equip(int slot, int itemId) {
    var cleanups = new Cleanups();
    EquipmentManager.setEquipment(
        slot, itemId == -1 ? EquipmentRequest.UNEQUIP : ItemPool.get(itemId));
    cleanups.add(() -> EquipmentManager.setEquipment(slot, EquipmentRequest.UNEQUIP));
    return cleanups;
  }

  public static Cleanups addItem(String name) {
    return addItem(name, 1);
  }

  public static Cleanups addItem(String name, int count) {
    int itemId = ItemDatabase.getItemId(name, count, false);
    return addItem(ItemPool.get(itemId, count));
  }

  public static Cleanups addItem(int itemId) {
    return addItem(itemId, 1);
  }

  public static Cleanups addItem(int itemId, int count) {
    return addItem(ItemPool.get(itemId, count));
  }

  public static Cleanups addItem(AdventureResult item) {
    return addToList(item, KoLConstants.inventory);
  }

  public static Cleanups addItemToCloset(String name) {
    int count = 1;
    int itemId = ItemDatabase.getItemId(name, count, false);
    AdventureResult item = ItemPool.get(itemId, count);
    return addToList(item, KoLConstants.closet);
  }

  public static Cleanups addItemToStash(String name) {
    int count = 1;
    int itemId = ItemDatabase.getItemId(name, count, false);
    AdventureResult item = ItemPool.get(itemId, count);
    return addToList(item, ClanManager.getStash());
  }

  private static Cleanups addToList(AdventureResult item, List<AdventureResult> list) {
    var cleanups = new Cleanups();
    AdventureResult.addResultToList(list, item);
    // Per midgleyc: "All the cleanups I wrote assume you're reverting back to a reset character"
    // Therefore, simply remove this item from inventory.
    cleanups.add(() -> AdventureResult.removeResultFromList(list, item));
    return cleanups;
  }

  public static Cleanups setMeat(long meat) {
    var oldMeat = KoLCharacter.getAvailableMeat();
    KoLCharacter.setAvailableMeat(meat);
    return new Cleanups(() -> KoLCharacter.setAvailableMeat(oldMeat));
  }

  public static Cleanups setClosetMeat(long meat) {
    var oldMeat = KoLCharacter.getClosetMeat();
    KoLCharacter.setClosetMeat(meat);
    return new Cleanups(() -> KoLCharacter.setClosetMeat(oldMeat));
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
    KoLCharacter.addFamiliar(familiar);
    return new Cleanups(() -> KoLCharacter.removeFamiliar(familiar));
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

  public static Cleanups setStats(int muscle, int mysticality, int moxie) {
    KoLCharacter.setStatPoints(
        muscle,
        (long) muscle * muscle,
        mysticality,
        (long) mysticality * mysticality,
        moxie,
        (long) moxie * moxie);
    KoLCharacter.recalculateAdjustments();
    return new Cleanups(() -> setStats(0, 0, 0));
  }

  public static Cleanups isLevel(int level) {
    int substats = (int) Math.pow(level, 2) - level * 2 + 5;
    return setStats(substats, substats, substats);
  }

  public static Cleanups setHP(long current, long maximum, long base) {
    KoLCharacter.setHP(current, maximum, base);
    KoLCharacter.recalculateAdjustments();
    return new Cleanups(() -> setHP(0, 0, 0));
  }

  public static Cleanups setMP(long current, long maximum, long base) {
    KoLCharacter.setMP(current, maximum, base);
    KoLCharacter.recalculateAdjustments();
    return new Cleanups(() -> setMP(0, 0, 0));
  }

  public static Cleanups withAdventuresLeft(int adventures) {
    KoLCharacter.setAdventuresLeft(adventures);
    return new Cleanups(() -> withAdventuresLeft(0));
  }

  public static Cleanups isClass(AscensionClass ascensionClass) {
    var old = KoLCharacter.getAscensionClass();
    KoLCharacter.setAscensionClass(ascensionClass);
    return new Cleanups(() -> isClass(old));
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

  public static Cleanups inAnapest() {
    FightRequest.anapest = true;
    return new Cleanups(() -> FightRequest.anapest = false);
  }

  public static Cleanups fightingMonster(MonsterData monster) {
    var previousMonster = MonsterStatusTracker.getLastMonster();
    MonsterStatusTracker.setNextMonster(monster);
    return new Cleanups(() -> MonsterStatusTracker.setNextMonster(previousMonster));
  }

  public static Cleanups fightingMonster(String monsterName) {
    return fightingMonster(MonsterDatabase.findMonster(monsterName));
  }

  public static Cleanups isDay(Calendar cal) {
    var mocked = mockStatic(HolidayDatabase.class, Mockito.CALLS_REAL_METHODS);
    mocked.when(HolidayDatabase::getDate).thenReturn(cal.getTime());
    mocked.when(HolidayDatabase::getCalendar).thenReturn(cal);
    mocked.when(HolidayDatabase::getKoLCalendar).thenReturn(cal);
    return new Cleanups(mocked::close);
  }

  public static Cleanups usedAbsorbs(int absorbs) {
    KoLCharacter.setAbsorbs(absorbs);
    return new Cleanups(() -> usedAbsorbs(0));
  }

  public static Cleanups isHardcore() {
    return isHardcore(true);
  }

  public static Cleanups isHardcore(boolean hardcore) {
    var wasHardcore = KoLCharacter.isHardcore();
    KoLCharacter.setHardcore(hardcore);
    return new Cleanups(() -> isHardcore(wasHardcore));
  }

  public static Cleanups addCampgroundItem(int id) {
    CampgroundRequest.setCampgroundItem(id, 1);
    return new Cleanups(() -> CampgroundRequest.removeCampgroundItem(ItemPool.get(id, 1)));
  }

  public static Cleanups setWorkshed(int id) {
    CampgroundRequest.setCurrentWorkshedItem(id);
    return new Cleanups(CampgroundRequest::resetCurrentWorkshedItem);
  }

  public static Cleanups hasRange() {
    KoLCharacter.setRange(true);
    ConcoctionDatabase.refreshConcoctions();
    return new Cleanups(
        () -> {
          KoLCharacter.setRange(false);
          ConcoctionDatabase.refreshConcoctions();
        });
  }

  public static Cleanups setProperty(String key, int value) {
    var oldValue = Preferences.getInteger(key);
    Preferences.setInteger(key, value);
    return new Cleanups(() -> Preferences.setInteger(key, oldValue));
  }

  public static Cleanups setProperty(String key, String value) {
    var oldValue = Preferences.getString(key);
    Preferences.setString(key, value);
    return new Cleanups(() -> Preferences.setString(key, oldValue));
  }

  public static Cleanups setProperty(String key, Boolean value) {
    var oldValue = Preferences.getBoolean(key);
    Preferences.setBoolean(key, value);
    return new Cleanups(() -> Preferences.setBoolean(key, oldValue));
  }

  public static Cleanups setQuest(QuestDatabase.Quest quest, String value) {
    var oldValue = QuestDatabase.getQuest(quest);
    QuestDatabase.setQuest(quest, value);
    return new Cleanups(() -> QuestDatabase.setQuest(quest, oldValue));
  }

  public static Cleanups setQuest(QuestDatabase.Quest quest, int step) {
    return setQuest(quest, "step" + step);
  }

  public static Cleanups setupFakeResponse(int code, String response) {
    var builder = new FakeHttpClientBuilder();
    HttpUtilities.setClientBuilder(() -> builder);
    GenericRequest.resetClient();
    GenericRequest.sessionId = "TEST"; // we fake the client, so "run" the requests
    builder.client.setResponse(code, response);

    return new Cleanups(
        () -> {
          GenericRequest.sessionId = null;
          HttpUtilities.setClientBuilder(FakeHttpClientBuilder::new);
          GenericRequest.resetClient();
        });
  }

  public static Cleanups withPostChoice2(int choice, int decision, String responseText) {
    ChoiceManager.lastChoice = choice;
    ChoiceManager.lastDecision = decision;
    var req = new GenericRequest("choice.php?choice=" + choice + "&option=" + decision);
    req.responseText = responseText;
    ChoiceControl.postChoice2("choice.php?choice=" + choice + "&option=" + decision, req);

    return new Cleanups(
        () -> {
          ChoiceManager.lastChoice = 0;
          ChoiceManager.lastDecision = 0;
        });
  }

  public static Cleanups withPostChoice2(int choice, int decision) {
    return withPostChoice2(choice, decision, "");
  }

  public static Cleanups withLastLocationName(final String lastLocationName) {
    var old = KoLAdventure.lastLocationName;
    KoLAdventure.lastLocationName = lastLocationName;
    return new Cleanups(
        () -> {
          KoLAdventure.lastLocationName = old;
        });
  }

  public static Cleanups withMultiFight() {
    var old = FightRequest.inMultiFight;
    FightRequest.inMultiFight = true;
    return new Cleanups(
        () -> {
          FightRequest.inMultiFight = old;
        });
  }

  public static Cleanups withItemMonster(final String itemMonster) {
    var old = GenericRequest.itemMonster;
    GenericRequest.itemMonster = itemMonster;
    return new Cleanups(
        () -> {
          GenericRequest.itemMonster = old;
        });
  }

  public static Cleanups canInteract(boolean canInteract) {
    var old = CharPaneRequest.canInteract();
    CharPaneRequest.setCanInteract(canInteract);
    return new Cleanups(() -> CharPaneRequest.setCanInteract(old));
  }

  public static Cleanups withBasementLevel(final int level) {
    var old = BasementRequest.getBasementLevel();
    BasementRequest.setBasementLevel(level);
    return new Cleanups(() -> BasementRequest.setBasementLevel(old));
  }

  public static Cleanups withBasementLevel() {
    return withBasementLevel(0);
  }

  public static Cleanups withContinuationState(final KoLConstants.MafiaState continuationState) {
    var old = StaticEntity.getContinuationState();
    StaticEntity.setContinuationState(continuationState);
    return new Cleanups(() -> StaticEntity.setContinuationState(old));
  }

  public static Cleanups withContinuationState() {
    return withContinuationState(KoLConstants.MafiaState.CONTINUE);
  }
}
