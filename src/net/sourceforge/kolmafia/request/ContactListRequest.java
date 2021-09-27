package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;

import net.sourceforge.kolmafia.session.ContactManager;

public class ContactListRequest
	extends GenericRequest
{
	private static final Pattern LIST_PATTERN = Pattern.compile( "<b>Contact List</b>.*?</table>" );
	private static final Pattern ENTRY_PATTERN =
		Pattern.compile( "<a href=\"showplayer.php\\?who=(\\d+)\".*?<b>(.*?)</b>" );

	public ContactListRequest()
	{
		super( "account_contactlist.php" );
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	@Override
	public void run()
	{
		super.run();
	}

	@Override
	public void processResults()
	{
		ContactListRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		ContactManager.clearMailContacts();

		ContactManager.addMailContact( KoLCharacter.getUserName(), KoLCharacter.getPlayerId() );

		Matcher listMatcher = ContactListRequest.LIST_PATTERN.matcher( responseText );

		if ( listMatcher.find() )
		{
			Matcher entryMatcher = ContactListRequest.ENTRY_PATTERN.matcher( listMatcher.group() );
			while ( entryMatcher.find() )
			{
				ContactManager.addMailContact( entryMatcher.group( 2 ), entryMatcher.group( 1 ) );
			}
		}
	}
}
