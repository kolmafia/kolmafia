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

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class BackupCameraCommand
	extends AbstractCommand
{
	public BackupCameraCommand()
	{
		this.usage = " [ml | meat | init | (reverser on | off )] - set your backup camera mode";
	}

	public static final String[][] MODE = { { "ml", "1" },  { "meat", "2" }, { "init", "3" } };

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( !InventoryManager.hasItem( ItemPool.BACKUP_CAMERA ) )
		{
			KoLmafia.updateDisplay( "You need a backup camera first." );
			return;
		}

		int choice = 0;

		for ( String[] mode : MODE )
		{
			if ( parameters.contains( mode[0] ) )
			{
				choice = Integer.parseInt( mode[1] );
			}
		}

		if ( choice == 0 && parameters.contains( "reverse" ) )
		{
			if ( parameters.contains( "off" ) || parameters.contains( "disable" ) )
			{
				choice = 5;
			}
			else
			{
				choice = 4;
			}
		}

		RequestThread.postRequest( new GenericRequest( "inventory.php?action=bcmode" ) );
		RequestThread.postRequest( new GenericRequest( "choice.php?whichchoice=1449&option=" + choice ) );
	}
}
