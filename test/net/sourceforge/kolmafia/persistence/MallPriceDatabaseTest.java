package net.sourceforge.kolmafia.persistence;

import static com.spotify.hamcrest.optional.OptionalMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.utilities.HttpUtilities;
import org.junit.jupiter.api.Test;

public class MallPriceDatabaseTest {

  @Test
  void submitsDataToUrl() {
    // setup fake client
    var fakeClientBuilder = new FakeHttpClientBuilder();
    HttpUtilities.setClientBuilder(() -> fakeClientBuilder);

    // ensure we have some data
    MallPriceDatabase.recordPrice(603, 5000, true);

    MallPriceDatabase.submitPrices("http://example.com");

    var fakeClient = fakeClientBuilder.client;
    var request = fakeClient.request;

    assertThat(request, notNullValue());
    assertThat(request.method(), equalTo("POST"));
    assertThat(request.uri().toString(), equalTo("http://example.com"));
    var contentType = request.headers().firstValue("Content-Type");
    assertThat(
        contentType,
        is(optionalWithValue(equalTo("multipart/form-data; boundary=--blahblahfishcakes"))));

    assertThat(request.bodyPublisher(), is(optionalWithValue()));
  }
}
