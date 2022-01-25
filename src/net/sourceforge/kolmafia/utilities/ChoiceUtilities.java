package net.sourceforge.kolmafia.utilities;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.session.ChoiceManager;

/** Utilities for extracting data from a choice.php response */
public class ChoiceUtilities {
  private static final Pattern FORM_PATTERN = Pattern.compile("<form.*?</form>", Pattern.DOTALL);
  private static final Pattern OPTION_PATTERN1 =
      Pattern.compile("name=[\"']?option[\"']? value=[\"']?(\\d+)[\"']?");
  private static final Pattern TEXT_PATTERN1 =
      Pattern.compile("class=[\"']?button[\"']?.*?value=(?:\"([^\"]*)\"|'([^']*)'|([^ >]*))");

  private static final Pattern LINK_PATTERN = Pattern.compile("<[aA] .*?</[aA]>", Pattern.DOTALL);
  private static final Pattern OPTION_PATTERN2 = Pattern.compile("&option=(\\d+)");
  private static final Pattern TEXT_PATTERN2 =
      Pattern.compile("title=(?:\"([^\"]*)\"|'([^']*)'|([^ >]*))");

  private static final Pattern[] CHOICE_PATTERNS = {
    Pattern.compile("name=['\"]?whichchoice['\"]? value=['\"]?(\\d+)['\"]?"),
    Pattern.compile("value=['\"]?(\\d+)['\"]? name=['\"]?whichchoice['\"]?"),
    Pattern.compile("whichchoice=(\\d+)"),
  };

  private ChoiceUtilities() {}

  public static int extractChoice(final String responseText) {
    for (Pattern pattern : ChoiceUtilities.CHOICE_PATTERNS) {
      Matcher matcher = pattern.matcher(responseText);
      if (matcher.find()) {
        return StringUtilities.parseInt(matcher.group(1));
      }
    }

    // Rarely, a choice isn't given, but try to identify it anyway:
    if (responseText.contains("<b>Hippy Talkin'</b>")) {
      // Is this really missing? My logs look normal
      return 798;
    } else if (responseText.contains("<b>The WLF Bunker</b>")) {
      return 1093;
    } else if (responseText.contains("<b>Lyle, LyleCo CEO</b>")) {
      return 1309;
    } else if (responseText.contains("<b>What the Future Holds</b>")) {
      return 1462;
    }

    return 0;
  }

  public static Map<Integer, String> parseChoices(final String responseText) {
    Map<Integer, String> rv = new TreeMap<Integer, String>();
    if (responseText == null) {
      return rv;
    }

    Matcher m = FORM_PATTERN.matcher(responseText);
    while (m.find()) {
      String form = m.group();
      if (!form.contains("choice.php")) {
        continue;
      }
      Matcher optMatcher = OPTION_PATTERN1.matcher(form);
      if (!optMatcher.find()) {
        continue;
      }
      int decision = Integer.parseInt(optMatcher.group(1));
      Integer key = IntegerPool.get(decision);
      if (rv.get(key) != null) {
        continue;
      }
      Matcher textMatcher = TEXT_PATTERN1.matcher(form);
      String text =
          !textMatcher.find()
              ? "(secret choice)"
              : textMatcher.group(1) != null
                  ? textMatcher.group(1)
                  : textMatcher.group(2) != null
                      ? textMatcher.group(2)
                      : textMatcher.group(3) != null ? textMatcher.group(3) : "(secret choice)";
      rv.put(key, text);
    }

    m = LINK_PATTERN.matcher(responseText);
    while (m.find()) {
      String form = m.group();
      if (!form.contains("choice.php")) {
        continue;
      }
      Matcher optMatcher = OPTION_PATTERN2.matcher(form);
      if (!optMatcher.find()) {
        continue;
      }
      int decision = Integer.parseInt(optMatcher.group(1));
      Integer key = IntegerPool.get(decision);
      if (rv.get(key) != null) {
        continue;
      }
      Matcher textMatcher = TEXT_PATTERN2.matcher(form);
      String text =
          !textMatcher.find()
              ? "(secret choice)"
              : textMatcher.group(1) != null
                  ? textMatcher.group(1)
                  : textMatcher.group(2) != null
                      ? textMatcher.group(2)
                      : textMatcher.group(3) != null ? textMatcher.group(3) : "(secret choice)";
      rv.put(key, text);
    }

    return rv;
  }

  public static Map<Integer, String> parseChoicesWithSpoilers(final String responseText) {
    Map<Integer, String> rv = ChoiceUtilities.parseChoices(responseText);
    if (responseText == null) {
      return rv;
    }

    if (!ChoiceManager.handlingChoice) {
      return rv;
    }

    Object[][] possibleDecisions = ChoiceManager.choiceSpoilers(ChoiceManager.lastChoice, null);
    if (possibleDecisions == null) {
      return rv;
    }

    Object[] options = possibleDecisions[2];
    if (options == null) {
      return rv;
    }

    for (Map.Entry<Integer, String> entry : rv.entrySet()) {
      Integer key = entry.getKey();
      Object option = ChoiceManager.findOption(options, key);
      if (option != null) {
        String text = entry.getValue() + " (" + option.toString() + ")";
        rv.put(key, text);
      }
    }

    return rv;
  }

  public static String actionOption(final String action, final String responseText) {
    Map<Integer, String> choices = ChoiceUtilities.parseChoices(responseText);
    for (Map.Entry<Integer, String> entry : choices.entrySet()) {
      if (entry.getValue().equals(action)) {
        return String.valueOf(entry.getKey());
      }
    }
    return null;
  }

  // Support for extra fields.
  //
  //	<select name=tossid><option value=7375>actual tapas  (5 casualties)</option></select>
  //	Coordinates: <input name=word type=text size=15 maxlength=7><br>(a valid set of coordinates is
  // 7 letters)<p>
  //
  // checkboxes (no examples)
  // radio buttons (no examples

  // <select name=tossid>><option value=7375>actual tapas  (5 casualties)</option>
  private static final Pattern SELECT_PATTERN =
      Pattern.compile("<select .*?name=['\"]?(.*?)['\"]?>(.*?)</select>", Pattern.DOTALL);
  private static final Pattern SELECT_OPTION_PATTERN =
      Pattern.compile("<option value=['\"]?(.*?)['\"]?>(.*?)</option>");

  public static Map<Integer, Map<String, Set<String>>> parseSelectInputs(
      final String responseText) {
    // Return a map from CHOICE => map from NAME => set of OPTIONS
    Map<Integer, Map<String, Set<String>>> rv = new TreeMap<Integer, Map<String, Set<String>>>();

    if (responseText == null) {
      return rv;
    }

    // Find all choice forms
    Matcher m = FORM_PATTERN.matcher(responseText);
    while (m.find()) {
      String form = m.group();
      if (!form.contains("choice.php")) {
        continue;
      }
      Matcher optMatcher = OPTION_PATTERN1.matcher(form);
      if (!optMatcher.find()) {
        continue;
      }

      // Collect all the selects from this form
      Map<String, Set<String>> choice = new TreeMap<String, Set<String>>();

      // Find all "select" tags within this form
      Matcher s = SELECT_PATTERN.matcher(form);
      while (s.find()) {
        String name = s.group(1);

        // For each, extract all the options into a set
        Set<String> options = new TreeSet<String>();

        Matcher o = SELECT_OPTION_PATTERN.matcher(s.group(2));
        while (o.find()) {
          options.add(o.group(1));
        }

        choice.put(name, options);
      }

      if (choice.size() > 0) {
        rv.put(Integer.parseInt(optMatcher.group(1)), choice);
      }
    }

    return rv;
  }

  public static Map<Integer, Map<String, Map<String, String>>> parseSelectInputsWithTags(
      final String responseText) {
    // Return a map from CHOICE => map from NAME => map from OPTION => SPOILER
    Map<Integer, Map<String, Map<String, String>>> rv =
        new TreeMap<Integer, Map<String, Map<String, String>>>();

    if (responseText == null) {
      return rv;
    }

    // Find all choice forms
    Matcher m = FORM_PATTERN.matcher(responseText);
    while (m.find()) {
      String form = m.group();
      if (!form.contains("choice.php")) {
        continue;
      }
      Matcher optMatcher = OPTION_PATTERN1.matcher(form);
      if (!optMatcher.find()) {
        continue;
      }

      // Collect all the selects from this form
      Map<String, Map<String, String>> choice = new TreeMap<String, Map<String, String>>();

      // Find all "select" tags within this form
      Matcher s = SELECT_PATTERN.matcher(form);
      while (s.find()) {
        String name = s.group(1);

        // For each, extract all the options into a map
        Map<String, String> options = new TreeMap<String, String>();

        Matcher o = SELECT_OPTION_PATTERN.matcher(s.group(2));
        while (o.find()) {
          String option = o.group(1);
          String tag = o.group(2);
          options.put(option, tag);
        }
        choice.put(name, options);
      }

      if (choice.size() > 0) {
        rv.put(Integer.parseInt(optMatcher.group(1)), choice);
      }
    }

    return rv;
  }

  // Coordinates: <input name=word type=text size=15 maxlength=7><br>(a valid set of coordinates is
  // 7 letters)<p>
  private static final Pattern INPUT_PATTERN = Pattern.compile("<input (.*?)>", Pattern.DOTALL);
  private static final Pattern NAME_PATTERN = Pattern.compile("name=['\"]?([^'\" >]+)['\"]?");
  private static final Pattern TYPE_PATTERN = Pattern.compile("type=['\"]?([^'\" >]+)['\"]?");

  public static Map<Integer, Set<String>> parseTextInputs(final String responseText) {
    // Return a map from CHOICE => set of NAME
    Map<Integer, Set<String>> rv = new TreeMap<Integer, Set<String>>();

    if (responseText == null) {
      return rv;
    }

    // Find all choice forms
    Matcher m = FORM_PATTERN.matcher(responseText);
    while (m.find()) {
      String form = m.group();
      if (!form.contains("choice.php")) {
        continue;
      }
      Matcher optMatcher = OPTION_PATTERN1.matcher(form);
      if (!optMatcher.find()) {
        continue;
      }

      // Collect all the text inputs from this form
      Set<String> choice = new TreeSet<String>();

      // Find all "input" tags within this form
      Matcher i = INPUT_PATTERN.matcher(form);
      while (i.find()) {
        String input = i.group(1);
        Matcher t = TYPE_PATTERN.matcher(input);
        if (!t.find()) {
          continue;
        }

        String type = t.group(1);

        if (!type.equals("text")) {
          continue;
        }

        Matcher n = NAME_PATTERN.matcher(input);
        if (!n.find()) {
          continue;
        }
        String name = n.group(1);
        choice.add(name);
      }

      if (choice.size() > 0) {
        rv.put(Integer.parseInt(optMatcher.group(1)), choice);
      }
    }

    return rv;
  }

  public static String validateChoiceFields(
      final String decision, final String extraFields, final String responseText) {
    // Given the response text from visiting a choice, determine if
    // a particular decision (option) and set of extra fields are valid.
    //
    // Some decisions are not always available.
    // Some decisions have extra fields from "select" inputs which must be specified.
    // Some select inputs are variable: available options vary.
    // Some decisions have extra fields from "text" inputs which must be specified.

    // This method checks all of the following:
    //
    // - The decision is currently available
    // - All required select and inputs are supplied
    // - No invalid select values are supplied
    //
    // If all is well, null is returned, and decision + extraFields
    // will work as a response to the choice as presented
    //
    // If there are errors, returns a string, suitable as an error
    // message, describing all of the issues.

    // Must have a response text to examine
    if (responseText == null) {
      return "No response text.";
    }

    // Figure out which choice we are in from responseText
    int choice = ChoiceUtilities.extractChoice(responseText);
    if (choice == 0) {
      return "No choice adventure in response text.";
    }

    String choiceOption = choice + "/" + decision;

    // See if supplied decision is available
    Map<Integer, String> choices = ChoiceUtilities.parseChoices(responseText);
    if (!choices.containsKey(StringUtilities.parseInt(decision))) {
      return "Choice option " + choiceOption + " is not available.";
    }

    // Accumulate multiple errors in a buffer
    StringBuilder errors = new StringBuilder();

    // Extract supplied extra fields
    Set<String> extras = new TreeSet<String>();
    for (String field : extraFields.split("&")) {
      if (field.equals("")) {
      } else if (field.contains("=")) {
        extras.add(field);
      } else {
        errors.append("Invalid extra field: '").append(field).append("'; no value supplied.\n");
      }
    }

    // Selects: get a map from CHOICE => map from NAME => set of OPTIONS
    Map<Integer, Map<String, Set<String>>> formSelects =
        ChoiceUtilities.parseSelectInputs(responseText);

    // Texts: get a map from CHOICE => set of NAMES
    Map<Integer, Set<String>> formTexts = ChoiceUtilities.parseTextInputs(responseText);

    // Does the decision have extra select or text inputs?
    Integer key = IntegerPool.get(StringUtilities.parseInt(decision));
    Map<String, Set<String>> selects = formSelects.get(key);
    Set<String> texts = formTexts.get(key);

    if (selects == null && texts == null) {
      // No. If the user supplied no extra fields, all is well
      if (extras.size() == 0) {
        return (errors.length() > 0) ? errors.toString() : null;
      }
      // Otherwise, list all unexpected extra fields
      for (String extra : extras) {
        errors
            .append("Choice option ")
            .append(choiceOption)
            .append("does not require '")
            .append(extra)
            .append("'.\n");
      }
      return errors.toString();
    }

    // There are select and/or text inputs available/required for this form.

    // Make a map from supplied field => value
    Map<String, String> suppliedFields = new TreeMap<String, String>();
    for (String field : extras) {
      // We validated this above; only fields with '=' are included
      int equals = field.indexOf("=");
      String name = field.substring(0, equals);
      String value = field.substring(equals + 1);
      suppliedFields.put(name, value);
    }

    // All selects in the form must have a value supplied
    if (selects != null) {
      for (Map.Entry<String, Set<String>> select : selects.entrySet()) {
        String name = select.getKey();
        Set<String> values = select.getValue();
        String supplied = suppliedFields.get(name);
        if (supplied == null) {
          // Did not supply a value for a field
          errors
              .append("Choice option ")
              .append(choiceOption)
              .append(" requires '")
              .append(name)
              .append("' but not supplied.\n");
        } else if (!values.contains(supplied)) {
          errors
              .append("Choice option ")
              .append(choiceOption)
              .append(" requires '")
              .append(name)
              .append("' but '")
              .append(supplied)
              .append("' is not a valid value.\n");
        } else {
          suppliedFields.remove(name);
        }
      }
    }

    // All text inputs in the form must have a value supplied
    if (texts != null) {
      for (String name : texts) {
        String supplied = suppliedFields.get(name);
        if (supplied == null) {
          // Did not supply a value for a field
          errors
              .append("Choice option ")
              .append(choiceOption)
              .append(" requires '")
              .append(name)
              .append("' but not supplied.\n");
        } else {
          suppliedFields.remove(name);
        }
      }
    }

    // No unnecessary fields in the form can be supplied
    for (Map.Entry<String, String> supplied : suppliedFields.entrySet()) {
      String name = supplied.getKey();
      errors
          .append("Choice option ")
          .append(choiceOption)
          .append("does not require '")
          .append(name)
          .append("'.\n");
    }

    return (errors.length() > 0) ? errors.toString() : null;
  }

  public static void printChoices(final String responseText) {
    Map<Integer, String> choices = ChoiceUtilities.parseChoicesWithSpoilers(responseText);
    Map<Integer, Map<String, Map<String, String>>> selects =
        ChoiceUtilities.parseSelectInputsWithTags(responseText);
    Map<Integer, Set<String>> texts = ChoiceUtilities.parseTextInputs(responseText);
    for (Map.Entry<Integer, String> choice : choices.entrySet()) {
      Integer choiceKey = choice.getKey();
      RequestLogger.printLine("<b>choice " + choiceKey + "</b>: " + choice.getValue());
      Map<String, Map<String, String>> choiceSelects = selects.get(choiceKey);
      if (choiceSelects != null) {
        for (Map.Entry<String, Map<String, String>> select : choiceSelects.entrySet()) {
          Map<String, String> options = select.getValue();
          RequestLogger.printLine(
              "\u00A0\u00A0select = <b>"
                  + select.getKey()
                  + "</b> ("
                  + options.size()
                  + " options)");
          for (Map.Entry<String, String> option : options.entrySet()) {
            RequestLogger.printLine(
                "\u00A0\u00A0\u00A0\u00A0" + option.getKey() + " => " + option.getValue());
          }
        }
      }
      Set<String> choiceTexts = texts.get(choiceKey);
      if (choiceTexts != null) {
        for (String name : choiceTexts) {
          RequestLogger.printLine("\u00A0\u00A0text = <b>" + name + "</b>");
        }
      }
    }
  }
}
