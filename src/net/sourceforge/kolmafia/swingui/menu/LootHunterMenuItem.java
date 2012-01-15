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

package net.sourceforge.kolmafia.swingui.menu;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.BountyHunterHunterRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.swingui.AdventureFrame;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class LootHunterMenuItem
	extends ThreadedMenuItem
{
	public LootHunterMenuItem()
	{
		super( "Visit Bounty Hunter", new LootHunterListener() );
	}

	private static class LootHunterListener
		extends ThreadedListener
	{
		protected void execute()
		{
			GenericRequest hunterRequest = new BountyHunterHunterRequest();
			RequestThread.postRequest( hunterRequest );

			Matcher bountyMatcher = Pattern.compile( "name=whichitem value=(\\d+)" ).matcher( hunterRequest.responseText );

			LockableListModel bounties = new LockableListModel();
			while ( bountyMatcher.find() )
			{
				String item = ItemDatabase.getItemName( StringUtilities.parseInt( bountyMatcher.group( 1 ) ) );
				if ( item == null )
				{
					continue;
				}

				KoLAdventure location = AdventureDatabase.getBountyLocation( item );
				if ( location == null )
				{
					continue;
				}

				bounties.add( item + " (" + location.getAdventureName() + ")" );
			}

			if ( bounties.isEmpty() )
			{
				int bounty = Preferences.getInteger( "currentBountyItem" );
				if ( hunterRequest.responseText.indexOf( "already turned in a Bounty today" ) != -1 )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You've already turned in a bounty today." );
					return;
				}

				if ( bounty > 0 )
				{
					AdventureFrame.updateSelectedAdventure( AdventureDatabase.getBountyLocation( bounty ) );
				}

				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You're already on a bounty hunt." );

				return;
			}

			String selectedValue = (String) InputFieldUtilities.input( "Time to collect bounties!", bounties );
			if ( selectedValue == null )
			{
				return;
			}

			String selection = selectedValue.substring( 0, selectedValue.indexOf( "(" ) - 1 );
			int itemId = ItemDatabase.getItemId( selection );
			RequestThread.postRequest( new BountyHunterHunterRequest( "takebounty", itemId ) );
		}
	}
}
