package net.sourceforge.kolmafia.shop;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withAscensions;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSkill;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import internal.helpers.SessionLoggerOutput;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ShopRequestTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("ShopRequest");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("ShopRequest");
  }

  @Test
  void visitingStillDetectsLights() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withClass(AscensionClass.DISCO_BANDIT),
            withSkill("Superhuman Cocktailcrafting"),
            withAscensions(10),
            withProperty("lastGuildStoreOpen", 10));

    try (cleanups) {
      client.addResponse(200, html("request/test_shop_still.html"));
      KoLCharacter.stillsAvailable = -1;
      assertThat(KoLCharacter.getStillsAvailable(), is(10));

      var requests = client.getRequests();
      assertThat(requests, hasSize(1));
      assertPostRequest(requests.get(0), "/shop.php", "whichshop=still");
    }
  }

  @Nested
  class InventoryParsing {
    //   public static final List<ShopRow> parseShopInventory(
    //       final String shopId, final String responseText, boolean force)

    @Test
    void canParseExistingNPCAndCoinmaster() {
      String shopId = "blackmarket";
      String responseText = html("request/test_shop_blackmarket.html");

      SessionLoggerOutput.startStream();
      var shopRows = ShopRequest.parseShopInventory(shopId, responseText, true);
      var text = SessionLoggerOutput.stopStream();

      var expected =
          """
    --------------------
    281	blackmarket	forged identification documents	5,000 Meat
    282	blackmarket	can of black paint	1,000 Meat
    283	blackmarket	black cherry soda	80 Meat
    284	blackmarket	black facepaint	300 Meat
    285	blackmarket	black sheepskin diploma	300 Meat
    286	blackmarket	Black Body&trade; spray	300 Meat
    287	blackmarket	exotic parrot egg	500,000 Meat
    289	blackmarket	Red Zeppelin ticket	5,000 Meat
    290	blackmarket	Red Zeppelin ticket	priceless diamond
    --------------------
    The Black Market	blackmarket	black cherry soda	80	ROW283
    The Black Market	blackmarket	Black Body&trade; spray	300	ROW286
    The Black Market	blackmarket	black facepaint	300	ROW284
    The Black Market	blackmarket	black sheepskin diploma	300	ROW285
    The Black Market	blackmarket	can of black paint	1000	ROW282
    The Black Market	blackmarket	forged identification documents	5000	ROW281
    The Black Market	blackmarket	exotic parrot egg	500000	ROW287
    The Black Market	blackmarket	Red Zeppelin ticket	5000	ROW289
    --------------------
    The Black Market	buy	1	Red Zeppelin ticket	ROW290
    --------------------""";

      assertThat(text, containsString(expected));
    }

    @Test
    void canParseExistingConcoction() {
      String shopId = "starchart";
      String responseText = html("request/test_shop_starchart.html");

      SessionLoggerOutput.startStream();
      var shopRows = ShopRequest.parseShopInventory(shopId, responseText, true);
      var text = SessionLoggerOutput.stopStream();

      var expected =
          """
    --------------------
    133	starchart	star sword	star (7)	line (4)	star chart
    134	starchart	star crossbow	star (5)	line (6)	star chart
    135	starchart	star staff	star (6)	line (5)	star chart
    136	starchart	star pants	star (7)	line (7)	star chart
    137	starchart	star hat	star (5)	line (3)	star chart
    138	starchart	star buckler	star (4)	line (6)	star chart
    139	starchart	star throwing star	star (4)	line (2)	star chart
    140	starchart	star starfish	star (6)	line (4)	star chart
    141	starchart	Richard's star key	star (8)	line (7)	star chart
    142	starchart	star shirt	star (15)	line (15)	star chart
    143	starchart	star stiletto	star (5)	line (4)	star chart
    144	starchart	star boomerang	star (4)	line (5)	star chart
    145	starchart	star spatula	star (5)	line (5)	star chart
    --------------------
    A Star Chart	ROW141	Richard's star key	star (8)	line (7)	star chart
    A Star Chart	ROW138	star buckler	star (4)	line (6)	star chart
    A Star Chart	ROW137	star hat	star (5)	line (3)	star chart
    A Star Chart	ROW136	star pants	star (7)	line (7)	star chart
    A Star Chart	ROW140	star starfish	star (6)	line (4)	star chart
    A Star Chart	ROW145	star spatula	star (5)	line (5)	star chart
    A Star Chart	ROW144	star boomerang	star (4)	line (5)	star chart
    A Star Chart	ROW143	star stiletto	star (5)	line (4)	star chart
    A Star Chart	ROW134	star crossbow	star (5)	line (6)	star chart
    A Star Chart	ROW135	star staff	star (6)	line (5)	star chart
    A Star Chart	ROW133	star sword	star (7)	line (4)	star chart
    A Star Chart	ROW139	star throwing star	star (4)	line (2)	star chart
    A Star Chart	ROW142	star shirt	star (15)	line (15)	star chart
    --------------------""";

      assertThat(text, containsString(expected));
    }

    @Test
    void canParseExistingTokenCoinmaster() {
      String shopId = "september";
      String responseText = html("request/test_shop_september.html");

      SessionLoggerOutput.startStream();
      var shopRows = ShopRequest.parseShopInventory(shopId, responseText, true);
      var text = SessionLoggerOutput.stopStream();

      var expected =
          """
    --------------------
    1510	september	blade of dismemberment	Ember
    1511	september	miniature Embering Hulk	Ember (6)
    1512	september	Mmm-brr! brand mouthwash	Ember (2)
    1513	september	Septapus summoning charm	Ember (2)
    1514	september	structural ember	Ember (4)
    1515	september	embers-only jacket	Ember
    1516	september	bembershoot	Ember
    1517	september	wheel of camembert	Ember
    1518	september	hat of remembering	Ember
    1519	september	throwin' ember	Ember (2)
    1520	september	head of emberg lettuce	Ember (2)
    --------------------
    Sept-Ember Censer	buy	1	bembershoot	ROW1516
    Sept-Ember Censer	buy	1	blade of dismemberment	ROW1510
    Sept-Ember Censer	buy	1	embers-only jacket	ROW1515
    Sept-Ember Censer	buy	1	hat of remembering	ROW1518
    Sept-Ember Censer	buy	1	wheel of camembert	ROW1517
    Sept-Ember Censer	buy	2	Mmm-brr! brand mouthwash	ROW1512
    Sept-Ember Censer	buy	2	head of emberg lettuce	ROW1520
    Sept-Ember Censer	buy	2	Septapus summoning charm	ROW1513
    Sept-Ember Censer	buy	2	throwin' ember	ROW1519
    Sept-Ember Censer	buy	4	structural ember	ROW1514
    Sept-Ember Censer	buy	6	miniature Embering Hulk	ROW1511
    --------------------""";

      assertThat(text, containsString(expected));
    }

    @Test
    void canParseExistingBuySellCoinmaster() {
      String shopId = "crimbo23_elf_armory";
      String responseText = html("request/test_armory_elf_visit.html");

      SessionLoggerOutput.startStream();
      var shopRows = ShopRequest.parseShopInventory(shopId, responseText, true);
      var text = SessionLoggerOutput.stopStream();

      var expected =
          """
    --------------------
    1411	crimbo23_elf_armory	Elf Guard honor present	Elf Army machine parts (200)
    1412	crimbo23_elf_armory	Elf Army machine parts (3)	Elf Guard commandeering gloves
    1413	crimbo23_elf_armory	Elf Army machine parts (3)	Elf Guard officer's sidearm
    1415	crimbo23_elf_armory	Elf Army machine parts (3)	Kelflar vest
    1416	crimbo23_elf_armory	Elf Army machine parts (3)	Elf Guard mouthknife
    --------------------
    Elf Guard Armory	buy	200	Elf Guard honor present	ROW1411
    --------------------
    Elf Guard Armory	sell	3	Elf Guard commandeering gloves	ROW1412
    Elf Guard Armory	sell	3	Kelflar vest	ROW1415
    Elf Guard Armory	sell	3	Elf Guard mouthknife	ROW1416
    Elf Guard Armory	sell	3	Elf Guard officer's sidearm	ROW1413
    --------------------""";

      assertThat(text, containsString(expected));
    }

    @Test
    void canParseExistingSkillCoinmaster() {
      String shopId = "mutate";
      String responseText = html("request/test_shop_mutate_visit.html");

      SessionLoggerOutput.startStream();
      var shopRows = ShopRequest.parseShopInventory(shopId, responseText, true);
      var text = SessionLoggerOutput.stopStream();

      var expected =
          """
    --------------------
    852	mutate	Boiling Tear Ducts	rad (30)
    853	mutate	Throat Refrigerant	rad (30)
    854	mutate	Skunk Glands	rad (30)
    855	mutate	Translucent Skin	rad (30)
    856	mutate	Projectile Salivary Glands	rad (30)
    857	mutate	Mind Bullets	rad (60)
    859	mutate	Metallic Skin	rad (90)
    860	mutate	Adipose Polymers	rad (90)
    861	mutate	Extra Muscles	rad (90)
    862	mutate	Extra Brain	rad (90)
    863	mutate	Hypno-Eyes	rad (90)
    864	mutate	Backwards Knees	rad (120)
    865	mutate	Sucker Fingers	rad (120)
    867	mutate	Flappy Ears	rad (60)
    868	mutate	Magic Sweat	rad (60)
    869	mutate	Steroid Bladder	rad (60)
    870	mutate	Intracranial Eye	rad (60)
    871	mutate	Self-Combing Hair	rad (60)
    872	mutate	Bone Springs	rad (90)
    873	mutate	Magnetic Ears	rad (90)
    874	mutate	Firefly Abdomen	rad (90)
    875	mutate	Squid Glands	rad (90)
    876	mutate	Extremely Punchable Face	rad (90)
    877	mutate	Extra Gall Bladder	rad (60)
    878	mutate	Extra Kidney	rad (60)
    879	mutate	Internal Soda Machine	rad (30)
    --------------------
    Genetic Fiddling	ROW853	Throat Refrigerant	rad (30)
    Genetic Fiddling	ROW854	Skunk Glands	rad (30)
    Genetic Fiddling	ROW855	Translucent Skin	rad (30)
    Genetic Fiddling	ROW856	Projectile Salivary Glands	rad (30)
    Genetic Fiddling	ROW852	Boiling Tear Ducts	rad (30)
    Genetic Fiddling	ROW869	Steroid Bladder	rad (60)
    Genetic Fiddling	ROW861	Extra Muscles	rad (90)
    Genetic Fiddling	ROW873	Magnetic Ears	rad (90)
    Genetic Fiddling	ROW865	Sucker Fingers	rad (120)
    Genetic Fiddling	ROW867	Flappy Ears	rad (60)
    Genetic Fiddling	ROW868	Magic Sweat	rad (60)
    Genetic Fiddling	ROW860	Adipose Polymers	rad (90)
    Genetic Fiddling	ROW859	Metallic Skin	rad (90)
    Genetic Fiddling	ROW863	Hypno-Eyes	rad (90)
    Genetic Fiddling	ROW871	Self-Combing Hair	rad (60)
    Genetic Fiddling	ROW876	Extremely Punchable Face	rad (90)
    Genetic Fiddling	ROW862	Extra Brain	rad (90)
    Genetic Fiddling	ROW870	Intracranial Eye	rad (60)
    Genetic Fiddling	ROW857	Mind Bullets	rad (60)
    Genetic Fiddling	ROW872	Bone Springs	rad (90)
    Genetic Fiddling	ROW874	Firefly Abdomen	rad (90)
    Genetic Fiddling	ROW875	Squid Glands	rad (90)
    Genetic Fiddling	ROW864	Backwards Knees	rad (120)
    Genetic Fiddling	ROW879	Internal Soda Machine	rad (30)
    Genetic Fiddling	ROW877	Extra Gall Bladder	rad (60)
    Genetic Fiddling	ROW878	Extra Kidney	rad (60)
    --------------------""";

      assertThat(text, containsString(expected));
    }

    @Test
    void canParseExistingShopRowCoinmaster() {
      String shopId = "twitchsoup";
      String responseText = html("request/test_shop_twitchsoup.html");

      SessionLoggerOutput.startStream();
      var shopRows = ShopRequest.parseShopInventory(shopId, responseText, true);
      var text = SessionLoggerOutput.stopStream();

      var expected =
          """
    --------------------
    1491	twitchsoup	bacteria bisque	Chroner (10)
    1492	twitchsoup	ciliophora chowder	Chroner (10)
    1493	twitchsoup	cream of chloroplasts	Chroner (10)
    1494	twitchsoup	lip soup	cream of chloroplasts	protogenetic chunklet (lips)
    1495	twitchsoup	lip soup	ciliophora chowder	protogenetic chunklet (lips)
    1496	twitchsoup	lip soup	bacteria bisque	protogenetic chunklet (lips)
    1497	twitchsoup	elbow soup	cream of chloroplasts	protogenetic chunklet (elbow)
    1498	twitchsoup	elbow soup	ciliophora chowder	protogenetic chunklet (elbow)
    1499	twitchsoup	elbow soup	bacteria bisque	protogenetic chunklet (elbow)
    1500	twitchsoup	flagellate soup	ciliophora chowder	protogenetic chunklet (flagellum)
    1501	twitchsoup	flagellate soup	cream of chloroplasts	protogenetic chunklet (flagellum)
    1502	twitchsoup	flagellate soup	bacteria bisque	protogenetic chunklet (flagellum)
    1503	twitchsoup	muscular soup	cream of chloroplasts	protogenetic chunklet (muscle)
    1504	twitchsoup	muscular soup	ciliophora chowder	protogenetic chunklet (muscle)
    1505	twitchsoup	muscular soup	bacteria bisque	protogenetic chunklet (muscle)
    1506	twitchsoup	synaptic soup	cream of chloroplasts	protogenetic chunklet (synapse)
    1507	twitchsoup	synaptic soup	ciliophora chowder	protogenetic chunklet (synapse)
    1508	twitchsoup	synaptic soup	bacteria bisque	protogenetic chunklet (synapse)
    --------------------
    The Primordial Soup Kitchen	ROW1491	bacteria bisque	Chroner (10)
    The Primordial Soup Kitchen	ROW1492	ciliophora chowder	Chroner (10)
    The Primordial Soup Kitchen	ROW1493	cream of chloroplasts	Chroner (10)
    The Primordial Soup Kitchen	ROW1497	elbow soup	cream of chloroplasts	protogenetic chunklet (elbow)
    The Primordial Soup Kitchen	ROW1498	elbow soup	ciliophora chowder	protogenetic chunklet (elbow)
    The Primordial Soup Kitchen	ROW1499	elbow soup	bacteria bisque	protogenetic chunklet (elbow)
    The Primordial Soup Kitchen	ROW1500	flagellate soup	ciliophora chowder	protogenetic chunklet (flagellum)
    The Primordial Soup Kitchen	ROW1502	flagellate soup	bacteria bisque	protogenetic chunklet (flagellum)
    The Primordial Soup Kitchen	ROW1501	flagellate soup	cream of chloroplasts	protogenetic chunklet (flagellum)
    The Primordial Soup Kitchen	ROW1496	lip soup	bacteria bisque	protogenetic chunklet (lips)
    The Primordial Soup Kitchen	ROW1495	lip soup	ciliophora chowder	protogenetic chunklet (lips)
    The Primordial Soup Kitchen	ROW1494	lip soup	cream of chloroplasts	protogenetic chunklet (lips)
    The Primordial Soup Kitchen	ROW1503	muscular soup	cream of chloroplasts	protogenetic chunklet (muscle)
    The Primordial Soup Kitchen	ROW1504	muscular soup	ciliophora chowder	protogenetic chunklet (muscle)
    The Primordial Soup Kitchen	ROW1505	muscular soup	bacteria bisque	protogenetic chunklet (muscle)
    The Primordial Soup Kitchen	ROW1506	synaptic soup	cream of chloroplasts	protogenetic chunklet (synapse)
    The Primordial Soup Kitchen	ROW1507	synaptic soup	ciliophora chowder	protogenetic chunklet (synapse)
    The Primordial Soup Kitchen	ROW1508	synaptic soup	bacteria bisque	protogenetic chunklet (synapse)
    --------------------""";

      assertThat(text, containsString(expected));
    }

    @Test
    void canParseNewShopWithMysteryBuyables() {
      String shopId = "dinobone";
      String responseText = html("request/test_shop_dinobone_mystery.html");

      SessionLoggerOutput.startStream();
      var shopRows = ShopRequest.parseShopInventory(shopId, responseText, true);
      var text = SessionLoggerOutput.stopStream();

      var expected =
          """
        --------------------
        1713	dinobone	null	dinosaur bone fragment (5)
        1714	dinobone	null	dinosaur bone fragment (10)
        1715	dinobone	null	dinosaur bone fragment (25)
        1716	dinobone	null	dinosaur bone fragment (50)
        1717	dinobone	null	dinosaur bone fragment (100)
        --------------------
        Dino Bone Fragment Assembly	ROW1713	null	dinosaur bone fragment (5)
        Dino Bone Fragment Assembly	ROW1714	null	dinosaur bone fragment (10)
        Dino Bone Fragment Assembly	ROW1715	null	dinosaur bone fragment (25)
        Dino Bone Fragment Assembly	ROW1716	null	dinosaur bone fragment (50)
        Dino Bone Fragment Assembly	ROW1717	null	dinosaur bone fragment (100)
        --------------------""";

      assertThat(text, containsString(expected));
    }
  }
}
