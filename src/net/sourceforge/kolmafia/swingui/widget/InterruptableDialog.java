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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.swingui.widget;

import java.awt.Frame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class InterruptableDialog
{
	private static PauseObject pauser = new PauseObject();

	private static class InterruptableDialogBox
		extends JOptionPane
		implements Runnable
	{
		private String message;
		private Boolean result = null;

		public InterruptableDialogBox( String mes )
		{
			this.message = mes;

			SwingUtilities.invokeLater( this );
		}

		public Boolean getResult()
		{
			return result;
		}

		public void setResult( boolean b )
		{
			this.result = b;
		}

		public void run()
		{
			this.setResult( JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog( null,
				StringUtilities.basicTextWrap( message ), "", JOptionPane.YES_NO_OPTION ) );

			synchronized ( InterruptableDialog.pauser )
			{
				InterruptableDialog.pauser.notifyAll();
			}
		}
	}

	public static Value confirm( Value message, Value timeOut, Value defaultBoolean )
	{
		if ( StaticEntity.isHeadless() )
		{
			// this doesn't support headless operation yet, sorry
			RequestLogger.printLine( message.toString() );
			RequestLogger.printLine( "(Y/N, leave blank to choose N)" );

			String reply = KoLmafiaCLI.DEFAULT_SHELL.getNextLine( " > " );

			return DataTypes.makeBooleanValue( reply.equalsIgnoreCase( "y" ) );
		}

		String mes = message.toString();
		long time = timeOut.intValue();

		InterruptableDialogBox r = new InterruptableDialogBox( mes );

		synchronized ( pauser ) // get the object's monitor
		{
			try
			{
				pauser.wait( time ); // wait for <time> millis, unless the dialog notifies the pauser
			}
			catch ( InterruptedException e )
			{
				e.printStackTrace();
			}
		}
		Boolean result = r.getResult(); // will be null if the user hasn't selected an option

		if ( result != null )
		{
			return DataTypes.makeBooleanValue( result );
		}

		// must have reached timeOut, so dispose of the frame and return defaultBoolean

		Frame f = JOptionPane.getFrameForComponent( r );

		if ( f != null )
		{
			f.dispose();
		}

		return defaultBoolean;
	}
}
