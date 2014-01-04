/**
 * Copyright (c) 2005-2014, KoLmafia development team
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
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PyramidRequest
	extends GenericRequest
{
	private static final Pattern IMAGE_PATTERN = Pattern.compile( "http://images.kingdomofloathing.com/otherimages/pyramid/pyramid4_([\\d,]+)(b)?.gif" );
	private static final Pattern WHEEL_PATTERN = Pattern
		.compile( "http://images.kingdomofloathing.com/otherimages/pyramid/pyramid3(a|b).gif" );
	private static Boolean pyramidWheelPlaced = null;
	private static final PyramidRequest PYRAMID = new PyramidRequest();

	public PyramidRequest()
	{
		this( false );
	}

	public PyramidRequest( boolean lower )
	{
		super( "pyramid.php");

		if ( lower )
		{
			this.addFormField( "action", "lower" );
		}
	}

	@Override
	public void processResults()
	{
		if ( !this.getURLString().startsWith( "pyramid.php" ) )
		{
			return;
		}

		PyramidRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		// You enter the first chamber, and find it empty. You continue
		// down a damp, slightly curved corridor, and find rubble piled
		// all the way ito the ceiling in the second chamber. You spend
		// some time trying to clear it away, but you don't really make
		// any progress.

		if ( responseText.indexOf( "spend some time trying to clear it away" ) != -1 )
		{
			PyramidRequest.setPyramidPosition( 1 );
			return;
		}

		// You head through the first chamber, down the curved
		// corridor, to the pile of rubble. You take the ancient bomb,
		// light it, and place it at the bottom of the pile.
		//
		// You run back to the first chamber, fingers in your ears, and
		// wait until the bomb goes off.
		//
		// When you return to the far chamber, you see an opening in
		// the ceiling that looks just low enough for you to reach.

		if ( responseText.indexOf( "you see an opening in the ceiling" ) != -1 )
		{
			PyramidRequest.setPyramidPosition( 1 );
			PyramidRequest.setPyramidBombUsed( true );
			ResultProcessor.processItem( ItemPool.ANCIENT_BOMB, -1 );
			return;
		}

		// You enter the first chamber -- the walls are riddle with
		// small holes, and you hear rustlings and twitterings from
		// within them. Nervously, you proceed down a damp, curved
		// corridor to the second room, where you find a stone basket
		// suspended from the ceiling by a rusty chain.

		if ( responseText.indexOf( "where you find a stone basket suspended from the ceiling" ) != -1 )
		{
			PyramidRequest.setPyramidPosition( 2 );
			
			// You look into the basket, and see that it's filled
			// with bronze coins! You help yourself to one, but no
			// sooner have you grasped it than a rat runs in from
			// the other chamber, knocks you out, and steals it
			// from you. Dangit!

			// What happens if you do this and already have a token
			// or a bomb?

			return;
		}

		// You enter the first chamber, and find it empty save for a
		// pile of rubble reaching all the way to the ceiling.

		if ( responseText.indexOf( "pile of rubble reaching all the way to the ceiling" ) != -1 )
		{
			PyramidRequest.setPyramidPosition( 3 );

			// You skirt the pile, and head down a damp, curving
			// corridor into the second chamber. It contains what
			// appears to be a vending machine full of bombs.

			// It only takes tokens, though, and you don't have
			// any. Tough luck.

			if ( responseText.indexOf( "It only takes tokens" ) != -1 )
			{
			}

			// You fish out your bronze token, drop it into the
			// slot, and collect your bomb. Score!

			else if ( responseText.indexOf( "collect your bomb" ) != -1 )
			{
				ResultProcessor.processItem( ItemPool.ANCIENT_BRONZE_TOKEN, -1 );
			}

			// You take the ancient bomb, light it, and place it at
			// the bottom of the pile. You run into the second
			// chamber and hide behind the vending machine until
			// the bomb goes off. When you return to the first
			// chamber, however, the pile of rubble is just as big
			// as it always was. More rubble must've poured in from
			// the ceiling after you blew up the first pile.

			else if ( responseText.indexOf( "hide behind the vending machine" ) != -1 )
			{
				ResultProcessor.processItem( ItemPool.ANCIENT_BOMB, -1 );
			}

			return;
		}

		// You enter the first chamber, and find a stone basket of
		// tokens hanging from the ceiling.

		if ( responseText.indexOf( "find a stone basket of tokens" ) != -1 )
		{
			PyramidRequest.setPyramidPosition( 4 );

			// If you have a bomb, you lose it and get a token
			if (InventoryManager.hasItem( ItemPool.ANCIENT_BOMB ) )
			{
				ResultProcessor.processItem( ItemPool.ANCIENT_BOMB, -1 );
			}

			return;
		}

		// You enter the first chamber, and see what appears to be a
		// vending machine full of bombs.

		if ( responseText.indexOf( "see what appears to be a vending machine full of bombs" ) != -1 )
		{
			PyramidRequest.setPyramidPosition( 5 );

			// You fish your bronze token out of your pocket, and
			// drop it into the slot.

			if ( responseText.indexOf( "You fish your bronze token out of your pocket, and drop it into the slot" ) != -1 )
			{
				ResultProcessor.processItem( ItemPool.ANCIENT_BRONZE_TOKEN, -1 );
			}

			return;
		}

                // If we got here, we might just be visiting the pyramid.

		// Check whether the wheel is placed based on the Middle Chamber image
		Matcher wheelMatcher = PyramidRequest.WHEEL_PATTERN.matcher( responseText );
		if ( !wheelMatcher.find() )
		{
			return;
		}
		String wheel = wheelMatcher.group( 1 );
		PyramidRequest.setPyramidWheelPlaced( wheel.equals( "b" ) );

		Matcher matcher = PyramidRequest.IMAGE_PATTERN.matcher( responseText );
		if ( !matcher.find() )
		{
			return;
		}

		int position = StringUtilities.parseInt( matcher.group(1) );
		PyramidRequest.setPyramidPosition( position );
		if ( position == 1 )
		{
			PyramidRequest.setPyramidBombUsed( matcher.group(2) != null );
		}
		else
		{
			// It is impossible for the bomb to have been used if the wheel is in another position.
			PyramidRequest.setPyramidBombUsed( false );
		}

		return;
	}

	public static final String getPyramidLocationString( final String urlString )
	{
		if ( !urlString.startsWith( "pyramid.php" ) ||
                     urlString.indexOf( "action=lower" ) == -1 )
		{
			return null;
		}

                String position = PyramidRequest.getPyramidPositionString();
		return "The Lower Chambers (" + position + ")";
	}

	public static final void decorateChoice( final int choice, final StringBuffer buffer )
	{
		// Handle only the two Wheel in the Pyramid choice adventures
		if ( choice != 134 && choice != 135 )
		{
			return;
		}

		// Choice #134 occurs when you first put the wheel on the
		// post. Therefore, we know that the pyramid is in position 1
		// and the bomb has not been used.

		if ( choice == 134 )
		{
			PyramidRequest.setPyramidPosition( 1 );
			PyramidRequest.setPyramidBombUsed( false );
		}

		// Get the current pyramid position. If we somehow got here
		// without navigating through the pyramid container, we might
		// not know. Do nothing, in that case.
		int position = PyramidRequest.getPyramidPosition();

		if (position == 0 )
		{
			return;
		}

		// Get the image of the pyramid and insert it
		StringUtilities.insertBefore( buffer, "</table></center></body>", PyramidRequest.pyramidHTML( false ) );
	}

	public static final void decorateChoiceResponse( final StringBuffer buffer )
	{
                // Make sure we know the current pyramid position
		if ( PyramidRequest.getPyramidPosition() == 0 )
		{
			RequestThread.postRequest( PyramidRequest.PYRAMID );
		}

		// Replace Adventure Again section with image of pyramid

		StringUtilities.singleStringReplace( buffer,
						     "<table><tr><td><center><p><a href=\"adventure.php?snarfblat=125\">Adventure Again (The Middle Chamber)</a><p><a href=\"pyramid.php\">Go back to The Ancient Buried Pyramid</a></center></center></td></tr>",
						     PyramidRequest.pyramidHTML( true ) );

	}

	public static final String getPyramidHTML()
	{
		// Make sure we know the current pyramid position
		if ( PyramidRequest.getPyramidPosition() == 0 )
		{
			RequestThread.postRequest( PyramidRequest.PYRAMID );
		}

		return PyramidRequest.pyramidHTML( true );
	}

	private static final String pyramidHTML( final boolean link )
	{
		StringBuffer buffer = new StringBuffer();

		// Start the table
		buffer.append( "<table cellpadding=0 cellspacing=0>" );

		// First row: the desert
		buffer.append( "<tr><td width=500 height=82>" );

		// Make a link, if requested.
		if ( link )
		{
			buffer.append( "<a href=\"beach.php\">" );
		}

		buffer.append( "<img src=\"http://images.kingdomofloathing.com/otherimages/pyramid/pyramid1.gif\" width=500 height=82 border=0>" );

		// End link, if requested.
		if ( link )
		{
			buffer.append( "</a>" );
		}

		buffer.append( "</td></tr>" );

		// Second row: the upper chamber
		buffer.append( "<tr><td width=500 height=111>" );

		// Make a link, if requested.
		if ( link )
		{
			buffer.append( "<a href=\"adventure.php?snarfblat=124\">" );
		}

		buffer.append( "<img src=\"http://images.kingdomofloathing.com/otherimages/pyramid/pyramid2.gif\" width=500 height=111 border=0>" );

		// End link, if requested.
		if ( link )
		{
			buffer.append( "</a>" );
		}

		buffer.append( "</td></tr>" );

		// Third row: the middle chamber
		buffer.append( "<tr><td width=500 height=84>" );

		// Make a link, if requested.
		if ( link )
		{
			buffer.append( "<a href=\"adventure.php?snarfblat=125\">" );
		}

		buffer.append( "<img src=\"http://images.kingdomofloathing.com/otherimages/pyramid/pyramid3b.gif\" width=500 height=84 border=0>" );

		// End link, if requested.
		if ( link )
		{
			buffer.append( "</a>" );
		}

		buffer.append( "</td></tr>" );

		// Fourth row: the lower chamber
		buffer.append( "<tr><td width=500 height=137>" );

		// Make a link, if requested.
		if ( link )
		{
			buffer.append( "<a href=\"pyramid.php?action=lower\">" );
		}

		// Show the current position of the lower chamber
		buffer.append( "<img src=\"http://images.kingdomofloathing.com/otherimages/pyramid/pyramid4_" );

		int position = Preferences.getInteger( "pyramidPosition" );
		buffer.append( String.valueOf( position ) );
		if ( position == 1 && Preferences.getBoolean( "pyramidBombUsed" ) )
		{
			buffer.append( "b" );
		}

		buffer.append (".gif\" width=500 height=137 border=0>" );

		// End link, if requested.
		if ( link )
		{
			buffer.append( "</a>" );
		}

		// End table
		buffer.append( "</td></tr></table>" );

		// Return as string
		return buffer.toString();
	}

	public static final void ensureUpdatedPyramid()
	{
		int lastAscension = Preferences.getInteger( "lastPyramidReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			Preferences.setInteger( "lastPyramidReset", KoLCharacter.getAscensions() );

			Preferences.setInteger( "pyramidPosition", 0 );
			Preferences.setBoolean( "pyramidBombUsed", false );
		}
	}

	public static final int getPyramidPosition()
	{
		PyramidRequest.ensureUpdatedPyramid();
		return Preferences.getInteger( "pyramidPosition" );
	}

	public static final String getPyramidPositionString()
	{
		switch ( PyramidRequest.getPyramidPosition() )
		{
		case 1:
			return PyramidRequest.getPyramidBombUsed() ?
				"Empty/Rubble": "Empty/Empty/Ed's Chamber";
		case 2:
			return "Rats/Token";
		case 3:
			return "Rubble/Bomb";
		case 4:
			return "Token/Empty";
		case 5:
			return "Bomb/Rats";
		}

		return "Unknown";
	}

	public static final void setPyramidPosition( int position )
	{
		PyramidRequest.ensureUpdatedPyramid();
		Preferences.setInteger( "pyramidPosition", position );
	}

	public static final int advancePyramidPosition()
	{
		// Since using the Middle Chamber to advance the pyramid position always places
		// the wheel first, the first two cases only apply to using a tomb ratchet
		if ( PyramidRequest.pyramidWheelPlaced == null )
		{
			RequestThread.postRequest( PyramidRequest.PYRAMID );
			return PyramidRequest.getPyramidPosition();
		}
		if ( !PyramidRequest.pyramidWheelPlaced )
		{
			PyramidRequest.setPyramidWheelPlaced();
			return PyramidRequest.getPyramidPosition();
		}
		int position = PyramidRequest.getPyramidPosition();
		if ( ++position > 5 )
		{
			position = 1;
		}
		Preferences.setInteger( "pyramidPosition", position );
		return position;
	}

	public static final boolean getPyramidBombUsed()
	{
		PyramidRequest.ensureUpdatedPyramid();
		return Preferences.getBoolean( "pyramidBombUsed" );
	}

	public static final void setPyramidBombUsed( boolean used )
	{
		PyramidRequest.ensureUpdatedPyramid();
		Preferences.setBoolean( "pyramidBombUsed", used );
	}

	public static final void setPyramidWheelPlaced()
	{
		PyramidRequest.setPyramidWheelPlaced( true );
	}

	private static final void setPyramidWheelPlaced( boolean wheelPlaced )
	{
		PyramidRequest.pyramidWheelPlaced = Boolean.valueOf( wheelPlaced );
	}
}
