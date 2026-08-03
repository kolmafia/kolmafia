package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.session.ContactManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContactListRequestTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("ContactListRequestTest");
  }

  @AfterEach
  public void afterEach() {
    ContactManager.reset();
  }

  @Test
  void canParseNonEmptyContactList() {
    ContactListRequest.parseResponse(
        "account_contactlist.php", html("request/test_account_contact_list.html"));

    assertThat(ContactManager.getMailContacts(), hasSize(2));
    assertThat(
        ContactManager.getMailContacts(),
        containsInAnyOrder("contactlistrequesttest", "torturebot"));
    assertThat(ContactManager.getPlayerId("TortureBot"), equalTo("3495347"));
  }

  @Test
  void canParseEmptyContactList() {
    ContactListRequest.parseResponse(
        "account_contactlist.php", html("request/test_account_contact_list_empty.html"));

    assertThat(ContactManager.getMailContacts(), hasSize(1));
    assertThat(ContactManager.getMailContacts(), containsInAnyOrder("contactlistrequesttest"));
  }
}
