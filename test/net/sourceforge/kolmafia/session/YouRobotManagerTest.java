package net.sourceforge.kolmafia.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.swing.JButton;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase.ConcoctionType;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.CharSheetRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.swingui.panel.ItemManagePanel;
import net.sourceforge.kolmafia.swingui.panel.UseItemDequeuePanel;
import net.sourceforge.kolmafia.swingui.panel.UseItemEnqueuePanel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class YouRobotManagerTest {

  private class TestListener implements Listener {
    private int calls = 0;

    public TestListener(String signal) {
      NamedListenerRegistry.registerNamedListener(signal, this);

      // Note that there is no need to provide a way to clean up once you're
      // done. a ListenerRegistry uses WeakReferences to Listeners, so as soon
      // as the object goes out of scope, the pointer is broken
    }

    public void reset() {
      this.calls = 0;
    }

    public void update() {
      this.calls++;
    }

    public int getCalls() {
      return this.calls;
    }
  }

  @BeforeAll
  private static void beforeAll() {
    // Simulate logging out and back in again.
    GenericRequest.passwordHash = "";
    KoLCharacter.reset("");
    KoLCharacter.reset("you robot manager user");
    KoLCharacter.setPath(Path.YOU_ROBOT);
    Preferences.saveSettingsToFile = false;
  }

  @AfterAll
  private static void afterAll() {
    Preferences.saveSettingsToFile = true;
  }

  @BeforeEach
  private void beforeEach() {
    Preferences.setInteger("statbotUses", 0);
    Preferences.setInteger("youRobotTop", 0);
    Preferences.setInteger("youRobotLeft", 0);
    Preferences.setInteger("youRobotRight", 0);
    Preferences.setInteger("youRobotBottom", 0);
    Preferences.setString("youRobotCPUUpgrades", "");
    YouRobotManager.reset();
    KoLCharacter.setAvatar("");
    KoLCharacter.resetSkills();
    KoLCharacter.setYouRobotEnergy(0);
    KoLCharacter.setYouRobotScraps(0);
    ChoiceManager.lastChoice = 0;
    ChoiceManager.lastDecision = 0;
  }

  static String loadHTMLResponse(String path) throws IOException {
    // Load the responseText from saved HTML file
    return Files.readString(Paths.get(path)).trim();
  }

  private void verifyNoAvatarOrProperties() {
    assertEquals(0, Preferences.getInteger("statbotUses"));
    assertEquals(0, Preferences.getInteger("youRobotTop"));
    assertEquals(0, Preferences.getInteger("youRobotLeft"));
    assertEquals(0, Preferences.getInteger("youRobotRight"));
    assertEquals(0, Preferences.getInteger("youRobotBottom"));
    List<String> avatar = KoLCharacter.getAvatar();
    assertEquals(1, KoLCharacter.getAvatar().size());
    assertEquals("", avatar.get(0));
  }

  private void verifyAvatarFromProperties() {
    String prefix = "otherimages/robot/";
    int top = Preferences.getInteger("youRobotTop");
    String topImage = prefix + "top" + top + ".png";
    int left = Preferences.getInteger("youRobotLeft");
    String leftImage = prefix + "left" + left + ".png";
    int right = Preferences.getInteger("youRobotRight");
    String rightImage = prefix + "right" + right + ".png";
    int bottom = Preferences.getInteger("youRobotBottom");
    String bottomImage = prefix + "bottom" + bottom + ".png";
    // We don't save the body in a property since it is constant
    // based on character type. The data files came from an AT
    int ascensionClass = AscensionClass.ACCORDION_THIEF.getId();
    String bodyImage = prefix + "body" + ascensionClass + ".png";

    // Parts that are missing (newly ascended character) do not have an image.
    int count =
        1 + (top == 0 ? 0 : 1) + (left == 0 ? 0 : 1) + (right == 0 ? 0 : 1) + (bottom == 0 ? 0 : 1);

    List<String> avatar = KoLCharacter.getAvatar();
    Set<String> images = new HashSet<>(avatar);
    assertEquals(count, images.size());
    if (top != 0) {
      assertTrue(images.contains(topImage));
    }
    if (left != 0) {
      assertTrue(images.contains(leftImage));
    }
    if (right != 0) {
      assertTrue(images.contains(rightImage));
    }
    if (bottom != 0) {
      assertTrue(images.contains(bottomImage));
    }
    assertTrue(images.contains(bodyImage));
  }

  @Test
  public void canFindAvatarOnCharSheet() throws IOException {
    String responseText = loadHTMLResponse("request/test_scrapheap_charsheet.html");

    // Verify that the properties and avatar are not set
    verifyNoAvatarOrProperties();

    CharSheetRequest.parseStatus(responseText);

    // Verify that the properties and avatar are now set
    verifyAvatarFromProperties();
  }

  @Test
  public void canFindAvatarOnCharPane() throws IOException {
    String responseText = loadHTMLResponse("request/test_scrapheap_charpane.html");

    // Verify that the properties and avatar are not set
    verifyNoAvatarOrProperties();

    CharPaneRequest.compactCharacterPane = false;
    CharPaneRequest.checkYouRobot(responseText);

    // Verify that the properties and avatar are now set
    verifyAvatarFromProperties();
  }

  @Test
  public void canFindAvatarOnReassemblyStationVisit() throws IOException {
    String responseText = loadHTMLResponse("request/test_scrapheap_reassembly_station.html");

    // Verify that the properties and avatar are not set
    verifyNoAvatarOrProperties();

    ChoiceManager.lastChoice = 1445;
    GenericRequest request = new GenericRequest("choice.php?forceoption=0");
    request.responseText = responseText;
    YouRobotManager.visitChoice(request);

    // Verify that the properties and avatar are now set
    verifyAvatarFromProperties();
  }

  @Test
  public void canHandleThreePartAvatar() throws IOException {
    // This is a newly ascended Accordion Thief
    String responseText = loadHTMLResponse("request/test_scrapheap_three_part_avatar.html");

    // Verify that the properties and avatar are not set
    verifyNoAvatarOrProperties();

    ChoiceManager.lastChoice = 1445;
    GenericRequest request = new GenericRequest("choice.php?forceoption=0");
    request.responseText = responseText;
    YouRobotManager.visitChoice(request);

    // Verify that the properties and avatar are now set
    assertEquals(1, Preferences.getInteger("youRobotTop"));
    assertEquals(0, Preferences.getInteger("youRobotLeft"));
    assertEquals(3, Preferences.getInteger("youRobotRight"));
    assertEquals(3, Preferences.getInteger("youRobotBottom"));
    assertEquals(6, Preferences.getInteger("youRobotBody"));
    verifyAvatarFromProperties();

    // Verify that we can't equip any items
    assertFalse(YouRobotManager.canEquip(KoLConstants.EQUIP_HAT));
    assertFalse(YouRobotManager.canEquip(KoLConstants.EQUIP_WEAPON));
    assertFalse(YouRobotManager.canEquip(KoLConstants.EQUIP_OFFHAND));
    assertFalse(YouRobotManager.canEquip(KoLConstants.EQUIP_PANTS));
    assertFalse(YouRobotManager.canEquip(KoLConstants.EQUIP_SHIRT));

    // Verify that our Pea Shooter is active.
    assertTrue(KoLCharacter.availableCombatSkill(SkillPool.SHOOT_PEA));
  }

  @Test
  public void canDiscoverStatbotCostOnVisit() throws IOException {
    String urlString = "choice.php?forceoption=0";
    String responseText = loadHTMLResponse("request/test_scrapheap_visit_statbot.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1447;
    assertEquals(0, Preferences.getInteger("statbotUses"));
    YouRobotManager.visitChoice(request);
    assertEquals(11, Preferences.getInteger("statbotUses"));
  }

  @Test
  public void canDiscoverStatbotCostOnActivation() throws IOException {
    String urlString = "choice.php?pwd&whichchoice=1447&option=3";
    String responseText = loadHTMLResponse("request/test_scrapheap_activate_statbot.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1447;
    assertEquals(0, Preferences.getInteger("statbotUses"));
    YouRobotManager.postChoice1(responseText, request);
    assertEquals(11, Preferences.getInteger("statbotUses"));
  }

  @Test
  public void canDiscoverStatbotCostOnFailedActivation() throws IOException {
    String urlString = "choice.php?pwd&whichchoice=1447&option=3";
    String responseText = loadHTMLResponse("request/test_scrapheap_activate_statbot_fails.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1447;
    assertEquals(0, Preferences.getInteger("statbotUses"));
    YouRobotManager.postChoice1(responseText, request);
    assertEquals(24, Preferences.getInteger("statbotUses"));
  }

  @Test
  public void canRegisterRequests() throws IOException {
    String urlString = "choice.php?whichchoice=1445&show=cpus";
    String expected = "Inspecting CPU Upgrade options at the Reassembly Station.";
    assertTrue(YouRobotManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);

    urlString = "choice.php?pwd&whichchoice=1445&part=cpus&show=cpus&option=2&p=robot_resist";
    expected = "Upgrading your CPU with Weather Control Algorithms for 40 energy.";
    assertTrue(YouRobotManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);

    urlString = "choice.php?whichchoice=1445&show=top";
    expected = "Inspecting Top Attachment options at the Reassembly Station.";
    assertTrue(YouRobotManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);

    urlString = "choice.php?pwd&whichchoice=1445&part=top&show=top&option=1&p=1";
    expected = "Installing Pea Shooter as your Top Attachment for 5 scrap.";
    assertTrue(YouRobotManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);

    urlString = "choice.php?whichchoice=1445&show=left";
    expected = "Inspecting Left Arm options at the Reassembly Station.";
    assertTrue(YouRobotManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);

    urlString = "choice.php?pwd&whichchoice=1445&part=left&show=left&option=1&p=4";
    expected = "Installing Vice Grips as your Left Arm for 15 scrap.";
    assertTrue(YouRobotManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);

    urlString = "choice.php?whichchoice=1445&show=right";
    expected = "Inspecting Right Arm options at the Reassembly Station.";
    assertTrue(YouRobotManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);

    urlString = "choice.php?pwd&whichchoice=1445&part=right&show=right&option=1&p=8";
    expected = "Installing Surplus Flamethrower as your Right Arm for 40 scrap.";
    assertTrue(YouRobotManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);

    urlString = "choice.php?whichchoice=1445&show=bottom";
    expected = "Inspecting Propulsion System options at the Reassembly Station.";
    assertTrue(YouRobotManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);

    urlString = "choice.php?pwd&whichchoice=1445&part=bottom&show=bottom&option=1&p=7";
    expected = "Installing Snowplow as your Propulsion System for 30 scrap.";
    assertTrue(YouRobotManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);

    urlString = "choice.php?pwd&whichchoice=1447&option=3";
    expected = "Spending 20 energy to upgrade Moxie by 5 points.";
    Preferences.setInteger("statbotUses", 10);
    assertTrue(YouRobotManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
  }

  @Test
  public void canPayUpgradeCosts() throws IOException {
    KoLCharacter.setYouRobotEnergy(100);
    KoLCharacter.setYouRobotScraps(100);

    // Install Tesla Blaster, lose Shoot Pea, gain Tesla Blast
    String urlString = "choice.php?pwd&whichchoice=1445&part=top&show=top&option=1&p=7";
    String responseText = loadHTMLResponse("request/test_scrapheap_add_skill.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1445;
    YouRobotManager.postChoice1(urlString, request);
    assertEquals(70, KoLCharacter.getYouRobotScraps());

    urlString = "choice.php?pwd&whichchoice=1445&part=cpus&show=cpus&option=2&p=robot_resist";
    responseText = loadHTMLResponse("request/test_scrapheap_cpu_upgrade.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1445;
    YouRobotManager.postChoice1(urlString, request);
    assertEquals(60, KoLCharacter.getYouRobotEnergy());
  }

  @Test
  public void canTrackChangesInCombatSkills() throws IOException {
    // Start with no known combat skills.

    assertFalse(KoLCharacter.availableCombatSkill(SkillPool.SHOOT_PEA));
    assertFalse(KoLCharacter.availableCombatSkill(SkillPool.TESLA_BLAST));
    assertEquals(0, Preferences.getInteger("youRobotTop"));

    // Look at Top Attachments
    String urlString = "choice.php?whichchoice=1445&show=top";
    String responseText = loadHTMLResponse("request/test_scrapheap_show_top.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1445;
    YouRobotManager.visitChoice(request);
    assertEquals(4, Preferences.getInteger("youRobotTop"));

    // Install Pea Shooter, learn Shoot Pea
    urlString = "choice.php?pwd&whichchoice=1445&part=top&show=top&option=1&p=1";
    responseText = loadHTMLResponse("request/test_scrapheap_add_skill.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1445;
    YouRobotManager.postChoice1(urlString, request);
    assertEquals(1, Preferences.getInteger("youRobotTop"));
    // *** Failure! YouRobotManager does not learn combat skills
    assertTrue(KoLCharacter.availableCombatSkill(SkillPool.SHOOT_PEA));

    // Install Tesla Blaster, lose Shoot Pea, gain Tesla Blast
    urlString = "choice.php?pwd&whichchoice=1445&part=top&show=top&option=1&p=7";
    responseText = loadHTMLResponse("request/test_scrapheap_add_skill_lose_skill.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1445;
    YouRobotManager.postChoice1(urlString, request);
    assertEquals(7, Preferences.getInteger("youRobotTop"));
    assertFalse(KoLCharacter.availableCombatSkill(SkillPool.SHOOT_PEA));
    // *** Failure! YouRobotManager does not learn combat skills
    assertTrue(KoLCharacter.availableCombatSkill(SkillPool.TESLA_BLAST));

    // Install Solar Panel, lost Tesla Blast
    urlString = "choice.php?pwd&whichchoice=1445&part=top&show=top&option=1&p=3";
    responseText = loadHTMLResponse("request/test_scrapheap_lose_skill.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1445;
    YouRobotManager.postChoice1(urlString, request);
    assertEquals(3, Preferences.getInteger("youRobotTop"));
    assertFalse(KoLCharacter.availableCombatSkill(SkillPool.TESLA_BLAST));
  }

  @Test
  public void willUnequipWhenSwapOutEquipPart() throws IOException {
    // Look at Top Attachments
    String urlString = "choice.php?whichchoice=1445&show=top";
    String responseText = loadHTMLResponse("request/test_scrapheap_show_top.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1445;
    YouRobotManager.visitChoice(request);
    assertEquals(4, Preferences.getInteger("youRobotTop"));

    // That is a Mannequin Head, which allows you to equip hats.
    assertTrue(EquipmentManager.canEquip(ItemPool.HELMET_TURTLE));

    // Put one on
    AdventureResult hat = ItemPool.get(ItemPool.HELMET_TURTLE, 1);
    EquipmentManager.setEquipment(EquipmentManager.HAT, hat);
    assertTrue(hat.equals(EquipmentManager.getEquipment(EquipmentManager.HAT)));

    // Install Pea Shooter
    urlString = "choice.php?pwd&whichchoice=1445&part=top&show=top&option=1&p=1";
    responseText = loadHTMLResponse("request/test_scrapheap_add_skill.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1445;
    YouRobotManager.postChoice1(urlString, request);

    // Verify that we are no longer wearing a hat
    assertEquals(EquipmentRequest.UNEQUIP, EquipmentManager.getEquipment(EquipmentManager.HAT));
  }

  @Test
  public void willUnsetFamiliarWhenUnequipBirdCage() throws IOException {
    // Look at Top Attachments
    String urlString = "choice.php?whichchoice=1445&show=top";
    String responseText = loadHTMLResponse("request/test_scrapheap_show_top_bird_cage.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1445;
    YouRobotManager.visitChoice(request);
    assertEquals(2, Preferences.getInteger("youRobotTop"));

    // That is a Bird Cage, which allows you to equip familiars.
    FamiliarData familiar = new FamiliarData(FamiliarPool.EMO_SQUID);
    assertTrue(familiar.canEquip());

    // Take it with you!
    KoLCharacter.setFamiliar(familiar);
    assertEquals(familiar, KoLCharacter.getFamiliar());

    // Install Mannequin Head
    urlString = "choice.php?pwd&whichchoice=1445&part=top&show=top&option=1&p=4";
    responseText = loadHTMLResponse("request/test_scrapheap_unequip_bird_cage.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1445;
    YouRobotManager.postChoice1(urlString, request);
    assertEquals(4, Preferences.getInteger("youRobotTop"));

    // Verify that we no longer have our familiar
    assertEquals(FamiliarData.NO_FAMILIAR, KoLCharacter.getFamiliar());
  }

  @Test
  public void willAddCPUUpgrades() throws IOException {
    // Look at CPU Upgrades
    String urlString = "choice.php?whichchoice=1445&show=cpus";
    String responseText = loadHTMLResponse("request/test_scrapheap_show_cpus.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1445;
    YouRobotManager.visitChoice(request);

    String[] keywords = Preferences.getString("youRobotCPUUpgrades").split(",");
    Set<String> cpus = new HashSet<>(Arrays.asList(keywords));
    assertEquals(7, cpus.size());
    assertTrue(cpus.contains("robot_energy"));
    assertTrue(cpus.contains("robot_potions"));
    assertTrue(cpus.contains("robot_meat"));
    assertTrue(cpus.contains("robot_items"));
    assertTrue(cpus.contains("robot_shirt"));
    assertTrue(cpus.contains("robot_hp1"));
    assertTrue(cpus.contains("robot_hp2"));

    // Buy a CPU Upgrade
    urlString = "choice.php?pwd&whichchoice=1445&part=cpus&show=cpus&option=2&p=robot_resist";
    responseText = loadHTMLResponse("request/test_scrapheap_cpu_upgrade.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1445;
    YouRobotManager.postChoice1(urlString, request);

    keywords = Preferences.getString("youRobotCPUUpgrades").split(",");
    cpus = new HashSet<>(Arrays.asList(keywords));
    assertEquals(8, cpus.size());
    assertTrue(cpus.contains("robot_energy"));
    assertTrue(cpus.contains("robot_potions"));
    assertTrue(cpus.contains("robot_meat"));
    assertTrue(cpus.contains("robot_items"));
    assertTrue(cpus.contains("robot_shirt"));
    assertTrue(cpus.contains("robot_hp1"));
    assertTrue(cpus.contains("robot_hp2"));
    assertTrue(cpus.contains("robot_resist"));
  }

  private JButton findItemManagePanelButton(ItemManagePanel panel, String name) {
    Optional<JButton> buttonSearch =
        Arrays.stream(panel.buttons).filter(b -> b.getText().equals(name)).findFirst();
    assertTrue(buttonSearch.isPresent());
    return buttonSearch.get();
  }

  @Test
  public void willAllowPotionUsage() throws IOException {
    TestListener potionListener = new TestListener("(potions)");

    // Start with no CPU upgrades. We cannot use potions.
    assertFalse(YouRobotManager.canUsePotions());

    // Make ItemManager potion panels
    UseItemEnqueuePanel enqueue = new UseItemEnqueuePanel(ConcoctionType.POTION, null);
    JButton enqueueButton = findItemManagePanelButton(enqueue, "consume");
    assertFalse(enqueueButton.isEnabled());

    UseItemDequeuePanel dequeue = new UseItemDequeuePanel(ConcoctionType.POTION);
    JButton dequeueButton = findItemManagePanelButton(dequeue, "consume");
    assertFalse(dequeueButton.isEnabled());

    // Look at CPU Upgrades
    String urlString = "choice.php?whichchoice=1445&show=cpus";
    String responseText = loadHTMLResponse("request/test_scrapheap_show_cpus.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1445;
    YouRobotManager.visitChoice(request);

    // Verify that our listener fired
    assertEquals(1, potionListener.getCalls());

    // Now we can use potions
    assertTrue(YouRobotManager.canUsePotions());

    // And the ItemManager GUI knows it.
    assertTrue(enqueueButton.isEnabled());
    assertTrue(dequeueButton.isEnabled());

    // Parse the same CPU Upgrade page
    YouRobotManager.visitChoice(request);

    // Verify that our listener did not fire
    assertEquals(1, potionListener.getCalls());
  }

  @Test
  public void canSetAvatarAndGetSignal() throws IOException {
    TestListener avatarListener = new TestListener("(avatar)");

    // We started out with an avatar = ""
    // Verify that if we set the same avatar, our listener doesn't fire.
    KoLCharacter.setAvatar("");
    assertEquals(0, avatarListener.getCalls());

    // Female Accordion Thief
    KoLCharacter.setAvatar("otherimages/classav6b_f.gif");
    assertEquals(1, avatarListener.getCalls());

    // Load a 3-part robot
    String responseText = loadHTMLResponse("request/test_scrapheap_three_part_avatar.html");

    ChoiceManager.lastChoice = 1445;
    GenericRequest request = new GenericRequest("choice.php?forceoption=0");
    request.responseText = responseText;
    YouRobotManager.visitChoice(request);

    // Our Listener fired
    assertEquals(2, avatarListener.getCalls());

    // Do it again with the same image
    YouRobotManager.visitChoice(request);
    assertEquals(2, avatarListener.getCalls());

    // Load a 4-part robot
    responseText = loadHTMLResponse("request/test_scrapheap_reassembly_station.html");
    ChoiceManager.lastChoice = 1445;
    request = new GenericRequest("choice.php?forceoption=0");
    request.responseText = responseText;
    YouRobotManager.visitChoice(request);

    // Our Listener fired
    assertEquals(3, avatarListener.getCalls());

    // Do it again with the same image
    YouRobotManager.visitChoice(request);
    assertEquals(3, avatarListener.getCalls());

    // Call setAvatar directly with the current images
    KoLCharacter.setAvatar(KoLCharacter.getAvatar());
    assertEquals(3, avatarListener.getCalls());
  }

  @Test
  public void canTrackStatbotEnergyCost() throws IOException {
    KoLCharacter.setYouRobotEnergy(100);

    String urlString = "choice.php?forceoption=0";
    String responseText = loadHTMLResponse("request/test_scrapheap_visit_statbot.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1447;
    assertEquals(0, Preferences.getInteger("statbotUses"));
    YouRobotManager.visitChoice(request);
    assertEquals(11, Preferences.getInteger("statbotUses"));

    urlString = "choice.php?pwd&whichchoice=1447&option=1";
    responseText = loadHTMLResponse("request/test_scrapheap_activate_statbot.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1447;
    YouRobotManager.postChoice1(responseText, request);
    assertEquals(11, Preferences.getInteger("statbotUses"));
    assertEquals(100 - 20, KoLCharacter.getYouRobotEnergy());
  }
}
