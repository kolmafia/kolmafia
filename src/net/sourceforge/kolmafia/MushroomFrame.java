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

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JEditorPane;

import net.java.dev.spellcast.utilities.JComponentUtilities;

public class MushroomFrame extends KoLFrame
{
	private JEditorPane currentDisplay, forecastDisplay;
	private LimitedSizeChatBuffer currentBuffer, forecastBuffer;

	public MushroomFrame( KoLmafia client )
	{
		super( client, "Mushroom Fields" );

		currentBuffer = new LimitedSizeChatBuffer( "Current Plot" );

		currentDisplay = new JEditorPane();
		JComponentUtilities.setComponentSize( currentDisplay, 200, 200 );

		currentDisplay.setEditable( false );
		currentDisplay.addHyperlinkListener( new KoLHyperlinkAdapter() );
		currentBuffer.setChatDisplay( currentDisplay );

		forecastBuffer = new LimitedSizeChatBuffer( "Forecast Plot" );

		forecastDisplay = new JEditorPane();
		JComponentUtilities.setComponentSize( forecastDisplay, 200, 200 );

		forecastDisplay.setEditable( false );
		forecastDisplay.addHyperlinkListener( new KoLHyperlinkAdapter() );
		forecastBuffer.setChatDisplay( forecastDisplay );

		getContentPane().setLayout( new BorderLayout() );

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout( new GridLayout( 1, 2, 20, 20 ) );
		centerPanel.add( currentDisplay );
		centerPanel.add( forecastDisplay );

		getContentPane().add( centerPanel, BorderLayout.CENTER );

		(new UpdateMushroomThread()).start();
	}

	/**
	 * Special thread which allows the current page to be updated outside
	 * of the Swing thread -- this means images can be downloaded without
	 * locking the UI.
	 */

	private class UpdateMushroomThread extends DaemonThread
	{
		public void run()
		{
			if ( client != null )
			{
				currentBuffer.clearBuffer();
				currentBuffer.append( MushroomPlot.getMushroomPlot( client, true ) );
				currentDisplay.setCaretPosition( 0 );

				forecastBuffer.clearBuffer();
				forecastBuffer.append( MushroomPlot.getForecastedPlot( client, true ) );
				forecastDisplay.setCaretPosition( 0 );
			}
		}
	}

	public static void main( String [] args )
	{
		Object [] parameters = new Object[1];
		parameters[0] = null;

		(new CreateFrameRunnable( MushroomFrame.class, parameters )).run();
	}
}
