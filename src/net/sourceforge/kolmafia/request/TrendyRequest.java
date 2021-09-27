package net.sourceforge.kolmafia.request;

import java.util.HashMap;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

public class TrendyRequest
	extends GenericRequest
{
	// Types: "Items", "Campground", Bookshelf", "Familiars", "Skills", "Clan Item".

	private final static Map<String, Boolean> itemMap = new HashMap<>();
	private final static Map<String, Boolean> campgroundMap = new HashMap<>();
	private final static Map<String, Boolean> bookshelfMap = new HashMap<>();
	private final static Map<String, Boolean> familiarMap = new HashMap<>();
	private final static Map<String, Boolean> skillMap = new HashMap<>();
	private final static Map<String, Boolean> clanMap = new HashMap<>();

	private static final TrendyRequest INSTANCE = new TrendyRequest();
	private static boolean running = false;

	private static boolean initialized = false;

	public static void reset()
	{
		TrendyRequest.initialized = false;
		TrendyRequest.itemMap.clear();
		TrendyRequest.campgroundMap.clear();
		TrendyRequest.bookshelfMap.clear();
		TrendyRequest.familiarMap.clear();
		TrendyRequest.skillMap.clear();
		TrendyRequest.clanMap.clear();
	}

	public static void initialize()
	{
		if ( !TrendyRequest.initialized )
		{
			RequestThread.postRequest( TrendyRequest.INSTANCE );
		}
	}

	private static Map<String, Boolean> typeToMap( final String type )
	{
		return	type.equals( "Items" ) ? TrendyRequest.itemMap :
			type.equals( "Campground" ) ? TrendyRequest.campgroundMap :
			type.equals( "Bookshelf" ) ? TrendyRequest.bookshelfMap :
			type.equals( "Familiars" ) ? TrendyRequest.familiarMap :
			type.equals( "Skills" ) ? TrendyRequest.skillMap :
			type.equals( "Clan Item" ) ? TrendyRequest.clanMap :
			null;
	}

	private static boolean isTrendy( final Map<String, Boolean> map, final String key )
	{
		TrendyRequest.initialize();
		Boolean value = map.get( key.toLowerCase() );
		return value == null || value.booleanValue();
	}

	public static boolean isTrendy( final String type, final String key )
	{
		Map<String, Boolean> map = TrendyRequest.typeToMap( type );
		return map != null && TrendyRequest.isTrendy( map, key );
	}

	public TrendyRequest()
	{
		super( "typeii.php" );
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	@Override
	public void run()
	{
		if ( TrendyRequest.running )
		{
			return;
		}

		TrendyRequest.running = true;
		KoLmafia.updateDisplay( "Seeing what's still trendy today..." );
		super.run();
		TrendyRequest.running = false;
	}

	@Override
	protected boolean processOnFailure()
	{
		return true;
	}

	@Override
	public void processResults()
	{
		if ( this.responseText.equals( "" ) )
		{
			KoLmafia.updateDisplay( "KoL returned a blank page. Giving up." );
			KoLmafia.forceContinue();
			TrendyRequest.initialized = true;
			return;
		}

		TrendyRequest.parseResponse( this.getURLString(), this.responseText );
		KoLmafia.updateDisplay( "Done. Are YOU a fashion plate?" );
	}

	/*
	  <table>
	    <tr>
	      <th>Last Month</th>
	      <th>Category</th>
	      <th>Items</th>
	    </tr>
	    <tr class="expired">
	      <td nowrap valign="top">2004-12</td>
	      <td valign="top">Items</td>
	      <td valign="top">Crimbo pressie, wrapping paper		</tr>
	    <tr class="soon">
	      <td nowrap valign="top">2011-11</td>
	      <td valign="top">Campground</td>
	      <td valign="top">Grumpy Bumpkin's Seed Catalog		</tr>
	    <tr class="">
	      <td nowrap valign="top">2011-12</td>
	      <td valign="top">Clan Item</td>
	      <td valign="top">Fax Machine		</tr>
	  </table>
	*/

	public static final Pattern TRENDY_PATTERN = Pattern.compile( "<tr class=\"([^\"]*)\">.*?<td[^>]*>([^<]*)</td>.*?<td[^>]*>([^<]*)</td>.*?<td[^>]*>((?:[^<]*(?:(?!</t[dr]>)<))*[^<]*)</t[dr]>", Pattern.DOTALL );

	public static final void parseResponse( final String location, final String responseText )
	{
		TrendyRequest.reset();

		Matcher matcher = TrendyRequest.TRENDY_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			String type = matcher.group( 3 );
			Map<String, Boolean> map = TrendyRequest.typeToMap( type );
			if ( map == null )
			{
				// Report it?
				continue;
			}

			String cat = matcher.group( 1 );
			boolean available = !cat.equals( "expired" );

			// String date = matcher.group( 2 );
			String objects = matcher.group( 4 );
			String[] splits = objects.split( ", " );
			for ( int i = 0; i < splits.length; ++i )
			{
				String object = splits[ i ].trim().toLowerCase();
				map.put( object, Boolean.valueOf( available ) );
			}
		}

		TrendyRequest.initialized = true;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "typeii.php" ) )
		{
			return false;
		}

		// We don't need to register this in the gCLI or the session log
		return true;
	}
}
