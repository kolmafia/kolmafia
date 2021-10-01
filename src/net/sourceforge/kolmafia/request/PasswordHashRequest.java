package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PasswordHashRequest extends GenericRequest {
  private static final Pattern HASH_PATTERN_1 =
      Pattern.compile("name=[\"']?pwd[\"']? value=[\"']([^\"']+)[\"']");
  private static final Pattern HASH_PATTERN_2 = Pattern.compile("pwd=([^&]+)");
  private static final Pattern HASH_PATTERN_3 = Pattern.compile("pwd = \"([^\"]+)\"");

  public PasswordHashRequest(final String location) {
    super(location);
  }

  @Override
  public void processResults() {
    super.processResults();

    if (!GenericRequest.passwordHash.equals("")) {
      return;
    }

    PasswordHashRequest.updatePasswordHash(this.responseText);
  }

  public static void updatePasswordHash(String responseText) {
    Matcher pwdmatch = PasswordHashRequest.HASH_PATTERN_1.matcher(responseText);
    if (pwdmatch.find()) {
      GenericRequest.setPasswordHash(pwdmatch.group(1));
      return;
    }

    pwdmatch = PasswordHashRequest.HASH_PATTERN_2.matcher(responseText);
    if (pwdmatch.find()) {
      GenericRequest.setPasswordHash(pwdmatch.group(1));
      return;
    }

    pwdmatch = PasswordHashRequest.HASH_PATTERN_3.matcher(responseText);
    if (pwdmatch.find()) {
      GenericRequest.setPasswordHash(pwdmatch.group(1));
      return;
    }
  }
}
