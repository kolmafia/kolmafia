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

import java.awt.Component;
import javax.swing.SwingUtilities;
import java.lang.reflect.Constructor;

/**
 * A special class which ensures that frames can be created inside
 * of the Swing thread.  This avoids deadlock problems that often
 * cause KoLmafia not to load properly.
 */

public class CreateFrameRunnable implements Runnable, KoLConstants
{
	private KoLmafia client;
	private Component creation;
	private boolean isEnabled;

	private Constructor creator;
	private Object [] parameters;

	public CreateFrameRunnable( Class creationType, Object [] parameters )
	{
		this.parameters = parameters;
		this.isEnabled = true;

		Class [] parameterTypes= new Class[ parameters.length ];
		for ( int i = 0; i < parameters.length; ++i )
			parameterTypes[i] = parameters[i] == null ? null : parameters[i].getClass();

		this.creator = null;
		boolean isValidConstructor = true;
		Class [] constructorParameterTypes;
		Constructor [] constructors = creationType.getConstructors();

		for ( int i = 0; i < constructors.length; ++i )
		{
			constructorParameterTypes = constructors[i].getParameterTypes();
			if ( constructorParameterTypes.length != parameters.length )
				continue;

			isValidConstructor = true;
			for ( int j = 0; j < constructorParameterTypes.length && isValidConstructor; ++j )
				if ( parameterTypes[j] != null && !constructorParameterTypes[j].isAssignableFrom( parameterTypes[j] ) )
					isValidConstructor = false;

			if ( isValidConstructor )
				this.creator = constructors[i];
		}
	}

	public void setEnabled( boolean isEnabled )
	{	this.isEnabled = isEnabled;
	}

	public Component getCreation()
	{	return creation;
	}

	public void run()
	{
		// If there is no creation creation, then return
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
			this.creation = (Component) creator.newInstance( parameters );

			if ( this.creation instanceof KoLFrame )
			{
				((KoLFrame)this.creation).pack();
				((KoLFrame)this.creation).setVisible( true );
				((KoLFrame)this.creation).setEnabled( isEnabled );
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
