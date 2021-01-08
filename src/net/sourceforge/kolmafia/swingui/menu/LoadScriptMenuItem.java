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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.swingui.menu;

import java.io.File;
import java.io.IOException;

import javax.swing.SwingUtilities;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

/**
 * In order to keep the user interface from freezing (or at least appearing to freeze), this internal class is used
 * to process the request for loading a script.
 */

public class LoadScriptMenuItem
	extends ThreadedMenuItem
{
	public LoadScriptMenuItem()
	{
		this( "Load script...", null );
	}

	public LoadScriptMenuItem( final String scriptName, final String scriptPath )
	{
		super( scriptName, new LoadScriptListener( scriptPath ) );
	}

	private static class LoadScriptListener
		extends ThreadedListener
	{
		private final String scriptPath;
		private String executePath;

		public LoadScriptListener( String scriptPath )
		{
			this.scriptPath = scriptPath;
		}

		@Override
		protected void execute()
		{
			this.executePath = this.scriptPath;

			if ( this.scriptPath == null )
			{
				try
				{
					SwingUtilities.invokeAndWait( new Runnable()
					{
						public void run()
						{
							File input = InputFieldUtilities.chooseInputFile( KoLConstants.SCRIPT_LOCATION, null );
							if ( input == null )
							{
								return;
							}

							try
							{
								LoadScriptListener.this.executePath = input.getCanonicalPath();
							}
							catch ( IOException e )
							{
							}
						}
					} );
				}
				catch ( Exception e )
				{
				}
			}

			if ( this.executePath == null )
			{
				return;
			}

			KoLmafia.forceContinue();

			if ( this.hasShiftModifier() )
			{
				CommandDisplayFrame.executeCommand( "edit " + this.executePath );
			}
			else
			{
				CommandDisplayFrame.executeCommand( "call " + this.executePath );
			}
		}
	}
}
