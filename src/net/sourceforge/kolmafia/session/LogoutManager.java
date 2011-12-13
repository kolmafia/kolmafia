/**
 * Copyright (c) 2005-2011, KoLmafia development team
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

package net.sourceforge.kolmafia.session;

import java.awt.Frame;

import net.sourceforge.kolmafia.BuffBotHome;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLDesktop;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.LogoutRequest;
import net.sourceforge.kolmafia.swingui.GenericFrame;
import net.sourceforge.kolmafia.swingui.LoginFrame;

public class LogoutManager
{
	public static void logout()
	{
		// Create login frame to ensure that there is an active frame.

		if ( StaticEntity.getClient() instanceof KoLmafiaGUI )
		{
			GenericFrame.createDisplay( LoginFrame.class );
		}

		KoLmafia.updateDisplay( "Preparing for logout..." );

		// Shut down main frame

		if ( KoLDesktop.instanceExists() )
		{
			KoLDesktop.getInstance().dispose();
		}

		// Close down any other active frames.	Since
		// there is at least one active, logout will
		// not be called again.

		Frame[] frames = Frame.getFrames();

		for ( int i = 0; i < frames.length; ++i )
		{
			if ( frames[ i ].getClass() != LoginFrame.class )
			{
				frames[ i ].dispose();
			}
		}

		// Shut down chat-related activity

		BuffBotHome.setBuffBotActive( false );
		ChatManager.dispose();

		// Run on-logout scripts

		String scriptSetting = Preferences.getString( "logoutScript" );
		if ( !scriptSetting.equals( "" ) )
		{
			KoLmafia.updateDisplay( "Executing logout script..." );
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( scriptSetting );
		}

		if ( Preferences.getBoolean( "sharePriceData" ) )
		{
			KoLmafia.updateDisplay( "Sharing mall price data with other users..." );
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "spade prices http://kolmafia.us/scripts/updateprices.php" );
		}

		// Clear out user data

		RequestLogger.closeSessionLog();
		RequestLogger.closeDebugLog();
		RequestLogger.closeMirror();

		GenericRequest.reset();
		KoLCharacter.reset( "" );

		// Execute the logout request

		RequestThread.postRequest( new LogoutRequest() );

		KoLmafia.updateDisplay( "Logout completed." );
	}
}
