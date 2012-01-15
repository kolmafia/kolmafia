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

package net.sourceforge.kolmafia.utilities;

import javax.swing.SwingUtilities;

import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;

public class PauseObject implements Runnable
{
	private long milliseconds = 0;

	public void run()
	{
		this.pause( this.milliseconds );
	}

	public void pause()
	{
		try
		{
			synchronized ( this )
			{
				this.wait();
			}
		}
		catch ( InterruptedException e )
		{
			// We expect this to happen only when we are
			// interrupted.  Fall through.
		}
	}

	public void pause( long milliseconds )
	{
		if ( milliseconds <= 0 )
		{
			return;
		}

		if ( SwingUtilities.isEventDispatchThread() )
		{
			if ( Preferences.getBoolean( "debugFoxtrotRemoval" ) )
			{
				StaticEntity.printStackTrace( "Pause object in event dispatch thread" );
			}

			return;
		}

		this.milliseconds = milliseconds;

		try
		{
			synchronized ( this )
			{
				this.wait( milliseconds );
			}
		}
		catch ( InterruptedException e )
		{
			// We expect this to happen only when we are
			// interrupted.  Fall through.
		}
	}

	public void unpause()
	{
		synchronized ( this )
		{
			this.notifyAll();
		}
	}
}
