package net.sourceforge.kolmafia.request;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SwaggerShopRequest
	extends CoinMasterRequest
{
	// When a new type of PVP season appears:
	//
	// name = "new"
	// itemId = ItemPool.NEW_PVP_REWARD
	// itemPrefix = "newPvpReward
	//
	// properties in defaults.txt:
	//
	// user	NAME + Swagger	0
	// user	ITEMPREFIX + Available	true
	// user	ITEMPREFIX + Cost	1000
	// 
	// Add it to the Season enum
	// Do NOT add the new item to coinmasters.txt

	public enum Season
	{
		PIRATE( "pirate", ItemPool.BLACK_BARTS_BOOTY, "blackBartsBooty" ),
		HOLIDAY( "holiday", ItemPool.HOLIDAY_FUN_BOOK, "holidayHalsBook" ),
		ICE( "ice", ItemPool.ANTAGONISTIC_SNOWMAN_KIT, "antagonisticSnowmanKit" ),
		DRUNKEN( "drunken", ItemPool.MAP_TO_KOKOMO, "mapToKokomo" ),
		BEAR( "bear", ItemPool.ESSENCE_OF_BEAR, "essenceOfBear" ),
		NUMERIC( "numeric", ItemPool.MANUAL_OF_NUMBEROLOGY, "manualOfNumberology" ),
		OPTIMAL( "optimal", ItemPool.ROM_OF_OPTIMALITY, "ROMOfOptimality" ),
		SCHOOL( "school", ItemPool.SCHOOL_OF_HARD_KNOCKS_DIPLOMA, "schoolOfHardKnocksDiploma" ),
		SAFARI( "safari", ItemPool.GUIDE_TO_SAFARI, "guideToSafari" ),
		GLITCH( "glitch", ItemPool.GLITCH_ITEM, "glitchItem" ),
		AVERAGE( "average", ItemPool.LAW_OF_AVERAGES, "lawOfAverages" ),
		SEASONING( "Seasoning", ItemPool.UNIVERSAL_SEASONING, "universalSeasoning" ),
		// Pseudo-season to handle Essence of Annoyance
		NONE( "none", ItemPool.ESSENCE_OF_ANNOYANCE, "essenceOfAnnoyance" );
			
		final public String name;
		final public int itemId;
		final public AdventureResult item;

		// Composed property names
		final public String swagger;	// name + "Swagger"
		final public String cost;	// itemPrefix + "Cost"
		final public String available;	// itemPrefix + "Available"

		Season( String name, int itemId, String itemPrefix )
		{
			this.name = name;
			this.itemId = itemId;
			this.item = ItemPool.get( itemId, 1 );
			this.swagger = name + "Swagger";
			this.cost = itemPrefix + "Cost";
			this.available = itemPrefix + "Available";
		}
	}

	// Discovered when we visit the Swagger Shop
	public static Season currentSeason = Season.NONE;

	final public static EnumSet<Season> allSeasons = EnumSet.allOf( Season.class );
	final public static Map<String, Season> nameToSeason = new HashMap<String, Season>();
	final public static Map<Integer, Season> itemIdToSeason = new HashMap<Integer, Season>();

	static
	{
		for ( Season season : allSeasons )
		{
			nameToSeason.put( season.name, season );
			itemIdToSeason.put( season.itemId, season );
		}

		String currentPVPSeason = Preferences.getString( "currentPVPSeason" );
		Season season = nameToSeason.get( currentPVPSeason );
		currentSeason = ( season == null ) ? Season.NONE : season;
	}

    public static final String master = "The Swagger Shop";
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( SwaggerShopRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( SwaggerShopRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You have ([\\d,]+) swagger" );

	public static final CoinmasterData SWAGGER_SHOP =
		new CoinmasterData(
			SwaggerShopRequest.master,
			"swagger",
			SwaggerShopRequest.class,
			"swagger",
			"You have 0 swagger",
			false,
			SwaggerShopRequest.TOKEN_PATTERN,
			null,
			"availableSwagger",
			null,
			"peevpee.php?place=shop",
			"buy",
			SwaggerShopRequest.buyItems,
			SwaggerShopRequest.buyPrices,
			null,
			null,
			null,
			null,
			"whichitem",
			GenericRequest.WHICHITEM_PATTERN,
			null,
			null,
			null,
			null,
			true
			)
		{
			@Override
			public final int getBuyPrice( final int itemId )
			{
				Season season = itemIdToSeason.get( itemId );
				return  ( season != null ) ?
					Preferences.getInteger( season.cost ) :
					super.getBuyPrice( itemId );
			}

			@Override
			public final boolean canBuyItem( final int itemId )
			{
				Season season = itemIdToSeason.get( itemId );
				return  ( season != null ) ?
					Preferences.getBoolean( season.available ) :
					super.canBuyItem( itemId );
			}

			@Override
			public final boolean availableItem( final int itemId )
			{
				Season season = itemIdToSeason.get( itemId );
				return  ( season == Season.NONE ) ?
					Preferences.getBoolean( season.available ) :
					( season == currentSeason ) ?
					Preferences.getBoolean( season.available ) && Preferences.getInteger( season.swagger ) >= Preferences.getInteger( season.cost ) :
					super.availableItem( itemId );
			}
		};

	static
	{
		ConcoctionPool.set( new Concoction( "swagger", "availableSwagger" ) );
		SWAGGER_SHOP.plural = "swagger";
	}

	public SwaggerShopRequest()
	{
		super( SwaggerShopRequest.SWAGGER_SHOP );
	}

	public SwaggerShopRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( SwaggerShopRequest.SWAGGER_SHOP, buying, attachments );
	}

	public SwaggerShopRequest( final boolean buying, final AdventureResult attachment )
	{
		super( SwaggerShopRequest.SWAGGER_SHOP, buying, attachment );
	}

	public SwaggerShopRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( SwaggerShopRequest.SWAGGER_SHOP, buying, itemId, quantity );
	}

	@Override
	public void run()
	{
		if ( this.action != null ) {
			if ( KoLCharacter.isHardcore() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You can't spend your swagger in Hardcore." );
				return;
			}

			if ( KoLCharacter.inRonin() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You can't spend your swagger until you get out of Ronin." );
				return;
			}
		}

		super.run();
	}

	@Override
	public void processResults()
	{
		SwaggerShopRequest.parseResponse( this.getURLString(), this.responseText );
	}

	// You've earned 600 swagger during a pirate season, yarrr.
	// You've earned 2 swagger during a holiday season, fun!
	// You've earned 0 swagger during an ice season, brrrr!
	// You've earned 152 swagger during a drunken season!
	// You've earned 0 swagger during bear season!
	// You've earned 0 swagger during a numeric season!
	// You've earned 37 swagger during an optimal season.
	// You've earned 0 swagger during a school season!
	// You've earned 349 swagger during a safari season!
	// You've earned -61 swagger during a glitch season!
	// You've earned -0 swagger during an average season.
	// You've earned 600 swagger during a Seasoning!

	private static final Pattern SEASON_PATTERN = Pattern.compile( "You've earned -?([\\d,]+) swagger during (?:a |an |)(pirate|holiday|ice|drunken|bear|numeric|optimal|school|safari|glitch|average|Seasoning)(?: season)?" );

	// <tr><td><img style='vertical-align: middle' class=hand src='http://images.kingdomofloathing.com/itemimages/radio.gif' onclick='descitem(475026869)'></td><td valign=center><b><span onclick='descitem(475026869)'>Huggler Radio<span>&nbsp;&nbsp;&nbsp;&nbsp;</b></td><td><form style="padding:0;margin:0;"><input type="hidden" name="action" value="buy" /><input type="hidden" name="place" value="shop" /><input type="hidden" name="pwd" value="0c6efe5fe0c70235b340073785255041" /><input type="hidden" name="whichitem" value="5656" /><input type="submit" class="button" value="Buy (50 swagger)" /></form></td></tr>

	private static final Pattern ITEM_PATTERN =
		Pattern.compile( "<tr><td><img.*?onclick='descitem\\((.*?)\\)'.*?<b>(?:<[^>]*>)?([^<]*).*?</b>.*?name=\"whichitem\" value=\"(.*?)\".*?\\((.*?) swagger\\).*?</td></tr>", Pattern.DOTALL );

	public static void parseResponse( final String urlString, final String responseText )
	{
		CoinmasterData data = SwaggerShopRequest.SWAGGER_SHOP;

		String action = GenericRequest.getAction( urlString );
		if ( action != null )
		{
			CoinMasterRequest.parseResponse( data, urlString, responseText );
			return;
		}

		// Learn new items by simply visiting the Swagger Shop
		// Refresh the Coin Master inventory every time we visit.

		LockableListModel<AdventureResult> items = SwaggerShopRequest.buyItems;
		Map<Integer, Integer> prices = SwaggerShopRequest.buyPrices;
		items.clear();
		prices.clear();

		Matcher matcher = ITEM_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			String descId = matcher.group(1);
			String itemName = matcher.group(2);
			int itemId = StringUtilities.parseInt( matcher.group(3) );
			int price = StringUtilities.parseInt( matcher.group(4) );

			String match = ItemDatabase.getItemDataName( itemId );
			if ( match == null || !match.equals( itemName ) )
			{
				ItemDatabase.registerItem( itemId, itemName, descId );
			}

			// Add it to the Swagger Shop inventory
			AdventureResult item = ItemPool.get( itemId, PurchaseRequest.MAX_QUANTITY );
			items.add( item );
			prices.put( itemId, price );

			Season itemSeason = itemIdToSeason.get( itemId );
			if ( itemSeason != null )
			{
				Preferences.setInteger( itemSeason.cost, price );
			}
		}

		// Find availability/cost of conditional items
		for ( Season season : allSeasons )
		{
			Preferences.setBoolean( season.available, items.contains( season.item ) );
		}

		// Register the purchase requests, now that we know what is available
		data.registerPurchaseRequests();

		// Parse current swagger
		CoinMasterRequest.parseBalance( data, responseText );

		// If this is a special season, determine how much swagger has been found
		Matcher seasonMatcher = SwaggerShopRequest.SEASON_PATTERN.matcher( responseText );
		if ( seasonMatcher.find() )
		{
			int seasonSwagger = StringUtilities.parseInt( seasonMatcher.group( 1 ) );
			String seasonName = seasonMatcher.group( 2 );
			Preferences.setString( "currentPVPSeason", seasonName );
			Season season = nameToSeason.get( seasonName );
			if ( season != null )
			{
				SwaggerShopRequest.currentSeason = season;
				Preferences.setInteger( season.swagger, seasonSwagger );
			}
			else
			{
				String message = "*** Unknown PVP season: " + seasonName;
				RequestLogger.printLine( message );
				RequestLogger.updateSessionLog( message );
			}
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		// We only claim peevpee.php?place=shop&action=buy
		if ( !urlString.startsWith( "peevpee.php" ) )
		{
			return false;
		}

		if ( !urlString.contains( "place=shop" ) && !urlString.contains( "action=buy" ) )
		{
			return false;
		}

		CoinmasterData data = SwaggerShopRequest.SWAGGER_SHOP;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}

	public static String accessible()
	{
		if ( KoLCharacter.isHardcore() || KoLCharacter.inRonin() )
		{
			return "Characters in Hardcore or Ronin cannot redeem Swagger";
		}
		return null;
	}
}
