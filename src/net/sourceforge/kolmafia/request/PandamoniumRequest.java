/**
 * Copyright (c) 2005-2010, KoLmafia development team
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PandamoniumRequest
	extends GenericRequest
{
	private static final Pattern MEMBER_PATTERN = Pattern.compile( "bandmember=([^&]*)" );
	private static final Pattern ITEM_PATTERN = Pattern.compile( "togive=(\\d*)" );

	public PandamoniumRequest()
	{
		super( "pandamonium.php" );
	}

	public static String actionToPlace( final String action )
	{

		return null;
	}

	private static String subvisitPlace( final String action, final String urlString )
	{
		if ( action.equals( "mourn" ) )
		{
			if ( urlString.indexOf( "preaction=insult" ) != -1 )
			{
				return "Trying to insult Mourn";
			}

			if ( urlString.indexOf( "preaction=observe" ) != -1 )
			{
				return "Trying some observational humor on Mourn";
			}

			if ( urlString.indexOf( "preaction=prop" ) != -1 )
			{
				return "Trying some prop comedy on Mourn";
			}

			return null;
		}

		if ( action.equals( "sven" ) )
		{
			if ( urlString.indexOf( "preaction=try" ) == -1 )
			{
				return null;
			}

			Matcher m = PandamoniumRequest.MEMBER_PATTERN.matcher( urlString );
			if ( !m.find() )
			{
				return null;
			}

			String bandmember =  m.group(1);

			m = PandamoniumRequest.ITEM_PATTERN.matcher( urlString );
			if ( !m.find() )
			{
				return null;
			}

			int itemId = StringUtilities.parseInt( m.group(1) );
			String itemName = ItemDatabase.getItemName( itemId );

			return "Giving " + itemName + " to " + bandmember;
		}

		return null;
	}

	private static String visitPlace( final String action, final String urlString )
	{
		if ( action.equals( "moan" ) )
		{
			return "Visiting Moaning Panda Square in Pandamonium";
		}

		if ( action.equals( "temp" ) )
		{
			return "Visiting Azazel's Temple in Pandamoneum";
		}

		if ( action.equals( "mourn" ) )
		{
			return "Talking to Mourn at Belilafs Comedy Club";
		}

		if ( action.equals( "sven" ) )
		{
			// pandamonium.php?action=sven&bandmember=Flargwurm&togive=4673&preaction=try
			if ( urlString.indexOf( "preaction=try" ) != -1 )
			{
				return null;
			}

			return "Talking to Sven Golly at the Hey Deze Arena";
		}

		return null;
	}

	public void processResults()
	{
		PandamoniumRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final boolean parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "pandamonium.php" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			return false;
		}

		// pandamonium.php?action=sven&bandmember=Flargwurm&togive=4673&preaction=try
		// When you give an item, it removes it from inventory, whether
		// or not it was the right item.

		// pandamonium.php?action=moan

		// When you bring 5 bus passes and 5 imp airs, they are removed
		// from inventory and you are given Azazel's tutu

		// pandamonium.php?action=mourn&preaction=insult
		// pandamonium.php?action=mourn&preaction=observe
		// pandamonium.php?action=mourn&preaction=prop

		return false;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "pandamonium.php" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );

		// We have nothing special to do for simple visits.
		if ( action == null )
		{
			return true;
		}

		// Container documents
		if ( action.equals( "beli" ) || action.equals( "infe" ) )
		{
			return true;
		}

		String message = PandamoniumRequest.subvisitPlace( action, urlString );

		if ( message != null )
		{
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
			return true;
		}

		message = PandamoniumRequest.visitPlace( action, urlString );

		if ( message == null )
		{
			return false;
		}

		RequestLogger.printLine( "" );
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
