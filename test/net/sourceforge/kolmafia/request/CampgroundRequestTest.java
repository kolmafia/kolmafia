import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CampgroundRequestTest {
    @BeforeEach
    public void beforeEach() {
      KoLCharacter.reset("CampgroundRequest");
      Preferences.reset("CampgroundRequest");
    }

    @Test
    void canDetectExhaustedMedicineCabinet() throws IOException {
        String html = Files.readString(Path.of("request/test_campground_medicine_cabinet_out_of_consults.html"));
        CampgroundRequest.parseResponse("campground.php?action=workshed", html);
        assertEquals(CampgroundRequest.getCurrentWorkshedItem().getItemId(), ItemPool.COLD_MEDICINE_CABINET);
    }
  
}
