package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.xml.parsers.ParserConfigurationException;
import net.sourceforge.kolmafia.request.CharSheetRequest.ParsedSkillInfo;
import net.sourceforge.kolmafia.request.CharSheetRequest.ParsedSkillInfo.PermStatus;
import net.sourceforge.kolmafia.utilities.HTMLParserUtils;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

public class CharSheetRequestTest {
  @Test
  public void parseSkills() throws IOException, ParserConfigurationException {
    byte[] fileData = Files.readAllBytes(Paths.get("request/test_charsheet.html"));
    String html = new String(fileData, StandardCharsets.UTF_8);

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
}
