package net.sourceforge.kolmafia.utilities;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import javax.swing.ImageIcon;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;

public class FileUtilities {
  private static final Pattern FILEID_PATTERN = Pattern.compile("(\\d+)\\.");

  public static final BufferedReader getReader(final String filename, final boolean allowOverride) {
    return FileUtilities.getReader(
        DataUtilities.getReader(KoLConstants.DATA_DIRECTORY, filename, allowOverride));
  }

  public static final BufferedReader getReader(final String filename) {
    return FileUtilities.getReader(DataUtilities.getReader(KoLConstants.DATA_DIRECTORY, filename));
  }

  public static final BufferedReader getReader(final File file) {
    return FileUtilities.getReader(DataUtilities.getReader(file));
  }

  public static final BufferedReader getReader(final InputStream istream) {
    return FileUtilities.getReader(DataUtilities.getReader(istream));
  }

  private static BufferedReader getReader(final BufferedReader reader) {
    String lastMessage = DataUtilities.getLastMessage();
    if (lastMessage != null) {
      RequestLogger.printLine(lastMessage);
    }
    return reader;
  }

  public static final BufferedReader getVersionedReader(final String filename, final int version) {
    BufferedReader reader =
        FileUtilities.getReader(
            DataUtilities.getReader(KoLConstants.DATA_DIRECTORY, filename, true));

    // If no file, no reader
    if (reader == null) {
      return null;
    }

    // Read the version number

    String line = FileUtilities.readLine(reader);

    // Parse the version number and validate

    try {
      int fileVersion = StringUtilities.parseInt(line);

      if (version == fileVersion) {
        return reader;
      }
      RequestLogger.printLine(
          "Incorrect version of \""
              + filename
              + "\". Found "
              + fileVersion
              + " require "
              + version);
    } catch (Exception e) {
      // Incompatible data file, use KoLmafia's internal
      // files instead.
    }

    // We don't understand this file format
    try {
      reader.close();
    } catch (Exception e) {
      StaticEntity.printStackTrace(e);
    }

    // Override file is wrong version. Get built-in file

    reader = DataUtilities.getReader(KoLConstants.DATA_DIRECTORY, filename, false);
    // Don't forget to skip past its version number:
    FileUtilities.readLine(reader);
    return reader;
  }

  public static final String readLine(final BufferedReader reader) {
    if (reader == null) {
      return null;
    }

    try {
      String line;

      // Read in all of the comment lines, or until
      // the end of file, whichever comes first.

      while ((line = reader.readLine()) != null && (line.startsWith("#") || line.length() == 0)) {}

      // If you've reached the end of file, then
      // return null.  Otherwise, return the line
      // that's been split on tabs.

      return line;
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
      return null;
    }
  }

  public static final String[] readData(final BufferedReader reader) {
    if (reader == null) {
      return null;
    }

    String line = readLine(reader);
    return line == null ? null : line.split("\t", -1);
  }

  public static final boolean internalRelayScriptExists(final String filename) {
    return DataUtilities.getInputStream(KoLConstants.RELAY_DIRECTORY, filename, false)
        != DataUtilities.EMPTY_STREAM;
  }

  public static final boolean loadLibrary(
      final File parent, final String directory, final String filename) {
    // Next, load the icon which will be used by KoLmafia
    // in the system tray.  For now, this will be the old
    // icon used by KoLmelion.

    File library = new File(parent, filename);

    if (library.exists()) {
      if (parent == KoLConstants.RELAY_LOCATION
          && !Preferences.getString("lastRelayUpdate").equals(StaticEntity.getVersion())) {
        library.delete();
      } else {
        return true;
      }
    }

    InputStream istream = DataUtilities.getInputStream(directory, filename);

    byte[] data = ByteBufferUtilities.read(istream);
    OutputStream output = DataUtilities.getOutputStream(library);

    try {
      output.write(data);
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }

    try {
      output.close();
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }

    return true;
  }

  private static HttpURLConnection connectToRemoteFile(final String remote) {
    try {
      return HttpUtilities.openConnection(new URL(null, remote));
    } catch (IOException e) {
      return null;
    }
  }

  private static InputStream getInputStreamFromConnection(
      final String remote, final HttpURLConnection connection) {
    if (RequestLogger.isDebugging()) {
      GenericRequest.printRequestProperties(remote, connection);
    }

    if (RequestLogger.isTracing()) {
      RequestLogger.trace("Requesting: " + remote);
    }

    try {
      int responseCode = connection.getResponseCode();
      String responseMessage = connection.getResponseMessage();
      switch (responseCode) {
        case 200:
          InputStream istream = connection.getInputStream();
          if ("gzip".equals(connection.getContentEncoding())) {
            istream = new GZIPInputStream(istream);
          }

          if (RequestLogger.isDebugging()) {
            GenericRequest.printHeaderFields(remote, connection);
          }

          if (RequestLogger.isTracing()) {
            RequestLogger.trace("Retrieved: " + remote);
          }

          return istream;
        case 304:
          // Requested variant not modified, fall through.
          if (RequestLogger.isDebugging()) {
            RequestLogger.updateDebugLog("Not modified: " + remote);
          }

          if (RequestLogger.isTracing()) {
            RequestLogger.trace("Not modified: " + remote);
          }
        default:
          if (RequestLogger.isDebugging()) {
            RequestLogger.updateDebugLog(
                "Server returned response code " + responseCode + " (" + responseMessage + ")");
          }
          return null;
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
      return null;
    }
  }

  private static void downloadFileToStream(
      final String remote, final InputStream istream, final OutputStream ostream) {
    try {
      // If it's Javascript, then modify it so that
      // all the variables point to KoLmafia.
      if (remote.endsWith(".js")) {
        byte[] bytes = ByteBufferUtilities.read(istream);
        String text = new String(bytes);
        text = StringUtilities.globalStringReplace(text, "location.hostname", "location.host");
        ostream.write(text.getBytes());
      } else if (remote.endsWith(".gif")) {
        byte[] bytes = ByteBufferUtilities.read(istream);
        String signature = new String(bytes, 0, 3);
        // Certain firewalls return garbage if they
        // prevent you from getting to the image
        // server. Don't cache that.

        // Additionally, don't cache KoL's blank image that seems
        // to be a standin for a 404 these days..
        // Removing this check for now, since only a legitimate
        // blank.gif currently returns a blank image
        // && bytes.length != 61
        if (signature.equals("GIF")) {
          ostream.write(bytes, 0, bytes.length);
        }
      } else {
        ByteBufferUtilities.read(istream, ostream);
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }

    try {
      ostream.close();
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  public static final StringBuffer downloadFile(final String remote) {
    HttpURLConnection connection = connectToRemoteFile(remote);
    if (connection == null) {
      System.out.println(remote);
      return new StringBuffer();
    }

    InputStream istream = getInputStreamFromConnection(remote, connection);
    if (istream == null) {
      return new StringBuffer();
    }

    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
    downloadFileToStream(remote, istream, ostream);
    return new StringBuffer(StringUtilities.getEncodedString(ostream.toByteArray(), "UTF-8"));
  }

  public static final void downloadFile(final String remote, final File local) {
    // Assume that any file with content is good
    FileUtilities.downloadFile(remote, local, false);
  }

  public static final void downloadFile(
      final String remote, final File local, boolean probeLastModified) {
    if (!local.exists() || local.length() == 0) {
      // If we don't have it cached, don't probe
      probeLastModified = false;
    } else if (!probeLastModified) {
      // If we are not probing, assume that a file with content is good
      return;
    }

    HttpURLConnection connection = connectToRemoteFile(remote);
    if (connection == null) {
      return;
    }

    if (probeLastModified) {
      // This isn't perfect, because the user could've modified the file themselves, but it's better
      // than nothing.
      connection.setIfModifiedSince(local.lastModified());
    }

    if (remote.startsWith("http://pics.communityofloathing.com")) {
      Matcher idMatcher = FileUtilities.FILEID_PATTERN.matcher(local.getPath());
      if (idMatcher.find()) {
        connection.setRequestProperty(
            "Referer", "http://www.kingdomofloathing.com/showplayer.php?who=" + idMatcher.group(1));
      }
    }

    InputStream istream = getInputStreamFromConnection(remote, connection);
    if (istream == null) {
      return;
    }

    OutputStream ostream = DataUtilities.getOutputStream(local);

    downloadFileToStream(remote, istream, ostream);

    // Don't keep a 0-length file
    if (local.exists() && local.length() == 0) {
      local.delete();
    } else {
      String lastModifiedString = connection.getHeaderField("Last-Modified");
      long lastModified = StringUtilities.parseDate(lastModifiedString);
      if (lastModified > 0) {
        local.setLastModified(lastModified);
      }
    }
  }

  /** Downloads the given file from the KoL images server and stores it locally. */
  private static String localImageName(final String filename) {
    if (filename == null || filename.equals("")) {
      return null;
    }
    String images = ".com";
    int index = filename.lastIndexOf(images);
    int offset = index == -1 ? 0 : (index + images.length() + 1);
    String localname = offset > 0 ? filename.substring(offset) : filename;
    if (localname.startsWith("albums/")) {
      localname = localname.substring(7);
    }
    return localname;
  }

  public static final File imageFile(final String filename) {
    return new File(KoLConstants.IMAGE_LOCATION, FileUtilities.localImageName(filename));
  }

  public static final File downloadImage(final String filename) {
    String localname = FileUtilities.localImageName(filename);
    File localfile = new File(KoLConstants.IMAGE_LOCATION, localname);

    try {
      if (!localfile.exists() || localfile.length() == 0) {
        if (JComponentUtilities.getImage(localname) != null) {
          loadLibrary(KoLConstants.IMAGE_LOCATION, KoLConstants.IMAGE_DIRECTORY, localname);
        } else {
          downloadFile(filename, localfile);
        }
      }

      return localfile;
    } catch (Exception e) {
      // This can happen whenever there is bad internet
      // or whenever the familiar is brand-new.

      return null;
    }
  }

  /** Downloads an image from the KoL image server and returns it as an icon */
  public static final ImageIcon downloadIcon(
      final String image, final String container, final String defaultImage) {
    if (image == null || image.equals("")) {
      return JComponentUtilities.getImage(defaultImage);
    }

    String path = container == null || container.equals("") ? image : container + "/" + image;

    File file = FileUtilities.downloadImage(KoLmafia.imageServerPath() + path);
    if (file == null) {
      return JComponentUtilities.getImage(defaultImage);
    }
    ImageIcon icon = JComponentUtilities.getImage(path);
    return icon != null ? icon : JComponentUtilities.getImage(defaultImage);
  }

  /** Copies a file. */
  public static void copyFile(File source, File destination) {
    InputStream sourceStream = DataUtilities.getInputStream(source);
    OutputStream destinationStream = DataUtilities.getOutputStream(destination);

    if (!(sourceStream instanceof FileInputStream)
        || !(destinationStream instanceof FileOutputStream)) {
      try {
        sourceStream.close();
      } catch (IOException e) {
        StaticEntity.printStackTrace(e);
      }

      try {
        destinationStream.close();
      } catch (IOException e) {
        StaticEntity.printStackTrace(e);
      }

      return;
    }

    FileChannel sourceChannel = ((FileInputStream) sourceStream).getChannel();
    FileChannel destinationChannel = ((FileOutputStream) destinationStream).getChannel();

    try {
      sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }

    try {
      sourceStream.close();
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }

    try {
      destinationStream.close();
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  private static List<Object> getPathList(File f) {
    List<Object> l = new ArrayList<Object>();
    File r;
    try {
      r = f.getCanonicalFile();
      while (r != null) {
        l.add(r.getName());
        r = r.getParentFile();
      }
    } catch (IOException e) {
      e.printStackTrace();
      l = null;
    }
    return l;
  }

  /**
   * figure out a string representing the relative path of 'f' with respect to 'r'
   *
   * @param r home path
   * @param f path of file
   */
  private static String matchPathLists(List<Object> r, List<Object> f) {
    int i;
    int j;
    String s;
    // start at the beginning of the lists
    // iterate while both lists are equal
    s = "";
    i = r.size() - 1;
    j = f.size() - 1;

    // first eliminate common root
    while ((i >= 0) && (j >= 0) && (r.get(i).equals(f.get(j)))) {
      i--;
      j--;
    }

    // for each remaining level in the home path, add a ..
    for (; i >= 0; i--) {
      s += ".." + File.separator;
    }

    // for each level in the file path, add the path
    for (; j >= 1; j--) {
      s += f.get(j) + File.separator;
    }

    // file name
    s += f.get(j);
    return s;
  }

  /**
   * get relative path of File 'f' with respect to 'home' directory example : home = /a/b/c f =
   * /a/d/e/x.txt s = getRelativePath(home,f) = ../../d/e/x.txt
   *
   * @param home base path, should be a directory, not a file, or it doesn't make sense
   * @param f file to generate path for
   * @return path from home to f as a string
   */
  public static String getRelativePath(File home, File f) {
    List<Object> homelist;
    List<Object> filelist;
    String s;

    homelist = getPathList(home);
    filelist = getPathList(f);
    s = matchPathLists(homelist, filelist);

    return s;
  }
}
