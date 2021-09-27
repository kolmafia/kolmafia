package net.sourceforge.kolmafia.request;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.utilities.LockableListFactory;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CafeRequest
	extends GenericRequest
{
	protected static final Pattern CAFE_PATTERN = Pattern.compile( "cafe.php.*cafeid=(\\d*)", Pattern.DOTALL );
	protected static final Pattern ITEM_PATTERN = Pattern.compile( "whichitem=(-?\\d*)", Pattern.DOTALL );
	private static final List<Concoction> existing = LockableListFactory.getInstance( Concoction.class );
	private static final AdventureResult LARP = ItemPool.get( ItemPool.LARP_MEMBERSHIP_CARD, 1 );
	private static final GenericRequest LARP_REQUEST = new StorageRequest( StorageRequest.STORAGE_TO_INVENTORY, new AdventureResult[] { CafeRequest.LARP } );

	protected String name = "";
	protected String itemName = null;
	protected boolean isPurchase = false;
	protected int price = 0;
	protected int fullness = 0;
	protected int inebriety = 0;

	public CafeRequest( final String name, final String cafeId )
	{
		super( "cafe.php" );
		this.addFormField( "cafeid", cafeId );
		this.name = name;
	}

	public static void pullLARPCard()
	{
		// You can only ever have a single LARP card.

		if ( LARP.getCount( KoLConstants.inventory ) > 0 )
		{
			return;
		}

		if ( LARP.getCount( KoLConstants.closet ) > 0 )
		{
			return;
		}

		// If you have a LARP card in storage, pull it.
		if ( InventoryManager.canUseStorage( LARP ) )
		{
			RequestThread.postRequest( LARP_REQUEST );
		}
	}

	public void setItem( final String itemName, final int itemId, final int price )
	{
		this.isPurchase = true;
		this.itemName = itemName;
		this.price = price;
		this.fullness = ConsumablesDatabase.getFullness( itemName );
		this.inebriety = ConsumablesDatabase.getInebriety( itemName );
		this.addFormField( "action", "CONSUME!" );
		this.addFormField( "whichitem", String.valueOf( itemId ) );
	}

	public static int discountedPrice( int price )
	{
		int count = LARP.getCount( KoLConstants.inventory ) + LARP.getCount( KoLConstants.closet );

		if ( count > 0 )
		{
			price = (int) Math.ceil( 0.90f * (float) price );
		}

		return price;
	}

	@Override
	public void run()
	{
		if ( !this.isPurchase )
		{
			// Just visiting to peek at the menu
			KoLmafia.updateDisplay( "Visiting " + this.name + "..." );
			super.run();
			return;
		}

		if ( this.fullness > 0 && !KoLCharacter.canEat() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't eat. Why are you here?" );
			return;
		}

		if ( this.inebriety > 0 && !KoLCharacter.canDrink() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't drink. Why are you here?" );
			return;
		}

		if ( this.price == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, this.name + " doesn't sell that." );
			return;
		}

		if ( this.price > KoLCharacter.getAvailableMeat() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Insufficient funds." );
			return;
		}

		if ( this.itemName == null )
		{
			return;
		}

		if ( this.fullness > 0 && !EatItemRequest.allowFoodConsumption( this.itemName, 1 ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Aborted eating " + this.itemName + "." );
			return;
		}

		if ( this.inebriety > 0 && !DrinkItemRequest.allowBoozeConsumption( this.itemName, 1 ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Aborted drinking " + this.itemName + "." );
			return;
		}

		KoLmafia.updateDisplay( "Purchasing " + this.itemName + " at the " + this.name + "..." );
		super.run();
	}

	@Override
	public void processResults()
	{
		if ( !this.isPurchase )
		{
			return;
		}

		if ( this.responseText.indexOf( "This is not currently available to you." ) != -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Couldn't find " + this.name );
			return;
		}

		if ( this.responseText.indexOf( "You're way too drunk already." ) != -1 || this.responseText.indexOf( "You're too full to eat that." ) != -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Consumption limit reached." );
			return;
		}

		if ( this.responseText.indexOf( "You can't afford that item." ) != -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Insufficient funds." );
			return;
		}

		// Successful purchase/consumption. Let subclass deal with with it

		this.parseResponse();

		KoLmafia.updateDisplay( "Goodie purchased." );
	}

	protected void parseResponse()
	{
	}

	protected static void addMenuItem( final List<String> menu, final String itemName, final int price )
	{
		menu.add( itemName );

		List<Concoction> usables = ConcoctionDatabase.getUsables();
		Concoction item = new Concoction( itemName, price );
		int index = usables.indexOf( item );
		if ( index != -1 )
		{
			Concoction old = usables.get( index );
			CafeRequest.existing.add( old );
			usables.set( index, item );
		}
		else
		{
			CafeRequest.existing.add( null );
			usables.add( item );
		}
	}

	public static final void reset( final List<String> menu )
	{
		// Restore usable list with original concoction
		List<Concoction> usables = ConcoctionDatabase.getUsables();
		for ( int i = 0; i < menu.size(); ++i )
		{
			String itemName = menu.get( i );
			Concoction junk = new Concoction( itemName, -1 );
			usables.remove( junk );
			Concoction old = CafeRequest.existing.get( i );
			if ( old != null )
			{
				usables.add( old );
			}
		}
		menu.clear();
		CafeRequest.existing.clear();
	}

	public static boolean registerRequest( final String urlString )
	{
		Matcher matcher = CafeRequest.CAFE_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return false;
		}

		matcher = CafeRequest.ITEM_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return true;
		}

		int itemId = StringUtilities.parseInt( matcher.group( 1 ) );
		String itemName = ItemDatabase.getItemName( itemId );
		int price = ItemDatabase.getPriceById( itemId ) * 3;
		if ( price < 0 )
		{
			// Not a real item. Get price from the cafe...
			price = 0;
		}
		CafeRequest.registerItemUsage( itemName, price );
		return true;
	}

	public static final void registerItemUsage( final String itemName, int price )
	{
		int inebriety = ConsumablesDatabase.getInebriety( itemName );
		String consume = inebriety > 0 ? "drink" : "eat";

		price = CafeRequest.discountedPrice( price );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "Buy and " + consume + " 1 " + itemName + " for " + price + " Meat" );

		if ( inebriety > 0 )
		{
			return;
		}
	}
}
