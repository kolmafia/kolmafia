/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui.listener;

import java.awt.Frame;

import net.sourceforge.kolmafia.KoLDesktop;
import net.sourceforge.kolmafia.KoLmafiaGUI;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.swingui.GenericFrame;

public class DisplayFrameListener
	extends ThreadedListener
{
	private final String frameClass;

	public DisplayFrameListener( String frameClass )
	{
		this.frameClass = frameClass;
	}

	@Override
	protected void execute()
	{
		 if ( this.frameClass == null )	 
		 {	 
			 String interfaceSetting = Preferences.getString( "initialDesktop" );	 
	 
			 Frame [] frames = Frame.getFrames();	 
	 
			 for ( int i = 0; i < frames.length; ++i )	 
			 {	 
				 if ( ( frames[ i ] instanceof GenericFrame ) )	 
				 {	 
					 GenericFrame frame = (GenericFrame) frames[ i ];	 
	 
					 if ( frame.showInWindowMenu() && interfaceSetting.indexOf( frame.getFrameName() ) == -1 )	 
					 {	 
						 frame.setVisible( true );	 
					 }	 
				 }	 
			 }	 
	 
			 if ( KoLDesktop.instanceExists() )	 
			 {	 
				 KoLDesktop.getInstance().setVisible( true );	 
			 }	 
		 }	 
		 else	 
		 {	 
			 KoLmafiaGUI.constructFrame( this.frameClass );	 
		 }
	}
}
