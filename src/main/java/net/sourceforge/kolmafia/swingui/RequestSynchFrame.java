package net.sourceforge.kolmafia.swingui;

import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.request.GenericRequest;

public class RequestSynchFrame extends RequestFrame {
  private static RequestSynchFrame INSTANCE = null;

  public RequestSynchFrame() {
    super("Request Synch");
    RequestSynchFrame.INSTANCE = this;
  }

  public static final void showLocation(final String location) {
    RequestSynchFrame.showRequest(RequestEditorKit.extractRequest(location));
  }

  public static final void showRequest(final GenericRequest request) {
    if (StaticEntity.isHeadless()) {
      return;
    }

    if (RequestSynchFrame.INSTANCE == null) {
      GenericFrame.createDisplay(RequestSynchFrame.class);
    }

    RequestSynchFrame.INSTANCE.refresh(request);
  }
}
