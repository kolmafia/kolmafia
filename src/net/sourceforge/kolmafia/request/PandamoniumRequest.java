/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PandamoniumRequest
	extends GenericRequest
{
	private static final Pattern MEMBER_PATTERN = Pattern.compile( "bandmember=([^&]*)" );
	private static final Pattern ITEM_PATTERN = Pattern.compile( "togive=(\\d*)" );

	public static final int MOAN = 1;
	public static final int COMEDY = 2;
	public static final int ARENA = 3;
	public static final int TEMPLE = 4;

	public static final String [] COMEDY_TYPES = new String[]
	{
		"insult",
		"observe",
		"prop",
	};

	public static String getComedyType( final String type )
	{
		for ( int i = 0; i < COMEDY_TYPES.length; ++i )
		{
			String test = COMEDY_TYPES[ i ];
			if ( type.equalsIgnoreCase( test ) )
			{
				return test;
			}
		}
		return null;
	}

	public static final String [][] BAND_MEMBERS = new String[][]
	{
		{
			"Bognort",
			"guitarist",
		},
		{
			"Stinkface",
			"vocalist",
		},
		{
			"Flargwurm",
			"bassist",
		},
		{
			"Jim",
			"drummer",
		},
	};

	public static String getBandMember( final String test )
	{
		for ( int i = 0; i < BAND_MEMBERS.length; ++i )
		{
			String [] member = BAND_MEMBERS[ i ];
			String name = member[0];
			String role = member[1];
			if ( test.equalsIgnoreCase( name ) || test.equalsIgnoreCase( role ) )
			{
				return name;
			}
		}
		return null;
	}

	public PandamoniumRequest()
	{
		super( "pandamonium.php" );
	}

	public PandamoniumRequest( final int where )
	{
		super( "pandamonium.php" );
		String action = null;
		switch (where )
		{
		case MOAN:
			action = "moan";
			break;
		case COMEDY:
			action = "mourn";
			break;
		case ARENA:
			action = "sven";
			break;
		case TEMPLE:
			action = "temp";
			break;
		}

		if ( action != null )
		{
			this.addFormField( "action", action );
		}
	}

	public PandamoniumRequest( final String comedy )
	{
		super( "pandamonium.php" );
		this.addFormField( "action", "mourn" );
		this.addFormField( "preaction", comedy );
	}

	public PandamoniumRequest( final String bandMember, final int itemId )
	{
		super( "pandamonium.php" );
		this.addFormField( "action", "sven" );
		this.addFormField( "bandmember", bandMember );
		this.addFormField( "togive", String.valueOf( itemId ) );
		this.addFormField( "preaction", "try" );
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
			return "Visiting Azazel's Temple in Pandamonium";
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

		if ( action.equals( "temp" ) )
		{
			// We used to remove the Talismans when you acquire a
			// steel item. It is now possible - once - to turn in
			// the items having already done the quest and
			// therefore not get the item again.
			//
			// "I sense that we've been here before, and I have
			// nothing left to give you."
			//
			// Therefore, we'll remove the Talismans by detecting
			// that you're turning them over to Azazel.
			//
			// "Talismans of evil power?" you say, incredulously.
			// You pull out the purple plush unicorn, the bright,
			// rainbow-colored lollipop, and the frilly pink tutu
			// and lay them in front of him.
			if ( responseText.indexOf( "and lay them in front of him" ) != -1 )
			{
				ResultProcessor.processItem( ItemPool.AZAZELS_UNICORN, -1 );
				ResultProcessor.processItem( ItemPool.AZAZELS_LOLLYPOP, -1 );
				ResultProcessor.processItem( ItemPool.AZAZELS_TUTU, -1 );
			}
			return false;
		}

		if ( action.equals( "sven" ) )
		{
			// pandamonium.php?action=sven&bandmember=Flargwurm&togive=4673&preaction=try
			// When you give an item, it removes it from inventory,
			// whether or not it was the right item.
			if ( urlString.indexOf( "preaction=try" ) == -1 )
			{
				return false;
			}

			Matcher m = PandamoniumRequest.MEMBER_PATTERN.matcher( urlString );
			if ( !m.find() )
			{
				return false;
			}

			m = PandamoniumRequest.ITEM_PATTERN.matcher( urlString );
			if ( !m.find() )
			{
				return false;
			}

			int itemId = StringUtilities.parseInt( m.group(1) );

			// Remove item from inventory
			ResultProcessor.processResult( ItemPool.get( itemId, -1 ) );

			return false;
		}

		// pandamonium.php?action=moan
		if ( action.equals( "moan" ) )
		{
			// When you bring 5 bus passes and 5 imp airs, they are
			// removed from inventory and you get Azazel's tutu
			if ( responseText.indexOf( "Here's your talisman" ) != -1 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.IMP_AIR, -5 ) );
				ResultProcessor.processResult( ItemPool.get( ItemPool.BUS_PASS, -5 ) );
			}

			return false;
		}

		if ( action.equals( "mourn" ) )
		{
			int itemId = -1;

			if ( urlString.indexOf( "preaction=insult" ) != -1 &&
			     responseText.indexOf( "Mourn chuckles appreciatively" ) != -1 )
			{
				itemId = ItemPool.INSULT_PUPPET;
			}
			else if ( urlString.indexOf( "preaction=observe" ) != -1  &&
				  responseText.indexOf( "Mourn slaps his knee and bellows laughter" ) != -1 )
			{
				itemId = ItemPool.OBSERVATIONAL_GLASSES;
			}
			else if ( urlString.indexOf( "preaction=prop" ) != -1 &&
				  responseText.indexOf( "Mourn giggles a little" ) != -1 )
			{
				itemId = ItemPool.COMEDY_PROP;
			}

			if ( itemId != -1 )
			{
				// You don't lose it.
				// EquipmentManager.discardEquipment( itemId );
			}

			return false;
		}

		return false;
	}

	public static final void decoratePandamonium( final String url, final StringBuffer buffer )
	{
		if ( !url.startsWith( "pandamonium.php" ) )
		{
			return;
		}

		if ( url.indexOf( "action=sven" ) != -1 )
		{
			PandamoniumRequest.decorateSven( buffer );
		}
	}

	private static final String svenFormStart= "<form name=\"bandcamp\" method=\"post\" action=\"pandamonium.php\">";
	private static final String svenFormEnd = "</form>";

	private static final void decorateSven( final StringBuffer buffer )
	{
		if ( !Preferences.getBoolean( "relayShowSpoilers" ) )
		{
			return;
		}

		int startIndex = buffer.indexOf( svenFormStart );
		if ( startIndex == -1 )
		{
			return;
		}

		int endIndex = buffer.indexOf( svenFormEnd, startIndex );
		if ( endIndex == -1 )
		{
			return;
		}

		// Completely replace the existing form
		StringBuffer form = new StringBuffer();

		form.append( "<form name=bandcamp action='" );
		form.append( "/KoLmafia/parameterizedCommand?cmd=sven&pwd=" );
		form.append( GenericRequest.passwordHash );
		form.append( "' method=post>" );

		form.append( "<table>" );
		if ( buffer.indexOf( "<option>Bognort</option>" ) != -1 )
		{
			PandamoniumRequest.addBandmember( form, "Bognort", ItemPool.GIANT_MARSHMALLOW, ItemPool.GIN_SOAKED_BLOTTER_PAPER );
		}
		if ( buffer.indexOf( "<option>Stinkface</option>" ) != -1 )
		{
			PandamoniumRequest.addBandmember( form, "Stinkface", ItemPool.BEER_SCENTED_TEDDY_BEAR, ItemPool.GIN_SOAKED_BLOTTER_PAPER );
		}
		if ( buffer.indexOf( "<option>Flargwurm</option>" ) != -1 )
		{
			PandamoniumRequest.addBandmember( form, "Flargwurm", ItemPool.BOOZE_SOAKED_CHERRY, ItemPool.SPONGE_CAKE );
		}
		if ( buffer.indexOf( "<option>Jim</option>" ) != -1 )
		{
			PandamoniumRequest.addBandmember( form, "Jim", ItemPool.SPONGE_CAKE, ItemPool.COMFY_PILLOW );
		}
		form.append( "</table>" );

		form.append( "<input class=button type=submit value=\"Give Items\">" );

		// Insert it into the page
		buffer.delete( startIndex, endIndex );
		buffer.insert( startIndex, form );

		PandamoniumRequest.saveSvenResponse( buffer.toString() );
	}

	private static final void addBandmember( final StringBuffer form, final String name, final int item1, final int item2 )
	{
		form.append( "<tr><td> Give " );
		form.append( name );
		form.append( " the </td><td>" );
		form.append( "<select name=" );
		form.append( name );
		form.append( "><option value=0>-- select an item --</option>" );
		PandamoniumRequest.addItem( form, item1 );
		PandamoniumRequest.addItem( form, item2 );
		form.append( "</select>" );
		form.append( "<img src='http://images.kingdomofloathing.com/itemimages/magnify.gif' style='vertical-align: middle; cursor: pointer' onClick='describe(document.bandcamp." );
		form.append( name );
		form.append( ");' title='View Item Description' alt='View Item Description'>" );
		form.append( "</td></tr>" );
	}

	private static final void addItem( final StringBuffer form, final int itemId )
	{
		AdventureResult item = ItemPool.get( itemId, 1 );
		if ( item.getCount( KoLConstants.inventory ) > 0 )
		{
			form.append( "<option value=\"" );
			form.append( String.valueOf( itemId ) );
			form.append( "\" descid=\"" );
			form.append( ItemDatabase.getDescriptionId( itemId ) );
			form.append( "\">" );
			form.append( item.getName() );
			form.append( "</option>" );
		}
	}

	private static String lastResponse = null;

	public static final void saveSvenResponse( final String responseText )
	{
		PandamoniumRequest.lastResponse = responseText;
	}

	private static final Pattern GIVE_PATTERN = Pattern.compile( "([^&=]*)=([^&]*)" );
	public static final void solveSven( final String parameters )
	{
		String response = PandamoniumRequest.lastResponse;

		if ( response == null )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't appear to be talking to Sven" );
			return;
		}

		PandamoniumRequest request = null;

		Matcher matcher = GIVE_PATTERN.matcher( parameters );
		while ( matcher.find() )
		{
			String member = matcher.group( 1 );
			String item = matcher.group( 2 );

			int itemId = -1;
			String itemName = null;
			String value = "";

			if ( StringUtilities.isNumeric( item ) )
			{
				itemId = StringUtilities.parseInt( item );
				itemName = ItemDatabase.getItemName( itemId );
			}
			else
			{
				itemId = ItemDatabase.getItemId( item, 1 );
				itemName = item;
			}

			if ( itemName == null || itemId < 1 )
			{
				continue;
			}

			request = new PandamoniumRequest( member, itemId );
			RequestThread.postRequest( request );
		}

		KoLmafia.updateDisplay( "Items given to bandmembers." );

		if ( request != null && request.responseText != null )
		{
			StringBuffer buffer = new StringBuffer( request.responseText );
			RequestEditorKit.getFeatureRichHTML( request.getURLString(), buffer, true );
			response = buffer.toString();
		}

		RelayRequest.specialCommandResponse = response;
		if ( response.indexOf( "<form" ) == -1 )
		{
			PandamoniumRequest.lastResponse = null;
		}
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
