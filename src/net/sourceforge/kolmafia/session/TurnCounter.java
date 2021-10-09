package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.Crimbo09Request;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TurnCounter implements Comparable<TurnCounter> {
  private static final ArrayList<TurnCounter> relayCounters = new ArrayList<TurnCounter>();
  private static final HashSet<String> ALL_LOCATIONS = new HashSet<>();

  private int value;
  private final String image;
  private String label;
  private String URL;
  private String parsedLabel;
  private HashSet<String> exemptions;
  private int lastWarned;
  private boolean wander = false;

  public TurnCounter(final int value, final String label, final String image) {
    this.value = KoLCharacter.getCurrentRun() + value;
    this.label = label.replaceAll(":", "");
    this.image = image.replaceAll(":", "");
    this.lastWarned = -1;
    this.parsedLabel = this.label;
    int pos = this.parsedLabel.lastIndexOf(" ");
    while (pos != -1) {
      String word = this.parsedLabel.substring(pos + 1).trim();
      if (word.equals("loc=*")) {
        this.exemptions = TurnCounter.ALL_LOCATIONS;
      } else if (word.startsWith("loc=")) {
        if (this.exemptions == TurnCounter.ALL_LOCATIONS) {
          this.parsedLabel = this.parsedLabel.substring(0, pos).trim();
          pos = this.parsedLabel.lastIndexOf(" ");
          continue;
        }

        if (this.exemptions == null) {
          this.exemptions = new HashSet<>();
        }
        this.exemptions.add(word.substring(4));
      } else if (word.startsWith("type=")) {
        if (word.substring(5).equals("wander")) {
          this.wander = true;
        }
      } else if (word.contains(".php")) {
        this.URL = word;
      } else break;

      this.parsedLabel = this.parsedLabel.substring(0, pos).trim();
      pos = this.parsedLabel.lastIndexOf(" ");
    }
    if (this.parsedLabel.length() == 0) {
      this.parsedLabel = "Manual";
    }
  }

  public boolean isExempt(final String adventureId) {
    if (this.exemptions == TurnCounter.ALL_LOCATIONS
        || (this.exemptions != null && this.exemptions.contains(adventureId))) {
      return true;
    }

    return false;
  }

  public String imageURL() {
    if (this.URL != null) return this.URL;

    if (this.exemptions != null && this.exemptions.size() == 1) { // Exactly one exempt location
      String loc = this.exemptions.iterator().next();
      return "adventure.php?snarfblat=" + loc;
    }

    return null;
  }

  public String getLabel() {
    return this.parsedLabel;
  }

  public String getImage() {
    return this.image;
  }

  public int getTurnsRemaining() {
    int remain = this.value - KoLCharacter.getCurrentRun();
    if (remain < 0 && this.wander) {
      this.value = KoLCharacter.getCurrentRun();
      remain = 0;
    }
    return remain;
  }

  public void resetForRun() {
    this.value = this.getTurnsRemaining();
  }

  public static int turnsRemaining(final String label) {
    synchronized (TurnCounter.relayCounters) {
      for (TurnCounter current : TurnCounter.relayCounters) {
        if (current.parsedLabel.equals(label)) {
          return current.value - KoLCharacter.getCurrentRun();
        }
      }
    }

    return -1;
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof TurnCounter)) {
      return false;
    }

    return this.label.equals(((TurnCounter) o).label) && this.value == ((TurnCounter) o).value;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += this.value;
    hash += 31 * (this.label != null ? this.label.hashCode() : 0);
    return hash;
  }

  public int compareTo(final TurnCounter o) {
    if (!(o instanceof TurnCounter)) {
      return -1;
    }

    return this.value - o.value;
  }

  public static final void clearCounters() {
    synchronized (TurnCounter.relayCounters) {
      TurnCounter.relayCounters.clear();
      TurnCounter.saveCounters();
    }
  }

  public static final void loadCounters() {
    synchronized (TurnCounter.relayCounters) {
      TurnCounter.relayCounters.clear();

      String counters = Preferences.getString("relayCounters");
      if (counters.length() == 0) {
        return;
      }

      StringTokenizer tokens = new StringTokenizer(counters, ":");
      while (tokens.hasMoreTokens()) {
        int turns = StringUtilities.parseInt(tokens.nextToken()) - KoLCharacter.getCurrentRun();
        if (!tokens.hasMoreTokens()) break;
        String name = tokens.nextToken();
        if (!tokens.hasMoreTokens()) break;
        String image = tokens.nextToken();
        startCountingInternal(turns, name, image);
      }
    }
  }

  public static final void saveCounters() {
    synchronized (TurnCounter.relayCounters) {
      StringBuilder counters = new StringBuilder();

      for (TurnCounter current : TurnCounter.relayCounters) {
        if (counters.length() > 0) {
          counters.append(":");
        }

        counters.append(current.value);
        counters.append(":");
        counters.append(current.label);
        counters.append(":");
        counters.append(current.image);
      }

      Preferences.setString("relayCounters", counters.toString());
    }
  }

  public static final TurnCounter getExpiredCounter(GenericRequest request, boolean informational) {
    String URL = request.getURLString();
    KoLAdventure adventure = AdventureDatabase.getAdventureByURL(URL);

    String adventureId;
    int turnsUsed;

    if (adventure != null) {
      adventureId = adventure.getAdventureId();
      turnsUsed = adventure.getRequest().getAdventuresUsed();
    } else if (AdventureDatabase.getUnknownName(URL) != null) {
      adventureId = "";
      turnsUsed = 1;
    } else {
      adventureId = "";
      turnsUsed = TurnCounter.getTurnsUsed(request);
    }

    if (turnsUsed == 0) {
      return null;
    }

    int thisTurn = KoLCharacter.getCurrentRun();
    int currentTurns = thisTurn + turnsUsed - 1;

    synchronized (TurnCounter.relayCounters) {
      Iterator<TurnCounter> it = TurnCounter.relayCounters.iterator();

      while (it.hasNext()) {
        TurnCounter current = it.next();

        if (current.value > currentTurns
            || current.lastWarned == thisTurn
            || current.isExempt(adventureId) != informational) {
          continue;
        }

        if (informational
            && current.value > thisTurn) { // Defer until later, there's no point in reporting an
          // informational counter prior to actual expiration.
          continue;
        }

        if (current.value < thisTurn) {
          if (current.wander) {
            // This might not actually be necessary
            continue;
          }
          it.remove();
        }

        current.lastWarned = thisTurn;
        return current;
      }
    }

    return null;
  }

  public static final String getUnexpiredCounters() {
    int currentTurns = KoLCharacter.getCurrentRun();
    StringBuilder counters = new StringBuilder();

    synchronized (TurnCounter.relayCounters) {
      for (TurnCounter current : TurnCounter.relayCounters) {
        if (current.value < currentTurns) {
          // Can't remove the counter - a counterScript
          // may still be waiting for it to be delivered.
          continue;
        }

        if (counters.length() > 0) {
          counters.append(KoLConstants.LINE_BREAK);
        }

        counters.append(current.parsedLabel);
        counters.append(" (");
        counters.append(current.value - currentTurns);
        counters.append(")");
      }
    }

    return counters.toString();
  }

  public static final void startCounting(final int value, final String label, final String image) {
    synchronized (TurnCounter.relayCounters) {
      TurnCounter.startCountingInternal(value, label, image);
      TurnCounter.saveCounters();
    }
  }

  private static void startCountingInternal(
      final int value, final String label, final String image) {
    // We don't synchronize here because caller has already done so.
    if (value >= 0) {
      TurnCounter counter = new TurnCounter(value, label, image);

      if (!TurnCounter.relayCounters.contains(counter)) {
        TurnCounter.relayCounters.add(counter);
      }
    }
  }

  public static final void stopCounting(final String label) {
    synchronized (TurnCounter.relayCounters) {
      Iterator<TurnCounter> it = TurnCounter.relayCounters.iterator();

      while (it.hasNext()) {
        TurnCounter current = it.next();
        if (current.parsedLabel.equals(label)) {
          it.remove();
        }
      }

      TurnCounter.saveCounters();
    }
  }

  public static final boolean isCounting(final String label, final int value) {
    int searchValue = KoLCharacter.getCurrentRun() + value;

    synchronized (TurnCounter.relayCounters) {
      for (TurnCounter current : TurnCounter.relayCounters) {
        if (current.parsedLabel.equals(label) && current.value == searchValue) {
          return true;
        }
      }
    }

    return false;
  }

  public static final boolean isCounting(final String label, final int start, final int stop) {
    int begin = KoLCharacter.getCurrentRun() + start;
    int end = KoLCharacter.getCurrentRun() + stop;

    synchronized (TurnCounter.relayCounters) {
      for (TurnCounter current : TurnCounter.relayCounters) {
        if (current.parsedLabel.equals(label) && current.value >= begin && current.value <= end) {
          return true;
        }
      }
    }

    return false;
  }

  public static final boolean isCounting(final String label) {
    synchronized (TurnCounter.relayCounters) {
      for (TurnCounter current : TurnCounter.relayCounters) {
        if (current.parsedLabel.equals(label) && current.value >= KoLCharacter.getCurrentRun()) {
          return true;
        }
      }
    }

    return false;
  }

  public static final TurnCounter[] getCounters() {
    TurnCounter[] counters;
    synchronized (TurnCounter.relayCounters) {
      counters =
          TurnCounter.relayCounters.toArray(new TurnCounter[TurnCounter.relayCounters.size()]);
    }
    Arrays.sort(counters);
    return counters;
  }

  public static final String getCounters(String label, int minTurns, int maxTurns) {
    label = label.toLowerCase();
    boolean checkExempt = label.length() == 0;
    minTurns += KoLCharacter.getCurrentRun();
    maxTurns += KoLCharacter.getCurrentRun();
    StringBuilder buf = new StringBuilder();

    synchronized (TurnCounter.relayCounters) {
      for (TurnCounter current : TurnCounter.relayCounters) {
        if (current.value < minTurns || current.value > maxTurns) {
          continue;
        }
        if (checkExempt && current.isExempt("")) {
          continue;
        }
        if (!current.parsedLabel.toLowerCase().contains(label)) {
          continue;
        }
        if (buf.length() != 0) {
          buf.append("\t");
        }
        buf.append(current.parsedLabel);
      }
    }

    return buf.toString();
  }

  public static final int getCounter(String label) {
    label = label.toLowerCase();
    boolean checkExempt = label.length() == 0;

    synchronized (TurnCounter.relayCounters) {
      for (TurnCounter current : TurnCounter.relayCounters) {
        if (checkExempt && current.isExempt("")) {
          continue;
        }
        if (!current.parsedLabel.toLowerCase().contains(label)) {
          continue;
        }
        return current.value - KoLCharacter.getCurrentRun();
      }
    }

    return -1;
  }

  public static final void startCountingTemporary(int value, String label, String image) {
    String temp = Preferences.getString("_tempRelayCounters");
    temp = temp + value + ":" + label + ":" + image + "|";
    Preferences.setString("_tempRelayCounters", temp);
  }

  public static final void handleTemporaryCounters(final String type, final String encounter) {
    String temp = Preferences.getString("_tempRelayCounters");
    if (temp.equals("")) {
      return;
    }
    if (KoLAdventure.lastVisitedLocation() == null
        || !KoLAdventure.lastVisitedLocation().hasWanderers()) {
      return;
    }
    if (type.equals("Combat")) {
      if (EncounterManager.isNoWanderMonster(encounter)) {
        return;
      }
    }
    String[] counters = temp.split("\\|");
    for (String counter : counters) {
      if (counter.equals("")) continue;
      String[] values = counter.split(":");
      TurnCounter.startCounting(StringUtilities.parseInt(values[0]), values[1], values[2]);
    }
    Preferences.setString("_tempRelayCounters", "");
  }

  private static int getTurnsUsed(GenericRequest request) {
    if (!(request instanceof RelayRequest)) {
      return request.getAdventuresUsed();
    }

    String urlString = request.getURLString();

    if (urlString.startsWith("adventure.php")) {
      // Assume unknown adventure locations take 1 turn each
      // This is likely not true under the Sea, for example,
      // but it's as good a guess as any we can make.

      return 1;
    }

    if (urlString.startsWith("inv_use.php") || urlString.startsWith("inv_eat.php")) {
      return UseItemRequest.getAdventuresUsed(urlString);
    }

    if (urlString.startsWith("runskillz.php")) {
      return UseSkillRequest.getAdventuresUsed(urlString);
    }

    if (urlString.startsWith("craft.php") || urlString.startsWith("guild.php")) {
      return CreateItemRequest.getAdventuresUsed(request);
    }

    if (urlString.startsWith("place.php?whichplace=chateau")
        && urlString.contains("action=chateau_painting")) {
      return Preferences.getBoolean("_chateauMonsterFought") ? 0 : 1;
    }

    if (urlString.startsWith("crimbo09.php")) {
      return Crimbo09Request.getTurnsUsed(request);
    }

    return 0;
  }

  public static final void addWarning(final String label) {
    synchronized (TurnCounter.relayCounters) {
      Iterator<TurnCounter> it = TurnCounter.relayCounters.iterator();

      while (it.hasNext()) {
        TurnCounter counter = it.next();
        if (counter.parsedLabel.equals(label) && counter.exemptions == TurnCounter.ALL_LOCATIONS) {
          counter.label = counter.label.replace(" loc=*", "");

          // Reload the counter, since it may have had its own exceptions in addition to the "
          // loc=*"
          counter = new TurnCounter(counter.value, counter.label, counter.image);
        }
      }

      TurnCounter.saveCounters();
    }
  }

  public static final void removeWarning(final String label) {
    synchronized (TurnCounter.relayCounters) {
      Iterator<TurnCounter> it = TurnCounter.relayCounters.iterator();

      while (it.hasNext()) {
        TurnCounter counter = it.next();
        if (counter.parsedLabel.equals(label) && counter.exemptions != TurnCounter.ALL_LOCATIONS) {
          counter.exemptions = TurnCounter.ALL_LOCATIONS;
          counter.label += " loc=*";
        }
      }

      TurnCounter.saveCounters();
    }
  }

  public static final void deleteByHash(final int hash) {
    synchronized (TurnCounter.relayCounters) {
      Iterator<TurnCounter> it = TurnCounter.relayCounters.iterator();

      while (it.hasNext()) {
        if (System.identityHashCode(it.next()) == hash) {
          it.remove();
        }
      }

      TurnCounter.saveCounters();
    }
  }

  public static final int count() {
    synchronized (TurnCounter.relayCounters) {
      return TurnCounter.relayCounters.size();
    }
  }
}
