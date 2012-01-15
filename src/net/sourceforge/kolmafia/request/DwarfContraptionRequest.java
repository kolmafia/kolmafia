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
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class DwarfContraptionRequest
	extends GenericRequest
{
	public static final Pattern GAUGES_PATTERN = Pattern.compile( "temp0=(\\d*)&temp1=(\\d*)&temp2=(\\d*)&temp3=(\\d*)" );
	public static final Pattern HOPPER_PATTERN = Pattern.compile( "action=dohopper(\\d*).*howmany=(\\d*).*whichore=([^&]*)" );
	public static final Pattern CHAMBER_PATTERN = Pattern.compile( "howmany=(\\d*).*whichitem=([^&]*)" );

	public DwarfContraptionRequest()
	{
		super( "dwarfcontraption.php" );
	}

	public DwarfContraptionRequest( final String action)
	{
		this();
		this.addFormField( "action", action );
	}

	private static String getPlaceName( final String action )
	{
		if ( action.equals( "hopper0" ) )
		{
			return "Hopper #1";
		}

		if ( action.equals( "hopper1" ) )
		{
			return "Hopper #2";
		}

		if ( action.equals( "hopper2" ) )
		{
			return "Hopper #3";
		}

		if ( action.equals( "hopper3" ) )
		{
			return "Hopper #4";
		}

		if ( action.equals( "gauges" ) )
		{
			return "Gauges";
		}

		if ( action.equals( "panelleft" ) )
		{
			return "Left Panel";
		}

		if ( action.equals( "panelright" ) )
		{
			return "Right Panel";
		}

		if ( action.equals( "bin" ) )
		{
			return "Bin";
		}

		if ( action.equals( "chamber" ) )
		{
			return "Vacuum Chamber";
		}

		return null;
	}

	private static String getCommand(  final String action, final String urlString )
	{
		if ( action.equals( "doleftpanel" ) )
		{
			if ( urlString.indexOf( "which1" ) != -1 )
			{
				return "Selecting pants";
			}
			if ( urlString.indexOf( "which2" ) != -1 )
			{
				return "Selecting weapon";
			}
			if ( urlString.indexOf( "which3" ) != -1 )
			{
				return "Selecting helmet";
			}
			return null;
		}

		if ( action.equals( "dorightpanel" ) )
		{
			return "Feeding punchcard into slot";
		}

		if ( action.equals( "doredbutton" ) )
		{
			return "Pushing the red button";
		}

		if ( action.equals( "dogauges" ) )
		{
			Matcher matcher = GAUGES_PATTERN.matcher( urlString );
			if ( matcher.find() )
			{
				return "Setting gauges to " +
					matcher.group(1) + ", " +
					matcher.group(2) + ", " +
					matcher.group(3) + ", " +
					matcher.group(4);
			}

			return null;
		}

		if ( action.startsWith( "dohopper" ) )
		{
			Matcher matcher = HOPPER_PATTERN.matcher( urlString );
			if ( matcher.find() )
			{
				int hopper = StringUtilities.parseInt( matcher.group(1) ) + 1;
				String count = matcher.group(2);
				String ore = DwarfContraptionRequest.oreName( matcher.group(3) );
				if ( urlString.indexOf( "addtake=take" ) != -1 )
				{
					return "Taking " + count + " " + ore + " from hopper #" + hopper;
				}
				else
				{
					return "Adding " + count + " " + ore + " to hopper #" + hopper;
				}
			}

			return null;
		}

		// action=dochamber
		return null;
	}

	private static String oreName( String token )
	{
		return token.equals( "coal" ) ? "lump of coal" : token + " ore";
	}

	public void processResults()
	{
		DwarfContraptionRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "dwarfcontraption.php" ) )
		{
			return;
		}

		Matcher actionMatcher = GenericRequest.ACTION_PATTERN.matcher( urlString );
		String action = null;

		while ( actionMatcher.find() )
		{
			action = actionMatcher.group(1);
		}

		if ( action == null )
		{
			return;
		}

		if ( action.equals( "hopper0" ) )
		{
			DwarfFactoryRequest.setHopperRune( 1, responseText );
			return;
		}

		if ( action.equals( "hopper1" ) )
		{
			DwarfFactoryRequest.setHopperRune( 2, responseText );
			return;
		}

		if ( action.equals( "hopper2" ) )
		{
			DwarfFactoryRequest.setHopperRune( 3, responseText );
			return;
		}

		if ( action.equals( "hopper3" ) )
		{
			DwarfFactoryRequest.setHopperRune( 4, responseText );
			return;
		}

		if ( action.startsWith( "dohopper" ) )
		{
			if ( responseText.indexOf( "You don't have" ) != -1 )
			{
				return;
			}

			// It doesn't seem like that is the right material for this hopper.
			if ( responseText.indexOf( "right material" ) != -1 )
			{
				return;
			}

			Matcher hopperMatcher = HOPPER_PATTERN.matcher( urlString );
			if ( !hopperMatcher.find() )
			{
				return;
			}

			int hopper = StringUtilities.parseInt( hopperMatcher.group(1) ) + 1;

			// Validate the hopper rune and adjust quantity of ore
			// in this hopper.
			DwarfFactoryRequest.setHopperRune( hopper, responseText );

			// Adjust inventory
			int count = StringUtilities.parseInt( hopperMatcher.group(2) );
			AdventureResult ore = ItemPool.get( DwarfContraptionRequest.oreName( hopperMatcher.group(3) ), -count );
			ResultProcessor.processResult( ore );

			// If it accepts this ore, we've identified the ore's
			// rune
			String rune = Preferences.getString( "lastDwarfHopper" + hopper );
			DwarfFactoryRequest.setItemRunes( ore.getItemId(), rune );

			return;
		}

		if ( action.equals( "dorightpanel" ) )
		{
			if ( responseText.indexOf( "You feed the punchcard into the slot" ) != -1 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.DWARVISH_PUNCHCARD, -1 ) );
			}

			return;
		}

		if ( action.startsWith( "doredbutton" ) )
		{
			if ( responseText.indexOf( "something falls into the bin" ) != -1 )
			{
				DwarfFactoryRequest.clearHoppers();
			}
			return;
		}

		if ( action.equals( "dochamber" ) )
		{
			// There's a loud wooooshing noise, then a *ping!* You
			// open the box and discover that nothing much has
			// happened.

			if ( responseText.indexOf( "nothing much has happened" ) != -1 )
			{
				return;
			}

			Matcher itemMatcher = CHAMBER_PATTERN.matcher( urlString );
			if ( !itemMatcher.find() )
			{
				return;
			}

			int count = StringUtilities.parseInt( itemMatcher.group(1) );
			int itemId = StringUtilities.parseInt( itemMatcher.group(2) );
			ResultProcessor.processResult( new AdventureResult( itemId, -count ) );
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "dwarfcontraption.php" ) )
		{
			return false;
		}

		Matcher matcher = GenericRequest.ACTION_PATTERN.matcher( urlString );
		String action = null;

		// The contraption can have URLs with multiple actions. The
		// first is part of the path, and the second is a field
		// submitted via POST.	For example:
		//
		// dwarfcontraption.php?action=panelleft&action=doleftpanel&activatewhich3=%C2%A0%C2%A0%C2%A0%C2%A0
		while ( matcher.find() )
		{
			action = matcher.group(1);
		}

		if ( action == null )
		{
			return true;
		}

		if ( action.equals( "dochamber" ) )
		{
			Matcher itemMatcher = CHAMBER_PATTERN.matcher( urlString );
			if ( !itemMatcher.find() )
			{
				return false;
			}

			int count = StringUtilities.parseInt( itemMatcher.group(1) );
			int itemId = StringUtilities.parseInt( itemMatcher.group(2) );
			AdventureResult item = new AdventureResult( itemId, count );
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "Putting " + item + " into the vacuum chamber." );
			return true;
		}

		String place = getPlaceName( action );

		if ( place != null )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "Visiting " + place + " in the Dwarven Factory Machine Room" );
			return true;
		}

		// Other actions in the Machine Room

		String command = getCommand( action, urlString );
		if ( command != null )
		{
			RequestLogger.updateSessionLog( command );
			return true;
		}

		return false;
	}
}
