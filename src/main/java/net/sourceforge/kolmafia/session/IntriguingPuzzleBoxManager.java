package net.sourceforge.kolmafia.session;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
import net.sourceforge.kolmafia.utilities.PHPMTRandom;

public class IntriguingPuzzleBoxManager {
  static final String OPTIONS_PREF = "intriguingPuzzleBoxOptions";
  static final String OUTCOMES_PREF = "intriguingPuzzleBoxOutcomes";
  private static final String STEP_PREF = "intriguingPuzzleBoxStep";

  private static final Map<String, String> FACE_MAP =
      Map.of("left", "L", "right", "R", "top", "T", "bottom", "B", "front", "F", "rear", "K");

  private static final Pattern OPTION =
      Pattern.compile(
          "(turn|push) the (left|right|top|bottom|front|rear) face(?: (clockwise|counterclockwise))?");

  private static final Pattern SAY_PATTERN = Pattern.compile("says &quot;(\\d+)&quot;");

  static final List<String> ACTIONS =
      List.of(
          "LCW", "LCCW", "RCW", "RCCW", "TCW", "TCCW", "BCW", "BCCW", "FCW", "FCCW", "KCW", "KCCW",
          "PL", "PR", "PT", "PB", "PF", "PK");

  static String parseOption(final String option) {
    var m = OPTION.matcher(option);
    if (!m.matches()) return null;
    var face = FACE_MAP.get(m.group(2));
    var direction = m.group(3);
    if (m.group(1).equals("push")) return direction == null ? "P" + face : null;
    if (direction == null) return null;
    return face + (direction.equals("clockwise") ? "CW" : "CCW");
  }

  // The outcome of a decision, from the result line. Null for text we don't recognise.
  static String parseOutcome(final String responseText) {
    if (responseText.contains("harsh buzzing noise")) return "buzz";
    if (responseText.contains("quiet 'click'")) return "click";
    if (responseText.contains("loud 'beep'")) return "beep";
    var say = SAY_PATTERN.matcher(responseText);
    if (say.find()) return "say" + say.group(1);
    return null;
  }

  // Each step since the last buzz offsets the seed by 99.
  static long getOptionsSeed(final int seed, final int step) {
    return seed + 99L * step;
  }

  static long getOutcomeSeed(final int seed, final int step, final int decision) {
    return getOptionsSeed(seed, step) + decision;
  }

  static List<String> getOptions(final long seed) {
    PHPMTRandom rng = new PHPMTRandom(seed);
    return IntStream.generate(() -> rng.nextInt(0, ACTIONS.size() - 1))
        .mapToObj(ACTIONS::get)
        .distinct()
        .limit(6)
        .toList();
  }

  static String getOutcome(final int seed, final int decision, final int step) {
    PHPMTRandom rng = new PHPMTRandom(getOutcomeSeed(seed, step, decision));
    int roll = rng.nextInt(1, 6);
    return switch (roll) {
      case 6 -> "buzz";
      case 5 -> "say" + rng.nextInt(1, 18);
      case 4 -> "beep";
      default -> "click";
    };
  }

  private static List<String> entries(final String pref) {
    return pref.isBlank() ? List.of() : List.of(pref.split("\\|"));
  }

  // The option codes shown at each step, ordered by step. An entry is "step:codes".
  static TreeMap<Integer, List<String>> parseOptions() {
    return entries(Preferences.getString(OPTIONS_PREF)).stream()
        .map(entry -> entry.split(":"))
        .collect(
            Collectors.toMap(
                parts -> Integer.parseInt(parts[0]),
                parts -> List.of(parts[1].split(",")),
                (a, b) -> a,
                TreeMap::new));
  }

  // The recorded outcomes, by step and then decision. An entry is "step,decision:outcome".
  static TreeMap<Integer, TreeMap<Integer, String>> parseOutcomes() {
    return entries(Preferences.getString(OUTCOMES_PREF)).stream()
        .map(entry -> entry.split(":"))
        .collect(
            Collectors.groupingBy(
                parts -> Integer.parseInt(parts[0].split(",")[0]),
                TreeMap::new,
                Collectors.toMap(
                    parts -> Integer.parseInt(parts[0].split(",")[1]),
                    parts -> parts[1],
                    (a, b) -> a,
                    TreeMap::new)));
  }

  public static void visitChoice(final String responseText, final int decision) {
    var choices = ChoiceUtilities.parseChoices(responseText);
    var codes =
        choices.values().stream()
            .map(text -> parseOption(text.replaceAll("\\.$", "").toLowerCase()))
            .filter(Objects::nonNull)
            .toList();

    // If something didn't parse right, do nothing.
    if (codes.size() != 6) return;

    var options = parseOptions();
    var outcomes = parseOutcomes();
    int step = Preferences.getInteger(STEP_PREF);

    if (decision == 0) {
      // Showing the step we're already on is a reload, so do nothing.
      if (step >= 0 && codes.equals(options.get(step))) return;
      // Any other page means the box was started again, so count from step 0 again.
      step = 0;
    } else {
      // A button press needs a known step and an options entry to attach it to.
      if (step < 0 || options.isEmpty()) return;

      var outcome = parseOutcome(responseText);
      if (outcome == null) {
        // Without the outcome we can't tell what step comes next, so stop recording steps.
        step = -1;
      } else {
        outcomes.computeIfAbsent(step, s -> new TreeMap<>()).putIfAbsent(decision, outcome);
        step = outcome.equals("buzz") ? 0 : step + 1;
      }
    }

    if (step >= 0) options.putIfAbsent(step, codes);
    save(options, outcomes, step);
  }

  private static void save(
      final TreeMap<Integer, List<String>> options,
      final Map<Integer, ? extends Map<Integer, String>> outcomes,
      final int step) {
    Preferences.setString(
        OPTIONS_PREF,
        options.entrySet().stream()
            .map(e -> e.getKey() + ":" + String.join(",", e.getValue()))
            .collect(Collectors.joining("|")));

    Preferences.setString(
        OUTCOMES_PREF,
        outcomes.entrySet().stream()
            .flatMap(
                s ->
                    s.getValue().entrySet().stream()
                        .map(d -> s.getKey() + "," + d.getKey() + ":" + d.getValue()))
            .collect(Collectors.joining("|")));

    Preferences.setInteger(STEP_PREF, step);
  }
}
