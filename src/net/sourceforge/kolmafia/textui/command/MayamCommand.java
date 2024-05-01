package net.sourceforge.kolmafia.textui.command;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class MayamCommand extends AbstractCommand {
  private static final List<List<String>> SYMBOL_POSITIONS =
      List.of(
          List.of("yam", "clock", "explosion"),
          List.of("yam", "eyepatch", "cheese", "wall"),
          List.of("yam", "lightning", "bottle", "board", "meat"),
          List.of("yam", "sword", "vessel", "fur", "chair", "eye"));

  public MayamCommand() {
    var symbols =
        SYMBOL_POSITIONS.stream().flatMap(List::stream).distinct().collect(Collectors.joining("|"));
    this.usage = "[" + symbols + "] - list symbols top to bottom (largest ring to smallest)";
  }

  private boolean lacksCalendar() {
    if (!InventoryManager.equippedOrInInventory(ItemPool.MAYAM_CALENDAR)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You need a Mayam Calendar");
      return true;
    }
    return false;
  }

  @Override
  public void run(final String cmd, String parameters) {
    String[] symbols = parameters.split(" ");

    if (lacksCalendar()) return;

    use();

    var symbolsUsed = Arrays.asList(Preferences.getString("_mayamSymbolsUsed").split(","));

    int ring = 4;
    for (var symbol : symbols) {
      int fromTop = (4 - --ring);

      var isYam = symbol.equals("yam");
      var nameInPref = isYam ? symbol + fromTop : symbol;
      if (symbolsUsed.contains(nameInPref)) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR,
            "You've already used the "
                + symbol
                + " symbol"
                + (isYam ? " in position " + fromTop : "")
                + ".");
        return;
      }

      int pos = SYMBOL_POSITIONS.get(ring).indexOf(symbol);

      if (pos < 0) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "Cannot match symbol " + symbol + " on ring " + fromTop + ".");
        return;
      }

      spin(ring, pos);
    }

    consider();

    KoLmafia.updateDisplay("Calendar considered.");
  }

  private void use() {
    GenericRequest useRequest = new GenericRequest("inv_use.php");
    useRequest.addFormField("pwd", GenericRequest.passwordHash);
    useRequest.addFormField("whichitem", String.valueOf(ItemPool.MAYAM_CALENDAR));
    RequestThread.postRequest(useRequest);
  }

  private void spin(int ring, int pos) {
    var request = new GenericRequest("choice.php");
    request.addFormField("whichchoice", "1527");
    request.addFormField("option", "2");
    request.addFormField("r", String.valueOf(ring));
    request.addFormField("p", String.valueOf(pos));
    request.addFormField("pwd", GenericRequest.passwordHash);
    RequestThread.postRequest(request);
  }

  private void consider() {
    var request = new GenericRequest("choice.php");
    request.addFormField("whichchoice", "1527");
    request.addFormField("option", "1");
    request.addFormField("pwd", GenericRequest.passwordHash);
    RequestThread.postRequest(request);
  }
}
