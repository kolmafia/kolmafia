package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TavernRequest
	extends GenericRequest
{
	private static final Pattern GOOFBALL_PATTERN = Pattern.compile( "Buy some goofballs \\((\\d+),000 Meat\\)" );

	// tavern.php?place=barkeep
	//	store.php?whichstore=v&buying=Yep.&phash&whichitem=xxx&howmany=y
	// tavern.php?place=susguy
	//	action=buygoofballs
	// tavern.php?place=pooltable
	//	action=pool&opponent=1&wager=50
	//	action=pool&opponent=2&wager=200
	//	action=pool&opponent=3&wager=500
	// cellar.php
	//	action=explore&whichspot=4

	public TavernRequest( final int itemId )
	{
		super( "tavern.php" );

		switch (itemId )
		{
		case ItemPool.GOOFBALLS:
			this.addFormField( "action", "buygoofballs" );
			break;
		case ItemPool.OILY_GOLDEN_MUSHROOM:
			this.addFormField( "sleazy", "1" );
			break;
		default:
			this.addFormField( "place", "susguy" );

			break;
		}
	}

	@Override
	public void processResults()
	{
		TavernRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		if ( !location.startsWith( "tavern.php" ) )
		{
			return;
		}

		if ( location.contains( "place=barkeep" ) )
		{
			if ( responseText.contains( "have a few drinks on the house" ) ||
			     responseText.contains( "something that wasn't booze" ) ||
			     responseText.contains( "a round on the house" ) ||
				 responseText.contains( "grab some mugs and pour yourself some tavern swill" ) )
			{
				QuestDatabase.setQuestProgress( Quest.RAT, QuestDatabase.FINISHED );
			}
			else
			{
				QuestDatabase.setQuestIfBetter( Quest.RAT, "step1" );
				ConcoctionDatabase.setRefreshNeeded( false );
			}
		}
	
		if ( location.contains( "place=susguy" ) ) {
			if ( !responseText.contains( "Take some goofballs (for free!)") ) {
				Preferences.setInteger( "lastGoofballBuy", KoLCharacter.getAscensions() );
			}
		}

		if ( location.contains( "action=buygoofballs" ) )
		{
			// Here you go, man. If you get caught, you didn't get
			// these from me, man.
			Preferences.setInteger( "lastGoofballBuy", KoLCharacter.getAscensions() );
			if ( !responseText.contains( "If you get caught" ) )
			{
				return;
			}

			Matcher matcher = GOOFBALL_PATTERN.matcher( responseText );
			if ( !matcher.find() )
			{
				return;
			}

			int cost = 1000 * Integer.parseInt( matcher.group( 1 ) ) - 1000;
			if ( cost > 0 )
			{
				ResultProcessor.processMeat( -cost );
			}

			return;
		}

		if ( location.contains( "sleazy=1" ) )
		{
			// The suspicious-looking guy takes your gloomy black
			// mushroom and smiles that unsettling little smile
			// that makes you nervous. "Sweet, man. Here ya go."

			if ( responseText.contains ("takes your gloomy black mushroom" ) )
			{
				ResultProcessor.processItem( ItemPool.GLOOMY_BLACK_MUSHROOM, -1 );
			}

			return;
		}
	}

	private static final Pattern MAP_PATTERN = Pattern.compile( "alt=\"([^\"]*) \\(([\\d]*),([\\d]*)\\)\"" );

	private static void parseCellarMap( final String text )
	{
		String oldLayout = TavernRequest.tavernLayout();
		StringBuilder layout = new StringBuilder( oldLayout );

		Matcher matcher = TavernRequest.MAP_PATTERN.matcher( text );
		while ( matcher.find() )
		{
			int col = StringUtilities.parseInt( matcher.group(2) );
			int row = StringUtilities.parseInt( matcher.group(3) );
			int square = ( row - 1 ) * 5 + ( col - 1 );

			if ( square < 0 || square >= 25 )
			{
				continue;
			}

			char code = layout.charAt( square );
			String type = matcher.group(1);

			if ( type.startsWith( "Darkness" ) )
			{
				code = '0';
			}
			else if ( type.startsWith( "Explored" ) )
			{
				if ( code == '1' || code == '2' || code == '5' )
				{
					continue;
				}
				code = '1';
			}
			else if ( type.startsWith( "A Rat Faucet" ) )
			{
				code = '3';
			}
			else if ( type.startsWith( "A Tiny Mansion" ) )
			{
				code = text.contains( "mansion2.gif" ) ? '6' : '4';
			}
			else if ( type.startsWith( "Stairs Up" ) )
			{
				code = '1';
			}
			else
			{
				continue;
			}

			layout.setCharAt( square, code );
		}

		String newLayout = layout.toString();

		if ( !oldLayout.equals( newLayout ) )
		{
			Preferences.setString( "tavernLayout", newLayout );
		}
	}

	private static final Pattern SPOT_PATTERN = Pattern.compile( "whichspot=([\\d,]+)" );
	private static int getSquare( final String urlString )
	{
		// cellar.php?action=explore&whichspot=4
		if ( !urlString.startsWith( "cellar.php" ) || !urlString.contains( "action=explore") )
		{
			return 0;
		}

		Matcher matcher = TavernRequest.SPOT_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return 0;
		}

		return StringUtilities.parseInt( matcher.group( 1 ) );
	}

	public static final String cellarLocationString( final String urlString )
	{
		int square = TavernRequest.getSquare( urlString );
		if ( square == 0 )
		{
			if ( !urlString.contains( "action=autofaucet" ) )
			{
				return "The Typical Tavern Cellar";
			}
			String layout = TavernRequest.tavernLayout();
			int faucet = layout.indexOf( "3" );
			if ( faucet == -1 )
			{
				return "The Typical Tavern Cellar (Faucet)";
			}
			square = faucet + 1;
		}

		int row = ( ( square - 1 ) / 5 ) + 1;
		int col = ( ( square - 1 ) % 5 ) + 1;
		return "The Typical Tavern Cellar (row " + row + ", col " + col + ")";
	}

	public static final void validateFaucetQuest()
	{
		int lastAscension = Preferences.getInteger( "lastTavernAscension" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			Preferences.setInteger( "lastTavernSquare", 0 );
			Preferences.setInteger( "lastTavernAscension", KoLCharacter.getAscensions() );
			Preferences.setString( "tavernLayout", "0000000000000000000000000" );
		}
	}

	public static final String tavernLayout()
	{
		TavernRequest.validateFaucetQuest();
		String layout = Preferences.getString( "tavernLayout" );
		if ( layout.length() != 25 )
		{
			layout = "0000000000000000000000000";
			Preferences.setString( "tavernLayout", layout );
		}
		return layout;
	}

	public static final void preTavernVisit( final GenericRequest request )
	{
		TavernRequest.validateFaucetQuest();

		String urlString = request.getURLString();
		int square = TavernRequest.getSquare( urlString );
		if ( square == 0 )
		{
			return;
		}

		Preferences.setInteger( "lastTavernSquare", square );
	}

	public static final void postTavernVisit( final GenericRequest request )
	{
		String urlString = request.getURLString();
		String responseText = request.responseText;

		if ( urlString.equals( "cellar.php" ) )
		{
			TavernRequest.parseCellarMap( responseText );
			return;
		}

		if ( KoLCharacter.getAdventuresLeft() == 0 ||
		     KoLCharacter.getCurrentHP() == 0 ||
		     KoLCharacter.getInebriety() > KoLCharacter.getInebrietyLimit() )
		{
			return;
		}

		if ( urlString.startsWith( "fight.php" ) || urlString.startsWith( "fambattle.php" ) )
		{
			int square = Preferences.getInteger( "lastTavernSquare" );
			char replacement = responseText.contains( "Baron" ) ? '4' : '1';
			TavernRequest.addTavernLocation( square, replacement );
			return;
		}

		int square = urlString.startsWith( "choice.php" ) ?
			Preferences.getInteger( "lastTavernSquare" ) :
			TavernRequest.getSquare( urlString );
		if ( square == 0 )
		{
			return;
		}

		char replacement = '1';
		if ( responseText.contains( "Those Who Came Before You" ) )
		{
			// Dead adventurer
			replacement = '2';
		}
		else if ( responseText.contains( "Of Course!" ) ||
			  responseText.contains( "Hot and Cold Running Rats" ) ||
			  responseText.contains( "Everything in Moderation" ) ||
			  responseText.contains( "Hot and Cold Dripping Rats" ) )
		{
			// Rat faucet, before and after turning off
			replacement = '3';
			QuestDatabase.setQuestIfBetter( Quest.RAT, "step2" );
		}
		else if ( responseText.contains( "is it Still a Mansion" ) )
		{
			// Baron von Ratsworth
			replacement = '4';
		}
		// The little mansion is silent and empty, you having slain the
		// man... er... the rat of the house.
		else if ( responseText.contains( "little mansion is silent and empty" ) )
		{
			// Defeated Baron von Ratsworth
			replacement = '6';
		}
		else if ( responseText.contains( "whichchoice" ) )
		{
			// Various Barrels
			replacement = '5';
		}

		TavernRequest.addTavernLocation( square, replacement );
		Preferences.setInteger( "lastTavernSquare", square );
	}

	public static final void addTavernLocation( final char value )
	{
		int square = Preferences.getInteger( "lastTavernSquare" );
		TavernRequest.addTavernLocation( square, value );
	}

	private static void addTavernLocation( final int square, final char value )
	{
		StringBuilder layout = new StringBuilder( TavernRequest.tavernLayout() );
		layout.setCharAt( square - 1, value );
		Preferences.setString( "tavernLayout", layout.toString() );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "tavern.php" ) )
		{
			return false;
		}

		String message;
		if ( urlString.contains( "action=buygoofballs" ) )
		{
			message = "Buying goofballs from the suspicious looking guy";
		}
		else if ( urlString.contains( "sleazy=1" ) )
		{
			message = "Trading a gloomy black mushroom for an oily golden mushroom";
		}
		else if ( urlString.contains( "sleazy=2" ) )
		{
			// Keeping your gloomy black mushroom
			return true;
		}
		else if ( urlString.contains( "place=susguy" ) )
		{
			RequestLogger.printLine( "" );
			RequestLogger.updateSessionLog();
			message = "Visiting the suspicious looking guy";
		}
		else if ( urlString.contains( "place=barkeep" ) )
		{
			RequestLogger.printLine( "" );
			RequestLogger.updateSessionLog();
			message = "Visiting Bart Ender";
		}
		else
		{
			return false;
		}

		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
