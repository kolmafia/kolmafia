package net.sourceforge.kolmafia;

import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;

import java.util.stream.Collectors;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ModeableTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("ModeableTest");
  }

  @Test
  void getCommand() {
    var modeable = Modeable.EDPIECE;
    assertThat(modeable.getCommand(), equalTo("edpiece"));
  }

  @Test
  void getItem() {
    var modeable = Modeable.SNOWSUIT;
    assertThat(modeable.getItem(), equalTo(ItemPool.get(ItemPool.SNOW_SUIT)));
  }

  @Test
  void getItemId() {
    var modeable = Modeable.SNOWSUIT;
    assertThat(modeable.getItemId(), equalTo(ItemPool.SNOW_SUIT));
  }

  @Test
  void getSlot() {
    var modeable = Modeable.BACKUPCAMERA;
    assertThat(modeable.getSlot(), equalTo(EquipmentManager.ACCESSORY1));
  }

  @Test
  void validate() {
    var modeable = Modeable.RETROCAPE;
    assertThat(modeable.validate("retrocape", "heck kiss"), equalTo(true));
  }

  @Test
  void getModes() {
    var modeable = Modeable.RETROCAPE;
    assertThat(modeable.getModes(), hasSize(12));
  }

  @Test
  void getState() {
    var cleanups = withProperty("umbrellaState", "broken");

    try (cleanups) {
      var modeable = Modeable.UMBRELLA;
      assertThat(modeable.getState(), equalTo("broken"));
    }
  }

  @Test
  void getModifier() {
    var modeable = Modeable.UMBRELLA;
    assertThat(modeable.getModifier(), equalTo("UnbreakableUmbrella"));
  }

  @Test
  void getEquipAfterChange() {
    var modeable = Modeable.UMBRELLA;
    assertThat(modeable.getEquipAfterChange(), equalTo(true));
  }

  @Test
  void findById() {
    var modeable = Modeable.find(ItemPool.BACKUP_CAMERA);
    assertThat(modeable, equalTo(Modeable.BACKUPCAMERA));
  }

  @Test
  void returnNullIfCannotFindById() {
    var modeable = Modeable.find(ItemPool.EYEPATCH);
    assertThat(modeable, equalTo(null));
  }

  @Test
  void findByCommand() {
    var modeable = Modeable.find("edpiece");
    assertThat(modeable, equalTo(Modeable.EDPIECE));
  }

  @Test
  void returnNullIfCannotFindByCommand() {
    var modeable = Modeable.find("win");
    assertThat(modeable, equalTo(null));
  }

  @Test
  void findByItem() {
    var modeable = Modeable.find(ItemPool.get(ItemPool.SNOW_SUIT));
    assertThat(modeable, equalTo(Modeable.SNOWSUIT));
  }

  @Test
  void returnNullIfCannotFindByItem() {
    var modeable = Modeable.find(ItemPool.get(ItemPool.WORTHLESS_ITEM));
    assertThat(modeable, equalTo(null));
  }

  @Test
  void getStateMap() {
    var map = Modeable.getStateMap();
    assertThat(map.keySet(), containsInAnyOrder(Modeable.values()));
  }

  @Test
  void getBooleanMap() {
    var map = Modeable.getBooleanMap();
    assertThat(map.keySet(), containsInAnyOrder(Modeable.values()));
    assertThat(map.values(), everyItem(equalTo(false)));
  }

  @Test
  void getBooleanMapWithCustomSupplier() {
    var map = Modeable.getBooleanMap(m -> m.getSlot() == EquipmentManager.HAT);
    assertThat(map, hasEntry(equalTo(Modeable.EDPIECE), equalTo(true)));
    assertThat(map.values().stream().filter(v -> v).collect(Collectors.toSet()), hasSize(1));
  }
}
