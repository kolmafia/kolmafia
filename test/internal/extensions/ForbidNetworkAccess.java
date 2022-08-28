package internal.extensions;

import static internal.extensions.CheckNested.isNested;

import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.utilities.HttpUtilities;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ForbidNetworkAccess implements BeforeAllCallback {

  @Override
  public void beforeAll(ExtensionContext context) {
    if (isNested(context)) return;
    ForbidNetworkAccess.blockNetwork();
  }

  public static void blockNetwork() {
    HttpUtilities.setClientBuilder(FakeHttpClientBuilder::new);
  }
}
