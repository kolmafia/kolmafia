package net.sourceforge.kolmafia.textui.command;

import static net.sourceforge.kolmafia.session.LeprecondoManager.useLeprecondo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.LeprecondoManager.Furniture;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class LeprecondoCommand extends AbstractCommand {
  public LeprecondoCommand() {
    this.usage = " <blank> | furnish [a,b,c,d] | available | missing  - deal with your Leprecondo";
  }

  @Override
  public void run(final String cmd, String parameters) {
    if (InventoryManager.getCount(ItemPool.LEPRECONDO) == 0) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You need a Leprecondo.");
      return;
    }

    String[] params = parameters.split(" ", 2);

    switch (params[0]) {
      case "" -> status();
      case "furnish" -> furnish(params);
      case "available" -> available();
      case "missing" -> missing();
      default -> KoLmafia.updateDisplay(MafiaState.ERROR, "Usage: leprecondo" + this.usage);
    }
  }

  public void status() {
    var output = new StringBuilder("You have the following furniture installed:<ul>");
    Arrays.stream(Preferences.getString("leprecondoInstalled").split(","))
        .map(StringUtilities::parseInt)
        .map(Furniture::byId)
        .forEachOrdered(
            f -> output.append("<li>").append(f == null ? "nothing" : f.getName()).append("</li>"));
    output.append("</ul>");

    RequestLogger.printHtml(output.toString());
  }

  public void furnish(String[] params) {
    if (Preferences.getInteger("_leprecondoRearrangements") >= 3) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "All leprecondo rearrangements used today");
    }

    String parameter;
    String[] furnitureInputs;
    if (params.length < 2
        || (parameter = params[1].trim()).isEmpty()
        || (furnitureInputs = parameter.split(",")).length != 4) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Usage: leprecondo furnish a,b,c,d");
      return;
    }

    var ownedFurnitureIds = discoveredFurnitureIds().collect(Collectors.toSet());
    var possibleFurnitures =
        Arrays.stream(Furniture.values())
            .filter(x -> ownedFurnitureIds.contains(x.getId()))
            .map(Furniture::getName)
            .map(String::toLowerCase)
            .toList();
    var chosenFurnitures = new ArrayList<Furniture>();
    for (var furn : furnitureInputs) {
      var potentials =
          StringUtilities.getMatchingNames(possibleFurnitures, furn.trim().toLowerCase());
      if (potentials.size() > 1) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Ambiguous furniture name: " + furn);
        return;
      }
      if (potentials.isEmpty()) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Unrecognised furniture name: " + furn);
        return;
      }
      chosenFurnitures.add(Furniture.byName(potentials.get(0)));
    }

    KoLmafia.updateDisplay(
        "Furnishing Leprecondo with "
            + chosenFurnitures.stream().map(Furniture::getName).collect(Collectors.joining(", ")));

    useLeprecondo();

    var request =
        new GenericRequest(
            "choice.php?whichchoice=1556&option=1&r0="
                + chosenFurnitures.get(0).getId()
                + "&r1="
                + chosenFurnitures.get(1).getId()
                + "&r2="
                + chosenFurnitures.get(2).getId()
                + "&r3="
                + chosenFurnitures.get(3).getId());
    RequestThread.postRequest(request);
  }

  public void available() {
    var discovered = discoveredFurnitureIds().map(Furniture::byId);
    var output =
        new StringBuilder(
            "<table border=1><tr><th>Furniture</th><th>Need 1</th><th>Need 2</th></tr>");
    discovered.forEachOrdered(
        f -> {
          output.append("<tr><td>").append(f.getName()).append("</td>");
          var needs = f.getNeeds().keySet().toArray();
          if (needs.length == 2) {
            output
                .append("<td>")
                .append(needs[0])
                .append("</td><td>")
                .append(needs[1])
                .append("</td></tr>");
          } else {
            output.append("<td colspan=2>").append(needs[0]).append("</td></tr>");
          }
        });
    output.append("</table>");

    RequestLogger.printHtml(output.toString());
  }

  public void missing() {
    var discovered = discoveredFurnitureIds().collect(Collectors.toSet());
    var output = new StringBuilder("<table border=1><tr><th>Furniture</th><th>Location</th></tr>");
    Arrays.stream(Furniture.values())
        .filter(x -> !discovered.contains(x.getId()))
        .forEachOrdered(
            f ->
                output
                    .append("<tr><td>")
                    .append(f.getName())
                    .append("</td><td>")
                    .append(f.getLocation())
                    .append("</td></tr>"));
    output.append("</table>");

    RequestLogger.printHtml(output.toString());
  }

  private Stream<Integer> discoveredFurnitureIds() {
    return Arrays.stream(Preferences.getString("leprecondoDiscovered").split(","))
        .map(StringUtilities::parseInt);
  }
}
