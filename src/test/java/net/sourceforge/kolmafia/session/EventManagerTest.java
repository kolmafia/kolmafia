package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.Test;

class EventManagerTest {
  @Test
  void canParseIotmEvents() {
    var html = html("request/test_main_loads_of_iotm_events.html");
    EventManager.checkForNewEvents(html);

    assertThat(
        EventManager.getEventTexts(),
        hasItem(
            "Oh look, it's that trunk of stuff from when you used to put on Mummer's Plays as a kid. You acquire an item: mumming trunk"));
    assertThat(EventManager.getEventTexts(), hasSize(48));
    EventManager.clearEventHistory();
  }
}
