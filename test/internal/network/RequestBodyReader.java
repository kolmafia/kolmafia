package internal.network;

import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

public class RequestBodyReader {

  public String bodyAsString(HttpRequest request) {
    var bodyPublisher = request.bodyPublisher();
    if (bodyPublisher.isEmpty()) return null;

    var publisher = bodyPublisher.get();

    var subscriber = new BodySubscriber();
    publisher.subscribe(subscriber);

    var bytes = subscriber.getBytes();
    return new String(bytes);
  }

  private static class BodySubscriber implements Subscriber<ByteBuffer> {

    private byte[] bytes = new byte[0];

    public byte[] getBytes() {
      return bytes;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
      bytes = new byte[0];
      subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(ByteBuffer item) {
      var newBytes = new byte[item.remaining()];
      item.get(newBytes);
      bytes = concatArrs(bytes, newBytes);
    }

    @Override
    public void onError(Throwable throwable) {}

    @Override
    public void onComplete() {}

    private byte[] concatArrs(byte[] first, byte[] second) {
      int fal = first.length;
      int sal = second.length;
      byte[] result = new byte[fal + sal];
      System.arraycopy(first, 0, result, 0, fal);
      System.arraycopy(second, 0, result, fal, sal);
      return result;
    }
  }
}
