package net.sourceforge.kolmafia.utilities;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class ByteBufferUtilities {
  private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

  private ByteBufferUtilities() {}

  public static byte[] read(File file) {
    try {
      return Files.readAllBytes(file.toPath());
    } catch (IOException e) {
      return EMPTY_BYTE_ARRAY;
    }
  }

  public static byte[] read(InputStream istream) {
    if (istream == null) {
      return EMPTY_BYTE_ARRAY;
    }

    try (istream) {
      return istream.readAllBytes();
    } catch (IOException e) {
      return EMPTY_BYTE_ARRAY;
    }
  }

  public static void read(InputStream istream, OutputStream ostream) {
    if (istream == null) {
      return;
    }
    try (istream) {
      istream.transferTo(ostream);
    } catch (IOException e) {
      // do nothing
    }
  }
}
