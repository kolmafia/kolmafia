package net.sourceforge.kolmafia.request;

public class UpdateSuppressedRequest extends GenericRequest {

  public UpdateSuppressedRequest(String url) {
    super(url);
  }

  public UpdateSuppressedRequest(String url, boolean post) {
    super(url, post);
  }

  @Override
  public boolean shouldSuppressUpdate() {
    return true;
  }
}
