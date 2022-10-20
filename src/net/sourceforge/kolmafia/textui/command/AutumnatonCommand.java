package net.sourceforge.kolmafia.textui.command;

import static net.sourceforge.kolmafia.session.AutumnatonManager.useAutumnaton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureDatabase.DifficultyLevel;
import net.sourceforge.kolmafia.persistence.AdventureDatabase.Environment;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.AutumnatonManager;
import net.sourceforge.kolmafia.session.InventoryManager;

public class AutumnatonCommand extends AbstractCommand {
  public AutumnatonCommand() {
    this.usage = " <blank> | upgrade | locations | send [location] - deal with your autumn-aton";
  }

  private static final Pattern UPGRADE_PATTERN = Pattern.compile("Attach the (.+) that you found.");

  @Override
  public void run(final String cmd, String parameters) {
    if (!Preferences.getBoolean("hasAutumnaton")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You need an autumn-aton.");
      return;
    }

    String[] params = parameters.split(" ", 2);

    switch (params[0]) {
      case "" -> status();
      case "send" -> send(params);
      case "upgrade" -> upgrade();
      case "locations" -> locations();
      default -> KoLmafia.updateDisplay(MafiaState.ERROR, "Usage: autumnaton" + this.usage);
    }
  }

  public void status() {
    var autumnLocation = Preferences.getString("autumnatonQuestLocation");
    if (autumnLocation.equals("")) {
      if (!InventoryManager.hasItem(ItemPool.AUTUMNATON)) {
        RequestLogger.printLine("Your autumn-aton is in an unknown location.");
      } else {
        RequestLogger.printLine("Your autumn-aton is ready to be sent somewhere.");
        String response = useAutumnaton();
        var upgrades = UPGRADE_PATTERN.matcher(response);
        if (upgrades.find()) {
          RequestLogger.printLine("Your autumn-aton has upgrades available: " + upgrades.group(1));
        }
      }
    } else {
      RequestLogger.printLine("Your autumn-aton is plundering in " + autumnLocation + ".");
      RequestLogger.printLine(turnsRemainingString());
    }
  }

  public void send(String[] params) {
    if (!InventoryManager.hasItem(ItemPool.AUTUMNATON)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Your autumn-aton is away.");
      return;
    }

    String parameter;
    if (params.length < 2 || (parameter = params[1].trim()).equals("")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Where do you want to send the little guy?");
      return;
    }

    KoLAdventure adventure = AdventureDatabase.getAdventure(parameter);
    if (adventure == null) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "I don't understand where " + parameter + " is.");
      return;
    }

    var advName = adventure.getAdventureName();

    if (!adventure.hasSnarfblat()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, advName + " is not a valid location");
      return;
    }

    KoLmafia.updateDisplay("Sending autumn-aton to " + advName);

    useAutumnaton();

    var request =
        new GenericRequest(
            "choice.php?whichchoice=1483&option=2&heythereprogrammer="
                + adventure.getAdventureId());
    RequestThread.postRequest(request);

    var sentTo = Preferences.getString("autumnatonQuestLocation");
    if ("".equals(sentTo)) {
      // perhaps tried to access a location that was inaccessible
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "Failed to send autumnaton to " + advName + ". Is it accessible?");
    } else {
      KoLmafia.updateDisplay("Sent autumn-aton to " + sentTo + ".");
      RequestLogger.printLine(turnsRemainingString());
    }
  }

  public void upgrade() {
    if (!InventoryManager.hasItem(ItemPool.AUTUMNATON)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Your autumn-aton is away.");
      return;
    }

    String response = useAutumnaton();

    var upgrades = UPGRADE_PATTERN.matcher(response);
    if (!upgrades.find()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "No upgrades available");
      return;
    }

    GenericRequest request = new GenericRequest("choice.php?whichchoice=1483&option=1");
    RequestThread.postRequest(request);

    var s = upgrades.group(1).contains(" and ") ? "s" : "";

    KoLmafia.updateDisplay("Added upgrade" + s + " " + upgrades.group(1));
  }

  private String turnsRemainingString() {
    var turns = Preferences.getInteger("autumnatonQuestTurn") - KoLCharacter.getTurnsPlayed();
    if (turns > 0) {
      var s = turns == 1 ? "" : "s";
      return "Your autumn-aton will return after " + turns + " turn" + s + ".";
    } else {
      return "Your autumn-aton will return after your next combat.";
    }
  }

  public void locations() {
    if (!InventoryManager.hasItem(ItemPool.AUTUMNATON)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Your autumn-aton is away.");
      return;
    }

    String response = useAutumnaton();
    var locs = AutumnatonManager.parseLocations(response);

    Map<Environment, Map<DifficultyLevel, List<KoLAdventure>>> locMapping = new HashMap<>();

    for (var id : locs) {
      var adv = AdventureDatabase.getAdventure(id);
      var env = locMapping.computeIfAbsent(adv.getEnvironment(), m -> new HashMap<>());
      env.computeIfAbsent(adv.getDifficultyLevel(), m -> new ArrayList<>()).add(adv);
    }

    StringBuilder output = new StringBuilder();

    output
        .append("You can send your autumn-aton to ")
        .append(locs.size())
        .append(" locations.\n\n");

    addEnvironmentLocations(output, locMapping, Environment.OUTDOOR);
    addEnvironmentLocations(output, locMapping, Environment.INDOOR);
    addEnvironmentLocations(output, locMapping, Environment.UNDERGROUND);

    RequestLogger.printLine(output.toString());
  }

  private void addEnvironmentLocations(
      StringBuilder output,
      Map<Environment, Map<DifficultyLevel, List<KoLAdventure>>> locMapping,
      Environment env) {
    var dl = locMapping.get(env);
    if (dl != null) {
      output.append("<b>").append(env.toTitle()).append("</b><br>");
      addDiffLevelLocations(output, dl, env, DifficultyLevel.LOW);
      addDiffLevelLocations(output, dl, env, DifficultyLevel.MID);
      addDiffLevelLocations(output, dl, env, DifficultyLevel.HIGH);
      addDiffLevelLocations(output, dl, env, DifficultyLevel.UNKNOWN);
    }
  }

  private void addDiffLevelLocations(
      StringBuilder output,
      Map<DifficultyLevel, List<KoLAdventure>> dlMapping,
      Environment env,
      DifficultyLevel level) {
    var advs = dlMapping.get(level);
    if (advs != null) {
      output.append(diffLevelDescription(env, level)).append(" <ul>");

      for (var item : advs) {
        output.append("<li>").append(item.getAdventureName()).append("</li>");
      }

      output.append("</ul>");
    }
  }

  private String diffLevelDescription(Environment env, DifficultyLevel level) {
    var title = level.toTitle();
    var desc =
        switch (env) {
          case OUTDOOR -> switch (level) {
            case LOW -> "gives autumn leaf (potion, +25% item, +5% combat chance)";
            case MID -> "gives autumn debris shield (shield, +10 DR, +30% init, +20 all hot)";
            case HIGH -> "gives autumn leaf pendant (accessory, +5 fam weight, +10 fam damage, +1 fam exp)";
            default -> "";
          };
          case INDOOR -> switch (level) {
            case LOW -> "gives AutumnFest Ale (booze, 1 drunk, 4-6 advs)";
            case MID -> "gives autumn-spice donut (food, 1 full, 4-6 advs)";
            case HIGH -> "gives autumn breeze (spleen, 1 toxicity, +100% all stats)";
            default -> "";
          };
          case UNDERGROUND -> switch (level) {
            case LOW -> "gives autumn sweater-weather sweater (shirt, +3 cold res, +50 HP, +8-12 HP Regen)";
            case MID -> "gives autumn dollar (potion, +50% meat)";
            case HIGH -> "gives autumn years wisdom (potion, +20% spell dmg, +100 MP, +15-20 MP Regen)";
            default -> "";
          };
          default -> "";
        };
    return title + ": " + desc + upgradeDescription(env, level);
  }

  private record Upgrade(String name, String description) {}

  private Optional<Upgrade> upgrade(Environment env, DifficultyLevel level) {
    return switch (env) {
      case OUTDOOR -> switch (level) {
        case LOW -> Optional.of(new Upgrade("energy-absorptive hat", "+2 base exp gain"));
        case MID -> Optional.of(new Upgrade("high performance right arm", "+1 zone item"));
        case HIGH -> Optional.of(new Upgrade("vision extender", "+1 visual acuity"));
        default -> Optional.empty();
      };
      case INDOOR -> switch (level) {
        case LOW -> Optional.of(new Upgrade("enhanced left arm", "+1 zone item"));
        case MID -> Optional.of(new Upgrade("high speed right leg", "-11 expedition turns"));
        case HIGH -> Optional.of(new Upgrade("radar dish", "+1 visual acuity"));
        default -> Optional.empty();
      };
      case UNDERGROUND -> switch (level) {
        case LOW -> Optional.of(new Upgrade("upgraded left leg", "-11 expedition turns"));
        case MID -> Optional.of(new Upgrade("collection prow", "+1 autumn item"));
        case HIGH -> Optional.of(new Upgrade("dual exhaust", "+2 base exp gain"));
        default -> Optional.empty();
      };
      default -> Optional.empty();
    };
  }

  private String upgradeDescription(Environment env, DifficultyLevel level) {
    var upgrade = upgrade(env, level);
    if (upgrade.isEmpty()) return "";
    var u = upgrade.get();
    if (AutumnatonManager.hasUpgrade(u.name)) return "";
    return ", " + u.name + " (" + u.description + ")";
  }
}
