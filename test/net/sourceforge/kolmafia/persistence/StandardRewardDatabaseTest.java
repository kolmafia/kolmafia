package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Networking.html;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.SessionLoggerOutput;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.shop.ShopRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class StandardRewardDatabaseTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("StandardRewardDatabaseTest");
  }

  @BeforeEach
  void beforeEach() {
    Preferences.reset("StandardRewardDatabaseTest");
  }

  @Nested
  class ArmoryAndLeggery {
    // The Armory & Leggery will trade a pulverized standard reward for
    // a previous year's standard reward.
    //
    // Therefore, 12 new rows are added each new year:
    //
    //   6 for Normal standard rewards
    //   6 for Hardcore standard rewards
    //
    // Our protocol when a new standard season starts:
    //
    // 1) Add the pulverized Normal and Hardcore rewards to standard-pulverized.txt
    // 2) Add the 12 new rewards to standard-rewards.txt with ROW = UNKNOWN
    // 3) Update the previous year's UNKNOWN rows with what Armory & Leggery says

    @Test
    void canLogStandardRequestRows() {
      SessionLoggerOutput.startStream();
      String responseText = html("request/test_armorer_2024.html");

      // Parse a responseText from visiting the Armory & Leggery.
      // This one has the 2023 and 2024 standard rewards available.
      // For each year
      //
      //   2 pulverized currencies
      //   6 Normal and 6 Hardcore rewards
      //
      // Our data files contain all of those.  Normally, we'd log only
      // new rows, but we can "force" everything to be logged.

      ShopRequest.parseShopInventory("armory", responseText, true);
      var text = SessionLoggerOutput.stopStream();

      // Lines that go into standard_pulverized.txt

      var expectedPulverized =
          """
    --------------------
    11034	2023	norm	chiffon carbage
    11026	2023	hard	ceramic scree
    11510	2024	norm	moss mulch
    11518	2024	hard	adobe assortment
    --------------------""";
      assertThat(text, containsString(expectedPulverized));

      // Lines that go into standard_rewards.txt

      var expectedRewards =
          """
    --------------------
    10130	2022	norm	SC	1446	loofah lumberjack's hat
    10131	2022	norm	TT	1447	loofah lei
    10132	2022	norm	PA	1297	loofah lederhosen
    10133	2022	norm	SA	1298	loofah ladle
    10134	2022	norm	DB	1299	loofah legwarmers
    10135	2022	norm	AT	1300	loofah lavalier
    10138	2022	hard	SC	1301	flagstone flag
    10139	2022	hard	TT	1302	flagstone flail
    10140	2022	hard	PA	1303	flagstone flip-flops
    10141	2022	hard	SA	1304	flagstone fez
    10142	2022	hard	DB	1305	flagstone fleece
    10143	2022	hard	AT	1306	flagstone fringe
    11028	2023	norm	SC	1296	chiffon chevrons
    11029	2023	norm	TT	1295	chiffon chapeau
    11030	2023	norm	PA	1442	chiffon chamberpot
    11031	2023	norm	SA	1443	chiffon chemise
    11032	2023	norm	DB	1444	chiffon chakram
    11033	2023	norm	AT	1445	chiffon chaps
    11020	2023	hard	SC	1448	ceramic cestus
    11021	2023	hard	TT	1449	ceramic centurion shield
    11022	2023	hard	PA	1450	ceramic celery grater
    11023	2023	hard	SA	1451	ceramic celsiturometer
    11024	2023	hard	DB	1452	ceramic cerecloth belt
    11025	2023	hard	AT	1453	ceramic cenobite's robe
    --------------------""";
      assertThat(text, containsString(expectedRewards));

      // Lines that go into shoprows.txt:

      var expectedShopRows =
          """
    1295	armory	chiffon chapeau	moss mulch
    1296	armory	chiffon chevrons	moss mulch
    1297	armory	loofah lederhosen	chiffon carbage
    1298	armory	loofah ladle	chiffon carbage
    1299	armory	loofah legwarmers	chiffon carbage
    1300	armory	loofah lavalier	chiffon carbage
    1301	armory	flagstone flag	ceramic scree
    1302	armory	flagstone flail	ceramic scree
    1303	armory	flagstone flip-flops	ceramic scree
    1304	armory	flagstone fez	ceramic scree
    1305	armory	flagstone fleece	ceramic scree
    1306	armory	flagstone fringe	ceramic scree
    1442	armory	chiffon chamberpot	moss mulch
    1443	armory	chiffon chemise	moss mulch
    1444	armory	chiffon chakram	moss mulch
    1445	armory	chiffon chaps	moss mulch
    1446	armory	loofah lumberjack's hat	chiffon carbage
    1447	armory	loofah lei	chiffon carbage
    1448	armory	ceramic cestus	adobe assortment
    1449	armory	ceramic centurion shield	adobe assortment
    1450	armory	ceramic celery grater	adobe assortment
    1451	armory	ceramic celsiturometer	adobe assortment
    1452	armory	ceramic cerecloth belt	adobe assortment
    1453	armory	ceramic cenobite's robe	adobe assortment""";
      assertThat(text, containsString(expectedShopRows));

      // Lines that would normally go into coinmasters.txt, since
      // ShopRequest knows that ArmoryAndLeggeryRequest is a coinmaster
      // that uses ShopRows.  It doesn't know that that coinmaster
      // constructs those rows using StandardRewardDatabase's data files.

      var expectedCoinmasterRows =
          """
    --------------------
    Armory and Leggery	ROW1446	loofah lumberjack's hat	chiffon carbage
    Armory and Leggery	ROW1447	loofah lei	chiffon carbage
    Armory and Leggery	ROW1297	loofah lederhosen	chiffon carbage
    Armory and Leggery	ROW1298	loofah ladle	chiffon carbage
    Armory and Leggery	ROW1299	loofah legwarmers	chiffon carbage
    Armory and Leggery	ROW1300	loofah lavalier	chiffon carbage
    Armory and Leggery	ROW1301	flagstone flag	ceramic scree
    Armory and Leggery	ROW1302	flagstone flail	ceramic scree
    Armory and Leggery	ROW1303	flagstone flip-flops	ceramic scree
    Armory and Leggery	ROW1304	flagstone fez	ceramic scree
    Armory and Leggery	ROW1305	flagstone fleece	ceramic scree
    Armory and Leggery	ROW1306	flagstone fringe	ceramic scree
    Armory and Leggery	ROW1296	chiffon chevrons	moss mulch
    Armory and Leggery	ROW1295	chiffon chapeau	moss mulch
    Armory and Leggery	ROW1442	chiffon chamberpot	moss mulch
    Armory and Leggery	ROW1443	chiffon chemise	moss mulch
    Armory and Leggery	ROW1444	chiffon chakram	moss mulch
    Armory and Leggery	ROW1445	chiffon chaps	moss mulch
    Armory and Leggery	ROW1448	ceramic cestus	adobe assortment
    Armory and Leggery	ROW1449	ceramic centurion shield	adobe assortment
    Armory and Leggery	ROW1450	ceramic celery grater	adobe assortment
    Armory and Leggery	ROW1451	ceramic celsiturometer	adobe assortment
    Armory and Leggery	ROW1452	ceramic cerecloth belt	adobe assortment
    Armory and Leggery	ROW1453	ceramic cenobite's robe	adobe assortment
    --------------------""";
      assertThat(text, containsString(expectedCoinmasterRows));

      // The Armory & Leggery is also an NPC store and sells items for Meat.

      // More lines that go into shoprows.txt:

      var expectedMeatShopRows =
          """
    466	armory	suede shortsword	270 Meat
    467	armory	bamboo bokuto	675 Meat
    468	armory	25-meat staff	472 Meat
    469	armory	two-handed depthsword	810 Meat
    470	armory	bill bec-de-bardiche glaive-guisarme	1,012 Meat
    471	armory	lead yo-yo	540 Meat
    472	armory	slightly peevedbow	405 Meat
    473	armory	sack of doorknobs	742 Meat
    474	armory	lucky ball-and-chain	877 Meat
    475	armory	automatic catapult	1,012 Meat
    476	armory	pentacorn hat	270 Meat
    477	armory	goofily-plumed helmet	472 Meat
    478	armory	yellow plastic hard hat	607 Meat
    479	armory	wooden salad bowl	810 Meat
    480	armory	football helmet	1,012 Meat
    481	armory	chain-mail monokini	202 Meat
    482	armory	union scalemail pants	337 Meat
    483	armory	paper-plate-mail pants	540 Meat
    484	armory	troutpiece	742 Meat
    485	armory	alpha-mail pants	945 Meat
    486	armory	Kentucky-style derby	45 Meat
    487	armory	cool whip	27 Meat
    488	armory	snorkel	27 Meat
    489	armory	studded leather boxer shorts	45 Meat
    490	armory	sweet ninja sword	45 Meat
    491	armory	toy accordion	135 Meat
    684	armory	rubber spatula	135 Meat
    685	armory	wooden spoon	270 Meat
    686	armory	crystalline reamer	472 Meat
    687	armory	macroplane grater	675 Meat
    688	armory	bastard baster	810 Meat
    689	armory	obsidian nutcracker	1,012 Meat
    818	armory	fishin' hat	9,000 Meat""";
      assertThat(text, containsString(expectedMeatShopRows));

      // Lines that go into npcstores.txt:

      var expectedNPCStoreRows =
          """
    --------------------
    Armory and Leggery	armory	cool whip	27	ROW487
    Armory and Leggery	armory	sweet ninja sword	45	ROW490
    Armory and Leggery	armory	suede shortsword	270	ROW466
    Armory and Leggery	armory	25-meat staff	472	ROW468
    Armory and Leggery	armory	bamboo bokuto	675	ROW467
    Armory and Leggery	armory	two-handed depthsword	810	ROW469
    Armory and Leggery	armory	bill bec-de-bardiche glaive-guisarme	1012	ROW470
    Armory and Leggery	armory	rubber spatula	135	ROW684
    Armory and Leggery	armory	wooden spoon	270	ROW685
    Armory and Leggery	armory	crystalline reamer	472	ROW686
    Armory and Leggery	armory	macroplane grater	675	ROW687
    Armory and Leggery	armory	bastard baster	810	ROW688
    Armory and Leggery	armory	obsidian nutcracker	1012	ROW689
    Armory and Leggery	armory	toy accordion	135	ROW491
    Armory and Leggery	armory	slightly peevedbow	405	ROW472
    Armory and Leggery	armory	lead yo-yo	540	ROW471
    Armory and Leggery	armory	sack of doorknobs	742	ROW473
    Armory and Leggery	armory	lucky ball-and-chain	877	ROW474
    Armory and Leggery	armory	automatic catapult	1012	ROW475
    Armory and Leggery	armory	snorkel	27	ROW488
    Armory and Leggery	armory	Kentucky-style derby	45	ROW486
    Armory and Leggery	armory	pentacorn hat	270	ROW476
    Armory and Leggery	armory	goofily-plumed helmet	472	ROW477
    Armory and Leggery	armory	yellow plastic hard hat	607	ROW478
    Armory and Leggery	armory	wooden salad bowl	810	ROW479
    Armory and Leggery	armory	football helmet	1012	ROW480
    Armory and Leggery	armory	fishin' hat	9000	ROW818
    Armory and Leggery	armory	studded leather boxer shorts	45	ROW489
    Armory and Leggery	armory	chain-mail monokini	202	ROW481
    Armory and Leggery	armory	union scalemail pants	337	ROW482
    Armory and Leggery	armory	paper-plate-mail pants	540	ROW483
    Armory and Leggery	armory	troutpiece	742	ROW484
    Armory and Leggery	armory	alpha-mail pants	945	ROW485
    --------------------""";
      assertThat(text, containsString(expectedNPCStoreRows));
    }

    @Test
    void willNotLogKnownRows() {
      SessionLoggerOutput.startStream();
      String responseText = html("request/test_armorer_2024.html");

      // Parse a responseText from visiting the Armory & Leggery.
      // This one has the 2023 and 2024 standard rewards available.
      // For each year
      //
      //   2 pulverized currencies
      //   6 Normal and 6 Hardcore rewards
      //
      // Our data files contain all of those.
      // Unless we "force" parsing the inventory, none will be logged.

      ShopRequest.parseShopInventory("armory", responseText, false);
      var text = SessionLoggerOutput.stopStream();

      assertThat(text, is(""));
    }
  }
}
