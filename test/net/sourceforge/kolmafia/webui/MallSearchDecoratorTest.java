package net.sourceforge.kolmafia.webui;

import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withUserId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.MallPurchaseRequest;
import net.sourceforge.kolmafia.request.MallSearchRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MallSearchDecoratorTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("Test Character");
    MallPurchaseRequest.reset();
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("Test Character");
  }

  @AfterEach
  public void AfterEach() {
    MallPurchaseRequest.reset();
  }

  @Test
  public void testForbiddenStoresDecorated() {
    String testString =
        """
        <tr class="graybelow" id="stock_11111_99"><td style="border:none"></td><td class="small store"><a class="nounder" href="mallstore.php?whichstore=11111&searchitem=99&searchprice=5000"><b>Generic Store</b></a>&nbsp;&nbsp;&nbsp;</td><td class="small stock">1</td><td class="small">&nbsp;</td><td class="small price"><a class="nounder" href="mallstore.php?whichstore=11111&searchitem=99&searchprice=5000">5,000&nbsp;Meat</a></td><td class="buyers" valign="center">&nbsp;</td></tr><tr class="graybelow" id="stock_12345_99"><td style="border:none"></td><td class="small store"><a class="nounder" href="mallstore.php?whichstore=12345&searchitem=99&searchprice=5000"><b>Generic Store</b></a>&nbsp;&nbsp;&nbsp;</td><td class="small stock">1</td><td class="small">&nbsp;</td><td class="small price"><a class="nounder" href="mallstore.php?whichstore=12345&searchitem=99&searchprice=5000">5,000&nbsp;Meat</a></td><td class="buyers" valign="center">&nbsp;</td></tr>""";

    String expectedString =
        """
        <tr class="graybelow" id="stock_11111_99" style="background-image:linear-gradient(to right, rgba(255,0,0,0), pink);" title="The preference 'forbiddenStores' contains this store."><td style="border:none"></td><td class="small store"><a class="nounder" href="mallstore.php?whichstore=11111&searchitem=99&searchprice=5000"><b>Generic Store</b></a>&nbsp;&nbsp;&nbsp;</td><td class="small stock">1</td><td class="small">&nbsp;</td><td class="small price"><a class="nounder" href="mallstore.php?whichstore=11111&searchitem=99&searchprice=5000">5,000&nbsp;Meat</a></td><td class="buyers" valign="center">&nbsp;</td></tr><tr class="graybelow" id="stock_12345_99"><td style="border:none"></td><td class="small store"><a class="nounder" href="mallstore.php?whichstore=12345&searchitem=99&searchprice=5000"><b>Generic Store</b></a>&nbsp;&nbsp;&nbsp;</td><td class="small stock">1</td><td class="small">&nbsp;</td><td class="small price"><a class="nounder" href="mallstore.php?whichstore=12345&searchitem=99&searchprice=5000">5,000&nbsp;Meat</a></td><td class="buyers" valign="center">&nbsp;</td></tr>""";

    StringBuffer buffer = new StringBuffer(testString);

    var cleanups = withProperty("forbiddenStores", "11111");

    try (cleanups) {
      MallSearchRequest.decorateMallSearchHighlightStores(buffer);

      assertThat(buffer.toString(), equalTo(expectedString));
    }
  }

  @Test
  public void testOwnStoreDecorated() {
    String testString =
        """
        <tr class="graybelow" id="stock_11111_99"><td style="border:none"></td><td class="small store"><a class="nounder" href="mallstore.php?whichstore=11111&searchitem=99&searchprice=5000"><b>Generic Store</b></a>&nbsp;&nbsp;&nbsp;</td><td class="small stock">1</td><td class="small">&nbsp;</td><td class="small price"><a class="nounder" href="mallstore.php?whichstore=11111&searchitem=99&searchprice=5000">5,000&nbsp;Meat</a></td><td class="buyers" valign="center">&nbsp;</td></tr><tr class="graybelow" id="stock_12345_99"><td style="border:none"></td><td class="small store"><a class="nounder" href="mallstore.php?whichstore=12345&searchitem=99&searchprice=5000"><b>Generic Store</b></a>&nbsp;&nbsp;&nbsp;</td><td class="small stock">1</td><td class="small">&nbsp;</td><td class="small price"><a class="nounder" href="mallstore.php?whichstore=12345&searchitem=99&searchprice=5000">5,000&nbsp;Meat</a></td><td class="buyers" valign="center">&nbsp;</td></tr>""";

    String expectedString =
        """
        <tr class="graybelow" id="stock_11111_99" style="background-image:linear-gradient(to right, rgba(0,0,255,0), lightblue);" title="This is your store."><td style="border:none"></td><td class="small store"><a class="nounder" href="mallstore.php?whichstore=11111&searchitem=99&searchprice=5000"><b>Generic Store</b></a>&nbsp;&nbsp;&nbsp;</td><td class="small stock">1</td><td class="small">&nbsp;</td><td class="small price"><a class="nounder" href="mallstore.php?whichstore=11111&searchitem=99&searchprice=5000">5,000&nbsp;Meat</a></td><td class="buyers" valign="center">&nbsp;</td></tr><tr class="graybelow" id="stock_12345_99"><td style="border:none"></td><td class="small store"><a class="nounder" href="mallstore.php?whichstore=12345&searchitem=99&searchprice=5000"><b>Generic Store</b></a>&nbsp;&nbsp;&nbsp;</td><td class="small stock">1</td><td class="small">&nbsp;</td><td class="small price"><a class="nounder" href="mallstore.php?whichstore=12345&searchitem=99&searchprice=5000">5,000&nbsp;Meat</a></td><td class="buyers" valign="center">&nbsp;</td></tr>""";

    StringBuffer buffer = new StringBuffer(testString);

    var cleanups = withUserId(11111);

    try (cleanups) {
      MallSearchRequest.decorateMallSearchHighlightStores(buffer);

      assertThat(buffer.toString(), equalTo(expectedString));
    }
  }
}
