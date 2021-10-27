package net.sourceforge.kolmafia.preferences;

import static org.junit.jupiter.api.Assertions.*;

import java.util.TreeMap;
import net.sourceforge.kolmafia.KoLCharacter;
import org.junit.jupiter.api.*;

class PreferencesTest {

  // Convert to the new way of doing things...
  @BeforeAll
  protected static void initAll() {
    KoLCharacter.reset("fakePrefUser");
    KoLCharacter.reset(true);
    Preferences.saveSettingsToFile = false;
  }

  @Test
  void ResetClearsPrefs() {
    String propName = "aTestProp";
    Preferences.setBoolean(propName, true);

    assertTrue(Preferences.getBoolean(propName), "Property Set but does not exist.");
    Preferences.reset("fakePrefUser"); // reload from disk
    assertFalse(Preferences.getBoolean(propName), "Property not restored from disk by reset.");
  }

  @Test
  void BaseUserName() {
    String testName = KoLCharacter.getUserName();
    String result = Preferences.baseUserName(testName);
    assertEquals(testName.toLowerCase(), result, "Base User Name does not match");
  }

  @Test
  void PropertyExists() {
    Preferences.setBoolean("ATestProperty", true);
    boolean result = Preferences.propertyExists("ATestProperty", false);

    assertTrue(result, "Property Set but does not exist.");

    Preferences.removeProperty("ATestProperty", false);
    result = Preferences.propertyExists("ATestProperty", false);
    assertFalse(result, "Property Remove but still found.");
  }

  @Test
  void PreferenceDefaults() {
    Integer result = Integer.parseInt(Preferences.getDefault("dailyDeedsVersion"));
    assertEquals(13, result, "dailyDeedsVersion unexpected result");
  }

  @Test
  void PreferenceProperties() {
    String userName = KoLCharacter.getUserName();
    Preferences.setString("userPref", "local");

    boolean result = Preferences.isGlobalProperty("userPref");
    assertFalse(result, "userPref is global");

    result = Preferences.isGlobalProperty("chatBeep");
    assertTrue(result, "chatBeep is not global");

    result = Preferences.isPerUserGlobalProperty("getBreakfast." + userName);
    assertTrue(result, "getBreakfast is not a per-user global property");

    result = Preferences.isUserEditable("saveState." + userName);
    assertFalse(result, "saveState is marked user editable");

    result = Preferences.isUserEditable("chatBeep");
    assertTrue(result, "chatBeep is NOT marked user editable");
  }

  @Test
  void StringProperties() {
    String PrefName = "aStringPref";
    Preferences.setString(PrefName, "");

    String checkPref = Preferences.getString(PrefName);
    assertEquals(checkPref, "", "Pref not cleared");

    String PrefValue = "ANewValue";
    Preferences.setString(PrefName, PrefValue);
    checkPref = Preferences.getString(PrefName);
    assertEquals(checkPref, PrefValue, "String not stored in Prefs");
  }

  @Test
  void BooleanProperties() {
    String PrefName = "aBooleanPref";
    Preferences.setBoolean(PrefName, true);

    boolean checkPref = Preferences.getBoolean(PrefName);
    assertTrue(checkPref, "Boolean Pref failed");
    Preferences.setBoolean(PrefName, false);

    checkPref = Preferences.getBoolean(PrefName);
    assertFalse(checkPref, "Preference not recorded");
  }

  @Test
  void IntegerProperties() {
    String prefName = "aBooleanPref";
    Integer prefValue = 73;
    Preferences.setInteger(prefName, prefValue);

    Integer checkPref = Preferences.getInteger(prefName);
    assertEquals(prefValue, checkPref, "Integer Pref failed");

    Preferences.setInteger(prefName, 0);
    checkPref = Preferences.getInteger(prefName);
    assertEquals(0, checkPref, "Preference not recorded");
  }

  @Test
  void FloatProperties() {
    String prefName = "aFloatPref";
    Float prefValue = (float) 7.3;
    Preferences.setFloat(prefName, prefValue);

    Float checkPref = Preferences.getFloat(prefName);
    assertEquals(prefValue, checkPref, "Float Pref failed");

    Preferences.setFloat(prefName, 0);
    checkPref = Preferences.getFloat(prefName);
    assertEquals(0, checkPref, "Preference not recorded");
  }

  @Test
  void LongPreferences() {
    String prefName = "aLongPref";
    Long prefValue = 73L;
    Preferences.setLong(prefName, prefValue);

    Long checkPref = Preferences.getLong(prefName);
    assertEquals(prefValue, checkPref, "Long Pref failed");

    Preferences.setLong(prefName, 0);
    checkPref = Preferences.getLong(prefName);
    assertEquals(0, checkPref, "Preference not recorded");
  }

  @Test
  void DoublePreferences() {
    String prefName = "aDoublePref";
    Double prefValue = 3.7;
    Preferences.setDouble(prefName, prefValue);

    Double checkPref = Preferences.getDouble(prefName);
    assertEquals(prefValue, checkPref, "Double Pref failed");

    Preferences.setDouble(prefName, 0);
    checkPref = Preferences.getDouble(prefName);
    assertEquals(0, checkPref, "Preference not recorded");
  }

  @Test
  void IncrementPref() {
    String prefName = "anIntegerPref";
    int prefValue = 73;
    String prefString = "aStringPref";

    Preferences.setInteger(prefName, prefValue);
    Preferences.increment(prefName);
    Integer checkIncrement = Preferences.getInteger(prefName);
    assertEquals(prefValue + 1, checkIncrement, "Increment by one failed");

    Preferences.increment(prefName, 9);
    checkIncrement = Preferences.getInteger(prefName);
    assertEquals(prefValue + 10, checkIncrement, "Increment by nine failed");

    Preferences.increment(prefName, 10, 80, false);
    checkIncrement = Preferences.getInteger(prefName);
    assertEquals(80, checkIncrement, "Increment Max 80 failed");

    Preferences.increment(prefName, 3, 10, true);
    checkIncrement = Preferences.getInteger(prefName);
    assertEquals(3, checkIncrement, "Increment mod 10 failed");

    Preferences.setString(prefString, prefName);
    Preferences.decrement(prefString);
    assertEquals(
        "0", Preferences.getString(prefString), "Decrement a string does not set property to 0");
  }

  @Test
  void DecrementPref() {
    String prefName = "anIntegerPref";
    int prefValue = 73;
    String prefString = "aStringPref";

    Preferences.setInteger(prefName, prefValue);
    Preferences.decrement(prefName);
    Integer checkIncrement = Preferences.getInteger(prefName);
    assertEquals(prefValue - 1, checkIncrement, "Decrement by one failed");

    Preferences.decrement(prefName, 9);
    checkIncrement = Preferences.getInteger(prefName);
    assertEquals(prefValue - 10, checkIncrement, "Decrement by nine failed");

    Preferences.decrement(prefName, 10, 80);
    checkIncrement = Preferences.getInteger(prefName);
    assertEquals(80, checkIncrement, "Decrement Max 80 failed");

    Preferences.setString(prefString, prefName);
    Preferences.decrement(prefString);
    assertEquals(
        "0", Preferences.getString(prefString), "Decrement a string does not set property to 0");
  }

  @Test
  void perUserString() {
    String userName = KoLCharacter.getUserName();
    String propName = "saveState";
    String propValue = "Vicuña"; // see me sneak a character set test in here...

    Preferences.setString(userName, propName, propValue);
    String result = Preferences.getString(userName, propName);
    assertEquals(propValue, result, "Could not set and retrieve per-user string pref");
  }

  @Test
  void perUserBoolean() {
    String userName = KoLCharacter.getUserName();
    String propName = "saveSettingsOnSet";

    Preferences.setBoolean(userName, propName, true);
    assertTrue(
        Preferences.getBoolean(userName, propName), "Could not set and retrieve per-user boolean");
  }

  @Test
  void perUserInteger() {
    String userName = KoLCharacter.getUserName();
    String propName = "saveState";
    Integer propValue = 44;

    Preferences.setInteger(userName, propName, propValue);
    assertEquals(
        propValue,
        Preferences.getInteger(userName, propName),
        "Could not set and retrieve per-user Integer");
  }

  @Test
  void perUserLong() {
    String userName = KoLCharacter.getUserName();
    String propName = "saveState";
    Long propValue = 443L;

    Preferences.setLong(userName, propName, propValue);
    assertEquals(
        propValue,
        Preferences.getLong(userName, propName),
        "Could not set and retrieve per-user Long");
  }

  @Test
  void perUserFloat() {
    String userName = KoLCharacter.getUserName();
    String propName = "saveState";
    Float propValue = 443.3f;

    Preferences.setFloat(userName, propName, propValue);
    assertEquals(
        propValue,
        Preferences.getFloat(userName, propName),
        "Could not set and retrieve per-user Float");
  }

  @Test
  void perUserDouble() {
    String userName = KoLCharacter.getUserName();
    String propName = "saveState";
    Double propValue = 443d;

    Preferences.setDouble(userName, propName, propValue);
    assertEquals(
        propValue,
        Preferences.getDouble(userName, propName),
        "Could not set and retrieve per-user Double");
  }

  @Test
  void PrefsMap() {
    String propName = "hpAutoRecoveryTarget";
    String globalProp = "dailyDeedsVersion";
    Integer globalIntProp = 13;
    Integer globalIntValue = 44;
    float userPropFloat = 22.2f;
    Float propDefaultValue = 1.0f;

    TreeMap<String, String> globalMap = Preferences.getMap(true, false);
    TreeMap<String, String> userMap = Preferences.getMap(true, true);

    // test with defaults == true, should return defaults for non-set values.
    assertEquals(
        propDefaultValue,
        Float.valueOf(userMap.get(propName)),
        "User map value not equal to default value");
    assertEquals(
        globalIntProp,
        Integer.valueOf(globalMap.get(globalProp)),
        "Global map value not equal to default value");
    // override values and re-check
    Preferences.setFloat(propName, userPropFloat);
    Preferences.setInteger(globalProp, globalIntValue);
    // get the maps again, because this is a snapshot of a moment-in-time
    globalMap = Preferences.getMap(true, false);
    userMap = Preferences.getMap(true, true);

    // Changing the values shouldn't change the defaults.
    assertEquals(
        propDefaultValue,
        Float.valueOf(userMap.get(propName)),
        "Default map value not equal to default value after setting");
    assertEquals(
        globalIntProp,
        Integer.valueOf(globalMap.get(globalProp)),
        "Global default map value not equal to default value after setting");

    // defaults == false means the these are the actual set values.
    TreeMap<String, String> userDefaultsMap = Preferences.getMap(false, true);
    TreeMap<String, String> globalDefaultsMap = Preferences.getMap(false, false);

    assertEquals(
        globalIntValue,
        Integer.parseInt(globalDefaultsMap.get(globalProp)),
        "Map value not equal to set value");
    assertNotEquals(
        userPropFloat, userDefaultsMap.get(propName), "Map value not equal to set value");
  }

  @Test
  void ResetToDefault() {
    String prefWithDefault = "dailyDeedsVersion";
    Integer prefDefault = 13;

    Integer result = Integer.parseInt(Preferences.getDefault(prefWithDefault));
    assertEquals(prefDefault, result, prefWithDefault + " does not equal " + prefDefault);
    Preferences.increment(prefWithDefault, 22);
    result = Preferences.getInteger(prefWithDefault);
    assertNotEquals(prefDefault, result, prefWithDefault + " should not equal " + prefDefault);
    Preferences.resetToDefault(prefWithDefault);
    result = Preferences.getInteger(prefWithDefault);
    assertEquals(
        prefDefault,
        result,
        "After resetToDefault, " + prefWithDefault + " does not equal " + prefDefault);
  }

  @Test
  void IsDaily() {
    String legacyDaily = "nunsVisits";
    String newStyleDaily = "_SomeDailyThing";
    String notADaily = "somePrefName";
    assertTrue(
        Preferences.isDaily(legacyDaily), legacyDaily + " should be a Daily pref, but isn't");
    assertTrue(
        Preferences.isDaily(newStyleDaily), newStyleDaily + " should be a Daily pref, but isn't");
    assertFalse(Preferences.isDaily(notADaily), notADaily + " should not be a Daily pref, but is");
  }

  @Test
  void ResetDailies() {
    String legacyDaily = "nunsVisits";
    String newStyleDaily = "_SomeDailyThing";
    String notADaily = "somePrefName";
    Integer legacyValue = 1;
    Integer newStyleValue = 2;
    Integer notADailyValue = 3;

    Preferences.setInteger(legacyDaily, legacyValue);
    Preferences.setInteger(newStyleDaily, newStyleValue);
    Preferences.setInteger(notADaily, notADailyValue);
    assertEquals(Preferences.getInteger(legacyDaily), legacyValue, legacyDaily + "value not set");
    assertEquals(
        Preferences.getInteger(newStyleDaily), newStyleValue, newStyleDaily + "value not set");
    assertEquals(Preferences.getInteger(notADaily), notADailyValue, notADaily + "value not set");

    Preferences.resetDailies();
    assertNotEquals(
        Preferences.getInteger(legacyDaily), legacyValue, legacyDaily + "value not reset");
    assertNotEquals(
        Preferences.getInteger(newStyleDaily), newStyleValue, newStyleDaily + "value not reset");
    assertEquals(
        Preferences.getInteger(notADaily), notADailyValue, notADaily + "value unexpectedly reset");
  }

  @Test
  void ResetGlobalDailies() {
    String gloablDailypref = "_testDaily";
    Integer testValue = 2112;
    Integer preTest = Preferences.getInteger("GLOBAL", gloablDailypref);
    assertNotEquals(testValue, preTest, "new pref " + gloablDailypref + " is not empty.");

    Preferences.setInteger("GLOBAL", gloablDailypref, testValue);
    Integer postSetPref = Preferences.getInteger(gloablDailypref);
    assertEquals(testValue, postSetPref, "new pref " + gloablDailypref + " should be set");

    Preferences.resetGlobalDailies();
    Integer afterReset = Preferences.getInteger("GLOBAL", gloablDailypref);
    assertNotEquals(
        testValue,
        afterReset,
        "new pref " + gloablDailypref + " should be reset by resetGlobalDailies");
  }

  @Test
  void ContainsDefault() {
    String prefName = "autoLogin";
    boolean result = Preferences.containsDefault(prefName);
    assertTrue(result, "default not in defaultsSet for pref " + prefName);
  }
}

// Generated with love by TestMe :) Please report issues and submit feature requests at:
// http://weirddev.com/forum#!/testme
