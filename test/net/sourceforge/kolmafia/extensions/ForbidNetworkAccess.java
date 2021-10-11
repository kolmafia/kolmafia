package net.sourceforge.kolmafia.extensions;

import net.sourceforge.kolmafia.utilities.HttpUtilities;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ForbidNetworkAccess implements BeforeAllCallback {

  @Override
  public void beforeAll(ExtensionContext context) {
    HttpUtilities.setOpen((url) -> null);
  }
}
