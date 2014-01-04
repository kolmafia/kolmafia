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

package net.sourceforge.kolmafia.swingui.panel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.moods.MoodManager;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.UneffectRequest;

import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;

public class StatusEffectPanel
	extends ScrollablePanel
{
	private final ShowDescriptionList elementList;

	public StatusEffectPanel()
	{
		this(new ShowDescriptionList( KoLConstants.activeEffects ) );
	}

	public StatusEffectPanel( final ShowDescriptionList list )
	{
		super( "Active Effects", "uneffect", "add to mood", list );
		this.elementList = (ShowDescriptionList) this.scrollComponent;
	}

	@Override
	public void actionConfirmed()
	{
		Object[] effects = this.elementList.getSelectedValues();
		for ( int i = 0; i < effects.length; ++i )
		{
			RequestThread.postRequest( new UneffectRequest( (AdventureResult) effects[ i ] ) );
		}
	}

	@Override
	public void actionCancelled()
	{
		Object[] effects = this.elementList.getSelectedValues();
		this.elementList.clearSelection();

		if ( Preferences.getString( "currentMood" ).equals( "apathetic" ) )
		{
			MoodManager.setMood( "default" );
		}

		int count = 0;

		for ( int i = 0; i < effects.length; ++i )
		{
			String name = ( (AdventureResult) effects[ i ] ).getName();
			String action = MoodManager.getDefaultAction( "lose_effect", name );
			if ( !action.equals( "" ) )
			{
				MoodManager.addTrigger( "lose_effect", name, action );
				count++;
				continue;
			}

			action = MoodManager.getDefaultAction( "gain_effect", name );
			if ( !action.equals( "" ) )
			{
				MoodManager.addTrigger( "gain_effect", name, action );
				count++;
			}
		}

		if ( count > 0 )
		{
			MoodManager.saveSettings();
		}
	}
}
