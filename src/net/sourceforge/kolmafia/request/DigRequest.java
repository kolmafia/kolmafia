package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class DigRequest extends GenericRequest {
  private static final Pattern SQUARE_PATTERN = Pattern.compile("s1=(\\d+)&s2=(\\d+)&s3=(\\d+)");

  public DigRequest() {
    super("dig.php");
  }

  public static final int getTurnsUsed(GenericRequest request) {
    String action = request.getFormField("action");
    return action != null && action.equals("dig") ? 1 : 0;
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("dig.php")) {
      return;
    }

    if (responseText.indexOf("Your archaeologing shovel can't take any more abuse.") != -1) {
      EquipmentManager.breakEquipment(
          ItemPool.ARCHAEOLOGING_SHOVEL, "Your archaeologing shovel crumbled.");
    }
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("dig.php")) {
      return false;
    }

    String action = GenericRequest.getAction(urlString);
    String message = null;

    if (action == null) {
      return true;
    }

    if (action.equals("dig")) {
      Matcher matcher = SQUARE_PATTERN.matcher(urlString);
      if (!matcher.find()) {
        return false;
      }
      int s1 = StringUtilities.parseInt(matcher.group(1));
      int s2 = StringUtilities.parseInt(matcher.group(2));
      int s3 = StringUtilities.parseInt(matcher.group(3));

      message = "Excavating square " + s1 + "/" + s2 + "/" + s3;
    }

    if (message == null) {
      return false;
    }

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(message);

    return true;
  }
}
