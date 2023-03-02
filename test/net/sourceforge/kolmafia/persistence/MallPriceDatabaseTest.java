package net.sourceforge.kolmafia.persistence;

import static com.spotify.hamcrest.optional.OptionalMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import internal.network.FakeHttpClientBuilder;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import net.sourceforge.kolmafia.utilities.HttpUtilities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MallPriceDatabaseTest {

  @BeforeAll
  public static void beforeAll() {
    MallPriceDatabase.savePricesToFile = false;
  }

  @AfterAll
  public static void afterAll() {
    MallPriceDatabase.savePricesToFile = true;
  }

  @Test
  void submitsDataToUrl() {
    // setup fake client
    var fakeClientBuilder = new FakeHttpClientBuilder();
    HttpUtilities.setClientBuilder(() -> fakeClientBuilder);

    // ensure we have some data
    MallPriceDatabase.recordPrice(603, 5000, true);

    MallPriceDatabase.submitPrices("http://example.com");

    var fakeClient = fakeClientBuilder.client;
    var request = fakeClient.getLastRequest();

    assertThat(request, notNullValue());
    assertThat(request.method(), equalTo("POST"));
    assertThat(request.uri().toString(), equalTo("http://example.com"));
    var contentType = request.headers().firstValue("Content-Type");
    assertThat(
        contentType,
        is(optionalWithValue(equalTo("multipart/form-data; boundary=--blahblahfishcakes"))));

    assertThat(request.bodyPublisher(), is(optionalWithValue()));
  }

  @Test
  void writesDataInItemIdOrder() {
    MallPriceDatabase.recordPrice(600, 5, true);
    MallPriceDatabase.recordPrice(607, 50, true);
    MallPriceDatabase.recordPrice(555, 500, true);

    var baos = new ByteArrayOutputStream();
    try (PrintStream writer = new PrintStream(baos, true, StandardCharsets.UTF_8)) {
      MallPriceDatabase.writePrices(writer);
    }
    var data = baos.toString(StandardCharsets.UTF_8);
    assertThat(data, startsWith(String.valueOf(0xF00D5)));
    var lines = data.split("\\r?\\n");
    assertThat(lines, arrayWithSize(4));
    assertThat(lines[1], matchesPattern("^555\t\\d+\t500$"));
    assertThat(lines[2], matchesPattern("^600\t\\d+\t5$"));
    assertThat(lines[3], matchesPattern("^607\t\\d+\t50$"));
  }
}
