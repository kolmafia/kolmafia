/**
 * Copyright (c) 2005-2013, KoLmafia development team
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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TaleOfDreadCommand
	extends AbstractCommand
{
	public TaleOfDreadCommand()
	{
		this.usage = " element monster - read the Tale of Dread unlocked by the monster";
	}

	private static final Pattern STORY_PATTERN = Pattern.compile( "<div class=tiny style='position: absolute; top: 55; left: 365; width: 285; height: 485; overflow-y:scroll; '>(.*?)</div>" );

	@Override
	public void run( final String cmd, String parameters )
	{
		String[] split = parameters.trim().split( " " );
		if ( split.length < 2 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Syntax: taleofdread element monster" );
			return;
		}

		int story = 0;

		String element = split[ 0 ];
		String monster = split[ 1 ];

		if ( monster.equals( "bugbear" ) )
		{
			story = 1;
		}
		else if ( monster.equals( "werewolf" ) )
		{
			story = 6;
		}
		else if ( monster.equals( "zombie" ) )
		{
			story = 11;
		}
		else if ( monster.equals( "ghost" ) )
		{
			story = 16;
		}
		else if ( monster.equals( "vampire" ) )
		{
			story = 21;
		}
		else if ( monster.equals( "skeleton" ) )
		{
			story = 26;
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "What kind of dreadful monster is a '" + monster + "'?" );
			return;
		}

		if ( element.equals( "hot" ) )
		{
			story += 0;
		}
		else if ( element.equals( "cold" ) )
		{
			story += 1;
		}
		else if ( element.equals( "spooky" ) )
		{
			story += 2;
		}
		else if ( element.equals( "stench" ) )
		{
			story += 3;
		}
		else if ( element.equals( "sleaze" ) )
		{
			story += 4;
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "What kind of element is '" + element + "'?" );
			return;
		}

		// inv_use.php?pwd&which=3&whichitem=6423&ajax=1
		// -> choice.php?forceoption=0
		// choice.php?whichchoice=767&whichstory=1

		GenericRequest request = new GenericRequest( "inv_use.php?pwd&which=3&whichitem=" + ItemPool.TALES_OF_DREAD + "&ajax=1" );
		RequestThread.postRequest( request );

		if ( !request.responseText.contains( "<b>Tales of Dread</b>" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You don't own the Tales of Dread" );
			return;
		}

		// This will redirect to choice.php
		String storyURL = "choice.php?whichchoice=767&whichstory=" + String.valueOf( story );

		if ( split.length > 2 && split[ 2 ].equals( "redirect" ) )
		{
			// Leave it to the Relay Browser to display the story
			RelayRequest.redirectedCommandURL = "/" + storyURL;
			return;
		}

		// Otherwise, get the story
		request.constructURLString( storyURL );
		RequestThread.postRequest( request );

		// Extract the story from the response.
		Matcher storyMatcher = STORY_PATTERN.matcher( request.responseText );
		if ( !storyMatcher.find() )
		{
			return;
		}

		String tale = storyMatcher.group( 1 );
		StringBuffer buffer = new StringBuffer( tale );
		StringUtilities.globalStringReplace( buffer, "<br>", "" );
		RequestLogger.printLine( buffer.toString() );
	}
}
