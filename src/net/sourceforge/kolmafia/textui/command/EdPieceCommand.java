package net.sourceforge.kolmafia.textui.command;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class EdPieceCommand extends AbstractCommand implements ModeCommand {
  private enum Animal {
    BEAR("bear", "muscle", 1, "Muscle: +20; +2 Muscle Stats Per Fight"),
    OWL("owl", "mysticality", 2, "Mysticality: +20; +2 Mysticality Stats Per Fight"),
    PUMA("puma", "moxie", 3, "Moxie: +20; +2 Moxie Stats Per Fight"),
    HYENA("hyena", "monster level", 4, "+20 to Monster Level"),
    MOUSE("mouse", "item/meat", 5, "+10% Item Drops From Monsters; +20% Meat from Monsters"),
    WEASEL(
        "weasel",
        "block/HP regen",
        6,
        "The first attack against you will always miss; Regenerate 10-20 HP per Adventure"),
    FISH("fish", "sea", 7, "Lets you breath Adventure");

    private final String name;
    private final String description;
    private final int decision;
    private final String effect;

    Animal(final String name, final String description, final int decision, final String effect) {
      this.name = name;
      this.description = description;
      this.decision = decision;
      this.effect = effect;
    }

    public String getName() {
      return this.name;
    }

    public String getDescription() {
      return description;
    }

    public int getDecision() {
      return decision;
    }

    public String getEffect() {
      return effect;
    }
  }

  private static Animal getModeFromDecision(int decision) {
    return Arrays.stream(Animal.values())
        .filter(a -> decision == a.getDecision())
        .findAny()
        .orElse(null);
  }

  public static String getStateFromDecision(int decision) {
    var animal = getModeFromDecision(decision);
    return animal != null ? animal.getName() : null;
  }

  public EdPieceCommand() {
    this.usage =
        "[?] <animal> - place a golden animal on the Crown of Ed (and equip it if unequipped)";
  }

  @Override
  public boolean validate(final String command, final String parameters) {
    return Arrays.stream(Animal.values()).anyMatch(a -> parameters.equalsIgnoreCase(a.getName()));
  }

  @Override
  public String normalize(String parameters) {
    return parameters;
  }

  @Override
  public Set<String> getModes() {
    return Arrays.stream(Animal.values()).map(Animal::getName).collect(Collectors.toSet());
  }

  @Override
  public void run(final String cmd, String parameters) {
    boolean checking = KoLmafiaCLI.isExecutingCheckOnlyCommand;

    if (checking) {
      StringBuilder output = new StringBuilder();

      output.append("<table border=2 cols=5>");
      output.append("<tr>");
      output.append("<th>Decoration</th>");
      output.append("<th>Effect</th>");
      output.append("</tr>");
      for (var animal : Animal.values()) {
        output.append("<tr>");
        output.append("<td valign=top>");
        output.append("golden ");
        output.append(animal.getName());
        output.append("</td>");
        output.append("<td valign=top>");
        output.append(animal.getEffect());
        output.append("</td>");
        output.append("</tr>");
      }

      output.append("</table>");

      RequestLogger.printLine(output.toString());
      RequestLogger.printLine();

      parameters = "";
    }

    String currentAnimal = Preferences.getString("edPiece");
    if (parameters.length() == 0) {
      StringBuilder output = new StringBuilder();

      output.append("The current decoration on The Crown of Ed the Undying is ");
      if (currentAnimal.equals("")) {
        output.append("&lt;nothing&gt;");
      } else {
        output.append("a golden ");
        output.append(currentAnimal);
      }
      output.append(".");

      RequestLogger.printLine(output.toString());
      RequestLogger.printLine();
      return;
    }

    String animal = parameters;
    int choice = 0;

    for (var a : Animal.values()) {
      if (animal.equalsIgnoreCase(a.getName()) || a.getDescription().contains(animal)) {
        choice = a.getDecision();
        animal = a.getName();
        break;
      }
    }

    if (choice == 0) {
      var values =
          StringUtilities.listToHumanString(
              Arrays.stream(Animal.values()).map(Animal::getName).toList());
      KoLmafia.updateDisplay(
          "Animal " + animal + " not recognised. Valid values are " + values + ".");
      return;
    }

    if (EquipmentManager.getEquipment(Slot.HAT).getItemId() != ItemPool.CROWN_OF_ED) {
      AdventureResult edPiece = ItemPool.get(ItemPool.CROWN_OF_ED);
      RequestThread.postRequest(new EquipmentRequest(edPiece, Slot.HAT));
    }

    if (animal.equalsIgnoreCase(currentAnimal)) {
      KoLmafia.updateDisplay("Animal " + animal + " already equipped.");
      return;
    }

    if (KoLmafia.permitsContinue()) {
      RequestThread.postRequest(new GenericRequest("inventory.php?action=activateedhat"));
    }
    if (KoLmafia.permitsContinue()) {
      RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1063&option=" + choice));
    }
    if (KoLmafia.permitsContinue()) {
      KoLmafia.updateDisplay("Crown of Ed decorated with golden " + animal + ".");
    }
  }
}
