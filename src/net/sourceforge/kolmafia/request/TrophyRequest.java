package net.sourceforge.kolmafia.request;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TrophyRequest extends GenericRequest {
  private static final Pattern TROPHY_PATTERN =
      Pattern.compile(
          "<td><img.*?src=[^>]*?(?:cloudfront.net|images.kingdomofloathing.com|/images)/(.+?)\".*?<td[^>]*?>(.+?)<.*?name=public(\\d+)\\s*(checked)?\\s*>",
          Pattern.DOTALL);
  private ArrayList<Trophy> trophies;

  // Get current trophies.
  public TrophyRequest() {
    this(null);
  }

  // Rearrange trophies.
  public TrophyRequest(ArrayList<Trophy> trophies) {
    super("trophies.php");
    this.trophies = trophies;
  }

  @Override
  public void run() {
    if (this.trophies == null) {
      super.run();
      this.trophies = new ArrayList<>();

      if (this.responseText == null) {
        return;
      }

      Matcher m = TrophyRequest.TROPHY_PATTERN.matcher(this.responseText);
      while (m.find()) {
        this.trophies.add(
            new Trophy(
                m.group(1), m.group(2), StringUtilities.parseInt(m.group(3)), m.group(4) != null));
      }
      return;
    }

    this.addFormField("action", "Yup.");
    Iterator<Trophy> i = this.trophies.iterator();
    while (i.hasNext()) {
      Trophy t = i.next();
      if (t.visible) {
        this.addFormField("public" + t.id, "on");
      }
    }
    super.run();

    // Multiple trophy moving only works via GET, not POST.
    StringBuilder buf = new StringBuilder("trophies.php?moveall=yes");
    i = this.trophies.iterator();
    int pos = 1;
    while (i.hasNext()) {
      Trophy t = i.next();
      buf.append("&trophy");
      buf.append(t.id);
      buf.append("=");
      buf.append(pos++);
    }

    this.constructURLString("blah", false); // clear out cached URL data
    this.constructURLString(buf.toString(), false);

    super.run();
  }

  public ArrayList<Trophy> getTrophies() {
    return this.trophies;
  }

  public static class Trophy {
    public final String filename;
    public final String name;
    public final int id;
    public boolean visible;

    public Trophy(String filename, String name, int id, boolean visible) {
      this.filename = filename;
      this.name = name;
      this.id = id;
      this.visible = visible;
    }
  }
}
