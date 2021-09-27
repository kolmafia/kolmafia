package net.sourceforge.kolmafia.request;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PizzaCubeRequest
	extends GenericRequest
{
	private final AdventureResult[] ingredients = new AdventureResult[4];

	/**
	 * Constructs a new <code>PizzaCubeRequest</code>
	 */

	public PizzaCubeRequest( final AdventureResult item1, final AdventureResult item2, final AdventureResult item3, final AdventureResult item4 )
	{
		super( "campground.php" );

		this.ingredients[0] = item1.getInstance( 1 );
		this.ingredients[1] = item2.getInstance( 1 );
		this.ingredients[2] = item3.getInstance( 1 );
		this.ingredients[3] = item4.getInstance( 1 );

		this.addFormField( "action", "pizza" );

		String pizzafield = PizzaCubeRequest.ingredientsToUrl( ingredients );
		if ( pizzafield != null )
		{
			this.addFormField( "pizza", pizzafield );
		}
	}

	private static final Pattern PIZZA_PATTERN = Pattern.compile( "pizza=(\\d+),(\\d+),(\\d+),(\\d+)" );

	private static AdventureResult[] urlToIngredients( final String urlString )
	{
		Matcher matcher = PizzaCubeRequest.PIZZA_PATTERN.matcher( StringUtilities.getURLDecode( urlString ) );
		if ( !matcher.find() )
		{
			return null;
		}

		AdventureResult[] ingredients = new AdventureResult[4];
		ingredients[0] = ItemPool.get( StringUtilities.parseInt( matcher.group( 1 ) ), 1 );
		ingredients[1] = ItemPool.get( StringUtilities.parseInt( matcher.group( 2 ) ), 1 );
		ingredients[2] = ItemPool.get( StringUtilities.parseInt( matcher.group( 3 ) ), 1 );
		ingredients[3] = ItemPool.get( StringUtilities.parseInt( matcher.group( 4 ) ), 1 );
		return ingredients;
	}

	private static String ingredientsToUrl( AdventureResult[] ingredients )
	{
		if ( ingredients.length != 4 )
		{
			return null;
		}

		StringBuilder buf = new StringBuilder();
		String sep = null;
		for ( AdventureResult ingredient : ingredients )
		{
			if ( ingredient == null )
			{
				return null;
			}
			if ( !ingredient.isItem() )
			{
				return null;
			}
			if ( sep != null )
			{
				buf.append( sep );
			}
			buf.append( ingredient.getItemId() );
			sep = ",";
		}
		return buf.toString();
	}

	@Override
	public void run()
	{
		if ( GenericRequest.abortIfInFightOrChoice() )
		{
			return;
		}

		// You can have duplicate ingredients
		Map<AdventureResult, Integer> imap = new HashMap<>();
		for ( AdventureResult ingredient : this.ingredients )
		{
			Integer count = imap.get( ingredient );
			imap.put( ingredient, count == null ? 1 : count.intValue() + 1 );
		}

		// Retrieve needed ingredients
		for ( Entry<AdventureResult, Integer> entry : imap.entrySet() )
		{
			AdventureResult ingredient = entry.getKey().getInstance( entry.getValue() );
			if ( !InventoryManager.retrieveItem( ingredient ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Unable to retrieve " + ingredient );
				return;
			}
		}

		// Make the pizza!
		super.run();
	}

	@Override
	public void processResults()
	{
		PizzaCubeRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "campground.php" ) || !urlString.contains( "action=makepizza" ) )
		{
			return;
		}

		AdventureResult[] ingredients = PizzaCubeRequest.urlToIngredients( urlString );
		if ( ingredients == null )
		{
			return;
		}

		for ( AdventureResult ingredient : ingredients )
		{
			ResultProcessor.removeItem( ingredient.getItemId() );
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "campground.php" ) || !urlString.contains( "action=makepizza" ) )
		{
			return false;
		}

		AdventureResult[] ingredients = PizzaCubeRequest.urlToIngredients( urlString );
		if ( ingredients == null )
		{
			return false;
		}

		StringBuilder buf = new StringBuilder( "pizza " );
		String sep = null;
		for ( AdventureResult ingredient : ingredients )
		{
			if ( ingredient != null )
			{
				if ( sep != null )
				{
					buf.append( sep );
				}
				else
				{
					sep = ", ";
				}
				buf.append( ingredient.getName() );
			}
		}
		String message = buf.toString();

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
