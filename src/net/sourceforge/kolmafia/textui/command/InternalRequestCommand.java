package net.sourceforge.kolmafia.textui.command;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import net.sourceforge.kolmafia.request.GenericRequest;

public class InternalRequestCommand extends AbstractCommand {
  private boolean isRunning = false;
  private final List<GenericRequest> requests = new LinkedList<>();

  public void addRequest(GenericRequest request) {
    if (this.isRunning) {
      return;
    }

    this.requests.add(request);
  }

  public void addRequests(GenericRequest[] requests) {
    if (this.isRunning) {
      return;
    }

    this.requests.addAll(Arrays.asList(requests));
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (this.isRunning) {
      return;
    }

    this.isRunning = true;

    Iterator<GenericRequest> requestIterator = this.requests.iterator();

    while (requestIterator.hasNext()) {
      GenericRequest request = requestIterator.next();

      request.run();
    }
  }
}
