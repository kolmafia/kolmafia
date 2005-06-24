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

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.text.DecimalFormatSymbols;
import java.lang.reflect.Constructor;

import java.awt.Component;
import javax.swing.SwingUtilities;
import java.lang.reflect.Constructor;
import net.java.dev.spellcast.utilities.UtilityConstants;

public interface KoLConstants extends UtilityConstants
{
	public static final List existingFrames = new ArrayList();

	public static final DecimalFormat df = new DecimalFormat(
		"#,##0", new DecimalFormatSymbols( Locale.US ) );

	public static final SimpleDateFormat sdf = new SimpleDateFormat( "yyyyMMdd" );

	public static final int NOCHANGE = 0;
	public static final int ERROR_STATE    = 1;
	public static final int ENABLED_STATE  = 2;
	public static final int DISABLED_STATE = 3;

	public class RequestThread extends Thread
	{
		public RequestThread()
		{	setDaemon( true );
		}
	}

	/**
	 * An internal class which ensures that frames can be created inside
	 * of the Swing thread.  This avoids deadlock problems that often
	 * cause KoLmafia not to load properly.
	 */

	public class CreateFrameRunnable implements Runnable
	{
		public KoLmafia client;
		public Component instance;

		private boolean isEnabled;
		private Constructor creator;
		private Object [] parameters;

		public CreateFrameRunnable( Class instanceClass, Object [] parameters )
		{
			this.parameters = parameters;
			this.isEnabled = true;

			try
			{
				Class [] parameterClasses = new Class[ parameters.length ];
				for ( int i = 0; i < parameters.length; ++i )
				{
					if ( parameters[i] instanceof KoLmafia )
					{
						parameterClasses[i] = KoLmafia.class;
						client = (KoLmafia) parameters[i];
					}
					else
						parameterClasses[i] = parameters[i].getClass();
				}

				this.creator = instanceClass.getConstructor( parameterClasses );
			}
			catch ( Exception e )
			{
				// If an exception happens, the creator stays null,
				// so there's nothing to worry about.  Just make
				// sure that there is a null check sometime when
				// this runnable runs.
			}
		}

		public void setEnabled( boolean isEnabled )
		{	this.isEnabled = isEnabled;
		}

		public void run()
		{
			// If there is no creation instance, then return
			// from the method because there's nothing to do.

			if ( this.creator == null )
			{
				if ( client != null )
					client.updateDisplay( ERROR_STATE, "Frame could not be loaded." );

				return;
			}

			// If you are not in the Swing thread, then wait
			// until you are in the Swing thread before making
			// the object to avoid deadlocks.

			if ( !SwingUtilities.isEventDispatchThread() )
			{
				SwingUtilities.invokeLater( this );
				return;
			}

			try
			{
				this.instance = (Component) creator.newInstance( parameters );

				if ( this.instance instanceof KoLFrame )
				{
					((KoLFrame)this.instance).pack();
					((KoLFrame)this.instance).setVisible( true );
					((KoLFrame)this.instance).setEnabled( isEnabled );
				}
			}
			catch ( Exception e )
			{
				// If this happens, update the display to indicate
				// that it failed to happen (eventhough technically,
				// this should never have happened)

				if ( client != null )
				{
					client.updateDisplay( ERROR_STATE, "Frame could not be loaded." );
					e.printStackTrace( client.getLogStream() );
				}
				else
					e.printStackTrace();

				return;
			}
		}
	}
}