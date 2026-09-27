package net.sourceforge.kolmafia.request;

import static internal.helpers.Equipment.assertItem;
import static internal.helpers.Equipment.assertItemUnequip;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.getPostRequestBody;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withHatTrickHat;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withUnequipped;
import static internal.helpers.Player.withoutItem;
import static internal.helpers.RequestLoggerOutput.startStream;
import static internal.helpers.RequestLoggerOutput.stopStream;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class EquipmentRequestTest {
  // Decodes to item IDs [9412, 0, 0, 0, 0].
  private static final String ALIEN_GEMSTONE_IN_FIRST_CODPIECE_SLOT = "~xEkAAAAA";

  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("EquipmentRequestTest");
    ChoiceManager.handlingChoice = false;
    ChoiceManager.lastChoice = 0;
  }

  @AfterEach
  public void afterEach() {
    ChoiceManager.handlingChoice = false;
    ChoiceManager.lastChoice = 0;
  }

  private AdventureResult makeItem(String name) {
    return new AdventureResult(name, 1, false);
  }

  @Test
  void reappliesCodpieceSlotsWhenAutomaticSavingIsDisabled() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    client.addResponse(200, "");
    client.addResponse(200, "");
    client.addResponse(200, "");
    client.addResponse(200, "");
    client.addResponse(200, "");
    client.addResponse(200, "");

    var outfit =
        new SpecialOutfit(-123, "Codpiece Test c=" + ALIEN_GEMSTONE_IN_FIRST_CODPIECE_SLOT);
    outfit.addPiece(ItemPool.get(ItemPool.THE_ETERNITY_CODPIECE));
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withProperty("includeCodpieceGemsInOutfits", false),
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.CODPIECE1, ItemPool.HAMETHYST),
            withItem(ItemPool.ALIEN_GEMSTONE));

    try (cleanups) {
      new EquipmentRequest(outfit).run();

      var requests = client.getRequests();
      var inventoryRequests =
          requests.stream()
              .filter(request -> request.uri().getPath().equals("/inventory.php"))
              .toList();
      var choiceRequests =
          requests.stream()
              .filter(request -> request.uri().getPath().equals("/choice.php"))
              .toList();
      assertEquals(2, inventoryRequests.size());
      assertEquals(2, choiceRequests.size());
      assertPostRequest(inventoryRequests.get(0), "/inventory.php", "action=docodpiece");
      assertPostRequest(choiceRequests.get(0), "/choice.php", "whichchoice=1588&option=2&which=1");
      assertPostRequest(inventoryRequests.get(1), "/inventory.php", "action=docodpiece");
      assertPostRequest(
          choiceRequests.get(1), "/choice.php", "whichchoice=1588&option=1&which=1&iid=9412");
    }
  }

  @Test
  void doesNothingWhenCodpieceSlotsAlreadyMatchOutfit() {
    var builder = new FakeHttpClientBuilder();
    var outfit =
        new SpecialOutfit(-123, "Codpiece Test c=" + ALIEN_GEMSTONE_IN_FIRST_CODPIECE_SLOT);
    outfit.addPiece(ItemPool.get(ItemPool.THE_ETERNITY_CODPIECE));
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.CODPIECE1, ItemPool.ALIEN_GEMSTONE));

    try (cleanups) {
      new EquipmentRequest(outfit).run();

      assertTrue(builder.client.getRequests().isEmpty());
    }
  }

  @Test
  void doesNothingWhenOutfitHasNoCodpieceConfiguration() {
    var builder = new FakeHttpClientBuilder();
    var outfit = new SpecialOutfit(-123, "Codpiece Test");
    outfit.addPiece(ItemPool.get(ItemPool.THE_ETERNITY_CODPIECE));
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE));

    try (cleanups) {
      new EquipmentRequest(outfit).run();

      assertTrue(builder.client.getRequests().isEmpty());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"m=apathetic", "c=echo ignored"})
  void ignoresNonCodpieceActionsWhenAlreadyWearingOutfit(String action) {
    var builder = new FakeHttpClientBuilder();
    var outfit = new SpecialOutfit(-123, "Codpiece Test " + action);
    outfit.addPiece(ItemPool.get(ItemPool.THE_ETERNITY_CODPIECE));
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE));

    try (cleanups) {
      new EquipmentRequest(outfit).run();

      assertTrue(builder.client.getRequests().isEmpty());
    }
  }

  @Test
  void leavesCodpieceSlotsAloneWhenRequiredGemIsUnavailable() {
    var builder = new FakeHttpClientBuilder();
    var outfit =
        new SpecialOutfit(-123, "Codpiece Test c=" + ALIEN_GEMSTONE_IN_FIRST_CODPIECE_SLOT);
    outfit.addPiece(ItemPool.get(ItemPool.THE_ETERNITY_CODPIECE));
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withContinuationState(),
            withProperty("autoSatisfyWithMall", false),
            withProperty("autoSatisfyWithNPCs", false),
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withoutItem(ItemPool.ALIEN_GEMSTONE));

    try (cleanups) {
      new EquipmentRequest(outfit).run();

      assertTrue(builder.client.getRequests().isEmpty());
    }
  }

  @Test
  void stopsRestoringCodpieceSlotsWhenFollowupInsertionFails() {
    var builder = new FakeHttpClientBuilder();
    builder.client.addResponse(502, "");
    // Decodes to item IDs [0, 9412, 0, 0, 0].
    var outfit = new SpecialOutfit(-123, "Codpiece Test c=~AMRJAAAA");
    outfit.addPiece(ItemPool.get(ItemPool.THE_ETERNITY_CODPIECE));
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withContinuationState(),
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.CODPIECE1, ItemPool.ALIEN_GEMSTONE));

    try (cleanups) {
      new EquipmentRequest(outfit).run();

      assertFalse(builder.client.getRequests().isEmpty());
      assertFalse(KoLmafia.permitsContinue());
      assertEquals(
          1,
          builder.client.getRequests().stream()
              .filter(request -> request.uri().getPath().equals("/choice.php"))
              .count());
    }
  }

  @Test
  void reportsSuccessfulOutfitSave() {
    var builder = new FakeHttpClientBuilder();
    builder.client.addResponse(200, "");
    var cleanups = new Cleanups(withHttpClientBuilder(builder), withContinuationState());

    try (cleanups) {
      new EquipmentRequest("Saved outfit").run();

      assertEquals("Outfit saved", KoLmafia.getLastMessage());
      assertThat(
          getPostRequestBody(builder.client.getRequests().getFirst()), not(containsString(" c=")));
    }
  }

  @Test
  void doesNotSaveCodpieceSlotsInOutfitNameByDefault() {
    var builder = new FakeHttpClientBuilder();
    builder.client.addResponse(200, "");
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withContinuationState(),
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.CODPIECE1, ItemPool.ALIEN_GEMSTONE),
            withEquipped(Slot.CODPIECE3, ItemPool.HAMETHYST));

    try (cleanups) {
      new EquipmentRequest("Saved outfit").run();

      assertThat(
          getPostRequestBody(builder.client.getRequests().getFirst()), not(containsString(" c=")));
    }
  }

  @Test
  void doesNotSaveCodpieceSlotsWhenEnabledWithoutEquippedCodpiece() {
    var builder = new FakeHttpClientBuilder();
    builder.client.addResponse(200, "");
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withContinuationState(),
            withProperty("includeCodpieceGemsInOutfits", true));

    try (cleanups) {
      new EquipmentRequest("Saved outfit").run();

      assertThat(
          getPostRequestBody(builder.client.getRequests().getFirst()), not(containsString(" c=")));
    }
  }

  @Test
  void savesCodpieceSlotsInOutfitNameWhenEnabled() {
    var builder = new FakeHttpClientBuilder();
    builder.client.addResponse(200, "");
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withContinuationState(),
            withProperty("includeCodpieceGemsInOutfits", true),
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.CODPIECE1, ItemPool.ALIEN_GEMSTONE),
            withEquipped(Slot.CODPIECE3, ItemPool.HAMETHYST));

    try (cleanups) {
      new EquipmentRequest("Saved outfit").run();

      // The suffix decodes to item IDs [9412, 0, 704, 0, 0].
      assertThat(
          getPostRequestBody(builder.client.getRequests().getFirst()),
          containsString("outfitname=Saved outfit c=~xEkAwAUAAA"));
    }
  }

  @Test
  void preservesExistingOutfitCommands() {
    var outfit = new SpecialOutfit(-123, "Command c=echo worked");
    var cleanups = new Cleanups(() -> EquipmentManager.setCustomOutfits(List.of()));

    try (cleanups) {
      EquipmentManager.setCustomOutfits(List.of(outfit));
      startStream();
      EquipmentRequest.registerRequest("inv_equip.php?action=outfit&whichoutfit=-123");
      EquipmentRequest.parseEquipmentChange(
          "inv_equip.php?action=outfit&whichoutfit=-123", "You put on an outfit.");
      String output = stopStream();

      assertThat(output, containsString("worked"));
    }
  }

  @Test
  void ignoresCodpieceConfigurationActionWithoutEquippedCodpiece() {
    var builder = new FakeHttpClientBuilder();
    var outfit = new SpecialOutfit(-123, "No Codpiece c=" + ALIEN_GEMSTONE_IN_FIRST_CODPIECE_SLOT);
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withItem(ItemPool.ALIEN_GEMSTONE),
            new Cleanups(() -> EquipmentManager.setCustomOutfits(List.of())));

    try (cleanups) {
      EquipmentManager.setCustomOutfits(List.of(outfit));
      EquipmentRequest.registerRequest("inv_equip.php?action=outfit&whichoutfit=-123");
      EquipmentRequest.parseEquipmentChange(
          "inv_equip.php?action=outfit&whichoutfit=-123", "You put on an outfit.");

      assertThat(EquipmentManager.getEquipment(Slot.CODPIECE1), equalTo(EquipmentRequest.UNEQUIP));
      assertTrue(
          builder.client.getRequests().stream()
              .noneMatch(request -> request.uri().getPath().equals("/choice.php")));
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "~not-base64!", // Invalid Base64
        "~AA", // Fewer than five varints
        "~gICAgAg", // Integer overflow
        "~gICAgIAA", // Unterminated varint
        "~AAAAAAAA" // Data after the fifth varint
      })
  void ignoresInvalidCodpieceConfiguration(String configuration) {
    var builder = new FakeHttpClientBuilder();
    var outfit = new SpecialOutfit(-123, "Codpiece Test c=" + configuration);
    outfit.addPiece(ItemPool.get(ItemPool.THE_ETERNITY_CODPIECE));
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE));

    try (cleanups) {
      startStream();
      new EquipmentRequest(outfit).run();
      String output = stopStream();

      assertThat(output, containsString("Invalid Codpiece outfit configuration."));
      assertTrue(builder.client.getRequests().isEmpty());
    }
  }

  @Test
  void treatsExistingNonGemItemIdAsAnEmptyCodpieceSlot() {
    var builder = new FakeHttpClientBuilder();
    builder.client.addResponse(200, "");
    builder.client.addResponse(200, "");
    // Decodes to [1, 0, 0, 0, 0]; item ID 1 is a non-socketable seal-clubbing club.
    var outfit = new SpecialOutfit(-123, "Codpiece Test c=~AQAAAAA");
    outfit.addPiece(ItemPool.get(ItemPool.THE_ETERNITY_CODPIECE));
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.CODPIECE1, ItemPool.ALIEN_GEMSTONE));

    try (cleanups) {
      assertThat(ItemDatabase.getItemName(ItemPool.SEAL_CLUB), equalTo("seal-clubbing club"));
      startStream();
      new EquipmentRequest(outfit).run();
      String output = stopStream();

      assertThat(output, containsString("Ignoring non-Codpiece gem item ID 1"));
      var requests = builder.client.getRequests();
      var inventoryRequests =
          requests.stream()
              .filter(request -> request.uri().getPath().equals("/inventory.php"))
              .toList();
      var choiceRequests =
          requests.stream()
              .filter(request -> request.uri().getPath().equals("/choice.php"))
              .toList();
      assertEquals(1, inventoryRequests.size());
      assertEquals(1, choiceRequests.size());
      assertPostRequest(inventoryRequests.getFirst(), "/inventory.php", "action=docodpiece");
      assertPostRequest(
          choiceRequests.getFirst(), "/choice.php", "whichchoice=1588&option=2&which=1");
    }
  }

  @Test
  void ignoresUnknownItemIdInCodpieceConfiguration() {
    var builder = new FakeHttpClientBuilder();
    // Decodes to [999999, 0, 0, 0, 0]; item ID 999999 is not in the item database.
    var outfit = new SpecialOutfit(-123, "Codpiece Test c=~v4Q9AAAAAA");
    outfit.addPiece(ItemPool.get(ItemPool.THE_ETERNITY_CODPIECE));
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE));

    try (cleanups) {
      assertNull(ItemDatabase.getItemName(999999));
      startStream();
      new EquipmentRequest(outfit).run();
      String output = stopStream();

      assertThat(output, containsString("Ignoring non-Codpiece gem item ID 999999"));
      assertTrue(builder.client.getRequests().isEmpty());
    }
  }

  @Test
  void restoresValidGemsAlongsideInvalidItemIds() {
    var builder = new FakeHttpClientBuilder();
    builder.client.addResponse(200, "");
    builder.client.addResponse(200, "");
    builder.client.addResponse(200, "");
    builder.client.addResponse(200, "");
    // Decodes to [9412, 1, 704, 999999, 0].
    var outfit = new SpecialOutfit(-123, "Codpiece Test c=~xEkBwAW_hD0A");
    outfit.addPiece(ItemPool.get(ItemPool.THE_ETERNITY_CODPIECE));
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
            withItem(ItemPool.ALIEN_GEMSTONE),
            withItem(ItemPool.HAMETHYST));

    try (cleanups) {
      new EquipmentRequest(outfit).run();

      var requests = builder.client.getRequests();
      var inventoryRequests =
          requests.stream()
              .filter(request -> request.uri().getPath().equals("/inventory.php"))
              .toList();
      var choiceRequests =
          requests.stream()
              .filter(request -> request.uri().getPath().equals("/choice.php"))
              .toList();
      assertEquals(2, inventoryRequests.size());
      assertEquals(2, choiceRequests.size());
      assertPostRequest(inventoryRequests.get(0), "/inventory.php", "action=docodpiece");
      assertPostRequest(
          choiceRequests.get(0), "/choice.php", "whichchoice=1588&option=1&which=1&iid=9412");
      assertPostRequest(inventoryRequests.get(1), "/inventory.php", "action=docodpiece");
      assertPostRequest(
          choiceRequests.get(1), "/choice.php", "whichchoice=1588&option=1&which=3&iid=704");
    }
  }

  @Test
  void reportsSuccessfulUnequipAll() {
    var builder = new FakeHttpClientBuilder();
    builder.client.addResponse(200, "");
    var cleanups = new Cleanups(withHttpClientBuilder(builder), withContinuationState());

    try (cleanups) {
      new EquipmentRequest(EquipmentRequest.EquipmentRequestType.UNEQUIP_ALL).run();

      assertEquals("Everything removed.", KoLmafia.getLastMessage());
    }
  }

  @Test
  public void canParseEquipmentPage() {
    String location = "inventory.php?which=2";
    String responseText = html("request/test_parse_equipment.html");

    var cleanups = new Cleanups(withFamiliar(FamiliarPool.TRICK_TOT));

    try (cleanups) {
      EquipmentRequest.parseEquipment(location, responseText);
      Map<Slot, AdventureResult> equipment = EquipmentManager.currentEquipment();

      assertEquals(equipment.get(Slot.HAT), makeItem("Daylight Shavings Helmet"));
      assertEquals(equipment.get(Slot.WEAPON), makeItem("June cleaver"));
      assertEquals(equipment.get(Slot.OFFHAND), makeItem("Drunkula's wineglass"));
      assertEquals(equipment.get(Slot.CONTAINER), makeItem("vampyric cloake"));
      assertEquals(equipment.get(Slot.SHIRT), makeItem("poncho de azucar"));
      assertEquals(equipment.get(Slot.PANTS), makeItem("purpleheart &quot;pants&quot;"));
      assertEquals(equipment.get(Slot.ACCESSORY1), makeItem("Draftsman's driving gloves"));
      assertEquals(equipment.get(Slot.ACCESSORY2), makeItem("fudgecycle"));
      assertEquals(equipment.get(Slot.ACCESSORY3), makeItem("Counterclockwise Watch"));
      assertEquals(equipment.get(Slot.FAMILIAR), makeItem("li'l unicorn costume"));
    }
  }

  @Test
  public void canParseHatsInHatTrick() {
    String location = "inventory.php?which=2";
    String responseText = html("request/test_parse_equipment_hattrick.html");

    var cleanups = new Cleanups(withPath(Path.HAT_TRICK));

    try (cleanups) {
      EquipmentRequest.parseEquipment(location, responseText);

      assertItemUnequip(Slot.HAT);

      var hats = EquipmentManager.getHatTrickHats();
      assertThat(hats, contains(11565, 2283));
    }
  }

  @Test
  public void canParseHatsInHatTrickEquippedWithChatCommand() {
    String location = "inv_equip.php?action=equip&whichitem=33&ajax=1&pwd";
    String responseText = html("request/test_parse_equipment_changed_hattrick.html");

    var cleanups = new Cleanups(withHatTrickHat(ItemPool.BUGGED_BEANIE), withPath(Path.HAT_TRICK));

    try (cleanups) {
      EquipmentRequest.parseEquipmentChange(location, responseText);

      var hats = EquipmentManager.getHatTrickHats();
      assertThat(hats, contains(ItemPool.BUGGED_BEANIE, 33));
    }
  }

  @Nested
  class FolderHolder {
    @Test
    public void canParseFolderHolderPage() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.FOLDER1, ItemPool.FOLDER_01),
              withEquipped(Slot.FOLDER2, ItemPool.FOLDER_01),
              withEquipped(Slot.FOLDER3, ItemPool.FOLDER_01),
              withEquipped(Slot.FOLDER4, ItemPool.FOLDER_01),
              withEquipped(Slot.FOLDER5, ItemPool.FOLDER_01));

      try (cleanups) {
        String text = html("request/test_folder_holder.html");

        EquipmentRequest.parseFolders(text);

        assertItem(Slot.FOLDER1, "folder (heavy metal)");
        assertItem(Slot.FOLDER2, "folder (tranquil landscape)");
        assertItem(Slot.FOLDER3, "folder (owl)");
        assertItemUnequip(Slot.FOLDER4);
        assertItemUnequip(Slot.FOLDER5);
      }
    }

    @Test
    public void canParseFolderHolderPageWithNoMoreFolders() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.FOLDER1, ItemPool.FOLDER_01),
              withEquipped(Slot.FOLDER2, ItemPool.FOLDER_01),
              withEquipped(Slot.FOLDER3, ItemPool.FOLDER_01),
              withEquipped(Slot.FOLDER4, ItemPool.FOLDER_01),
              withEquipped(Slot.FOLDER5, ItemPool.FOLDER_01));

      try (cleanups) {
        String text = html("request/test_folder_holder_no_more_folders.html");

        EquipmentRequest.parseFolders(text);

        assertItem(Slot.FOLDER1, "folder (heavy metal)");
        assertItem(Slot.FOLDER2, "folder (tranquil landscape)");
        assertItem(Slot.FOLDER3, "folder (owl)");
        assertItemUnequip(Slot.FOLDER4);
        assertItemUnequip(Slot.FOLDER5);
      }
    }
  }

  @Nested
  class Codpiece {
    @Test
    public void canParseCodpieceInsert() {
      // put baconstone in empty slot
      String text = html("request/test_codpiece_insert.html");
      var cleanups =
          new Cleanups(withUnequipped(Slot.CODPIECE1), withItem(ItemPool.BACONSTONE, 10));
      try (cleanups) {
        EquipmentRequest req =
            new EquipmentRequest(ItemPool.get(ItemPool.BACONSTONE), Slot.CODPIECE1);
        req.setHasResult(true);
        req.responseText = text;
        ChoiceManager.preChoice(req);
        req.processResponse();

        assertThat(InventoryManager.getCount(ItemPool.BACONSTONE), equalTo(9));
      }
    }

    @Test
    public void canParseCodpieceChange() {
      // swap in hamethyst for baconstone
      String text = html("request/test_codpiece_change.html");
      var cleanups =
          new Cleanups(
              withEquipped(Slot.CODPIECE1, ItemPool.BACONSTONE),
              withItem(ItemPool.BACONSTONE, 10),
              withItem(ItemPool.HAMETHYST, 10));
      try (cleanups) {
        EquipmentRequest req =
            new EquipmentRequest(ItemPool.get(ItemPool.HAMETHYST), Slot.CODPIECE1);
        req.setHasResult(true);
        req.responseText = text;
        ChoiceManager.preChoice(req);
        req.processResponse();

        assertThat(InventoryManager.getCount(ItemPool.BACONSTONE), equalTo(11));
        assertThat(InventoryManager.getCount(ItemPool.HAMETHYST), equalTo(9));
      }
    }

    @Test
    public void canParseCodpiecePageForAllSlots() {
      String text = html("request/test_codpiece_full_page.html");
      var cleanups =
          new Cleanups(
              withEquipped(Slot.CODPIECE1, ItemPool.HAMETHYST),
              withEquipped(Slot.CODPIECE2, ItemPool.HAMETHYST),
              withEquipped(Slot.CODPIECE3, ItemPool.HAMETHYST),
              withEquipped(Slot.CODPIECE4, ItemPool.HAMETHYST),
              withEquipped(Slot.CODPIECE5, ItemPool.HAMETHYST));
      try (cleanups) {
        EquipmentRequest.parseCodpiecePage(text);

        assertItem(Slot.CODPIECE1, "blood cubic zirconia");
        assertItem(Slot.CODPIECE2, "massive gemstone");
        assertItemUnequip(Slot.CODPIECE3);
        assertItem(Slot.CODPIECE4, "Heartstone");
        assertItem(Slot.CODPIECE5, "Peridot of Peril");
      }
    }

    @Test
    public void canParseCodpiecePageWithEmptySlots() {
      // only slot 1 is filled; slots 2-5 are empty
      String text = html("request/test_codpiece_insert.html");
      var cleanups =
          new Cleanups(
              withUnequipped(Slot.CODPIECE1),
              withEquipped(Slot.CODPIECE2, ItemPool.HAMETHYST),
              withEquipped(Slot.CODPIECE3, ItemPool.HAMETHYST),
              withEquipped(Slot.CODPIECE4, ItemPool.HAMETHYST),
              withEquipped(Slot.CODPIECE5, ItemPool.HAMETHYST));
      try (cleanups) {
        EquipmentRequest.parseCodpiecePage(text);

        assertItem(Slot.CODPIECE1, "baconstone");
        assertItemUnequip(Slot.CODPIECE2);
        assertItemUnequip(Slot.CODPIECE3);
        assertItemUnequip(Slot.CODPIECE4);
        assertItemUnequip(Slot.CODPIECE5);
      }
    }

    @Test
    public void canParseCodpieceRemove() {
      // clear slot containing hamethyst
      String text = html("request/test_codpiece_remove.html");
      var cleanups =
          new Cleanups(
              withEquipped(Slot.CODPIECE1, ItemPool.HAMETHYST), withItem(ItemPool.HAMETHYST, 10));
      try (cleanups) {
        EquipmentRequest req = new EquipmentRequest(EquipmentRequest.UNEQUIP, Slot.CODPIECE1);
        req.setHasResult(true);
        req.responseText = text;
        ChoiceManager.preChoice(req);
        req.processResponse();

        assertThat(InventoryManager.getCount(ItemPool.HAMETHYST), equalTo(11));
      }
    }
  }
}
