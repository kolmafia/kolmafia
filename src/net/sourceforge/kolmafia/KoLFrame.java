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

import javax.swing.SwingUtilities;
import net.java.dev.spellcast.utilities.ActionVerifyPanel;

/**
 * An extended <code>JFrame</code> which provides all the frames in
 * KoLmafia the ability to update their displays, given some integer
 * value and the message to use for updating.
 */

public abstract class KoLFrame extends javax.swing.JFrame
{
	public static final int PRE_LOGIN_STATE     = 1;
	public static final int SENDING_LOGIN_STATE = 2;
	public static final int LOGGED_IN_STATE     = 3;
	public static final int ADVENTURING_STATE   = 4;

	protected KoLmafia client;
	protected KoLPanel contentPanel;

	protected KoLFrame( String title, KoLmafia client )
	{
		super( title );
		this.client = client;
	}

	public void updateDisplay( int displayState, String message )
	{	(new DisplayStatus( displayState, message )).run();
	}


	protected abstract class KoLPanel extends ActionVerifyPanel
	{
		protected KoLPanel( String confirmedText, String cancelledText )
		{
			super( confirmedText, cancelledText );
		}

		public abstract void clear();
		public abstract void setStatusMessage( String s );
	}

	/**
	 * A <code>Runnable</code> object which can be placed inside of
	 * a call to <code>javax.swing.SwingUtilities.invokeLater()</code>
	 * to ensure that the GUI is only modified inside of the AWT thread.
	 */

	protected class DisplayStatus implements Runnable
	{
		private int displayState;
		private String status;

		public DisplayStatus( int displayState, String status )
		{
			this.displayState = displayState;
			this.status = status;
		}

		public void run()
		{
			if ( !SwingUtilities.isEventDispatchThread() )
			{
				SwingUtilities.invokeLater( this );
				return;
			}

			contentPanel.setStatusMessage( status );

			switch ( displayState )
			{
				case PRE_LOGIN_STATE:
				case LOGGED_IN_STATE:
					contentPanel.setEnabled( true );
					contentPanel.clear();
					break;

				case SENDING_LOGIN_STATE:
				case ADVENTURING_STATE:
					contentPanel.setEnabled( false );
					break;
			}
		}
	}
}