package internal.helpers;

import internal.network.FakeHttpClientBuilder;
import java.net.http.HttpRequest;
import java.util.List;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.HttpUtilities;

public class HttpClientWrapper {

  public static FakeHttpClientBuilder fakeClientBuilder = new FakeHttpClientBuilder();

  public static List<HttpRequest> getRequests() {
    return fakeClientBuilder.client.getRequests();
  }

  public static HttpRequest getLastRequest() {
    return fakeClientBuilder.client.getLastRequest();
  }

  public static void setupFakeClient() {
    GenericRequest.sessionId = "real"; // do "send" requests
    HttpUtilities.setClientBuilder(() -> fakeClientBuilder);
    GenericRequest.resetClient();
    fakeClientBuilder.client.clear();
  }
}
