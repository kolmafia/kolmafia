package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.PocketDatabase;
import net.sourceforge.kolmafia.persistence.PocketDatabase.Pocket;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CargoCultistShortsRequest
	extends GenericRequest
{
	public static final String EMPTY_POCKETS_PROPERTY = "cargoPocketsEmptied";
	public static final String PICKED_POCKET_PROPERTY = "_cargoPocketEmptied";
	public static final String POCKET_SCRAPS_PROPERTY = "cargoPocketScraps";

	public static final Set<Integer> pickedPockets = new TreeSet<>();

	public static void loadPockets()
	{
		Set<Integer> pockets = CargoCultistShortsRequest.pickedPockets;
		pockets.clear();

		for ( String pocket : Preferences.getString( EMPTY_POCKETS_PROPERTY ).split( " *, *" ) )
		{
			if ( StringUtilities.isNumeric( pocket ) )
			{
				int num = StringUtilities.parseInt( pocket );
				if ( num >= 1 && num <= 666 )
				{
					pockets.add( Integer.valueOf( num ) );
				}
			}
		}
	}

	public static void savePockets()
	{
		Set<Integer> pockets = CargoCultistShortsRequest.pickedPockets;

		StringBuilder buffer = new StringBuilder();
		for ( Integer pocket : pockets )
		{
			if ( buffer.length() > 0 )
			{
				buffer.append( "," );
			}
			buffer.append( pocket );
		}

		Preferences.setString( EMPTY_POCKETS_PROPERTY, buffer.toString() );
	}

	int pocket = 0;

	public CargoCultistShortsRequest()
	{
		super( "choice.php" );
		this.addFormField( "whichchoice", "1420" );
		this.addFormField( "option", "2" );
		this.pocket = 0;
	}

	public CargoCultistShortsRequest( int pocket )
	{
		super( "choice.php" );
		this.addFormField( "whichchoice", "1420" );
		this.addFormField( "option", "1" );
		this.addFormField( "pocket", String.valueOf( pocket ) );
		this.pocket = pocket;
	}

	@Override
	protected boolean shouldFollowRedirect()
	{
		return true;
	}

	public static MonsterData getMonsterFight( final String urlString )
	{
		int pocket = CargoCultistShortsRequest.extractPocketFromURL( urlString );
		return CargoCultistShortsRequest.getMonsterFight( pocket );
	}

	public static MonsterData getMonsterFight( final int pocket )
	{
		return PocketDatabase.monsterByNumber( pocket );
	}

	@Override
	public void run()
	{
		if ( GenericRequest.abortIfInFightOrChoice() )
		{
			return;
		}

		// If you can't get a pair of cargo cultist shorts, punt
		if ( !KoLCharacter.hasEquipped( ItemPool.CARGO_CULTIST_SHORTS, EquipmentManager.PANTS ) &&
		     !InventoryManager.retrieveItem( ItemPool.CARGO_CULTIST_SHORTS, 1, true ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have a pair of Cargo Cultist Shorts available" );
			return;
		}

		if ( this.pocket != 0 && Preferences.getBoolean( PICKED_POCKET_PROPERTY ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You've already looted a pocket from your Cargo Cultist Shorts today" );
			return;
		}

		Set<Integer> pockets = CargoCultistShortsRequest.pickedPockets;
		if ( this.pocket != 0 && pockets.contains( this.pocket ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You've already emptied that pocket this ascension." );
			return;
		}

		// If we are requesting a pocket which leads to a free fight,
		// recover first.
		if ( this.pocket != 0 && this.getMonsterFight( this.pocket ) != null )
		{
			// set location to "None" for the benefit of
			// betweenBattleScripts
			Preferences.setString( "nextAdventure", "None" );
			RecoveryManager.runBetweenBattleChecks( true );
		}

		GenericRequest useRequest = new GenericRequest( "inventory.php" );
		useRequest.addFormField( "action", "pocket" );
		useRequest.run();

		String responseText = useRequest.responseText;

		// No response because of I/O error
		if ( responseText == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "I/O error" );
			return;
		}

		// The request redirected to a choice and the available pockets
		// have been parsed.

		// If we are not looking to empty a pocket here, we are done.
		// We could just walk away from the choice, but lets exit it.

		if ( this.pocket == 0 )
		{
			super.run();
			return;
		}

		// If we have already emptied this pocket this ascension, it is not available.
		// ChoiceManager called parseAvailablePockets, which updated our Set

		if ( pockets.contains( this.pocket ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You've already emptied that pocket this ascension." );
			this.constructURLString( "choice.php?whichchoice=1420&option=2", true );
			super.run();
			return;
		}

		// Pick the pocket!
		super.run();

		// Some pockets redirect to a fight. If not, we'll be here.
		// ChoiceManager called parsePocketPick, which updated our Set

		responseText = this.responseText;
		if ( responseText == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "I/O error" );
			return;
		}

		// It seems like the power of the pockets has been exhausted for the day.
		if ( responseText.contains( "the power of the pockets has been exhausted for the day" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You already picked a pocket today." );
			return;
		}

		// Those that did not, might leave us in the choice

		if ( ChoiceManager.handlingChoice )
		{
			this.constructURLString( "choice.php?whichchoice=1420&option=2", true );
			super.run();
		}
	}

	// <form method="post" action="choice.php" style="display: inline">
	public static final Pattern AVAILABLE_POCKET_PATTERN = Pattern.compile( "<form method=\"post\" action=\"choice.php\" style=\"display: inline\">.*?name=\"pocket\" value=\"(\\d+)\".*?</form>", Pattern.DOTALL );

	public static void parseAvailablePockets( final String responseText )
	{
		// Iterate over the pockets in the form and remove them from the set of all pockets
		if ( !responseText.contains( "There appear to be 666 pockets on these shorts." ) )
		{
			return;
		}

		Set<Integer> pockets = CargoCultistShortsRequest.pickedPockets;
		pockets.clear();

		Matcher pocketMatcher = CargoCultistShortsRequest.AVAILABLE_POCKET_PATTERN.matcher( responseText );
		int expected = 1;
		int pocket = 0;
		while ( pocketMatcher.find() )
		{
			pocket = StringUtilities.parseInt( pocketMatcher.group( 1 ) );
			while ( expected < pocket )
			{
				pockets.add( Integer.valueOf( expected++ ) );
			}
			expected++;
		}

		while ( pocket < 666 )
		{
			pockets.add( Integer.valueOf( ++pocket ) );
		}

		// Save the set of pockets we have emptied in the property
		CargoCultistShortsRequest.savePockets();
	}

	// This pocket contains a scrap of paper that reads: <b>XTNQ: Ga</b>
	// This pocket contains a scrap of paper that reads: <B>ESUQQ: Go</b>
	// This pocket contains a waterlogged scrap of paper that reads: <b>QDL XLR KVSJGGJV QRGL</b>
	public static final Pattern SCRAP_PATTERN = Pattern.compile( "This pocket contains a (waterlogged )?scrap of paper that reads: <[Bb]>([^<]+)</[Bb]>" );

	private static void checkScrapPocket( int pocket, String responseText )
	{
		// Waterlogged scraps encode a poem, which has been solved.
		// We'll log the scrap in your session log, in case you want to
		// try solving it, but it's not per-character, so doesn't need
		// to be saved in a property.

		Matcher scrapMatcher = SCRAP_PATTERN.matcher( responseText );
		if ( !scrapMatcher.find() )
		{
			return;
		}

		String printit = scrapMatcher.group( 0 );
		RequestLogger.printLine( printit );
		RequestLogger.updateSessionLog( printit );

		boolean waterlogged = scrapMatcher.group( 1 ) != null;
		if ( waterlogged )
		{
			return;
		}

		// Extract the syllable from the message on the scrap
		String syllable = scrapMatcher.group( 2 );
		int colon = syllable.indexOf( ":" );
		if ( colon == -1 )
		{
			// Unexpected
			return;
		}
		syllable = syllable.substring( colon + 1 ).trim();

		// Get a map from pocket -> syllable for previously seen pockets
		Map<Integer, String> map = knownScrapPockets();

		// Add the current pocket to the map
		map.put( IntegerPool.get( pocket ), syllable );

		// Rebuild the value of the property
		saveScrapPockets( map );

		// All 7 scraps will reveal a demon name
		SummoningChamberRequest.updateYegName( map );
	}

	public static Map<Integer, String> knownScrapPockets()
	{
		String value = Preferences.getString( POCKET_SCRAPS_PROPERTY );
		Map<Integer, String> map = new TreeMap<>();

		if ( value.equals( "" ) )
		{
			return map;
		}

		// Backwards compatibility: original implementation would store
		// something like: "7:ESUQQ: Go"
		//
		// Since ESUQQ is "three" - and the number strings do not vary
		// from character to character - convert to only have "7:Go"

		for ( String item : value.split( "\\|" ) )
		{
			String[] parts = item.split( ": *" );
			int key = StringUtilities.parseInt( parts[0] );
			String syllable = parts.length == 3 ? parts[2] : parts[ 1 ];
			map.put( key, syllable.trim() );
		}

		return map;
	}

	public static void saveScrapPockets( final Map<Integer, String> map )
	{
		// Rebuild the setting in the order the syllables are used

		StringBuilder value = new StringBuilder();
		for ( Pocket p : PocketDatabase.scrapSyllables )
		{
			Integer pocket = p.getPocket();
			String syllable = map.get( pocket );
			if ( syllable == null )
			{
				continue;
			}

			if ( value.length() > 0 )
			{
				value.append( "|" );
			}
			value.append( pocket );
			value.append( ":" );
			value.append( syllable );
		}

		Preferences.setString( POCKET_SCRAPS_PROPERTY, value.toString() );
	}

	// <span class='guts'>You pull a note out of your pocket.  It's wrapped around a pile of meat.<blockquote style='border: 1px solid black; text-align: center; padding: 1em'>Being at the level of the narrowest part of the torso</blockquote><center><table><tr><td><img src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/meat.gif" height=30 width=30 alt="Meat"></td><td valign=center>You gain 917 Meat.</td></tr></table></center></span>

	public static final Pattern MEAT_NOTE_PATTERN = Pattern.compile( "(You pull a note out of your pocket.  It's wrapped around a pile of meat.).*?<blockquote[^>]*>([^<]*)<" );

	private static void checkMeatNotePocket( int pocket, String responseText )
	{
		// Notes wrapped around Meat are a puzzle, which has been solved.
		// We'll log the note in your session log, in case you want to
		// try solving it, but it's not per-character, so doesn't need
		// to be saved in a property.

		Matcher noteMatcher = MEAT_NOTE_PATTERN.matcher( responseText );
		if ( !noteMatcher.find() )
		{
			return;
		}

		String printit = noteMatcher.group( 1 ) + ":";
		RequestLogger.printLine( printit );
		RequestLogger.updateSessionLog( printit );
		printit = "\"" + noteMatcher.group( 2 ) + "\"";
		RequestLogger.printLine( printit );
		RequestLogger.updateSessionLog( printit );
	}

	public static void parsePocketPick( final String urlString, final String responseText )
	{
		int pocket = CargoCultistShortsRequest.extractPocketFromURL( urlString );

		// That's not a pocket.
		if ( pocket < 1 || pocket > 666 )
		{
			return;
		}

		// You decide to leave your pockets unplundered for now
		if ( responseText.contains( "leave your pockets unplundered" ) )
		{
			return;
		}

		// It seems like the power of the pockets has been exhausted for the day.
		if ( responseText.contains( "the power of the pockets has been exhausted for the day" ) )
		{
			Preferences.setBoolean( PICKED_POCKET_PROPERTY, true );
			return;
		}

		// The pocket was not rejected
		if ( !CargoCultistShortsRequest.pickedPockets.contains( pocket ) )
		{
			CargoCultistShortsRequest.pickedPockets.add( pocket );
			CargoCultistShortsRequest.savePockets();
		}

		// That pocket is empty.
		if ( responseText.contains( "That pocket is empty" ) )
		{
			return;
		}

		// Successful pick
		Preferences.setBoolean( PICKED_POCKET_PROPERTY, true );

		CargoCultistShortsRequest.checkScrapPocket( pocket, responseText );
		CargoCultistShortsRequest.checkMeatNotePocket( pocket, responseText );
	}

	public static void registerPocketFight( final String urlString )
	{
		int pocket = CargoCultistShortsRequest.extractPocketFromURL( urlString );
		Preferences.setBoolean( PICKED_POCKET_PROPERTY, true );
		CargoCultistShortsRequest.pickedPockets.add( pocket );
		CargoCultistShortsRequest.savePockets();
	}

	public static final Pattern URL_POCKET_PATTERN = Pattern.compile( "pocket=(\\d+)" );
	public static int extractPocketFromURL( final String urlString )
	{
		Matcher matcher = CargoCultistShortsRequest.URL_POCKET_PATTERN.matcher( urlString );
		return  matcher.find() ?
			StringUtilities.parseInt( matcher.group( 1 ) ) :
			0;
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( urlString.equals( "inventory.php?action=pocket" ) )
		{
			String  message = "Inspecting Cargo Cultist Shorts";
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
			return true;
		}

		if ( !urlString.startsWith( "choice.php" ) )
		{
			return false;
		}

		int choice = ChoiceManager.extractChoiceFromURL( urlString );

		if ( choice != 1420 )
		{
			return false;
		}

		int pocket = CargoCultistShortsRequest.extractPocketFromURL( urlString );
		if ( pocket == 0 )
		{
			// Pocket must be from 1-666
			return true;
		}

		String  message = "picking pocket " + pocket;
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
