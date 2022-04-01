package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.MallPriceManager;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.HttpUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MallPriceDatabase {
  // If false, blocks saving of mall prices. Do not modify outside of tests.
  public static boolean savePricesToFile = true;

  private static final PriceArray prices = new PriceArray();
  private static final HashSet<String> updated = new HashSet<String>();
  private static final HashSet<String> submitted = new HashSet<String>();
  private static int modCount = 0;

  private static final int CONNECT_TIMEOUT = 15 * 1000;

  static {
    updatePricesFromSource("mallprices.txt");
    MallPriceDatabase.modCount = 0;
  }

  private MallPriceDatabase() {}

  private static int updatePricesFromSource(String filename) {
    int count = 0;
    try (BufferedReader reader = FileUtilities.getReader(filename)) {

      String line = FileUtilities.readLine(reader);
      if (line == null) {
        RequestLogger.printLine("(file not found)");
        return 0;
      }

      if (StringUtilities.parseInt(line) != KoLConstants.MALLPRICES_VERSION) {
        RequestLogger.printLine("(incompatible price file format)");
        return 0;
      }

      String[] data;
      long now = MallPriceManager.currentTimeMillis() / 1000L;

      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length < 3) {
          continue;
        }

        int id = StringUtilities.parseInt(data[0]);
        long timestamp = Math.min(now, Long.parseLong(data[1]));
        int price = StringUtilities.parseInt(data[2]);
        if (id < 1
            || id > ItemDatabase.maxItemId()
            || price < 1
            || price > 999999999
            || timestamp <= 0) { // Something's fishy with this file...
          continue;
        }

        if (!ItemDatabase.isTradeable(id)) continue;
        Price p = MallPriceDatabase.prices.get(id);
        if (p == null) {
          MallPriceDatabase.prices.set(id, new Price(price, timestamp));
          ++count;
          ++MallPriceDatabase.modCount;
        } else if (timestamp > p.timestamp) {
          p.price = price;
          p.timestamp = timestamp;
          ++count;
          ++MallPriceDatabase.modCount;
        }
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
    return count;
  }

  public static void updatePrices(String filename) {
    if (filename.length() == 0) {
      RequestLogger.printLine("No URL or filename specified.");
      return;
    }

    if (filename.startsWith("http://") || filename.startsWith("https://")) {
      if (MallPriceDatabase.updated.contains(filename)) {
        RequestLogger.printLine("Already updated from " + filename + " in this session.");
        return;
      }
      MallPriceDatabase.updated.add(filename);
    }
    int count = MallPriceDatabase.updatePricesFromSource(filename);
    if (count > 0) {
      MallPriceDatabase.writePrices();
      ConcoctionDatabase.refreshConcoctions();
    }
    RequestLogger.printLine(
        count + " price" + (count != 1 ? "s" : "") + " updated from " + filename);
  }

  public static void updatePricesInParallel(String filename) {
    RequestThread.runInParallel(new UpdatePricesRunnable(filename), false);
  }

  private static class UpdatePricesRunnable implements Runnable {
    private final String filename;

    public UpdatePricesRunnable(String filename) {
      this.filename = filename;
    }

    @Override
    public void run() {
      MallPriceDatabase.updatePrices(this.filename);
    }
  }

  public static void recordPrice(int itemId, int price) {
    MallPriceDatabase.recordPrice(itemId, price, false);
  }

  public static void recordPrice(int itemId, int price, boolean deferred) {
    long timestamp = MallPriceManager.currentTimeMillis() / 1000L;
    Price p = MallPriceDatabase.prices.get(itemId);
    if (p == null) {
      MallPriceDatabase.prices.set(itemId, new Price(price, timestamp));
    } else {
      p.price = price;
      p.timestamp = timestamp;
    }
    ++MallPriceDatabase.modCount;
    if (!deferred) {
      MallPriceDatabase.writePrices();
    }
  }

  public static void writePrices() {
    if (!MallPriceDatabase.savePricesToFile) {
      return;
    }

    File output = new File(KoLConstants.DATA_LOCATION, "mallprices.txt");
    PrintStream writer = LogStream.openStream(output, true);
    writer.println(KoLConstants.MALLPRICES_VERSION);

    for (int i = 1; i < MallPriceDatabase.prices.size(); ++i) {
      Price p = MallPriceDatabase.prices.get(i);
      if (p == null) continue;
      writer.println(i + "\t" + p.timestamp + "\t" + p.price);
    }

    writer.close();
  }

  public static void submitPrices(String url) {
    if (url.length() == 0) {
      RequestLogger.printLine("No URL specified.");
      return;
    }

    if (MallPriceDatabase.modCount == 0) {
      RequestLogger.printLine("You have no updated price data to submit.");
      return;
    }
    if (MallPriceDatabase.submitted.contains(url)) {
      RequestLogger.printLine("Already submitted to " + url + " in this session.");
      return;
    }

    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException e) {
      RequestLogger.printLine("Failed to parse " + url + ": " + e);
      return;
    }

    HttpClient client =
        HttpUtilities.getClientBuilder().connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT)).build();
    HttpRequest req =
        HttpRequest.newBuilder(uri)
            .header("User-Agent", GenericRequest.getUserAgent())
            .header("Content-Type", "multipart/form-data; boundary=--blahblahfishcakes")
            .POST(BodyPublishers.ofString(getPostData()))
            .build();

    HttpResponse<Stream<String>> res;
    try {
      res = client.send(req, BodyHandlers.ofLines());
    } catch (HttpConnectTimeoutException e) {
      RequestLogger.printLine("Connection timed out: " + e);
      return;
    } catch (Exception e) {
      RequestLogger.printLine("Submission failed: " + e);
      return;
    }

    int code = res.statusCode();
    if (code == 200) {
      RequestLogger.printLine("Success: " + res.body().findFirst().orElse(""));
      MallPriceDatabase.submitted.add(url);
    } else {
      RequestLogger.printLine("Error " + code + ": " + res.body().collect(Collectors.joining()));
    }
  }

  private static String getPostData() {
    StringBuilder builder = new StringBuilder();
    builder.append("----blahblahfishcakes\r\n");
    builder.append(
        "Content-Disposition: form-data; name=\"upload\"; filename=\"mallprices.txt\"\r\n\r\n");

    try (BufferedReader reader = FileUtilities.getReader("mallprices.txt")) {
      String line;
      while ((line = FileUtilities.readLine(reader)) != null) {
        builder.append(line);
        builder.append('\n');
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
    builder.append("\r\n----blahblahfishcakes--\r\n");
    return builder.toString();
  }

  public static int getPrice(int itemId) {
    Price p = MallPriceDatabase.prices.get(itemId);
    return p == null ? 0 : p.price;
  }

  // Return age of price data, in fractional days
  public static float getAge(int itemId) {
    Price p = MallPriceDatabase.prices.get(itemId);
    long now = MallPriceManager.currentTimeMillis() / 1000L;
    return p == null ? Float.POSITIVE_INFINITY : (now - p.timestamp) / 86400.0f;
  }

  private static class Price {
    int price;
    long timestamp;

    public Price(int price, long timestamp) {
      this.price = price;
      this.timestamp = timestamp;
    }
  }

  /**
   * Internal class which functions exactly an array of Prices, except it uses "sets" and "gets"
   * like a list. This could be done with generics (Java 1.5) but is done like this so that we get
   * backwards compatibility.
   */
  public static class PriceArray {
    private final ArrayList<Price> internalList = new ArrayList<>();

    public Price get(final int index) {
      return index < 0 || index >= this.internalList.size() ? null : this.internalList.get(index);
    }

    public void set(final int index, final Price value) {
      for (int i = this.internalList.size(); i <= index; ++i) {
        this.internalList.add(null);
      }

      this.internalList.set(index, value);
    }

    public int size() {
      return this.internalList.size();
    }
  }
}
