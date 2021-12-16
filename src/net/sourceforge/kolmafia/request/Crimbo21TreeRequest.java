package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.ResultProcessor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Crimbo21TreeRequest extends GenericRequest {
    public Crimbo21TreeRequest() {
        super("crimbo21tree.php");
    }

    @Override
    public void processResults() {
        parseResponse(this.getURLString(), this.responseText);
    }

    public static final AdventureResult getAmmo(final String urlString) {
        Matcher matcher = AMMO_PATTERN.matcher(urlString);

        if (!matcher.find()) {
            return null;
        }

        // Counts are -1 because using ammo results in item loss.
        switch (GenericRequest.decodeField(matcher.group(1))) {
            case "1":
                return ItemPool.get(ItemPool.BIG_ROCK, -1);
            case "2":
                return ItemPool.get("Black Crimbo ball", -1);
            case "3":
                return ItemPool.get("White Crimbo ball", -1);
            default:
                return null;
        }
    }

    public static final void parseResponse(final String location, final String responseText) {
        if (!location.startsWith("crimbo21tree.php")) {
            return;
        }

        String action = GenericRequest.getAction(location);

        if (action == "j") {
            ResultProcessor.processResult(getAmmo(location));
        }
    }

    public static final Pattern AMMO_PATTERN = Pattern.compile("c=([^&]*)");

    public static final boolean registerRequest(final String urlString) {
        if (!urlString.startsWith("crimbo21tree.php")) {
            return false;
        }

        String action = GenericRequest.getAction(urlString);
        String message = null;

        if (action != "j") {
            AdventureResult item = getAmmo(urlString);
            message = "Throwing " + (item == null ? "something" : item.getName()) + " at the Crimbo tree";
        }

        if (message == null) {
            return true;
        }

        RequestLogger.printLine(message);
        RequestLogger.updateSessionLog(message);

        return true;
    }
}
