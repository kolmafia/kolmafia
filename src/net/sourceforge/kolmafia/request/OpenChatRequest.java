package net.sourceforge.kolmafia.request;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.chat.ChatManager;

public class OpenChatRequest extends GenericRequest {
  private static final Pattern CHANNEL_PATTERN = Pattern.compile(", channel: '([a-z]+)', msg: ");
  private static final Pattern ACTIVE_CHANNEL = Pattern.compile("active: \"([a-z]+)\",");

  public OpenChatRequest() {
    super("mchat.php", false);
  }

  @Override
  public void run() {
    if (!ChatManager.chatLiterate()) {
      return;
    }

    super.run();
  }

  @Override
  protected boolean retryOnTimeout() {
    return true;
  }

  public List<String> getChannels() {
    List<String> channels = new ArrayList<>();

    Matcher matcher = CHANNEL_PATTERN.matcher(this.responseText);

    while (matcher.find()) {
      channels.add("/" + matcher.group(1));
    }

    return channels;
  }

  public String getActiveChannel() {
    Matcher matcher = ACTIVE_CHANNEL.matcher(this.responseText);

    if (!matcher.find()) {
      return null;
    }

    return "/" + matcher.group(1);
  }
}
