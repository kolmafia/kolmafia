package net.sourceforge.kolmafia.request;

import java.util.GregorianCalendar;

public class PingRequest extends GenericRequest {

  private long startTime = 0L;
  private long endTime = 0L;

  public PingRequest() {
    this("main.php");
  }

  public PingRequest(String pingURL) {
    super(pingURL);
  }

  @Override
  public void run() {
    // You can reuse this request.
    this.startTime = this.endTime = 0;

    this.startTime = new GregorianCalendar().getTimeInMillis();
    super.run();
    // *** check if we were redirected
    // *** check if we got a responseText; If not, timed out?
    this.endTime = new GregorianCalendar().getTimeInMillis();
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
