package net.sourceforge.kolmafia.request;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;
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

  private static void registerResearch(
      Integer index, String field, int cost, String parent, String name, String effect) {
    Research research = new Research(index, field, cost, parent, name, effect);
    allResearch.add(research);
    fieldToResearch.put("wereprof_" + field, research);
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
        && !text.contains("Relaxin balm (20 rp)")
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
