package internal.helpers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import internal.network.RequestBodyReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.hamcrest.Matcher;
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
      return JSON.parseObject(str);
    } catch (JSONException e) {
      Assertions.fail("Failed to parse JSON");
      throw new AssertionError(e);
    }
  }

  public static byte[] bytes(String path) {
    try {
      return Files.readAllBytes(Paths.get(path));
    } catch (IOException e) {
      Assertions.fail("Failed to load binary file: " + path);
      throw new AssertionError(e);
    }
  }

  public static void printRequests(List<HttpRequest> requests) {
    for (HttpRequest req : requests) {
      String method = req.method();
      var uri = req.uri();
      String path = uri.getPath();
      switch (method) {
        case "GET" -> System.out.println("GET " + path + " -> " + uri.getQuery());
        case "POST" -> System.out.println("POST " + path + " -> " + getPostRequestBody(req));
      }
    }
  }

  public static void assertGetRequest(HttpRequest request, String path) {
    assertGetRequest(request, path, null);
  }

  public static void assertGetRequest(HttpRequest request, String path, String query) {
    assertGetRequest(request, equalTo(path), equalTo(query));
  }

  public static void assertGetRequest(
      HttpRequest request, Matcher<String> path, Matcher<String> query) {
    assertThat(request.method(), equalTo("GET"));
    var uri = request.uri();
    assertThat(uri.getPath(), path);
    assertThat(uri.getQuery(), query);
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

  public static void assertPostRequest(
      HttpRequest request, String path, Matcher<String> bodyMatcher) {
    assertThat(request.method(), equalTo("POST"));
    var uri = request.uri();
    assertThat(uri.getPath(), equalTo(path));
    assertThat(getPostRequestBody(request), bodyMatcher);
  }
}
