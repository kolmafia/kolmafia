package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CombineMeatRequest
	extends CreateItemRequest
{
	public CombineMeatRequest( final Concoction conc )
	{
		super( "craft.php", conc );

		this.addFormField( "action", "makepaste" );
		this.addFormField( "whichitem", String.valueOf( this.getItemId() ) );
		this.addFormField( "ajax", "1" );
	}

	public static int getCost( int itemId )
	{
		switch ( itemId )
		{
		case ItemPool.MEAT_PASTE:
			return 10;
		case ItemPool.MEAT_STACK:
			return 100;
		case ItemPool.DENSE_STACK:
			return 1000;
		}
		return 0;
	}

	@Override
	public void run()
	{
		String name = this.getName();
		int count = this.getQuantityNeeded();
		int cost = CombineMeatRequest.getCost( this.getItemId() );

		if ( cost * count > KoLCharacter.getAvailableMeat() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Insufficient funds to make " + count + " " + name );
			return;
		}

		KoLmafia.updateDisplay( "Creating " + count + " " + name + "..." );
		this.addFormField( "qty", String.valueOf( count ) );
		super.run();
	}

	public static final boolean registerRequest( final String urlString )
	{
		Matcher itemMatcher = CreateItemRequest.ITEMID_PATTERN.matcher( urlString );
		if ( !itemMatcher.find() )
		{
			return false;
		}

		int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
		int cost = CombineMeatRequest.getCost( itemId );

		if ( cost == 0 )
		{
			return false;
		}

		Matcher quantityMatcher = GenericRequest.QTY_PATTERN.matcher( urlString );
		int quantity = quantityMatcher.find() ? StringUtilities.parseInt( quantityMatcher.group( 1 ) ) : 1;
		int total = cost * quantity;

		if ( total > KoLCharacter.getAvailableMeat() )
		{
			return true;
		}

		// We can combine meat either through crafting or via the
		// inventory. The former tells you how much meat you lost when
		// it delivers your items, the latter does not.

		if ( urlString.startsWith( "inventory.php" ) )
		{
			ResultProcessor.processMeat( 0 - total );
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "Create " + quantity + " " + ItemDatabase.getItemName( itemId ) );

		return true;
	}
}
