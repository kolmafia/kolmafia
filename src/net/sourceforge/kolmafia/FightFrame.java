/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

package net.sourceforge.kolmafia;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * A class which allows the user to finish up a fight or choice.
 */

public class FightFrame extends RequestFrame
{
	private static FightFrame INSTANCE = null;

	public FightFrame()
	{	this( null );
	}

	public FightFrame( KoLRequest request )
	{
		super( "Combat Handler", null, request );
		FightFrame.INSTANCE = this;
	}

	public void dispose()
	{
		INSTANCE = null;
		super.dispose();
	}

	public static void showLocation( String location )
	{	showRequest( RequestEditorKit.extractRequest( location ) );
	}

	public static void showRequest( KoLRequest request )
	{
		if ( request.responseText == null )
			request.run();

		// If there's a currently open mini-browser window
		// that has a sidebar, then use it instead.

		KoLFrame [] frames = new KoLFrame[ existingFrames.size() ];
		existingFrames.toArray( frames );

		for ( int i = 0; i < frames.length; ++i )
		{
			if ( frames[i].getClass() == RequestFrame.class && ((RequestFrame)frames[i]).hasSideBar() )
			{
				((RequestFrame)frames[i]).refresh( request );
				return;
			}
		}

		// If you can find an instance of a fight frame,
		// go ahead and refresh it.  Otherwise, create a
		// new frame which renders the request.

		StaticEntity.getClient().setCurrentRequest( request );

		if ( INSTANCE == null )
		{
			// Parameters which will be used to render the
			// request frame.

			(new CreateFrameRunnable( FightFrame.class, new Object [] { request } )).run();
		}
		else
			INSTANCE.refresh( request );
	}
}
