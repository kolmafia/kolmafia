package net.sourceforge.kolmafia.maximizer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.Modeable;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class BoostTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("BoostTest");
    Preferences.reset("BoostTest");
  }

  @Nested
  class Constructors {
    @Test
    public void basicConstructor() {
      AdventureResult item = ItemPool.get(ItemPool.HELMET_TURTLE);
      Boost boost = new Boost("equip hat helmet turtle", "helmet turtle (+1)", item, 1.0);

      assertEquals("equip hat helmet turtle", boost.getCmd());
      assertEquals("helmet turtle (+1)", boost.toString());
      assertEquals(item, boost.getItem());
      assertEquals(1.0, boost.getBoost(), 0.01);
    }

    @Test
    public void emptyCommandMakesGrayText() {
      AdventureResult item = ItemPool.get(ItemPool.HELMET_TURTLE);
      Boost boost = new Boost("", "unavailable item", item, 0.0);

      assertThat(boost.toString(), containsString("<html><font color=gray>"));
      assertEquals("", boost.getCmd());
    }

    @Test
    public void equipmentConstructorWithSlot() {
      AdventureResult item = ItemPool.get(ItemPool.HELMET_TURTLE);
      Boost boost = new Boost("equip hat helmet turtle", "helmet turtle (+1)", Slot.HAT, item, 1.0);

      assertTrue(boost.isEquipment());
      assertEquals(Slot.HAT, boost.getSlot());
    }

    @Test
    public void effectConstructor() {
      AdventureResult effect = new AdventureResult("Disco Leer", 10, true);
      Boost boost =
          new Boost("cast disco leer", "Disco Leer (+10% meat)", effect, false, null, 10.0, false);

      assertFalse(boost.isEquipment());
      assertEquals(effect, boost.getItem(true));
    }

    @Test
    public void shrugEffectConstructor() {
      AdventureResult effect = new AdventureResult("Cunctatitis", 10, true);
      Boost boost =
          new Boost("shrug cunctatitis", "shrug Cunctatitis", effect, true, null, 5.0, false);

      assertFalse(boost.isEquipment());
    }

    @Test
    public void priorityEffectConstructor() {
      AdventureResult effect = new AdventureResult("Disco Leer", 10, true);
      Boost boost =
          new Boost("cast disco leer", "Disco Leer (+10% meat)", effect, false, null, 10.0, true);

      assertFalse(boost.isEquipment());
    }

    @Test
    public void familiarConstructor() {
      FamiliarData fam = new FamiliarData(1); // Baby Gravy Fairy
      Boost boost = new Boost("familiar baby gravy fairy", "Baby Gravy Fairy", fam, 5.0);

      assertTrue(boost.isEquipment());
      assertEquals(Slot.NONE, boost.getSlot());
    }

    @Test
    public void horseConstructor() {
      Boost boost = new Boost("horsery dark", "dark horse (+meat)", "dark", 10.0);

      assertFalse(boost.isEquipment());
      assertNull(boost.getItem());
    }

    @Test
    public void equipmentWithEnthronedAndBjorned() {
      AdventureResult item = ItemPool.get(ItemPool.HELMET_TURTLE);
      FamiliarData enthroned = new FamiliarData(1);
      FamiliarData bjorned = new FamiliarData(2);
      var modeables = new HashMap<Modeable, String>();

      Boost boost =
          new Boost(
              "equip hat helmet turtle",
              "helmet turtle",
              Slot.HAT,
              item,
              5.0,
              enthroned,
              bjorned,
              modeables);

      assertTrue(boost.isEquipment());
      assertEquals(Slot.HAT, boost.getSlot());
    }
  }

  @Nested
  class CompareTo {
    @Test
    public void compareToNull() {
      AdventureResult item = ItemPool.get(ItemPool.HELMET_TURTLE);
      Boost boost = new Boost("equip hat helmet turtle", "helmet turtle", item, 1.0);

      assertThat(boost.compareTo(null), is(lessThan(0)));
    }

    @Test
    public void equipmentBeforeNonEquipment() {
      AdventureResult item = ItemPool.get(ItemPool.HELMET_TURTLE);
      Boost equipment = new Boost("equip hat helmet turtle", "helmet turtle", Slot.HAT, item, 1.0);

      AdventureResult effect = new AdventureResult("Disco Leer", 10, true);
      Boost nonEquipment =
          new Boost("cast disco leer", "Disco Leer", effect, false, null, 10.0, false);

      assertThat(equipment.compareTo(nonEquipment), is(lessThan(0)));
      assertThat(nonEquipment.compareTo(equipment), is(greaterThan(0)));
    }

    @Test
    public void priorityBeforeNonPriority() {
      AdventureResult effect1 = new AdventureResult("Disco Leer", 10, true);
      Boost priority =
          new Boost("cast disco leer", "Disco Leer", effect1, false, null, 5.0, true);

      AdventureResult effect2 = new AdventureResult("Fat Leon's Phat Loot Lyric", 10, true);
      Boost nonPriority =
          new Boost("cast fat leons", "Fat Leon's", effect2, false, null, 10.0, false);

      assertThat(priority.compareTo(nonPriority), is(lessThan(0)));
      assertThat(nonPriority.compareTo(priority), is(greaterThan(0)));
    }

    @Test
    public void higherBoostBeforeLowerForEffects() {
      AdventureResult effect1 = new AdventureResult("Disco Leer", 10, true);
      Boost higher = new Boost("cast disco leer", "Disco Leer", effect1, false, null, 10.0, false);

      AdventureResult effect2 = new AdventureResult("Other Effect", 10, true);
      Boost lower = new Boost("cast other", "Other", effect2, false, null, 5.0, false);

      assertThat(higher.compareTo(lower), is(lessThan(0)));
      assertThat(lower.compareTo(higher), is(greaterThan(0)));
    }

    @Test
    public void equipmentPreservesOrder() {
      AdventureResult item1 = ItemPool.get(ItemPool.HELMET_TURTLE);
      Boost equip1 = new Boost("equip hat helmet turtle", "helmet turtle", Slot.HAT, item1, 1.0);

      AdventureResult item2 = ItemPool.get(ItemPool.MARIACHI_HAT);
      Boost equip2 =
          new Boost("equip hat mariachi hat", "mariachi hat", Slot.HAT, item2, 2.0);

      // Equipment with same priority preserves order (returns 0)
      assertEquals(0, equip1.compareTo(equip2));
    }

    @Test
    public void equalEffectsCompareByBoost() {
      AdventureResult effect = new AdventureResult("Disco Leer", 10, true);
      Boost boost1 = new Boost("cast disco leer", "Disco Leer", effect, false, null, 10.0, false);
      Boost boost2 = new Boost("cast disco leer", "Disco Leer", effect, false, null, 10.0, false);

      assertEquals(0, boost1.compareTo(boost2));
    }
  }

  @Nested
  class Getters {
    @Test
    public void getItemReturnsItem() {
      AdventureResult item = ItemPool.get(ItemPool.HELMET_TURTLE);
      Boost boost = new Boost("equip hat helmet turtle", "helmet turtle", item, 1.0);

      assertEquals(item, boost.getItem());
      assertEquals(item, boost.getItem(false));
    }

    @Test
    public void getItemPrefersEffectWhenTrue() {
      AdventureResult effect = new AdventureResult("Disco Leer", 10, true);
      AdventureResult item = ItemPool.get(ItemPool.HELMET_TURTLE);
      Boost boost = new Boost("cast disco leer", "Disco Leer", effect, false, item, 10.0, false);

      assertEquals(effect, boost.getItem(true));
      assertEquals(item, boost.getItem(false));
    }

    @Test
    public void getBoostReturnsBoostValue() {
      AdventureResult item = ItemPool.get(ItemPool.HELMET_TURTLE);
      Boost boost = new Boost("equip hat helmet turtle", "helmet turtle", item, 5.5);

      assertEquals(5.5, boost.getBoost(), 0.01);
    }

    @Test
    public void getCmdReturnsCommand() {
      AdventureResult item = ItemPool.get(ItemPool.HELMET_TURTLE);
      Boost boost = new Boost("equip hat helmet turtle", "helmet turtle", item, 1.0);

      assertEquals("equip hat helmet turtle", boost.getCmd());
    }

    @Test
    public void getSlotReturnsSlot() {
      AdventureResult item = ItemPool.get(ItemPool.HELMET_TURTLE);
      Boost boost = new Boost("equip hat helmet turtle", "helmet turtle", Slot.HAT, item, 1.0);

      assertEquals(Slot.HAT, boost.getSlot());
    }

    @Test
    public void isEquipmentReturnsCorrectly() {
      AdventureResult item = ItemPool.get(ItemPool.HELMET_TURTLE);
      Boost equipment = new Boost("equip hat helmet turtle", "helmet turtle", Slot.HAT, item, 1.0);

      AdventureResult effect = new AdventureResult("Disco Leer", 10, true);
      Boost nonEquipment =
          new Boost("cast disco leer", "Disco Leer", effect, false, null, 10.0, false);

      assertTrue(equipment.isEquipment());
      assertFalse(nonEquipment.isEquipment());
    }
  }

  @Nested
  class Execute {
    @Test
    public void executeWithEmptyCommandReturnsFalse() {
      AdventureResult item = ItemPool.get(ItemPool.HELMET_TURTLE);
      Boost boost = new Boost("", "unavailable", item, 0.0);

      assertFalse(boost.execute(false));
      assertFalse(boost.execute(true));
    }

    @Test
    public void executeEquipOnlyWithNonEquipmentReturnsFalse() {
      AdventureResult effect = new AdventureResult("Disco Leer", 10, true);
      Boost boost = new Boost("cast disco leer", "Disco Leer", effect, false, null, 10.0, false);

      assertFalse(boost.execute(true));
    }
  }

  @Nested
  class ToString {
    @Test
    public void toStringReturnsText() {
      AdventureResult item = ItemPool.get(ItemPool.HELMET_TURTLE);
      Boost boost = new Boost("equip hat helmet turtle", "helmet turtle (+1)", item, 1.0);

      assertEquals("helmet turtle (+1)", boost.toString());
    }

    @Test
    public void toStringWithAmpersandEscaped() {
      AdventureResult item = ItemPool.get(ItemPool.HELMET_TURTLE);
      Boost boost = new Boost("", "item & stuff", item, 0.0);

      assertThat(boost.toString(), containsString("&amp;"));
    }
  }

  @Nested
  class AddTo {
    @Test
    public void addToWithFamiliar() {
      FamiliarData fam = new FamiliarData(1); // Baby Gravy Fairy
      Boost boost = new Boost("familiar baby gravy fairy", "Baby Gravy Fairy", fam, 5.0);

      MaximizerSpeculation spec = new MaximizerSpeculation();
      boost.addTo(spec);

      // The familiar should be set on the speculation
      assertEquals(fam, spec.getFamiliar());
    }

    @Test
    public void addToWithEquipment() {
      AdventureResult item = ItemPool.get(ItemPool.HELMET_TURTLE);
      var modeables = new HashMap<Modeable, String>();

      Boost boost =
          new Boost(
              "equip hat helmet turtle",
              "helmet turtle",
              Slot.HAT,
              item,
              5.0,
              null, // enthroned
              null, // bjorned
              modeables);

      MaximizerSpeculation spec = new MaximizerSpeculation();
      boost.addTo(spec);

      // The equipment should be set on the speculation
      assertEquals(item, spec.equipment.get(Slot.HAT));
    }

    @Test
    public void addToWithEnthronedFamiliar() {
      AdventureResult crownOfThrones = ItemPool.get(ItemPool.HATSEAT);
      FamiliarData enthroned = new FamiliarData(1);
      var modeables = new HashMap<Modeable, String>();

      Boost boost =
          new Boost(
              "enthrone baby gravy fairy",
              "crown of thrones with baby gravy fairy",
              Slot.HAT,
              crownOfThrones,
              5.0,
              enthroned,
              null, // bjorned
              modeables);

      MaximizerSpeculation spec = new MaximizerSpeculation();
      boost.addTo(spec);

      // Both the crown and enthroned familiar should be set
      assertEquals(crownOfThrones, spec.equipment.get(Slot.HAT));
      assertEquals(enthroned, spec.getEnthroned());
    }

    @Test
    public void addToWithBjornedFamiliar() {
      AdventureResult bjorn = ItemPool.get(ItemPool.BUDDY_BJORN);
      FamiliarData bjorned = new FamiliarData(2);
      var modeables = new HashMap<Modeable, String>();

      Boost boost =
          new Boost(
              "bjornify something",
              "buddy bjorn with familiar",
              Slot.CONTAINER,
              bjorn,
              5.0,
              null, // enthroned
              bjorned,
              modeables);

      MaximizerSpeculation spec = new MaximizerSpeculation();
      boost.addTo(spec);

      // Both the bjorn and bjorned familiar should be set
      assertEquals(bjorn, spec.equipment.get(Slot.CONTAINER));
      assertEquals(bjorned, spec.getBjorned());
    }

    @Test
    public void addToWithModeables() {
      AdventureResult item = ItemPool.get(ItemPool.UNBREAKABLE_UMBRELLA);
      var modeables = new HashMap<Modeable, String>();
      modeables.put(Modeable.UMBRELLA, "broken");

      Boost boost =
          new Boost(
              "equip offhand umbrella",
              "unbreakable umbrella (broken)",
              Slot.OFFHAND,
              item,
              5.0,
              null, // enthroned
              null, // bjorned
              modeables);

      MaximizerSpeculation spec = new MaximizerSpeculation();
      boost.addTo(spec);

      // The equipment and modeable should be set
      assertEquals(item, spec.equipment.get(Slot.OFFHAND));
    }

    @Test
    public void addToWithEffect() {
      AdventureResult effect = new AdventureResult("Disco Leer", 10, true);
      Boost boost =
          new Boost("cast disco leer", "Disco Leer (+10% meat)", effect, false, null, 10.0, false);

      MaximizerSpeculation spec = new MaximizerSpeculation();
      boost.addTo(spec);

      // The effect should be added to the speculation
      assertTrue(spec.hasEffect(effect));
    }

    @Test
    public void addToWithShrugEffect() {
      AdventureResult effect = new AdventureResult("Cunctatitis", 10, true);
      Boost boost =
          new Boost("shrug cunctatitis", "shrug Cunctatitis", effect, true, null, 5.0, false);

      MaximizerSpeculation spec = new MaximizerSpeculation();
      // First add the effect so we can remove it
      spec.addEffect(effect);
      assertTrue(spec.hasEffect(effect));

      boost.addTo(spec);

      // The effect should be removed from the speculation
      assertFalse(spec.hasEffect(effect));
    }

    @Test
    public void addToWithHorse() {
      Boost boost = new Boost("horsery dark", "dark horse (+meat)", "dark", 10.0);

      MaximizerSpeculation spec = new MaximizerSpeculation();
      boost.addTo(spec);

      // The horse should be set on the speculation
      assertEquals("dark", spec.getHorsery());
    }

    @Test
    public void addToWithNullModeable() {
      AdventureResult item = ItemPool.get(ItemPool.HELMET_TURTLE);
      var modeables = new HashMap<Modeable, String>();
      modeables.put(Modeable.UMBRELLA, null); // null modeable should be skipped

      Boost boost =
          new Boost(
              "equip hat helmet turtle",
              "helmet turtle",
              Slot.HAT,
              item,
              5.0,
              null, // enthroned
              null, // bjorned
              modeables);

      MaximizerSpeculation spec = new MaximizerSpeculation();
      // Should not throw
      boost.addTo(spec);

      assertEquals(item, spec.equipment.get(Slot.HAT));
    }
  }
}
