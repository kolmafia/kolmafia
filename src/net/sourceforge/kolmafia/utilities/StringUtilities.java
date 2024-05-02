package net.sourceforge.kolmafia.utilities;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;

public class StringUtilities {
  private static final HashMap<String, String> entityEncodeCache = new HashMap<>();
  private static final HashMap<String, String> entityDecodeCache = new HashMap<>();

  private static final HashMap<String, String> urlEncodeCache = new HashMap<>();
  private static final HashMap<String, String> urlDecodeCache = new HashMap<>();

  private static final HashMap<String, String> displayNameCache = new HashMap<>();
  private static final HashMap<String, String> canonicalNameCache = new HashMap<>();

  private static final HashMap<String, String> prepositionsMap = new HashMap<>();
  private static final WeakHashMap<String[], int[]> hashCache = new WeakHashMap<>();

  private static final Pattern NONINTEGER_PATTERN = Pattern.compile("[^0-9\\-]+");

  private static final Pattern PREPOSITIONS_PATTERN =
      Pattern.compile(
          "\\b(?:about|above|across|after|against|along|among|around|at|before|behind|"
              + "below|beneath|beside|between|beyond|by|down|during|except|for|from|in|inside|"
              + "into|like|near|of|off|on|onto|out|outside|over|past|through|throughout|to|"
              + "under|up|upon|with|within|without)\\b");

  private static final Pattern WHITESPACE = Pattern.compile("\n\\s*");
  private static final Pattern LINE_BREAK = Pattern.compile("<br/?>", Pattern.CASE_INSENSITIVE);

  private static final SimpleDateFormat DATE_FORMAT =
      new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);

  static {
    DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  private StringUtilities() {}

  public static synchronized long parseDate(final String dateString) {
    if (dateString != null) {
      try {
        return StringUtilities.DATE_FORMAT.parse(dateString).getTime();
      } catch (Exception e) {
        return 0;
      }
    }
    return 0;
  }

  public static String formatDate(final long date) {
    return StringUtilities.formatDate(new Date(date));
  }

  public static synchronized String formatDate(final Date date) {
    try {
      return StringUtilities.DATE_FORMAT.format(date);
    } catch (Exception e) {
      return "";
    }
  }

  /** Returns the encoded-encoded version of the provided UTF-8 string. */
  public static String getEntityEncode(final String utf8String) {
    return StringUtilities.getEntityEncode(utf8String, true);
  }

  public static String getEntityEncode(String utf8String, final boolean cache) {
    if (utf8String == null) {
      return utf8String;
    }

    String entityString = null;

    if (cache) {
      entityString = StringUtilities.entityEncodeCache.get(utf8String);
    }

    if (entityString == null) {
      if (utf8String.contains("&") && utf8String.contains(";")) {
        entityString = CharacterEntities.escape(CharacterEntities.unescape(utf8String));
      } else {
        entityString = CharacterEntities.escape(utf8String);
      }

      // The following replacement makes the Hodgman journals (which have
      // a double space after the colon) unsearchable in the Mall.
      // entityString = StringUtilities.globalStringReplace( entityString, "  ", " " );

      if (cache && utf8String.length() < 100) {
        StringUtilities.entityEncodeCache.put(utf8String, entityString);
      }
    }

    return entityString;
  }

  /** Returns the UTF-8 version of the provided character entity string. */
  public static String getEntityDecode(final String entityString) {
    return StringUtilities.getEntityDecode(entityString, true);
  }

  public static String getEntityDecode(String entityString, final boolean cache) {
    if (entityString == null) {
      return entityString;
    }

    String utf8String = null;

    if (cache) {
      utf8String = StringUtilities.entityDecodeCache.get(entityString);
    }

    if (utf8String == null) {
      utf8String = CharacterEntities.unescape(entityString);

      if (cache && entityString.length() < 100) {
        StringUtilities.entityDecodeCache.put(entityString, utf8String);
      }
    }

    return utf8String;
  }

  /** Returns the URL-encoded version of the provided URL string. */
  public static String getURLEncode(final String url) {
    if (url == null) {
      return url;
    }

    String encodedURL = StringUtilities.urlEncodeCache.get(url);

    if (encodedURL == null) {
      encodedURL = URLEncoder.encode(url, StandardCharsets.UTF_8);

      StringUtilities.urlEncodeCache.put(url, encodedURL);
    }

    return encodedURL;
  }

  /** Returns the URL-decoded version of the provided URL string. */
  public static String getURLDecode(final String url) {
    if (url == null) {
      return url;
    }

    String encodedURL = StringUtilities.urlDecodeCache.get(url);

    if (encodedURL == null) {
      encodedURL = URLDecoder.decode(url, StandardCharsets.UTF_8);

      StringUtilities.urlDecodeCache.put(url, encodedURL);
    }

    return encodedURL;
  }

  /** Returns the display name for the provided canonical name. */
  public static String getDisplayName(String name) {
    if (name == null) {
      return name;
    }

    String displayName = StringUtilities.displayNameCache.get(name);

    if (displayName == null) {
      displayName = StringUtilities.getEntityDecode(name);
      StringUtilities.displayNameCache.put(name, displayName);
    }

    return displayName;
  }

  /** Returns the canonicalized name for the provided display name. */
  public static String getCanonicalName(String name) {
    if (name == null) {
      return null;
    }

    String canonicalName = StringUtilities.canonicalNameCache.get(name);

    if (canonicalName == null) {
      canonicalName = StringUtilities.getEntityEncode(name).toLowerCase();
      if (name.length() < 100) {
        StringUtilities.canonicalNameCache.put(name, canonicalName);
      }
    }

    return canonicalName;
  }

  /**
   * Returns a list of all elements which contain the given substring in their name.
   *
   * @param names The map in which to search for the string
   * @param searchString The substring for which to search
   */
  public static List<String> getMatchingNames(final Collection<String> names, String searchString) {
    return getMatchingNames(names.toArray(new String[0]), searchString);
  }

  /**
   * Returns a list of all elements which contain the given substring in their name.
   *
   * @param names The map in which to search for the string
   * @param searchString The substring for which to search
   */
  public static List<String> getMatchingNames(final String[] names, String searchString) {
    if (searchString == null) {
      searchString = "";
    }

    searchString = searchString.trim();

    boolean isExactMatch = searchString.startsWith("\"");
    List<String> matchList = new ArrayList<>();

    if (isExactMatch) {
      String fullString = StringUtilities.getCanonicalName(searchString);
      if (Arrays.binarySearch(names, fullString) >= 0) {
        matchList.add(fullString);
        return matchList;
      }

      int end = searchString.endsWith("\"") ? searchString.length() - 1 : searchString.length();
      searchString = searchString.substring(1, end);
    }

    searchString = StringUtilities.getCanonicalName(searchString);

    if (searchString.length() == 0) {
      return matchList;
    }

    if (Arrays.binarySearch(names, searchString) >= 0) {
      matchList.add(searchString);
      return matchList;
    }

    if (isExactMatch) {
      return matchList;
    }

    int nameCount = names.length;
    int[] hashes = StringUtilities.hashCache.get(names);
    if (hashes == null) {
      hashes = new int[nameCount];
      for (int i = 0; i < nameCount; ++i) {
        hashes[i] = StringUtilities.stringHash(names[i]);
      }
      StringUtilities.hashCache.put(names, hashes);
    }
    int hash = StringUtilities.stringHash(searchString);

    for (int i = 0; i < nameCount; ++i) {
      if ((hashes[i] & hash) == hash
          && StringUtilities.substringMatches(names[i], searchString, true)) {
        matchList.add(names[i]);
      }
    }

    if (!matchList.isEmpty()) {
      return matchList;
    }

    for (int i = 0; i < nameCount; ++i) {
      if ((hashes[i] & hash) == hash
          && StringUtilities.substringMatches(names[i], searchString, false)) {
        matchList.add(names[i]);
      }
    }

    if (!matchList.isEmpty()) {
      return matchList;
    }

    // There is an oddball special case here: a search string containing
    // spaces can successfully fuzzy-match an item name with no spaces,
    // for example "in the box" will match "chef-in-the-box".  However,
    // the hash check would prevent us from even trying such a match.
    // Therefore, strip out the bit representing a space in the hash:
    hash &= ~StringUtilities.stringHash(" ");

    for (int i = 0; i < nameCount; ++i) {
      if ((hashes[i] & hash) == hash && StringUtilities.fuzzyMatches(names[i], searchString)) {
        matchList.add(names[i]);
      }
    }

    return matchList;
  }

  private static int stringHash(final String s) {
    int hash = 0;
    for (int i = s.length() - 1; i >= 0; --i) {
      hash |= 1 << (s.charAt(i) & 0x1F);
    }
    return hash;
  }

  public static boolean substringMatches(
      final String source, final String substring, final boolean checkBoundaries) {
    if (source == null) {
      return false;
    }

    if (substring == null || substring.length() == 0) {
      return true;
    }

    int index = source.indexOf(substring);
    if (index == -1) {
      return false;
    }

    if (!checkBoundaries || index == 0) {
      return true;
    }

    return !Character.isLetterOrDigit(source.charAt(index - 1));
  }

  public static boolean fuzzyMatches(final String sourceString, final String searchString) {
    if (sourceString == null) {
      return false;
    }

    if (searchString == null || searchString.length() == 0) {
      return true;
    }

    return StringUtilities.fuzzyMatches(sourceString, searchString, -1, -1);
  }

  private static boolean fuzzyMatches(
      final String sourceString,
      final String searchString,
      final int lastSourceIndex,
      final int lastSearchIndex) {
    int maxSearchIndex = searchString.length() - 1;

    if (lastSearchIndex == maxSearchIndex) {
      return true;
    }

    // Skip over any non alphanumeric characters in the search string
    // since they hold no meaning.

    char searchChar;
    int searchIndex = lastSearchIndex;

    do {
      if (++searchIndex > maxSearchIndex) {
        return true;
      }

      searchChar = searchString.charAt(searchIndex);
    } while (Character.isWhitespace(searchChar));

    // If it matched the first character in the source string, the
    // character right after the last search, or the match is on a
    // word boundary, continue searching.

    int sourceIndex = sourceString.indexOf(searchChar, lastSourceIndex + 1);

    while (sourceIndex != -1) {
      if (sourceIndex == 0
          || sourceIndex == lastSourceIndex + 1
          || isWordBoundary(sourceString.charAt(sourceIndex - 1))) {
        if (StringUtilities.fuzzyMatches(sourceString, searchString, sourceIndex, searchIndex)) {
          return true;
        }
      }

      sourceIndex = sourceString.indexOf(searchChar, sourceIndex + 1);
    }

    return false;
  }

  private static boolean isWordBoundary(char ch) {
    return ch != '#' && !Character.isLetterOrDigit(ch);
  }

  public static void insertBefore(
      final StringBuffer buffer, final String searchString, final String insertString) {
    int searchIndex = buffer.indexOf(searchString);
    if (searchIndex == -1) {
      return;
    }

    buffer.insert(searchIndex, insertString);
  }

  public static void insertAfter(
      final StringBuffer buffer, final String searchString, final String insertString) {
    int searchIndex = buffer.indexOf(searchString);
    if (searchIndex == -1) {
      return;
    }

    buffer.insert(searchIndex + searchString.length(), insertString);
  }

  public static String singleStringDelete(final String originalString, final String searchString) {
    return StringUtilities.singleStringReplace(originalString, searchString, "");
  }

  public static String singleStringReplace(
      final String originalString, final String searchString, final String replaceString) {
    if (originalString == null) {
      return null;
    }

    // Using a regular expression, while faster, results
    // in a lot of String allocation overhead.  So, use
    // a static finally-allocated StringBuffers.

    int lastIndex = originalString.indexOf(searchString);
    if (lastIndex == -1) {
      return originalString;
    }

    return originalString.substring(0, lastIndex)
        + replaceString
        + originalString.substring(lastIndex + searchString.length());
  }

  public static void singleStringDelete(final StringBuffer buffer, final String searchString) {
    StringUtilities.singleStringReplace(buffer, searchString, "");
  }

  public static void singleStringReplace(
      final StringBuffer buffer, final String searchString, final String replaceString) {
    int index = buffer.indexOf(searchString);
    if (index != -1) {
      buffer.replace(index, index + searchString.length(), replaceString);
    }
  }

  public static String globalStringDelete(final String originalString, final String searchString) {
    return StringUtilities.globalStringReplace(originalString, searchString, "");
  }

  public static String globalStringReplace(
      final String originalString, final String searchString, final String replaceString) {
    if (originalString == null) {
      return null;
    }

    if (searchString.equals("")) {
      return originalString;
    }

    // Using a regular expression, while faster, results
    // in a lot of String allocation overhead.  So, use
    // a static finally-allocated StringBuffers.

    int lastIndex = originalString.indexOf(searchString);
    if (lastIndex == -1) {
      return originalString;
    }

    StringBuilder buffer = new StringBuilder(originalString);
    while (lastIndex != -1) {
      buffer.replace(lastIndex, lastIndex + searchString.length(), replaceString);
      lastIndex = buffer.indexOf(searchString, lastIndex + replaceString.length());
    }

    return buffer.toString();
  }

  public static void globalStringReplace(
      final StringBuffer buffer, final String tag, final int replaceWith) {
    StringUtilities.globalStringReplace(buffer, tag, String.valueOf(replaceWith));
  }

  public static void globalStringDelete(final StringBuffer buffer, final String tag) {
    StringUtilities.globalStringReplace(buffer, tag, "");
  }

  public static void globalStringReplace(
      final StringBuffer buffer, final String tag, String replaceWith) {
    if (buffer == null) {
      return;
    }

    if (tag.equals("")) {
      return;
    }

    if (replaceWith == null) {
      replaceWith = "";
    }

    // Using a regular expression, while faster, results
    // in a lot of String allocation overhead.  So, use
    // a static finally-allocated StringBuffers.

    int lastIndex = buffer.indexOf(tag);
    while (lastIndex != -1) {
      buffer.replace(lastIndex, lastIndex + tag.length(), replaceWith);
      lastIndex = buffer.indexOf(tag, lastIndex + replaceWith.length());
    }
  }

  public static boolean isNumeric(String string) {
    if (string == null || string.isEmpty()) {
      return false;
    }

    char ch = string.charAt(0);

    if ((ch != '-') && (ch != '+') && !Character.isDigit(ch)) {
      return false;
    }

    for (int i = 1; i < string.length(); ++i) {
      ch = string.charAt(i);

      if ((ch != ',') && !Character.isDigit(ch)) {
        return false;
      }
    }

    return true;
  }

  public static boolean isFloat(String string) {
    if (string == null || string.length() == 0) {
      return false;
    }

    char ch = string.charAt(0);

    if ((ch != '-') && (ch != '+') && (ch != '.') && !Character.isDigit(ch)) {
      return false;
    }

    boolean hasDecimalSeparator = false;

    for (int i = 1; i < string.length(); ++i) {
      ch = string.charAt(i);

      if (ch == '.') {
        if (hasDecimalSeparator) {
          return false;
        }

        hasDecimalSeparator = true;
      }

      if (ch != '.') {
        if ((ch != ',') && !Character.isDigit(ch)) {
          return false;
        }
      }
    }

    return true;
  }

  public static int parseInt(String string) {
    long ret = StringUtilities.parseLongInternal1(string, false);
    if ((Integer.MIN_VALUE <= ret) && (ret <= Integer.MAX_VALUE)) {
      return (int) ret;
    } else {
      return 0;
    }
  }

  public static int parseIntInternal2(String string) throws NumberFormatException {
    long ret = StringUtilities.parseLongInternal2(string);
    if ((Integer.MIN_VALUE <= ret) && (ret <= Integer.MAX_VALUE)) {
      return (int) ret;
    } else {
      return 0;
    }
  }

  public static long parseLong(String string) {
    return StringUtilities.parseLongInternal1(string, false);
  }

  public static long parseLongInternal1(String string, boolean throwException)
      throws NumberFormatException {
    if (string == null) {
      return 0L;
    }

    // Remove commas anywhere in the string
    string = StringUtilities.globalStringDelete(string, ",");

    // Remove whitespace from front and end of string
    string = string.trim();

    // Remove + sign from start of string
    if (string.startsWith("+")) {
      string = string.substring(1);
    }

    if (string.length() == 0) {
      return 0L;
    }

    String tstring = string.substring(0, string.length() - 1);
    char ch = string.charAt(string.length() - 1);
    long multiplier = (ch == 'k' || ch == 'K') ? 1000 : (ch == 'm' || ch == 'M') ? 1000000 : 1;

    if (multiplier > 1 && StringUtilities.isNumeric(tstring)) {
      try {
        long rVal = Long.parseLong(tstring);
        if ((Long.MIN_VALUE / multiplier <= rVal) && (rVal <= Long.MAX_VALUE / multiplier)) {
          return rVal * multiplier;
        } else {
          RequestLogger.printLine(string + " is out of range, returning 0");
          return 0;
        }
      } catch (NumberFormatException e) {
        RequestLogger.printLine(string + " is out of range, returning 0");
        return 0;
      }
    }

    if (StringUtilities.isNumeric(string)) {
      try {
        return Long.parseLong(string);
      } catch (NumberFormatException e) {
        RequestLogger.printLine(string + " is out of range, returning 0");
        return 0L;
      }
    }

    if (StringUtilities.isFloat(tstring)) {
      return (long) (StringUtilities.parseDouble(tstring) * multiplier);
    }

    if (throwException) {
      throw new NumberFormatException(string);
    }

    return StringUtilities.parseLongInternal2(string);
  }

  public static long parseLongInternal2(String string) throws NumberFormatException {
    if (string == null) {
      return 0L;
    }
    string = NONINTEGER_PATTERN.matcher(string).replaceAll("");

    if (string.length() == 0) {
      return 0L;
    }

    try {
      return Long.parseLong(string);
    } catch (NumberFormatException e) {
      RequestLogger.printLine(string + " is out of range, returning 0");
      return 0L;
    }
  }

  public static float parseFloat(String string) {
    if (string == null) {
      return 0.0f;
    }

    if (string.startsWith("+")) {
      string = string.substring(1);
    }

    string = StringUtilities.globalStringDelete(string, ",");
    string = StringUtilities.globalStringDelete(string, " ");

    if (string.length() == 0) {
      return 0.0f;
    }

    if (!StringUtilities.isFloat(string)) {
      return 0.0f;
    }

    return Float.parseFloat(string);
  }

  public static double parseDouble(String string) {
    if (string == null) {
      return 0.0;
    }

    if (string.startsWith("+")) {
      string = string.substring(1);
    }

    string = StringUtilities.globalStringDelete(string, ",");
    string = StringUtilities.globalStringDelete(string, " ");

    if (string.length() == 0) {
      return 0.0;
    }

    if (!StringUtilities.isFloat(string)) {
      return 0.0;
    }

    return Double.parseDouble(string);
  }

  public static String basicTextWrap(String text) {
    return basicTextWrap(text, 80);
  }

  public static String basicTextWrap(String text, int breakPosition) {

    if (text == null) {
      return null;
    }

    if (text.length() < breakPosition || text.startsWith("<html>")) {
      return text;
    }

    StringBuilder result = new StringBuilder();

    while (text.length() > 0) {
      if (text.length() < breakPosition) {
        result.append(text);
        break;
      }

      int spaceIndex = text.lastIndexOf(" ", breakPosition);
      int breakIndex = text.lastIndexOf("\n", spaceIndex);

      if (breakIndex != -1) {
        result.append(text, 0, breakIndex);
        result.append("\n");
        text = text.substring(breakIndex).trim();
      } else if (spaceIndex != -1) {
        result.append(text.substring(0, spaceIndex).trim());
        result.append("\n");
        text = text.substring(spaceIndex).trim();
      } else {
        result.append(text.substring(0, breakPosition).trim());
        result.append("\n");
        text = text.substring(breakPosition).trim();
      }
    }

    return result.toString();
  }

  public static void registerPrepositions(final String text) {
    Matcher m = StringUtilities.PREPOSITIONS_PATTERN.matcher(text);
    if (!m.find()) {
      return;
    }
    StringUtilities.prepositionsMap.put(m.replaceAll("@"), text);
  }

  public static String lookupPrepositions(final String text) {
    Matcher m = StringUtilities.PREPOSITIONS_PATTERN.matcher(text);
    if (!m.find()) {
      return text;
    }
    String rv = StringUtilities.prepositionsMap.get(m.replaceAll("@"));
    return rv == null ? text : rv;
  }

  public static Map<String, String> getCopyOfPrepositionsMap() {
    return new HashMap<>(prepositionsMap);
  }

  public static void unregisterPrepositions() {
    prepositionsMap.clear();
  }

  public static String toTitleCase(final String s) {
    boolean found = false;
    char[] chars = s.toLowerCase().toCharArray();

    for (int i = 0; i < chars.length; ++i) {
      if (!found && Character.isLetter(chars[i])) {
        chars[i] = Character.toUpperCase(chars[i]);
        found = true;
      } else if (Character.isWhitespace(chars[i])) {
        found = false;
      }
    }

    return String.valueOf(chars);
  }

  public static String leetify(final String text) {
    // It makes no sense to leetify character entities, so convert
    // them to UTF-8 characters.
    String decoded = StringUtilities.getEntityDecode(text);
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < decoded.length(); ++i) {
      char c = decoded.charAt(i);
      switch (c) {
        case 'O', 'o' -> b.append("0");
        case 'I', 'i', 'L', 'l' -> b.append("1");
        case 'E', 'e' -> b.append("3");
        case 'A', 'a' -> b.append("4");
        case 'S', 's' -> b.append("5");
        case 'T', 't' -> b.append("7");
        default -> b.append(c);
      }
    }
    return b.toString();
  }

  public static boolean isVowel(char letter) {
    return switch (Character.toLowerCase(letter)) {
      case 'a', 'e', 'i', 'o', 'u' -> true;
      default -> false;
    };
  }

  public static int getBracketedId(final String name) {
    if (name.startsWith("[")) {
      int index = name.indexOf("]");
      if (index > 0) {
        String idString = name.substring(1, index);
        if (StringUtilities.isNumeric(idString)) {
          return StringUtilities.parseInt(idString);
        }
      }
    }
    return -1;
  }

  public static String removeBracketedId(final String name) {
    if (name.startsWith("[")) {
      int index = name.indexOf("]");
      if (index > 0) {
        String idString = name.substring(1, index);
        if (StringUtilities.isNumeric(idString)) {
          return name.substring(index + 1);
        }
      }
    }
    return name;
  }

  public static List<String> tokenizeString(String s, char sep, char escape, boolean trim)
      throws Exception {
    List<String> tokens = new ArrayList<>();
    StringBuilder sb = new StringBuilder();

    boolean inEscape = false;
    for (char c : s.toCharArray()) {
      if (inEscape) {
        inEscape = false;
      } else if (c == escape) {
        inEscape = true;
        continue;
      } else if (c == sep) {
        String token = sb.toString();
        tokens.add(trim ? token.trim() : token);
        sb.setLength(0);
        continue;
      }
      sb.append(c);
    }

    if (inEscape) {
      throw new Exception("Invalid terminal escape");
    }

    String finalToken = sb.toString();
    tokens.add(trim ? finalToken.trim() : finalToken);

    return tokens;
  }

  public static List<String> tokenizeString(String s, char sep, char escape) throws Exception {
    return tokenizeString(s, sep, escape, true);
  }

  public static List<String> tokenizeString(String s, char sep) throws Exception {
    return tokenizeString(s, sep, '\\');
  }

  public static List<String> tokenizeString(String s) throws Exception {
    return tokenizeString(s, ',');
  }

  public static String listToHumanString(List<String> l) {
    int last = l.size() - 1;
    if (last == 0) return l.get(last);
    return String.join(" and ", String.join(", ", l.subList(0, last)), l.get(last));
  }

  public static String stripHtml(String s) {
    // Now we begin trimming out some of the whitespace,
    // because that causes some strange rendering problems.

    s = StringUtilities.WHITESPACE.matcher(s).replaceAll("\n");

    s = StringUtilities.globalStringDelete(s, "\r");
    s = StringUtilities.globalStringDelete(s, "\n");
    s = StringUtilities.globalStringDelete(s, "\t");

    // Finally, we start replacing the various HTML tags
    // with emptiness, except for the <br> tag which is
    // rendered as a new line.

    s = StringUtilities.LINE_BREAK.matcher(s).replaceAll("\n").trim();
    s = KoLConstants.ANYTAG_PATTERN.matcher(s).replaceAll("");

    return StringUtilities.getEntityDecode(s, false).trim();
  }

  public static String capitalize(final String s) {
    return s.substring(0, 1).toUpperCase() + s.substring(1);
  }

  public static String upperSnakeToPascalCase(final String s) {
    return Arrays.stream(s.split("_"))
        .map(c -> capitalize(c.toLowerCase()))
        .collect(Collectors.joining(""));
  }

  public static String upperSnakeToWords(final String s) {
    return Arrays.stream(s.split("_"))
        .map(c -> capitalize(c.toLowerCase()))
        .collect(Collectors.joining(" "));
  }

  /**
   * Return whether a string matches a filter.
   *
   * @param str string to attempt match on
   * @param filter filter. Filters can contain '*' which are interpreted as 'any string'.
   * @return whether the string matches the filter
   */
  public static boolean matchesFilter(String str, String filter) {
    if ("".equals(filter)) return "".equals(str);

    if (!filter.contains("*")) return filter.equals(str);

    // guarantee subFilters will have positive length
    if (filter.equals("*")) return true;

    String[] subFilters = filter.split("[*]");

    if (!filter.startsWith("*")) {
      var firstFilter = subFilters[0];
      if (!str.startsWith(firstFilter)) return false;
    }

    if (!filter.endsWith("*")) {
      var lastFilter = subFilters[subFilters.length - 1];
      if (!str.endsWith(lastFilter)) return false;
    }

    int searchIndex = 0;
    for (var s : subFilters) {
      var index = str.indexOf(s, searchIndex);
      if (index == -1) return false;
      searchIndex = index + s.length();
    }
    return true;
  }

  public static final Pattern URL_IID_PATTERN = Pattern.compile("iid=(\\d+)");

  public static int extractIidFromURL(final String urlString) {
    Matcher matcher = URL_IID_PATTERN.matcher(urlString);
    return matcher.find() ? StringUtilities.parseInt(matcher.group(1)) : -1;
  }

  public static String withOrdinalSuffix(final int num) {
    return num
        + switch (num % 100) {
          case 11, 12, 13 -> "th";
          default -> switch (num % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
          };
        };
  }
}
