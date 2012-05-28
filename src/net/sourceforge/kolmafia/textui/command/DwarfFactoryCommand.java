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
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.ItemFinder;

import net.sourceforge.kolmafia.request.DwarfContraptionRequest;
import net.sourceforge.kolmafia.request.DwarfFactoryRequest;

public class DwarfFactoryCommand
	extends AbstractCommand
{
	public DwarfFactoryCommand()
	{
		this.usage = " report <digits> - Given a string of 7 dwarven digits, report on factory.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		String[] tokens = parameters.split( "\\s+" );
		if ( tokens.length < 1 )
		{
			return;
		}

		String option = tokens[ 0 ];

		if ( option.equals( "vacuum" ) )
		{
			String itemString = parameters.substring( 6 ).trim();
			AdventureResult item = ItemFinder.getFirstMatchingItem( itemString, ItemFinder.ANY_MATCH, true );
			if ( item == null )
			{
				return;
			}

			DwarfContraptionRequest request = new DwarfContraptionRequest( "dochamber" );
			request.addFormField( "howmany", String.valueOf( item.getCount() ) );
			request.addFormField( "whichitem", String.valueOf( item.getItemId() ) );
			RequestThread.postRequest( request );

			return;
		}

		if ( option.equals( "check" ) )
		{
			DwarfFactoryRequest.check( false );
			return;
		}

		if ( option.equals( "report" ) )
		{
			if ( tokens.length >= 2 )
			{
				String digits = tokens[ 1 ].trim().toUpperCase();
				DwarfFactoryRequest.report( digits );
			}
			else
			{
				DwarfFactoryRequest.report();
			}
			return;
		}

		if ( option.equals( "setdigits" ) )
		{
			String digits = "";
			if ( tokens.length >= 2 )
			{
				digits = tokens[ 1 ].trim().toUpperCase();
			}

			if ( digits.length() != 7 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Must supply a 7 character digit string" );
				return;
			}
			DwarfFactoryRequest.setDigits( digits );
			return;
		}

		if ( option.equals( "solve" ) )
		{
			DwarfFactoryRequest.solve();
			return;
		}
	}
}
