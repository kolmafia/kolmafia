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

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafiaGUI;

import net.sourceforge.kolmafia.webui.RelayLoader;

public class WindowOpenCommand
	extends AbstractCommand
{
	public WindowOpenCommand()
	{
		this.usage = " - switch to tab or open window";
	}

	public void run( final String command, final String parameters )
	{
		if ( command.equals( "chat" ) )
		{
			KoLmafiaGUI.constructFrame( "ChatManager" );
			return;
		}

		if ( command.equals( "mail" ) )
		{
			KoLmafiaGUI.constructFrame( "MailboxFrame" );
			return;
		}

		if ( command.startsWith( "opt" ) )
		{
			KoLmafiaGUI.constructFrame( "OptionsFrame" );
			return;
		}

		if ( command.equals( "item" ) )
		{
			KoLmafiaGUI.constructFrame( "ItemManageFrame" );
			return;
		}

		if ( command.equals( "gear" ) )
		{
			KoLmafiaGUI.constructFrame( "GearChangeFrame" );
			return;
		}

		if ( command.equals( "radio" ) )
		{
			RelayLoader.openSystemBrowser( "http://209.9.238.5:8794/listen.pls" );
			return;
		}
	}
}
