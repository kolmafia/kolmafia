package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.IslandManager;
import net.sourceforge.kolmafia.session.IslandManager.Quest;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class IslandRequest
	extends GenericRequest
{
	private static final Pattern OPTION_PATTERN = Pattern.compile( "option=(\\d+)" );

	public static final AdventureResult GUNPOWDER = ItemPool.get( ItemPool.GUNPOWDER, 1 );

	public static final String[][] HIPPY_CONCERTS =
	{
		{ "Moon'd", "+5 Stat(s) Per Fight" },
		{ "Dilated Pupils", "Item Drop +20%" },
		{ "Optimist Primal", "Familiar Weight +5" },
	};

	public static final String[][] FRATBOY_CONCERTS =
	{
		{ "Elvish", "All Attributes +10%" },
		{ "Winklered", "Meat Drop +40%" },
		{ "White-boy Angst", "Initiative +50%" },
	};

	private static int effectToConcertNumber( final String completer, final String effect )
	{
		String [][] array =
			completer.equals( "hippies" ) ?
			HIPPY_CONCERTS :
			completer.equals( "fratboys" ) ?
			FRATBOY_CONCERTS :
			null;

		if ( array == null )
		{
			return 0;
		}

		String compare = effect.toLowerCase();
		for ( int i = 0; i < array.length; ++i )
		{
			if ( array[i][0].toLowerCase().startsWith( compare ) )
			{
				return i + 1;
			}
		}

		return 0;
	}

	private Quest quest;

	public IslandRequest()
	{
		super( IslandManager.currentIsland() );
		this.quest = Quest.NONE;
	}

	public static IslandRequest getConcertRequest( final int option )
	{
		IslandRequest request = new IslandRequest();

		if ( request.getPath().equals( "bogus.php" ) )
		{
			return null;
		}

		if ( option < 0 || option > 3 )
		{
			return null;
		}

		request.quest = Quest.ARENA;

		request.addFormField( "action", "concert" );
		request.addFormField( "option", String.valueOf( option ) );

		return request;
	}

	public static IslandRequest getConcertRequest( final String effect )
	{
		String completer = IslandManager.questCompleter( "sidequestArenaCompleted" );
		int option = IslandRequest.effectToConcertNumber( completer, effect );
		return ( option == 0 ) ? null : IslandRequest.getConcertRequest( option );
	}

	public static String concertError( final String arg )
	{
		if ( IslandManager.warProgress().equals( "unstarted" ) )
		{
			return "You have not started the island war yet.";
		}

		String completer = IslandManager.questCompleter( "sidequestArenaCompleted" );
		if ( completer.equals( "none" ) )
		{
			return "The arena is not open.";
		}

		String loser = Preferences.getString( "sideDefeated" );
		if ( loser.equals( completer ) || loser.equals( "both" ) )
		{
			return "The arena's fans were defeated in the war.";
		}

		if ( Character.isDigit( arg.charAt( 0 ) ) )
		{
			// Raw concert number
			int option = StringUtilities.parseInt( arg );
			if ( option < 0 || option > 3 )
			{
				return "Invalid concert number.";
			}
		}
		else
		{
			// Effect name
			int option = IslandRequest.effectToConcertNumber( completer, arg );
			if ( option == 0 )
			{
				return "The \"" + arg + "\" effect is not available to " + completer;
			}
		}

		return "";
	}

	public static IslandRequest getPyroRequest()
	{
		IslandRequest request = new IslandRequest();

		if ( request.getPath().equals( "bogus.php" ) )
		{
			return null;
		}

		request.quest = Quest.LIGHTHOUSE;
		request.addFormField( "place", "lighthouse" );
		request.addFormField( "action", "pyro" );

		return request;
	}

	public static final String getPyroURL()
	{
		return IslandManager.currentIsland() + "?place=lighthouse&action=pyro";
	}

	public static IslandRequest getFarmerRequest()
	{
		IslandRequest request = new IslandRequest();

		if ( request.getPath().equals( "bogus.php" ) )
		{
			return null;
		}

		request.quest = Quest.FARM;
		request.addFormField( "place", "farm" );
		request.addFormField( "action", "farmer" );

		return request;
	}

	public static IslandRequest getNunneryRequest()
	{
		IslandRequest request = new IslandRequest();

		if ( request.getPath().equals( "bogus.php" ) )
		{
			return null;
		}

		request.quest = Quest.NUNS;
		request.addFormField( "place", "nunnery" );
		request.addFormField( "action", "nuns" );

		return request;
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	@Override
	public void run()
	{
		switch ( this.quest )
		{
		case ARENA:
			KoLmafia.updateDisplay( "Visiting the Mysterious Island Arena..." );
			break;
		case LIGHTHOUSE:
			KoLmafia.updateDisplay( "Visiting the Lighthouse Keeper..." );
			break;
		case FARM:
			KoLmafia.updateDisplay( "Visiting the Farmer..." );
			break;
		}

		super.run();
	}

	@Override
	public void processResults()
	{
		if ( this.responseText == null || this.responseText.equals( "" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't find the Mysterious Island." );
			return;
		}

		IslandRequest.parseResponse( this.getURLString(), this.responseText );

		switch ( this.quest )
		{
		case ARENA:
			// Unfortunately, you think you've pretty much tapped out this
			// event's entertainment potential for today
			//
			// You're all rocked out.

			if ( this.responseText.contains( "pretty much tapped out" ) ||
			     this.responseText.contains( "You're all rocked out" ) )
			{
				KoLmafia.updateDisplay( "You can only visit the Mysterious Island Arena once a day." );
				return;
			}

			// The stage at the Mysterious Island Arena is empty
			if ( this.responseText.contains( "The stage at the Mysterious Island Arena is empty" ) )
			{
				KoLmafia.updateDisplay( "Nobody is performing." );
				return;
			}

			if ( !this.responseText.contains( "You acquire an effect" ) )
			{
				KoLmafia.updateDisplay( "You couldn't get to the Mysterious Island Arena." );
				return;
			}

			KoLmafia.updateDisplay( "A music lover is you." );
			break;

		case LIGHTHOUSE:
			KoLmafia.updateDisplay( "Done visiting the Lighthouse Keeper." );
			break;

		case FARM:
			KoLmafia.updateDisplay( "Done visiting the Farmer." );
			break;
		}
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		// Let the Island Manager deduce things about the state of the
		// island based on the responseText
		IslandManager.parseIsland( location, responseText );

		// Do things that depend on actual actions

		String action = GenericRequest.getAction( location );
		if ( action == null )
		{
			return;
		}

		if ( action.equals( "concert" ) )
		{
			if ( responseText.contains( "You acquire an effect" ) ||
			     responseText.contains( "pretty much tapped out" ) ||
			     responseText.contains( "You're all rocked out" ) )
			{
				Preferences.setBoolean( "concertVisited", true );
			}
			return;
		}

		if ( action.equals( "farmer" ) )
		{
			if ( responseText.contains( "Ach, here ye are" ) ||
			     responseText.contains( "already got yer stuff today" ) )
			{
				Preferences.setBoolean( "_farmerItemsCollected", true );
			}
			return;
		}

		if ( action.equals( "pyro" ) )
		{
			// "The Lighthouse Keeper's eyes light up as he sees your
			// gunpowder.<p>&quot;Big boom!	 Big big boom!	Give me those,
			// <i>bumpty-bump</i>, and I'll make you the big
			// boom!&quot;<p>He takes the gunpowder into a back room, and
			// returns with an armload of big bombs."
			if ( responseText.contains( "eyes light up" ) )
			{
				int count = IslandRequest.GUNPOWDER.getCount( KoLConstants.inventory );
				ResultProcessor.processItem( ItemPool.GUNPOWDER, -count );
			}
			return;
		}
	}

	private static final Pattern CAMP_PATTERN = Pattern.compile( "whichcamp=(\\d+)" );
	public static CoinmasterData findCampMaster( final String urlString )
	{
		Matcher campMatcher = IslandRequest.CAMP_PATTERN.matcher( urlString );
		if ( !campMatcher.find() )
		{
			return null;
		}

		String camp = campMatcher.group(1);

		if ( camp.equals( "1" ) )
		{
			return DimemasterRequest.HIPPY;
		}

		if ( camp.equals( "2" ) )
		{
			return QuartersmasterRequest.FRATBOY;
		}

		return null;
	}

	static public CoinmasterData lastCampVisited = null;
	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "bigisland.php" ) && !urlString.startsWith( "postwarisland.php" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );

		if ( urlString.startsWith( "bigisland.php" ) )
		{
			// You can only visit the two camps during the war
			CoinmasterData data = IslandRequest.findCampMaster( urlString );
			if ( data != null )
			{
				IslandRequest.lastCampVisited = data;
				return CoinMasterRequest.registerRequest( data, urlString );
			}

			if ( action != null && action.equals( "bossfight" ) )
			{
				// You can only get here by visiting a specific
				// camp first. We saved that above.

				KoLAdventure location = null;
				String headquarters = null;

				if ( IslandRequest.lastCampVisited == DimemasterRequest.HIPPY )
				{
					headquarters = "Hippy Camp";
					location = AdventureDatabase.getAdventure( "The Battlefield (Frat Uniform)" );
				}
				else if ( IslandRequest.lastCampVisited == QuartersmasterRequest.FRATBOY )
				{
					headquarters = "Frat House";
					location = AdventureDatabase.getAdventure( "The Battlefield (Hippy Uniform)" );
				}
				else
				{
					// This shouldn't happen; you can't get to the
					// boss fight without visiting a camp first.
					headquarters = "Headquarters";
					location = AdventureDatabase.getAdventure( "The Battlefield (Hippy Uniform)" );
                }

				// Remember that this counts as a battlefield fight,
				// even if the player just went somewhere else.

				KoLAdventure.lastVisitedLocation = location;
				RequestLogger.registerLocation( headquarters );
				return true;
			}
		}

		String message = null;
		boolean gcli = false;

		if ( action == null )
		{
			String place = GenericRequest.getPlace( urlString );

			// place=concert
			// place=junkyard
			// place=orchard
			// place=farm
			// place=nunnery
			// place=lighthouse

			// Most of these are containers and simply visiting the
			// place does nothing; you can either adventure in a
			// location or visit an NPC with action=xxx

			// Visiting the arena before you have started the quest
			// grants you advertising flyers
			if ( place.equals( "concert" ) )
			{
				message = "Visiting the Mysterious Island Arena";
				RequestLogger.updateSessionLog();
				RequestLogger.updateSessionLog( message );
			}
			return true;
		}

		if ( action.equals( "concert" ) )
		{
			Matcher matcher = OPTION_PATTERN.matcher( urlString );
			if ( !matcher.find() )
			{
				return true;
			}
			message = "concert " + matcher.group( 1 );
		}
		else if ( action.equals( "junkman" ) )
		{
			message = "Visiting Yossarian";
		}
		else if ( action.equals( "stand" ) )
		{
			message = "Visiting The Organic Produce Stand";
			gcli = true;	// Part of Breakfast
		}
		else if ( action.equals( "farmer" ) )
		{
			message = "Visiting Farmer McMillicancuddy";
			gcli = true;	// Part of Breakfast
		}
		else if ( action.equals( "nuns" ) )
		{
			message = "Visiting Our Lady of Perpetual Indecision ";
		}
		else if ( action.equals( "pyro" ) )
		{
			int count = IslandRequest.GUNPOWDER.getCount( KoLConstants.inventory );
			message = "Visiting the lighthouse keeper with " + count + " barrel" + ( count == 1 ? "" : "s" ) + " of gunpowder.";
			gcli = true;	// Part of Breakfast
		}

		if ( message == null )
		{
			// Log URL of unknown actions
			return false;
		}

		if ( gcli )
		{
			RequestLogger.printLine( message );
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
