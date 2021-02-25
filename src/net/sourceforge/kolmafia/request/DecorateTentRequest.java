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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

public class DecorateTentRequest
	extends GenericRequest
{
	public DecorateTentRequest()
	{
		super( "choice.php" );
	}

	public static final void parseDecoration( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichchoice=1392" ) )
		{
			return;
		}

		int decision = ChoiceManager.extractOptionFromURL( urlString );
		switch ( decision )
		{
		case 1:
			// Muscular Intentions
			//
			// You burn some wood into charcoal, and use it to draw camouflage patterns all over your tent.
			// Like a soldier or a tough hunting guy! Or like a guy who wants to seem like a soldier or a
			// tough hunting guy!
			if ( !responseText.contains( "camouflage patterns" ) )
			{
				return;
			}
			break;
		case 2:
			// Mystical Intentions
			//
			// You burn some wood into charcoal, and use it to draw magical symbols all over your tent.
			// Many of them are just squiggles that you improvise on the spot, but in your experience
			// most magical symbols are exactly that.
			if ( !responseText.contains( "magical symbols" ) )
			{
				return;
			}
			break;
		case 3:
			// Moxious Intentions
			//
			// You burn some wood into charcoal, and use it to draw a sweet skull and crossbones on the
			// side of your tent. Since bears don't understand this iconography, you'll easily be able to
			// sneak away from them while they're puzzling it out.
			if ( !responseText.contains( "sweet skull and crossbones" ) )
			{
				return;
			}
			break;
		case 4:
			return;
		}

		ResultProcessor.processItem( ItemPool.BURNT_STICK, -1 );

		Preferences.setInteger( "campAwayDecoration", decision );
	}
}
