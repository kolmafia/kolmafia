package net.sourceforge.kolmafia.request;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ResearchBenchRequest extends GenericRequest {

  // There are hidden skill trees involved. For example, you cannot
  // research "rend1" until you have researched "mus3",
  //
  // I'd like to include that in this record.

  record Research(Integer key, String field, int cost, String parent, String name, String effect)
      implements Comparable<Research> {
    @Override
    public int compareTo(Research o) {
      return o == null ? -1 : this.key.compareTo(o.key);
    }
  }

  private static Set<Research> allResearch = new TreeSet<>();
  private static Map<String, Research> fieldToResearch = new HashMap<>();
  private static Set<Research> terminalResearch = new HashSet<>();

  private static void registerResearch(
      Integer index, String field, int cost, String parent, String name, String effect) {
    Research research = new Research(index, field, cost, parent, name, effect);
    allResearch.add(research);
    fieldToResearch.put(field, research);
    fieldToResearch.put("wereprof_" + field, research);
    if (cost == 100) {
      terminalResearch.add(research);
    }
  }

  static {
    try (BufferedReader reader =
        FileUtilities.getVersionedReader("wereprofessor.txt", KoLConstants.WEREPROFESSOR_VERSION)) {
      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length < 6) {
          continue;
        }

        Integer key = StringUtilities.parseInt(data[0]);
        String field = data[1];
        int cost = StringUtilities.parseInt(data[2]);
        String parent = data[3];
        String name = data[4];
        String effect = data[5];

        registerResearch(key, field, cost, parent, name, effect);
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  private final boolean visiting;
  public final String research;
  private final Research skillToResearch;

  public ResearchBenchRequest() {
    super("");
    this.visiting = true;
    this.research = null;
    this.skillToResearch = null;
  }

  public ResearchBenchRequest(final String research) {
    super("choice.php");
    this.addFormField("whichchoice", "1523");
    this.addFormField("option", "1");

    this.visiting = false;
    this.research = research;
    this.skillToResearch = fieldToResearch.get(research);

    if (this.skillToResearch == null) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Research '" + this.research + "' is not valid.");
      return;
    }

    String rfield = research.startsWith("wereprof_") ? research : "wereprof_" + research;
    this.addFormField("r", "wereprof_" + this.skillToResearch.field());
  }

  @Override
  protected boolean shouldFollowRedirect() {
    return true;
  }

  @Override
  public void run() {
    if (this.skillToResearch == null && !visiting) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Research '" + this.research + "' is not valid.");
      return;
    }

    if (!KoLCharacter.isMildManneredProfessor()) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "Only Mild-Mannered Professors can research at their Research Bench.");
      return;
    }

    // You can walk away from choice 1523, so this won't abort if we are
    // already there.
    if (GenericRequest.abortIfInFightOrChoice()) {
      return;
    }

    // If we are already handling choice 1523, no need to visit the Research Bench
    if (!ChoiceManager.handlingChoice || ChoiceManager.lastChoice != 1523) {
      PlaceRequest visitRequest =
          new PlaceRequest("wereprof_cottage", "wereprof_researchbench", true);
      visitRequest.run();
    }

    // If only requesting a visit, we're done here
    if (visiting) {
      return;
    }

    // Now that we have visited the Research Bench, we know which
    // skills are available to research and which are already known

    Set<Research> knownResearch = loadResearch(KNOWN_RESEARCH);
    if (knownResearch.contains(this.skillToResearch)) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "You have already researched '" + this.research + "'.");
      return;
    }

    Set<Research> availableResearch = loadResearch(AVAILABLE_RESEARCH);
    if (!availableResearch.contains(this.skillToResearch)) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "You cannot research '" + this.research + "' at this time.");
      return;
    }

    int rp = Preferences.getInteger("wereProfessorResearchPoints");
    if (this.skillToResearch.cost() > rp) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "You don't have enough rp to research '" + this.research + "'.");
      return;
    }

    super.run();
  }

  // *** Skill derivation ***
  //
  // Given a set of Research on sale at the Research Bench,
  // derive which Research you already have researched.

  public static Set<Research> deriveKnownResearch(Set<Research> available) {
    // For each "terminal" research
    //   if it is "available"
    //     everything before it is known
    //   else
    //      walk up tree discovering current "available"
    //      if none
    //         entire tree is known
    //      else
    //         everything before current "available" is known

    Set<Research> known = new HashSet<>();

    for (Research terminal : terminalResearch) {
      Research research = terminal;

      // Walk up the tree finding top unknown node
      while (research != null) {
        if (available.contains(research)) {
          break;
        }
        String parent = research.parent();
        research = parent.equals("none") ? null : fieldToResearch.get(parent);
      }

      Research top = research != null ? fieldToResearch.get(research.parent()) : terminal;
      while (top != null) {
        known.add(top);
        String parent = top.parent();
        top = parent.equals("none") ? null : fieldToResearch.get(parent);
      }
    }

    return known;
  }

  // *** Properties ***

  static final String KNOWN_RESEARCH = "beastSkillsKnown";
  static final String AVAILABLE_RESEARCH = "beastSkillsAvailable";

  private static Set<Research> stringToResearchSet(String value) {
    return Arrays.stream(value.split(","))
        .map(f -> fieldToResearch.get(f))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  private static String researchSetToString(Set<Research> research) {
    return research.stream().sorted().map(Research::field).collect(Collectors.joining(","));
  }

  private static Set<Research> loadResearch(String property) {
    String value = Preferences.getString(property);
    return stringToResearchSet(value);
  }

  private static void saveResearch(final String property, final Set<Research> research) {
    String value = researchSetToString(research);
    Preferences.setString(property, value);
  }

  // *** Research Bench ***

  // <input type="hidden" name="r" value="wereprof_rend2" />
  private static final Pattern AVAILABLE_RESEARCH_PATTERN =
      Pattern.compile("name=\"r\" value=\"([^\"]+)\"");

  private static Set<Research> parseAvailableResearch(final String text) {
    Matcher matcher = AVAILABLE_RESEARCH_PATTERN.matcher(text);
    Set<Research> result = new HashSet<>();
    while (matcher.find()) {
      Research research = fieldToResearch.get(matcher.group(1));
      if (research != null) {
        result.add(research);
      }
    }
    return result;
  }

  private static final Pattern RESEARCH_PATTERN = Pattern.compile("[?&]r=([^&]+)");

  private static Research getResearch(final String urlString) {
    Matcher matcher = RESEARCH_PATTERN.matcher(urlString);
    return matcher.find() ? fieldToResearch.get(matcher.group(1)) : null;
  }

  // <p>You have 108 research points (rp).
  private static final Pattern RESEARCH_POINTS_PATTERN =
      Pattern.compile("<p>You have (\\d+) research points \\(rp\\)");

  private static int parseResearchPoints(final String text) {
    Matcher matcher = RESEARCH_POINTS_PATTERN.matcher(text);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : 0;
  }

  public static void visitChoice(final String text) {
    int rp = parseResearchPoints(text);
    Preferences.setInteger("wereProfessorResearchPoints", rp);

    Set<Research> availableResearch = parseAvailableResearch(text);
    String availableResearchString = researchSetToString(availableResearch);
    // System.out.println("Available (" + availableResearch.size() + "): " +
    // availableResearchString);
    saveResearch(AVAILABLE_RESEARCH, availableResearch);

    Set<Research> knownResearch = deriveKnownResearch(availableResearch);
    String knownResearchString = researchSetToString(knownResearch);
    // System.out.println("Known (" + knownResearch.size() + "): " + knownResearchString);
    saveResearch(KNOWN_RESEARCH, knownResearch);

    int wereStomach = 0;
    wereStomach += knownResearchString.contains("stomach1") ? 1 : 0;
    wereStomach += knownResearchString.contains("stomach2") ? 1 : 0;
    wereStomach += knownResearchString.contains("stomach3") ? 1 : 0;
    Preferences.setInteger("wereProfessorStomach", wereStomach);

    int wereLiver = 0;
    wereLiver += knownResearchString.contains("liver1") ? 1 : 0;
    wereLiver += knownResearchString.contains("liver2") ? 1 : 0;
    wereLiver += knownResearchString.contains("liver3") ? 1 : 0;
    Preferences.setInteger("wereProfessorLiver", wereLiver);

    int wereBite = 0;
    wereBite += knownResearchString.contains("bite1") ? 1 : 0;
    wereBite += knownResearchString.contains("bite2") ? 1 : 0;
    wereBite += knownResearchString.contains("bite3") ? 1 : 0;
    Preferences.setInteger("wereProfessorBite", wereBite);

    int wereKick = 0;
    wereKick += knownResearchString.contains("kick1") ? 1 : 0;
    wereKick += knownResearchString.contains("kick2") ? 1 : 0;
    wereKick += knownResearchString.contains("kick3") ? 1 : 0;
    Preferences.setInteger("wereProfessorKick", wereKick);

    int wereRend = 0;
    wereRend += knownResearchString.contains("rend1") ? 1 : 0;
    wereRend += knownResearchString.contains("rend2") ? 1 : 0;
    wereRend += knownResearchString.contains("rend3") ? 1 : 0;
    Preferences.setInteger("wereProfessorRend", wereRend);
  }

  public static void postChoice0(final String urlString, final String text) {
    // Called before registering the request
    Research research = getResearch(urlString);
    if (research == null) {
      return;
    }

    String message =
        "Researching "
            + research.name()
            + " ("
            + research.field()
            + ") for "
            + research.cost()
            + " rp.";
    RequestLogger.updateSessionLog(message);
  }

  // You successfully research Janus kinase blockers.
  private static final Pattern DO_RESEARCH_PATTERN =
      Pattern.compile("You successfully research ([^.]+)\\.");

  public static void postChoice2(final String urlString, final String text) {
    // Called after registering the request and processing results.
    // ChoiceManager.handlingChoice will be true if we are still in a choice

    Research research = getResearch(urlString);
    if (research == null) {
      // Just visiting
      return;
    }

    // You successfully research Janus kinase blockers.
    Matcher matcher = DO_RESEARCH_PATTERN.matcher(text);
    if (!matcher.find()) {
      // "You have to pick an available thing to research."
      String message = "You failed to research " + research.name() + ".";
      RequestLogger.updateSessionLog(message);
      return;
    }

    String message = "You spent " + research.cost() + " rp to research " + research.name() + ".";
    RequestLogger.updateSessionLog(message);

    // Normally, ChoiceManager will follow this up with a visitChoice to process the responseText.
    // However, if this was the final skill researched, there is not a reference to choice.php,
    // and that call will not be made.

    // Do it ourself to update properties.
    if (!text.contains("choice.php")) {
      visitChoice(text);
    }
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("choice.php")) {
      return false;
    }

    int choice = ChoiceUtilities.extractChoiceFromURL(urlString);

    if (choice != 1523) {
      return false;
    }

    Research research = fieldToResearch.get(urlString);
    if (research == null) {
      return false;
    }

    String message = "Took choice 1523/1: " + research.name() + " (" + research.cost + " rp)";

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(message);

    return true;
  }
}
