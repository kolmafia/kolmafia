/*
 * Copyright (c) 2005-2021, KoLmafia development team
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

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.GenericRequest;

public class EudoraCommand
	extends AbstractCommand
{
	public enum Correspondent
	{
		NONE( 0, "None", "none" ),
		PENPAL( 1, "Pen Pal", "My Own Pen Pal kit" ),
		GAME( 2, "GameInformPowerDailyPro Magazine", "GameInformPowerDailyPro subscription card" ),
		XI( 3, "Xi Receiver Unit" , "Xi Receiver Unit" ),
		NEWYOU( 4, "New-You Club", "New-You Club Membership Form" );

		private final int id;
		private final String name;
		private final String item;

		public static Correspondent find( final int id )
		{
			for ( Correspondent correspondent : Correspondent.values() )
			{
				if ( correspondent.getId() == id )
				{
					return correspondent;
				}
			}
			return Correspondent.NONE;
		}

		public static Correspondent find( final String query )
		{
			for ( Correspondent correspondent : Correspondent.values() )
			{
				String name = correspondent.getName();
				if ( name.contains( query ) || name.replaceAll( "[- ]", "" ).contains( query ) )
				{
					return correspondent;
				}
			}
			return Correspondent.NONE;
		}

		public static Correspondent findByItem( final String query )
		{
			for ( Correspondent correspondent : Correspondent.values() )
			{
				if ( correspondent.getItem().contains( query ) )
				{
					return correspondent;
				}
			}
			return Correspondent.NONE;
		}

		Correspondent( int id, String name, String item )
		{
			this.id = id;
			this.name = name;
			this.item = item;
		}

		public int getId()
		{
			return this.id;
		}
		public String getName() { return this.name; }
		public String getItem() { return this.item; }
		public String getSlug()
		{
			if ( !this.name.contains( " " ) )
			{
				return this.name;
			}

			return this.name.substring(0, this.name.indexOf( " " ) );
		}
	}

	public EudoraCommand()
	{
		this.usage = " penpal|game|xi|newyou - switch to the specified correspondent";
	}

	public static boolean switchTo( final Correspondent correspondent )
	{
		if ( correspondent == Correspondent.NONE )
		{
			return false;
		}

		String requestString = "account.php?am=1&action=whichpenpal&ajax=1&pwd=" +
				GenericRequest.passwordHash + "&value=";

		GenericRequest request = new GenericRequest( requestString + correspondent.getId() );
		request.run();

		if ( KoLCharacter.getEudora() == correspondent )
		{
			KoLmafia.updateDisplay( "Switched to " + correspondent.getName() );
			return true;
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Cannot switch to" + correspondent.getName() );
			return false;
		}
	}

	public static boolean switchTo( final String query )
	{
		Correspondent correspondent = Correspondent.find( query );
		return switchTo( correspondent );
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		parameters = parameters.trim();
		if ( parameters.length() == 0 )
		{
			KoLmafia.updateDisplay( "Current correspondent is " + KoLCharacter.getEudora().getName() );
		}
		else if ( !switchTo( parameters) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "That is not a valid correspondent" );
		}
	}
}
