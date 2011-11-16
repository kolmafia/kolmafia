/**
 * Copyright (c) 2005-2011, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.request;

import java.util.HashMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

public class TrendyRequest
	extends GenericRequest
{
	// Types: "Items", "Campground", Bookshelf", "Familiars", "Skills", "Clan Item".

	private final static HashMap itemMap = new HashMap();
	private final static HashMap campgroundMap = new HashMap();
	private final static HashMap bookshelfMap = new HashMap();
	private final static HashMap familiarMap = new HashMap();
	private final static HashMap skillMap = new HashMap();
	private final static HashMap clanMap = new HashMap();

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
			RequestThread.postRequest( new TrendyRequest() );
		}
	}

	private static HashMap typeToMap( final String type )
	{
		return	type.equals( "Items" ) ? TrendyRequest.itemMap :
			type.equals( "Campground" ) ? TrendyRequest.campgroundMap :
			type.equals( "Bookshelf" ) ? TrendyRequest.bookshelfMap :
			type.equals( "Familiars" ) ? TrendyRequest.familiarMap :
			type.equals( "Skills" ) ? TrendyRequest.skillMap :
			type.equals( "Clan Item" ) ? TrendyRequest.clanMap :
			null;
	}

	private static boolean isTrendy( final HashMap map, final String key )
	{
		TrendyRequest.initialize();
		Boolean value = (Boolean)map.get( key.toLowerCase() );
		return value == null || value.booleanValue();
	}

	public static boolean isTrendy( final String type, final String key )
	{
		HashMap map = TrendyRequest.typeToMap( type );
		return map != null && TrendyRequest.isTrendy( map, key );
	}

	public TrendyRequest()
	{
		super( "typeii.php" );
	}

	protected boolean retryOnTimeout()
	{
		return true;
	}

	public void run()
	{
		KoLmafia.updateDisplay( "Seeing what's still trendy today..." );
		super.run();
	}

	public void processResults()
	{
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
			HashMap map = TrendyRequest. typeToMap( type );
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
				map.put( object, new Boolean( available ) );
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

		// We don't really need to register this in the gCLI or the session log
		return true;
	}
}
