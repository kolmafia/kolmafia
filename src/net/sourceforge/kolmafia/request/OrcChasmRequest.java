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

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class OrcChasmRequest
	extends GenericRequest
{
	private static final Pattern ACTION_PATTERN = Pattern.compile( "action=bridge([^>]*)" );

	public OrcChasmRequest()
	{
		super( "place.php?whichplace=orc_chasm" );
	}

	@Override
	public void processResults()
	{
		if ( !this.getURLString().startsWith( "place.php?whichplace=orc_chasm" ) )
		{
			return;
		}

		OrcChasmRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		Matcher actionMatcher = OrcChasmRequest.ACTION_PATTERN.matcher( responseText );
		String action = actionMatcher.find() ? actionMatcher.group(1) : null;

		if ( action != null )
		{
			int previous = OrcChasmRequest.getChasmProgress();
			if ( action.equals( "_done" ) )
			{
				OrcChasmRequest.setChasmProgress( 30 );
			}
			else
			{
				OrcChasmRequest.setChasmProgress( StringUtilities.parseInt( action ) );
			}
			int current = OrcChasmRequest.getChasmProgress();
			OrcChasmRequest.subtractBridgeParts( current - previous );
		}

		if ( responseText.contains( "You disassemble it into usable lumber and fasteners." ) )
		{
			ResultProcessor.processItem( ItemPool.BRIDGE, -1 );
		}
		else if ( responseText.contains( "miniature suspension bridge" ) )
		{
			ResultProcessor.removeItem( ItemPool.MINIATURE_SUSPENSION_BRIDGE );
		}
		else if ( responseText.contains( "snow boards" ) )
		{
			ResultProcessor.processItem( ItemPool.SNOW_BOARDS, -1 * InventoryManager.getCount( ItemPool.SNOW_BOARDS ) );
		}
		else if ( responseText.contains( "oil painting" ) )
		{
			ResultProcessor.processItem( ItemPool.FANCY_OIL_PAINTING, -1 * InventoryManager.getCount( ItemPool.FANCY_OIL_PAINTING ) );
		}
	}

	private static final void ensureUpdatedChasm()
	{
		int lastAscension = Preferences.getInteger( "lastChasmReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			Preferences.setInteger( "lastChasmReset", KoLCharacter.getAscensions() );
			
			Preferences.setInteger( "chasmBridgeProgress", 0 );
		}
	}

	public static final int getChasmProgress()
	{
		OrcChasmRequest.ensureUpdatedChasm();
		return Preferences.getInteger( "chasmBridgeProgress" );
	}

	private static final void setChasmProgress( int progress )
	{
		OrcChasmRequest.ensureUpdatedChasm();
		Preferences.setInteger( "chasmBridgeProgress", progress );
	}

	private static final void subtractBridgeParts( final int parts )
	{
		if ( parts == 0 )
		{
			return;
		}
		int remove;
		int lumber = parts;
		//remove morningwood plank
		remove = Math.min( lumber, InventoryManager.getCount( ItemPool.MORNINGWOOD_PLANK ) );
		ResultProcessor.processItem( ItemPool.MORNINGWOOD_PLANK, -remove );
		lumber -= remove;
		//remove raging hardwood plank
		remove = Math.min( lumber, InventoryManager.getCount( ItemPool.HARDWOOD_PLANK ) );
		ResultProcessor.processItem( ItemPool.HARDWOOD_PLANK, -remove );
		lumber -= remove;
		//remove weirdwood plank
		remove = Math.min( lumber, InventoryManager.getCount( ItemPool.WEIRDWOOD_PLANK ) );
		ResultProcessor.processItem( ItemPool.WEIRDWOOD_PLANK, -remove );

		int fastener = parts;
		//remove thick caulk
		remove = Math.min( fastener, InventoryManager.getCount( ItemPool.THICK_CAULK ) );
		ResultProcessor.processItem( ItemPool.THICK_CAULK, -remove );
		fastener -= remove;
		//remove long hard screw
		remove = Math.min( fastener, InventoryManager.getCount( ItemPool.LONG_SCREW ) );
		ResultProcessor.processItem( ItemPool.LONG_SCREW, -remove );
		fastener -= remove;
		//remove messy butt joint
		remove = Math.min( fastener, InventoryManager.getCount( ItemPool.BUTT_JOINT ) );
		ResultProcessor.processItem( ItemPool.BUTT_JOINT, -remove );
	}

}
