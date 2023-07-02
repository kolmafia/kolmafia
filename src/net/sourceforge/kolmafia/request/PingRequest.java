package net.sourceforge.kolmafia.request;

public class PingRequest extends GenericRequest {

  private String pingURL = "";
  private long startTime = 0L;
  private long endTime = 0L;

  // main.php will redirect if we are in a fight or choice.
  // api.php will redirect to afterlife.php in Valhalla.
  //
  // api.php's responseText is about 1/4 the size of main.php's - and
  // measured ping time is about 1/4 as long.

  public PingRequest() {
    this("api.php");
  }

  public PingRequest(String pingURL) {
    super(pingURL);
    this.pingURL = pingURL;
  }

  private boolean isSafeToRun() {
    return switch (this.pingURL) {
      case "main.php" -> !GenericRequest.abortIfInFightOrChoice(true);
      case "api.php" -> true;
      default -> false;
    };
  }

  @Override
  public void run() {
    // You can reuse this request.
    this.startTime = this.endTime = 0L;

    // If we know we will be redirected, punt
    if (!isSafeToRun()) {
      return;
    }

    this.startTime = System.currentTimeMillis();
    super.run();
    // *** check if we were redirected
    // *** check if we got a responseText; If not, timed out?
    this.endTime = System.currentTimeMillis();
  }

  @Override
  public void processResponse() {
    // GenericRequest calls this method immediately after it receives a
    // responseText from KoL. After logging the responseText to the
    // DEBUG log, it returns immediately with no processing for various
    // simple requests.
    //
    // For api.php, it processes the JSON result is processed and returns.
    //
    // Most requests have additional processing to register encounters,
    // handle choices, look at results, and so on.
    //
    // We don't want or need any of that, since all we care about is the
    // raw request/response time.
  }

  @Override
  public boolean hasResult() {
    return false;
  }

  @Override
  public boolean hasResult(String location) {
    return false;
  }

  @Override
  protected boolean shouldFollowRedirect() {
    return false;
  }

  public long getStartTime() {
    return this.startTime;
  }

  public long getEndTime() {
    return this.startTime;
  }

  public long getElapsedTime() {
    if (this.endTime == 0) {
      return 0L;
    }
    return this.endTime - this.startTime;
  }
}
