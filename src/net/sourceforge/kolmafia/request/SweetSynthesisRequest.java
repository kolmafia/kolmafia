package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.SkillPool;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SweetSynthesisRequest
	extends GenericRequest
{
	// runskillz.php?action=Skillz&whichskill=166&targetplayer=XXXXX&quantity=1
	// choice.php?a=XXXX&b=YYYY&q=W&whichchoice=1217&option=1

	private static final Pattern COUNT_PATTERN = Pattern.compile( "[?&]q=([\\d]+)" );
	private static final Pattern ITEMID1_PATTERN = Pattern.compile( "[?&]a=([\\d]+)" );
	private static final Pattern ITEMID2_PATTERN = Pattern.compile( "[?&]b=([\\d]+)" );

	final int count;
	final int itemId1;
	final int itemId2;

	public SweetSynthesisRequest( final int itemId1, final int itemId2 )
	{
		this( 1, itemId1, itemId2 );
	}

	public SweetSynthesisRequest( final int count, final int itemId1, final int itemId2 )
	{
		super( "choice.php" );
		this.addFormField( "whichchoice", "1217" );
		this.addFormField( "option", "1" );
		this.addFormField( "a", String.valueOf( itemId1 ) );
		this.addFormField( "b", String.valueOf( itemId2 ) );
		this.addFormField( "q", String.valueOf( count ) );
		this.count = count;
		this.itemId1 = itemId1;
		this.itemId2 = itemId2;
	}

	private static int extractItemId( final String urlString, Pattern pattern )
	{
		Matcher matcher = pattern.matcher( urlString );
		return matcher.find() ?
			StringUtilities.parseInt( matcher.group( 1 ) ):
			0;
	}

	private static int extractCount( final String urlString )
	{
		Matcher matcher = SweetSynthesisRequest.COUNT_PATTERN.matcher( urlString );
		return matcher.find() ?
			Math.max( 1, StringUtilities.parseInt( matcher.group ( 1 ) ) ) :
			1;
	}

	@Override
	public void run()
	{
		if ( GenericRequest.abortIfInFightOrChoice() )
		{
			return;
		}

		// Check that you have spleen available
		if ( ( KoLCharacter.getSpleenUse() + this.count ) > KoLCharacter.getSpleenLimit() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Your spleen has been abused enough today" );
			return;
		}

		String itemName1 = ItemDatabase.getDataName( this.itemId1 );
		String itemName2 = ItemDatabase.getDataName( this.itemId2 );

		if ( !ItemDatabase.isCandyItem( this.itemId1 ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Item '" + itemName1 + "' is not candy" );
			return;
		}

		if ( !ItemDatabase.isCandyItem( this.itemId2 ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Item '" + itemName2 + "' is not candy" );
			return;
		}

		// If you are under Standard restrictions, there is nothing you
		// can choose to do to get a restricted candy into inventory,
		// but there are ways to find restricted candy in-run
		//
		// Sweet Synthesis is willing to synthesize using such candies.
		// Therefore, do not enforce Standard restrictions here; let
		// retrieveItem fail if the requested candies are not on hand

		// Some candies can be created from ingredients using methods that consume turns.
		// Do not allow creation; only retrieve the finished product
		if ( this.itemId1 == this.itemId2 )
		{
			// Acquire both candies
			if ( !InventoryManager.retrieveItem( this.itemId1, 2 * this.count, true, false, false ) )
			{
				return;
			}
		}
		else
		{
			// Acquire the first candy
			if ( !InventoryManager.retrieveItem( this.itemId1, this.count, true, false, false ) )
			{
				return;
			}

			// Acquire the second candy
			if ( !InventoryManager.retrieveItem( this.itemId2, this.count, true, false, false ) )
			{
				return;
			}
		}

		// Run the skill
		GenericRequest skillRequest = new GenericRequest( "runskillz.php" );
		skillRequest.addFormField( "action", "Skillz" );
		skillRequest.addFormField( "whichskill", String.valueOf( SkillPool.SWEET_SYNTHESIS ) );
		skillRequest.addFormField( "targetplayer", KoLCharacter.getPlayerId() );
		skillRequest.addFormField( "quantity", "1" );

		skillRequest.run();

		String responseText = skillRequest.responseText;

		// No response because of I/O error
		if ( responseText == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "I/O error" );
			return;
		}

		// Now run the choice
		super.run();

		responseText = this.responseText;

		// This should have been caught above
		if ( responseText.contains( "Your spleen has already taken enough abuse for one day." ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Your spleen has been abused enough today" );
			return;
		}

		if ( responseText.contains( "You have to pick two candies!" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Something went wrong with candy selection" );
			return;
		}

		if ( responseText.contains( "You don't have that candy!" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "KoLmafia thinks you have a candy that KoL thinks you don't" );
			InventoryManager.refresh();
			return;
		}

		// ChoiceManager will invoke our postChoice1 method
	}

	public static void postChoice1( final String urlString, final String responseText )
	{
		// Your spleen has already taken enough abuse for one day.
		// You have to pick two candies!

		// Rather than detecting various failures, look for success.
		if ( responseText.contains( "You acquire an effect" ) )
		{
			int count = SweetSynthesisRequest.extractCount( urlString );
			// We just poisoned some spleen
			KoLCharacter.setSpleenUse( KoLCharacter.getSpleenUse() + count );
			KoLCharacter.updateStatus();

			// And used up some candies
			int itemId1 = SweetSynthesisRequest.extractItemId( urlString, ITEMID1_PATTERN );
			int itemId2 = SweetSynthesisRequest.extractItemId( urlString, ITEMID2_PATTERN );
			ResultProcessor.processItem( itemId1, -count );
			ResultProcessor.processItem( itemId2, -count );
		}
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "choice.php" ) )
		{
			return false;
		}

		int choice = ChoiceManager.extractChoiceFromURL( urlString );

		if ( choice != 1217 )
		{
			return false;
		}

		int itemId1 = SweetSynthesisRequest.extractItemId( urlString, ITEMID1_PATTERN );
		int itemId2 = SweetSynthesisRequest.extractItemId( urlString, ITEMID2_PATTERN );

		if ( itemId1 == 0 || itemId2 == 0)
		{
			return false;
		}

		int count = SweetSynthesisRequest.extractCount( urlString );
		String name1 = ItemDatabase.getDataName( itemId1 );
		String name2 = ItemDatabase.getDataName( itemId2 );

		String message = "synthesize " + count + " " + name1 + ", " + name2;

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
