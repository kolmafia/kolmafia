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
    this.addFormFields();
  }

  private void addFormFields() {
    switch (this.pingURL) {
      case "api.php" -> {
        // This will lengthen the elapsed time by making KoL look up
        // user data, rather than simply returning boilerplate text.
        // Do we care?
        // this.addFormField("what", "status");
        // this.addFormField("for", "KoLmafia");
      }
    }
  }

  private boolean isSafeToRun() {
    return switch (this.pingURL) {
      case "main.php", "council.php" -> !GenericRequest.abortIfInFightOrChoice(true);
      case "api.php" -> true;
      case "afterlife.php" -> true;
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

    // We can get redirected if we are logged out or in Valhalla
    if (this.redirectLocation != null) {
      this.constructURLString(this.redirectLocation, false);
      this.startTime = System.currentTimeMillis();
      this.pingURL = this.redirectLocation;
      super.run();
    }

    // We can have an empty responseText on a timeout or I/O error
    if (this.responseText == null) {
      // leave endTime at 0
      return;
    }

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
