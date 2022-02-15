package net.sourceforge.kolmafia.request;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.xml.parsers.ParserConfigurationException;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.request.CharSheetRequest.ParsedSkillInfo;
import net.sourceforge.kolmafia.request.CharSheetRequest.ParsedSkillInfo.PermStatus;
import net.sourceforge.kolmafia.utilities.HTMLParserUtils;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.w3c.dom.Document;

public class CharSheetRequestTest {
  @Test
  public void parseSkills() throws IOException, ParserConfigurationException {
    String html = Files.readString(Paths.get("request/test_charsheet_normal.html"));

    HtmlCleaner cleaner = HTMLParserUtils.configureDefaultParser();
    DomSerializer domSerializer = new DomSerializer(cleaner.getProperties());
    Document doc = domSerializer.createDOM(cleaner.clean(html));

    ParsedSkillInfo[] skillInfos =
        CharSheetRequest.parseSkills(doc).toArray(new ParsedSkillInfo[0]);

    ParsedSkillInfo[] expected = {
      new ParsedSkillInfo(5, "Stomach of Steel", PermStatus.NONE),
      new ParsedSkillInfo(10, "Powers of Observatiogn", PermStatus.HARDCORE),
      new ParsedSkillInfo(11, "Gnefarious Pickpocketing", PermStatus.HARDCORE),
      new ParsedSkillInfo(12, "Torso Awaregness", PermStatus.HARDCORE),
      new ParsedSkillInfo(13, "Gnomish Hardigness", PermStatus.HARDCORE),
      new ParsedSkillInfo(14, "Cosmic Ugnderstanding", PermStatus.HARDCORE),
      new ParsedSkillInfo(15, "CLEESH", PermStatus.NONE),
      new ParsedSkillInfo(45, "Vent Rage Gland", PermStatus.HARDCORE),
      new ParsedSkillInfo(54, "Unaccompanied Miner", PermStatus.HARDCORE),
      new ParsedSkillInfo(82, "Request Sandwich", PermStatus.HARDCORE),
      new ParsedSkillInfo(116, "Hollow Leg", PermStatus.NONE),
      new ParsedSkillInfo(1005, "Lunging Thrust-Smack", PermStatus.HARDCORE),
      new ParsedSkillInfo(1006, "Super-Advanced Meatsmithing", PermStatus.HARDCORE),
      new ParsedSkillInfo(1007, "Blubber Up", PermStatus.HARDCORE),
      new ParsedSkillInfo(1011, "Hide of the Walrus", PermStatus.HARDCORE),
      new ParsedSkillInfo(1014, "Batter Up!", PermStatus.HARDCORE),
      new ParsedSkillInfo(1015, "Rage of the Reindeer", PermStatus.HARDCORE),
      new ParsedSkillInfo(1016, "Pulverize", PermStatus.HARDCORE),
      new ParsedSkillInfo(1018, "Northern Exposure", PermStatus.HARDCORE),
      new ParsedSkillInfo(2011, "Wisdom of the Elder Tortoises", PermStatus.HARDCORE),
      new ParsedSkillInfo(2014, "Amphibian Sympathy", PermStatus.HARDCORE),
      new ParsedSkillInfo(2020, "Hero of the Half-Shell", PermStatus.HARDCORE),
      new ParsedSkillInfo(2021, "Tao of the Terrapin", PermStatus.HARDCORE),
      new ParsedSkillInfo(3004, "Entangling Noodles", PermStatus.SOFTCORE),
      new ParsedSkillInfo(3005, "Cannelloni Cannon", PermStatus.HARDCORE),
      new ParsedSkillInfo(3006, "Pastamastery", PermStatus.HARDCORE),
      new ParsedSkillInfo(3010, "Leash of Linguini", PermStatus.HARDCORE),
      new ParsedSkillInfo(3012, "Cannelloni Cocoon", PermStatus.HARDCORE),
      new ParsedSkillInfo(3014, "Spirit of Ravioli", PermStatus.HARDCORE),
      new ParsedSkillInfo(3015, "Springy Fusilli", PermStatus.HARDCORE),
      new ParsedSkillInfo(3017, "Flavour of Magic", PermStatus.HARDCORE),
      new ParsedSkillInfo(3029, "Bind Vermincelli", PermStatus.HARDCORE),
      new ParsedSkillInfo(4000, "Sauce Contemplation", PermStatus.NONE),
      new ParsedSkillInfo(4003, "Stream of Sauce", PermStatus.NONE),
      new ParsedSkillInfo(4005, "Saucestorm", PermStatus.HARDCORE),
      new ParsedSkillInfo(4006, "Advanced Saucecrafting", PermStatus.HARDCORE),
      new ParsedSkillInfo(4010, "Intrinsic Spiciness", PermStatus.NONE),
      new ParsedSkillInfo(4011, "Master Saucier", PermStatus.NONE),
      new ParsedSkillInfo(4015, "Impetuous Sauciness", PermStatus.HARDCORE),
      new ParsedSkillInfo(4017, "Irrepressible Spunk", PermStatus.NONE),
      new ParsedSkillInfo(4018, "The Way of Sauce", PermStatus.HARDCORE),
      new ParsedSkillInfo(4020, "Salsaball", PermStatus.NONE),
      new ParsedSkillInfo(4027, "Soul Saucery", PermStatus.NONE),
      new ParsedSkillInfo(4028, "Inner Sauce", PermStatus.HARDCORE),
      new ParsedSkillInfo(4030, "Itchy Curse Finger", PermStatus.NONE),
      new ParsedSkillInfo(4033, "Antibiotic Saucesphere", PermStatus.NONE),
      new ParsedSkillInfo(4034, "Curse of Weaksauce", PermStatus.NONE),
      new ParsedSkillInfo(4039, "Saucemaven", PermStatus.HARDCORE),
      new ParsedSkillInfo(5004, "Nimble Fingers", PermStatus.HARDCORE),
      new ParsedSkillInfo(5006, "Mad Looting Skillz", PermStatus.HARDCORE),
      new ParsedSkillInfo(5007, "Disco Nap", PermStatus.HARDCORE),
      new ParsedSkillInfo(5009, "Disco Fever", PermStatus.HARDCORE),
      new ParsedSkillInfo(5010, "Overdeveloped Sense of Self Preservation", PermStatus.HARDCORE),
      new ParsedSkillInfo(5011, "Adventurer of Leisure", PermStatus.HARDCORE),
      new ParsedSkillInfo(5014, "Advanced Cocktailcrafting", PermStatus.HARDCORE),
      new ParsedSkillInfo(5015, "Ambidextrous Funkslinging", PermStatus.HARDCORE),
      new ParsedSkillInfo(5016, "Heart of Polyester", PermStatus.HARDCORE),
      new ParsedSkillInfo(5017, "Smooth Movement", PermStatus.HARDCORE),
      new ParsedSkillInfo(5018, "Superhuman Cocktailcrafting", PermStatus.HARDCORE),
      new ParsedSkillInfo(6004, "The Moxious Madrigal", PermStatus.HARDCORE),
      new ParsedSkillInfo(6007, "The Magical Mojomuscular Melody", PermStatus.HARDCORE),
      new ParsedSkillInfo(6008, "The Power Ballad of the Arrowsmith", PermStatus.HARDCORE),
      new ParsedSkillInfo(6010, "Fat Leon's Phat Loot Lyric", PermStatus.HARDCORE),
      new ParsedSkillInfo(6014, "The Ode to Booze", PermStatus.HARDCORE),
      new ParsedSkillInfo(6015, "The Sonata of Sneakiness", PermStatus.HARDCORE),
      new ParsedSkillInfo(6016, "Carlweather's Cantata of Confrontation", PermStatus.HARDCORE),
      new ParsedSkillInfo(6017, "Ur-Kel's Aria of Annoyance", PermStatus.HARDCORE),
      new ParsedSkillInfo(6027, "Cringle's Curative Carol", PermStatus.HARDCORE),
      new ParsedSkillInfo(6035, "Five Finger Discount", PermStatus.HARDCORE),
      new ParsedSkillInfo(6038, "Thief Among the Honorable", PermStatus.HARDCORE),
      new ParsedSkillInfo(6043, "Mariachi Memory", PermStatus.HARDCORE),
      new ParsedSkillInfo(6045, "Paul's Passionate Pop Song", PermStatus.HARDCORE),
      new ParsedSkillInfo(19, "Transcendent Olfaction", PermStatus.HARDCORE),
    };

    assertArrayEquals(expected, skillInfos);
  }

  @ParameterizedTest
  @CsvSource({"normal, 123", "unbuffed_stats, 1199739", "grey_you, 2395753"})
  public void parsePlayerId(String page, String expected) throws IOException {
    String html = Files.readString(Paths.get("request/test_charsheet_" + page + ".html"));
    CharSheetRequest.parseStatus(html);

    assertThat(KoLCharacter.getPlayerId(), equalTo(expected));
  }

  @ParameterizedTest
  @CsvSource({"normal, 394, 394, 394", "unbuffed_stats, 162, 243, 243", "grey_you, 25, 25, 25"})
  public void parseHP(
      String page, String expectedCurrentHP, String expectedMaxHP, String expectedBaseMaxHP)
      throws IOException {
    String html = Files.readString(Paths.get("request/test_charsheet_" + page + ".html"));
    CharSheetRequest.parseStatus(html);

    assertThat(KoLCharacter.getCurrentHP(), equalTo(Long.parseLong(expectedCurrentHP)));
    assertThat(KoLCharacter.getMaximumHP(), equalTo(Long.parseLong(expectedMaxHP)));
    assertThat(KoLCharacter.getBaseMaxHP(), equalTo(Long.parseLong(expectedBaseMaxHP)));
  }

  @ParameterizedTest
  @CsvSource({"normal, 359, 1221, 1221", "unbuffed_stats, 225, 225, 225", "grey_you, 5, 5, 5"})
  public void parseMP(
      String page, String expectedCurrentMP, String expectedMaxMP, String expectedBaseMaxMP)
      throws IOException {
    String html = Files.readString(Paths.get("request/test_charsheet_" + page + ".html"));
    CharSheetRequest.parseStatus(html);

    assertThat(KoLCharacter.getCurrentMP(), equalTo(Long.parseLong(expectedCurrentMP)));
    assertThat(KoLCharacter.getMaximumMP(), equalTo(Long.parseLong(expectedMaxMP)));
    assertThat(KoLCharacter.getBaseMaxMP(), equalTo(Long.parseLong(expectedBaseMaxMP)));
  }

  @Test
  public void parseUnbuffedStats() throws IOException {
    String html = Files.readString(Paths.get("request/test_charsheet_unbuffed_stats.html"));
    CharSheetRequest.parseStatus(html);

    assertThat(KoLCharacter.getTotalMuscle(), equalTo(25313L));
    assertThat(KoLCharacter.getAdjustedMuscle(), equalTo(159));
    assertThat(KoLCharacter.getTotalMysticality(), equalTo(50625L));
    assertThat(KoLCharacter.getAdjustedMysticality(), equalTo(225));
    assertThat(KoLCharacter.getTotalMoxie(), equalTo(25313L));
    assertThat(KoLCharacter.getAdjustedMoxie(), equalTo(159));
  }

  @Test
  public void parseBuffedStats() throws IOException {
    String html = Files.readString(Paths.get("request/test_charsheet_normal.html"));
    CharSheetRequest.parseStatus(html);

    assertThat(KoLCharacter.getTotalMuscle(), equalTo(29492L));
    assertThat(KoLCharacter.getAdjustedMuscle(), equalTo(229));
    assertThat(KoLCharacter.getTotalMysticality(), equalTo(205776L));
    assertThat(KoLCharacter.getAdjustedMysticality(), equalTo(581));
    assertThat(KoLCharacter.getTotalMoxie(), equalTo(25158L));
    assertThat(KoLCharacter.getAdjustedMoxie(), equalTo(164));
  }

  @Test
  public void parseGreyYouStats() throws IOException {
    String html = Files.readString(Paths.get("request/test_charsheet_grey_you.html"));
    CharSheetRequest.parseStatus(html);

    assertThat(KoLCharacter.getTotalMuscle(), equalTo(9L));
    assertThat(KoLCharacter.getAdjustedMuscle(), equalTo(3));
    assertThat(KoLCharacter.getTotalMysticality(), equalTo(9L));
    assertThat(KoLCharacter.getAdjustedMysticality(), equalTo(3));
    assertThat(KoLCharacter.getTotalMoxie(), equalTo(9L));
    assertThat(KoLCharacter.getAdjustedMoxie(), equalTo(3));
  }

  @ParameterizedTest
  @CsvSource({"normal, 44", "unbuffed_stats, 494", "grey_you, 20"})
  public void parseAscensions(String page, String expected) throws IOException {
    String html = Files.readString(Paths.get("request/test_charsheet_" + page + ".html"));
    CharSheetRequest.parseStatus(html);

    assertThat(KoLCharacter.getAscensions(), equalTo(Integer.parseInt(expected)));
  }

  @ParameterizedTest
  @CsvSource({"normal, 8621947", "unbuffed_stats, 593", "grey_you, 0"})
  public void parseAvailableMeat(String page, String expected) throws IOException {
    String html = Files.readString(Paths.get("request/test_charsheet_" + page + ".html"));
    CharSheetRequest.parseStatus(html);

    assertThat(KoLCharacter.getAvailableMeat(), equalTo(Long.parseLong(expected)));
  }

  @ParameterizedTest
  @CsvSource({"normal, 2979", "unbuffed_stats, 30", "grey_you, 0"})
  public void parseCurrentRun(String page, String expected) throws IOException {
    String html = Files.readString(Paths.get("request/test_charsheet_" + page + ".html"));
    CharSheetRequest.parseStatus(html);

    assertThat(KoLCharacter.getCurrentRun(), equalTo(Integer.parseInt(expected)));
  }

  @ParameterizedTest
  @CsvSource({"normal, 14", "unbuffed_stats, 5", "grey_you, 1"})
  public void parseCurrentDays(String page, String expected) throws IOException {
    String html = Files.readString(Paths.get("request/test_charsheet_" + page + ".html"));
    CharSheetRequest.parseStatus(html);

    assertThat(KoLCharacter.getCurrentDays(), equalTo(Integer.parseInt(expected)));
  }

  @ParameterizedTest
  @CsvSource({"normal, Mongoose", "unbuffed_stats, Mongoose", "grey_you, Vole"})
  public void parseSign(String page, String expected) throws IOException {
    String html = Files.readString(Paths.get("request/test_charsheet_" + page + ".html"));
    CharSheetRequest.parseStatus(html);

    assertThat(KoLCharacter.getSign(), equalTo(expected));
  }

  @ParameterizedTest
  @CsvSource({"normal, false", "unbuffed_stats, false", "grey_you, false"})
  public void parseHardcore(String page, String expected) throws IOException {
    String html = Files.readString(Paths.get("request/test_charsheet_" + page + ".html"));
    CharSheetRequest.parseStatus(html);

    assertThat(KoLCharacter.isHardcore(), equalTo(Boolean.parseBoolean(expected)));
  }

  @ParameterizedTest
  @CsvSource({"normal, 0", "unbuffed_stats, 970", "grey_you, 1000"})
  public void parseRonin(String page, String expected) throws IOException {
    String html = Files.readString(Paths.get("request/test_charsheet_" + page + ".html"));
    CharSheetRequest.parseStatus(html);

    assertThat(KoLCharacter.roninLeft(), equalTo(Integer.parseInt(expected)));
  }

  @Test
  public void unascendedCharacterHasNoPathOrSign() throws IOException {
    String html = Files.readString(Paths.get("request/test_charsheet_unascended.html"));
    CharSheetRequest.parseStatus(html);

    assertThat(KoLCharacter.getAscensions(), equalTo(0));
    assertThat(KoLCharacter.getSign(), equalTo(ZodiacSign.NONE.toString()));
  }

  @Test
  public void parsesDrunkenness() throws IOException {
    String html = Files.readString(Paths.get("request/test_charsheet_unascended.html"));
    CharSheetRequest.parseStatus(html);

    assertThat(KoLCharacter.getInebriety(), equalTo(3));
  }
}
