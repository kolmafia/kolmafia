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
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * A class which allows the user to finish up a fight or choice.
 */

public class FightFrame extends RequestFrame
{
	private static FightFrame INSTANCE = null;

	public FightFrame( KoLmafia client, KoLRequest request )
	{
		super( client, null, request );
		FightFrame.INSTANCE = this;
		addWindowListener( new CloseFightFrameListener() );
	}

	protected final class CloseFightFrameListener extends WindowAdapter
	{
		public void windowClosed( WindowEvent e )
		{	INSTANCE = null;
		}
	}

	public static void showRequest( KoLRequest request )
	{
		// Parameters which will be used to render the
		// request frame.

		Object [] parameters = new Object[2];

		parameters[0] = StaticEntity.getClient();
		parameters[1] = request;

		// If you can find an instance of a fight frame,
		// go ahead and refresh it.  Otherwise, create a
		// new frame which renders the request.

		client.setCurrentRequest( request );
		if ( INSTANCE == null )
			(new CreateFrameRunnable( FightFrame.class, parameters )).run();
		else
			INSTANCE.refresh( request );
	}

	public static void main( String [] args )
	{
		Object [] parameters = new Object[2];
		parameters[0] = null;
		parameters[1] = null;

		(new CreateFrameRunnable( FightFrame.class, parameters )).run();
	}
}
