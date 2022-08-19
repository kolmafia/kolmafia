package internal.helpers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import internal.network.RequestBodyReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;

public class Networking {
  public static String html(String path) {
    try {
      return Files.readString(Paths.get(path)).trim();
    } catch (IOException e) {
      Assertions.fail("Failed to load HTML file: " + path);
      throw new AssertionError(e);
    }
  }

  public static JSONObject json(String str) {
    try {
      return new JSONObject(str);
    } catch (JSONException e) {
      Assertions.fail("Failed to parse JSON");
      throw new AssertionError(e);
    }
  }

  public static void assertGetRequest(HttpRequest request, String path) {
    assertGetRequest(request, path, null);
  }

  public static void assertGetRequest(HttpRequest request, String path, String query) {
    assertThat(request.method(), equalTo("GET"));
    var uri = request.uri();
    assertThat(uri.getPath(), equalTo(path));
    assertThat(uri.getQuery(), equalTo(query));
  }

  public static String getPostRequestBody(HttpRequest request) {
    assertThat(request.method(), equalTo("POST"));
    var reqBody = new RequestBodyReader().bodyAsString(request);
    return URLDecoder.decode(reqBody, StandardCharsets.UTF_8);
  }

  public static void assertPostRequest(HttpRequest request, String path, String body) {
    assertThat(request.method(), equalTo("POST"));
    var uri = request.uri();
    assertThat(uri.getPath(), equalTo(path));
    assertThat(getPostRequestBody(request), equalTo(body));
  }
}
