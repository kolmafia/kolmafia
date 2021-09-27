package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.session.ResultProcessor;

public class ArtistRequest
	extends PlaceRequest
{
	public static final AdventureResult WHISKER = ItemPool.get( ItemPool.RAT_WHISKER, 1 );

	public ArtistRequest()
	{
		this( false );
	}

	public ArtistRequest( boolean whiskers )
	{
		super( "town_wrong", "townwrong_artist_quest" );
		if ( whiskers )
		{
			this.addFormField( "subaction", "whisker" );
		}
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		if ( !location.startsWith( "place.php" ) ||
		     ( !location.contains( "action=townwrong_artist_quest" ) &&
		       !location.contains( "action=townwrong_artist_noquest" ) ) )
		{
			return;
		}

		String message = "You have unlocked a new tattoo.";
		if ( responseText.contains( message ) )
		{
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
		}

		// First time accepting:
		// Great. If I'm going to work, I'll need my paintbrush, my palette, and my paint.
		if ( responseText.indexOf( "If I'm going to work, I'll need my paintbrush" ) != -1 )
		{
			QuestDatabase.setQuestProgress( Quest.ARTIST, QuestDatabase.STARTED );
		}

		// Subsequent times:
		// You still need to find my tools! Please hurry!
		else if ( responseText.indexOf( "still need to find my tools" ) != -1 )
		{
			QuestDatabase.setQuestProgress( Quest.ARTIST, QuestDatabase.STARTED );
		}

		// The artist pours the pail of paint into a huge barrel, then
		// says "Oh, hey, umm, do you want this empty pail? I don't
		// really have room for it, so if you want it, you can have it.

		if ( responseText.indexOf( "do you want this empty pail" ) != -1 )
		{
			ResultProcessor.processItem( ItemPool.PRETENTIOUS_PALETTE, -1 );
			ResultProcessor.processItem( ItemPool.PRETENTIOUS_PAINTBRUSH, -1 );
			ResultProcessor.processItem( ItemPool.PRETENTIOUS_PAIL, -1 );
			QuestDatabase.setQuestProgress( Quest.ARTIST, QuestDatabase.FINISHED );
			return;
		}

		if ( location.contains( "subaction=whiskers" ) &&
		     responseText.contains( "Thanks, Adventurer." ) )
		{
			int count = ArtistRequest.WHISKER.getCount( KoLConstants.inventory );
			ResultProcessor.processItem( ItemPool.RAT_WHISKER, -count );
			return;
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "place.php" ) ||
		     ( !urlString.contains( "action=townwrong_artist_quest" ) &&
		       !urlString.contains( "action=townwrong_artist_noquest" ) ) )
		{
			return false;
		}

		String message;
		if ( urlString.contains( "subaction=whisker" ) )
		{
			int count = ArtistRequest.WHISKER.getCount( KoLConstants.inventory );
			message = "Selling " + count + " rat whisker" + ( count > 1 ? "s" : "" ) + " to the pretentious artist";
		}
		else
		{
			RequestLogger.printLine( "" );
			RequestLogger.updateSessionLog();
			message = "Visiting the pretentious artist";
		}

		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
