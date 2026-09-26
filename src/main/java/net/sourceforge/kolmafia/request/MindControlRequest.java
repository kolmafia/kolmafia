package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.InventoryManager;

public class MindControlRequest extends GenericRequest {
  private final int level;
  private final int maxLevel;

  private static final AdventureResult RADIO = ItemPool.get(ItemPool.DETUNED_RADIO, 1);

  private static final Pattern GNOME_PATTERN = Pattern.compile("whichlevel=(\\d+)");
  private static final Pattern KNOLL_PATTERN = Pattern.compile("tuneradio=(\\d+)");
  private static final Pattern CANADIA_PATTERN = Pattern.compile("setting=(\\d)");

  public MindControlRequest(final int level) {
    super(
        KoLCharacter.canadiaAvailable()
            ? "choice.php"
            : KoLCharacter.gnomadsAvailable()
                ? "gnomes.php"
                : (KoLCharacter.knollAvailable() && !KoLCharacter.inGLover())
                    ? "inv_use.php"
                    : "bogus.php");

    if (KoLCharacter.canadiaAvailable()) {
      this.addFormField("whichchoice", "769");
      this.addFormField("option", "1");
      this.addFormField("setting", String.valueOf(level));
    } else if (KoLCharacter.gnomadsAvailable()) {
      this.addFormField("action", "changedial");
      this.addFormField("whichlevel", String.valueOf(level));
    } else if (KoLCharacter.knollAvailable() && !KoLCharacter.inGLover()) {
      this.addFormField("whichitem", String.valueOf(ItemPool.DETUNED_RADIO));
      this.addFormField("tuneradio", String.valueOf(level));
      this.addFormField("ajax", "1");
    }

    this.level = level;
    this.maxLevel = KoLCharacter.canadiaAvailable() ? 11 : 10;
  }

  @Override
  protected boolean retryOnTimeout() {
    return true;
  }

  @Override
  public boolean shouldFollowRedirect() {
    // Musc sign MCD redirects to a message page, processResults()
    // doesn't get called if the redirect is ignored.
    return true;
  }

  @Override
  public void run() {
    // Avoid server hits if user gives an invalid level

    if (this.level < 0 || this.level > this.maxLevel) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "The dial only goes from 0 to " + this.maxLevel + ".");
      return;
    }

    if (this.level == KoLCharacter.getMindControlLevel()) {
      KoLmafia.updateDisplay("Mind control device already at " + this.level);
      return;
    }

    if (KoLCharacter.knollAvailable()) {
      if (KoLCharacter.inGLover()) {
        // Can't use detuned radio in G-Lover run
        return;
      }
      if (MindControlRequest.RADIO.getCount(KoLConstants.inventory) == 0) {
        if (!InventoryManager.checkpointedRetrieveItem(MindControlRequest.RADIO)) {
          return;
        }
      }
    }

    KoLmafia.updateDisplay("Resetting mind control device...");

    // Visit the first URL to set it up, then let the second URL be handled normally
    if (KoLCharacter.canadiaAvailable()) {
      RequestThread.postRequest(new PlaceRequest("canadia", "lc_mcd", true));
    }
    super.run();
  }

  @Override
  public void processResults() {
    if (this.responseText.contains("the radio")
        || this.responseText.contains("You switch the dial")) {
      KoLmafia.updateDisplay("Mind control device reset.");
      KoLCharacter.setMindControlLevel(this.level);
    } else {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You failed to set the mind control device");
    }
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.contains("action=changedial")
        && !urlString.contains("tuneradio")
        && !urlString.contains("whichchoice=769")) {
      return false;
    }

    Matcher levelMatcher =
        KoLCharacter.knollAvailable()
            ? MindControlRequest.KNOLL_PATTERN.matcher(urlString)
            : KoLCharacter.canadiaAvailable()
                ? MindControlRequest.CANADIA_PATTERN.matcher(urlString)
                : MindControlRequest.GNOME_PATTERN.matcher(urlString);

    if (!levelMatcher.find()) {
      return false;
    }

    RequestLogger.updateSessionLog("mcd " + levelMatcher.group(1));
    return true;
  }
}
