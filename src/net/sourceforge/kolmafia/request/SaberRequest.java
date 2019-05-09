/**
 * Copyright (c) 2005-2019, KoLmafia development team
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

import net.sourceforge.kolmafia.preferences.Preferences;

public class SaberRequest
	extends GenericRequest
{
	public SaberRequest()
	{
		super( "choice.php" );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		// choice 5, walk away
		// You'll decide later. Maybe in a sequel.
		if ( urlString.contains( "option=5" ) )
		{
			return;
		}
		// choice 1, 10-15 MP regen
		// You fit the Kaiburr crystal to the end of your saber and feel energy course through you.
		if ( urlString.contains( "option=1" ) && responseText.contains( "Kaiburr crystal" ) )
		{
			Preferences.setInteger( "_saberMod", 1 );
		}

		// choice 2, 20 ML
		// You pry out your boring blue crystal and put in a badass purple crystal.
		else if ( urlString.contains( "option=2" ) && responseText.contains( "blue crystal" ) )
		{
			Preferences.setInteger( "_saberMod", 2 );
		}

		// choice 3, 3 resist all
		// Afraid of falling into some lava, you opt fo[sic] the resistance multiplier. The Force sure works in mysterious ways.
		else if ( urlString.contains( "option=3" ) && responseText.contains( "resistance multiplier" ) )
		{
			Preferences.setInteger( "_saberMod", 3 );
		}

		// choice 4, 10 familiar wt
		// You click the empathy chip in to place and really start to feel for your familiar companions.
		else if ( urlString.contains( "option=4" ) && responseText.contains( "empathy chip" ) )
		{
			Preferences.setInteger( "_saberMod", 4 );
		}
	}
}