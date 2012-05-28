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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CreateItemRequest;

public class BangPotionsCommand
	extends AbstractCommand
{
	public BangPotionsCommand()
	{
		this.usage = " - list the potions you've identified.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		int first = 819, last = 827;
		int chopl = 0, chopr = 7;
		String pref = "lastBangPotion";
		if ( cmd.startsWith( "v" ) )
		{
			first = ItemPool.VIAL_OF_RED_SLIME;
			last = ItemPool.VIAL_OF_PURPLE_SLIME;
			chopl = 8;
			chopr = 6;
			pref = "lastSlimeVial";
		}
		
		for ( int i = first; i <= last; ++i )
		{
			String potion = ItemDatabase.getItemName( i );
			potion = potion.substring( chopl, potion.length() - chopr );
			StringBuffer buf = new StringBuffer( potion );
			buf.append( ": " );
			buf.append( Preferences.getString( pref + i ) );
			AdventureResult item = ItemPool.get( i, 1 );
			int have = item.getCount( KoLConstants.inventory );
			int closet = item.getCount( KoLConstants.closet );
			CreateItemRequest creator = CreateItemRequest.getInstance( item );
			int create = creator == null ? 0 : creator.getQuantityPossible();
			if ( have + closet + create > 0 )
			{
				buf.append( " (have " );
				buf.append( have );
				if ( closet > 0 )
				{
					buf.append( ", " );
					buf.append( closet );
					buf.append( " in closet" );
				}
				if ( create > 0 )
				{
					buf.append( ", can make " );
					buf.append( create );
				}
				buf.append( ")" );
			
			}
			RequestLogger.printLine( buf.toString() );
		}
	}
}
