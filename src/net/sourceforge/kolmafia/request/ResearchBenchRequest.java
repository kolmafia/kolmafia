package net.sourceforge.kolmafia.request;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;

public class ResearchBenchRequest extends GenericRequest {

  // There are hidden skill trees involved. For example, you cannot
  // research "rend1" until you have researched "mus3",
  //
  // I'd like to include that in this record.

  record Research(String name, String field, int cost, String effect, String prereq) {}

  private static Set<Research> allResearch = new HashSet<>();
  private static Map<String, Research> fieldToResearch = new HashMap<>();

  private static void registerResearch(
      String name, String field, int cost, String effect, String prereq) {
    Research research = new Research(name, field, cost, effect, prereq);
    allResearch.add(research);
    fieldToResearch.put("wereprof_" + field, research);
  }

  private static void registerResearch(String name, String field, int cost, String effect) {
    registerResearch(name, field, cost, effect, null);
  }

  // Skill trees
  //
  // mus1 mus2 mus3
  //                rend1 rend2 rend3 unknown1
  //                hp1 hp2 hp3
  //                            skin1 skin2 skin3 unknown2
  //                            stomach1 stomach2 stomach3 feed
  // myst1 myst2 myst3
  //                bite1 bite2 bite3 howl
  //                res1 res2 res3
  //                            items1 items2 items3 hunt
  //                            ml1 ml2 ml3 feasting
  // mox1 mox2 mox3
  //                kick1 kick2 kick3 punt
  //                init1 init2 init3
  //                            meat1 meat2 meat3 unknown3
  //                            liver1 liver2 liver3 unknown4

  static {
    // *** Muscle Skill Tree

    // +Muscle%
    registerResearch("Osteocalcin injection", "mus1", 10, "Muscle +20%");
    registerResearch("Somatostatin catalyst", "mus2", 20, "Muscle +30%", "mus1");
    registerResearch("Endothelin suspension", "mus3", 30, "Muscle +50%", "mus2");

    // Rend
    registerResearch("Ultraprogesterone potion", "rend1", 20, "Unlock Rend", "mus3");
    registerResearch("Lactide blocker", "rend2", 30, "Increase damage of Rend", "rend1");
    registerResearch(
        "Haemostatic membrane treatment", "rend3", 40, "Rend restores some HP", "rend2");
    registerResearch(
        "Norepinephrine transfusion",
        "unknown1",
        100,
        "Unlocks a devastating and instantaneous physical attack",
        "rend3");

    // Max HP%
    registerResearch("Synthetic prostaglandin", "hp1", 20, "Maximum HP +20%", "mus3");
    registerResearch("Leukotriene elixir", "hp2", 30, "Maximum HP +30%", "hp1");
    registerResearch("Thromboxane inhibitor", "hp3", 40, "Maximum HP +50%", "hp2");
    // Damage Reduction
    registerResearch("Calcitonin powder", "skin1", 40, "Damage Reduction: 5", "hp3");
    registerResearch("Enkephalin activator", "skin2", 50, "Damage Reduction: 10", "skin1");
    registerResearch("Oxytocin inversion", "skin3", 60, "Damage Reduction: 15", "skin2");
    registerResearch(
        "Hemostatic accelerant", "unknown2", 100, "Regenerate 8-10 HP per Adventure", "skin3");

    // Stomach capacity
    registerResearch(
        "Triiodothyronine accelerator", "stomach1", 40, "Increase stomach capacity by 3", "hp3");
    registerResearch(
        "Thyroxine supplements", "stomach2", 50, "Increase stomach capacity by 3", "stomach1");
    registerResearch(
        "Amyloid polypeptide mixture",
        "stomach3",
        60,
        "Increase stomach capacity by 3",
        "stomach2");
    registerResearch(
        "Cholecystokinin antagonist", "feed", 100, "will do something after combat", "stomach3");

    // *** Mysticality Skill Tree

    // +Mys%
    registerResearch("Galanin precipitate", "myst1", 10, "Mysticality +20%");
    registerResearch("Cortistatin blocker", "myst2", 20, "Mysticality +30%", "myst1");
    registerResearch("Prolactin inhibitor", "myst3", 30, "Mysticality +50%", "myst2");

    // Bite
    registerResearch("Fluoride rinse", "bite1", 20, "Unlock Bite", "myst3");
    registerResearch(
        "Proton pump eliminator",
        "bite2",
        30,
        "Adds another element to the Beast's bite attack",
        "bite1");
    registerResearch("Bisphosphonate drip", "bite3", 40, "Increase damage of the Bite");
    registerResearch("Albuterol innundation", "howl", 100, "Unlock Howl");

    // Elemental Resistance
    registerResearch("Omega-3 megadose", "res1", 20, "All Resistance +20%", "myst3");
    registerResearch("Omega-6 hyperdose", "res2", 30, "All Resistance +20%", "res1");
    registerResearch("Omega-9 omegadose", "res3", 40, "All Resistance +20%", "res2");
    // Item Drop
    registerResearch(
        "Diphenhydramine eyedrops", "items1", 40, "+25% Item Drops from Monsters", "res3");
    registerResearch(
        "Carbinoxamine eye wash", "items2", 50, "+25% Item Drops from Monsters", "items1");
    registerResearch(
        "Intraocular cyproheptadine injections",
        "items3",
        60,
        "+25% Item Drops from Monsters",
        "items2");
    // Hunt
    registerResearch("Phantosmic tincture", "hunt", 100, "Unlock Hunt", "items3");

    // +ML
    registerResearch("Anabolic megatestosterone", "ml1", 40, "+10 to Monster Level", "res3");
    registerResearch("Hyperadrenal Pheremones", "ml2", 50, "+15 to Monster Level", "ml1");
    registerResearch("Synthetic Rhabdovirus", "ml3", 60, "+25 to Monster Level", "ml2");
    registerResearch(
        "Peptide catalyst", "feasting", 100, "Regain more HP from eating other animals", "ml3");

    // *** Moxie Skill Tree

    // +Mox%
    registerResearch("Dopamine slurry", "mox1", 10, "Moxie +20%");
    registerResearch("Relaxin balm", "mox2", 20, "Moxie +30%", "mox1");
    registerResearch("Melatonin suppositories", "mox3", 30, "Moxie +50%", "mox2");

    // Kick
    registerResearch("Hamstring-tightening solution", "kick1", 20, "Unlocks Kick", "mox3");
    registerResearch(
        "Gluteal 4-Androstenediol inection",
        "kick2",
        30,
        "Increase the effectiveness of the Beast's kick",
        "kick1");
    registerResearch(
        "Subcutaneous dimethandrolone implant",
        "kick3",
        40,
        "The Beast's kick will briefly stun opponents",
        "kick2");
    // Punt
    registerResearch("Novel catecholamine synthesis", "punt", 100, "Unlocks Punt", "kick3");

    // Initiative
    registerResearch("Adrenal decoction", "init1", 20, "+50% Combat Initiative", "mox3");
    registerResearch("Adrenal distillate", "init2", 30, "+50% Combat Initiative", "init1");
    registerResearch(
        "Concentrated adrenaline extract", "init3", 40, "+100% Combat Initiative", "init2");
    // Meat Drop
    registerResearch("Leptin modulator", "meat1", 40, "+25% Meat from Monsters", "init3");
    registerResearch(
        "Carnal dehydrogenase infusion", "meat2", 50, "+50% Meat from Monsters", "meat1");
    registerResearch(
        "Dihydrobenzophenanthridine injection", "meat3", 60, "+75% Meat from Monsters", "meat2");
    // Stats
    registerResearch("Janus kinase blockers", "unknown3", 100, "+5 Stats Per Fight", "meat3");

    // Liver Capacity
    registerResearch("Glucagon condensate", "liver1", 40, "Increase liver capacity by 3", "init3");
    registerResearch("Secretin agonist", "liver2", 50, "Increase liver capacity by 3", "liver1");
    registerResearch(
        "Synthetic aldosterone", "liver3", 60, "Increase liver capacity by 3", "liver2");
    registerResearch(
        "Synthroid-parathormone cocktail",
        "unknown4",
        100,
        "Everything will Look Red for less time",
        "liver3");
  }

  public ResearchBenchRequest(final String research) {
    super("choice.php");
    this.addFormField("whichchoice", "1523");
    this.addFormField("option", "1");
    String rfield = research.startsWith("wereprof_") ? research : "wereprof_" + research;
    this.addFormField("r", rfield);
  }

  public static void visitChoice(final String text) {
    // calculate stomach
    int wereStomach = 0;
    if (!text.contains("Osteocalcin injection (10 rp)")
        && !text.contains("Somatostatin catalyst (20 rp)")
        && !text.contains("Endothelin suspension (30 rp)")
        && !text.contains("Synthetic prostaglandin (20 rp)")
        && !text.contains("Leukotriene elixir (30 rp)")
        && !text.contains("Thromboxane inhibitor (40 rp)")) {
      wereStomach += !text.contains("Triiodothyronine accelerator (40 rp)") ? 1 : 0;
      wereStomach += wereStomach == 1 && !text.contains("Thyroxine supplements (50 rp)") ? 1 : 0;
      wereStomach +=
          wereStomach == 2 && !text.contains("Amyloid polypeptide mixture (60 rp)") ? 1 : 0;
    }
    Preferences.setInteger("wereProfessorStomach", wereStomach);

    // calculate liver
    int wereLiver = 0;
    if (!text.contains("Dopamine slurry (10 rp)")
        && !text.contains("Relaxin Balm (20 rp)")
        && !text.contains("Melatonin suppositories (30 rp)")
        && !text.contains("Adrenal decoction (20 rp)")
        && !text.contains("Adrenal distillate (30 rp)")
        && !text.contains("Concentrated adrenaline extract (40 rp)")) {
      wereLiver += !text.contains("Glucagon condensate (40 rp)") ? 1 : 0;
      wereLiver += wereLiver == 1 && !text.contains("Secretin agonist (50 rp)") ? 1 : 0;
      wereLiver += wereLiver == 2 && !text.contains("Synthetic aldosterone (60 rp)") ? 1 : 0;
    }
    Preferences.setInteger("wereProfessorLiver", wereLiver);
  }

  private static final Pattern RESEARCH_PATTERN = Pattern.compile("[?&]r=([^&]+)");

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("choice.php")) {
      return false;
    }

    int choice = ChoiceUtilities.extractChoiceFromURL(urlString);

    if (choice != 1523) {
      return false;
    }

    Matcher matcher = RESEARCH_PATTERN.matcher(urlString);
    if (!matcher.find()) {
      // Just visiting
      return false;
    }

    String field = matcher.group(1);
    Research research = fieldToResearch.get(field);
    String message;
    if (research == null) {
      message = "Took choice 1523/1: r=" + field;
    } else {
      message = "Took choice 1523/1: " + research.name() + "(" + research.cost + ")";
    }

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(message);

    return true;
  }
}
