package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.webui.BarrelDecorator;

public class BarrelRequest extends GenericRequest {
  public BarrelRequest() {
    super("barrel.php");
  }

  @Override
  public void processResults() {
    BarrelDecorator.parseResponse(this.getURLString(), this.responseText);
  }
}
