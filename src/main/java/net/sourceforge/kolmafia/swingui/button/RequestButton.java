package net.sourceforge.kolmafia.swingui.button;

import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.swingui.JComponentUtilities;

public class RequestButton extends ThreadedButton {
  public RequestButton(final String title, final GenericRequest request) {
    super(title, new RequestRunnable(request));
  }

  public RequestButton(final String title, final String icon, final GenericRequest request) {
    super(JComponentUtilities.getImage(icon), new RequestRunnable(request));
    this.setToolTipText(title);
  }

  private static class RequestRunnable implements Runnable {
    private final GenericRequest request;

    public RequestRunnable(GenericRequest request) {
      this.request = request;
    }

    @Override
    public void run() {
      this.request.run();
    }
  }
}
