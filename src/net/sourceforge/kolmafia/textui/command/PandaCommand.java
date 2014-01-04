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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.ItemFinder;

import net.sourceforge.kolmafia.request.PandamoniumRequest;

public class PandaCommand
	extends AbstractCommand
{
	private static final Pattern COMMAND_PATTERN1 = Pattern.compile( "^\\s*(moan|temple)\\s*$" );
	private static final Pattern COMMAND_PATTERN2 = Pattern.compile( "^\\s*comedy\\s*([^\\s]+)\\s*$" );
	private static final Pattern COMMAND_PATTERN3 = Pattern.compile( "^\\s*arena\\s*([^\\s]+)\\s*(.+)\\s*$" );

	public PandaCommand()
	{
		this.usage = " moan | temple | comedy <type> | arena <bandmember> <item> - interact with NPCs in Pandamonium";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		parameters = parameters.trim();
		if ( parameters.equals( "" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "What do you want to do in Pandamonium?" );
			return;
		}

		PandamoniumRequest request = null;
		Matcher m = COMMAND_PATTERN1.matcher( parameters );
		if ( m.find() )
		{
			// Visit a place in Pandamonium
			String location = m.group(1);
			int place = 0;
			if ( location.equalsIgnoreCase( "moan" ) )
			{
				place = PandamoniumRequest.MOAN;
			}
			else if ( location.equalsIgnoreCase( "temple" ) )
			{
				place = PandamoniumRequest.TEMPLE;
			}
			request = new PandamoniumRequest( place );
		}

		m =  COMMAND_PATTERN2.matcher( parameters );
		if ( request == null && m.find() )
		{
			// Attempt comedy on Mourn in the comedy club
			String type = m.group(1);
			String comedy = PandamoniumRequest.getComedyType( type );
			if ( comedy == null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "What kind of comedy is \"" + type + "\"?" );
				return;
			}
			request = new PandamoniumRequest( type );
		}

		m =  COMMAND_PATTERN3.matcher( parameters );
		if ( request == null && m.find() )
		{
			// Give an item to a bandmember
			String demon = m.group(1);
			String member = PandamoniumRequest.getBandMember( demon );
			if ( member == null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "I don't think \"" +	demon + "\" is a member of the band." );
				return;
			}

			String itemName = m.group(2);
			AdventureResult item = ItemFinder.getFirstMatchingItem( itemName, ItemFinder.ANY_MATCH );
			if ( item == null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "WHAT did you want to give to " + member + "?" );
				return;
			}
			request = new PandamoniumRequest( member, item.getItemId() );
		}

		if ( request == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "What do you want to do in Pandamonium?" );
			return;
		}

		RequestThread.postRequest( request );
	}
}
