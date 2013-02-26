/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;

import net.sourceforge.kolmafia.swingui.listener.DefaultComponentFocusTraversalPolicy;
import net.sourceforge.kolmafia.swingui.listener.HyperlinkAdapter;
import net.sourceforge.kolmafia.swingui.listener.StickyListener;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.RequestPane;

import net.sourceforge.kolmafia.utilities.RollingLinkedList;

public class CommandDisplayPanel
	extends JPanel
	implements FocusListener
{
	private final RollingLinkedList commandHistory = new RollingLinkedList( 10 );
	private final AutoHighlightTextField entryField;
	private final JButton entryButton;

	private int commandIndex = 0;

	public CommandDisplayPanel()
	{
		RequestPane outputDisplay = new RequestPane();
		outputDisplay.addHyperlinkListener( new HyperlinkAdapter() );

		JScrollPane scrollPane = KoLConstants.commandBuffer.addDisplay( outputDisplay );
		scrollPane.getVerticalScrollBar().addAdjustmentListener( new StickyListener( KoLConstants.commandBuffer, outputDisplay, 200 ) );
		JComponentUtilities.setComponentSize( scrollPane, 400, 300 );

		JPanel entryPanel = new JPanel( new BorderLayout() );
		this.entryField = new AutoHighlightTextField();
		this.entryField.addKeyListener( new CommandEntryListener() );

		this.entryButton = new JButton( "exec" );
		this.entryButton.addActionListener( new CommandEntryListener() );

		entryPanel.add( this.entryField, BorderLayout.CENTER );
		entryPanel.add( this.entryButton, BorderLayout.EAST );

		this.setLayout( new BorderLayout( 1, 1 ) );
		this.add( scrollPane, BorderLayout.CENTER );
		this.add( entryPanel, BorderLayout.SOUTH );

		this.setFocusCycleRoot( true );
		this.setFocusTraversalPolicy( new DefaultComponentFocusTraversalPolicy( this.entryField ) );

		this.addFocusListener( this );
	}

	public void focusGained( FocusEvent e )
	{
		this.entryField.requestFocus();
	}

	public void focusLost( FocusEvent e )
	{
	}

	private class CommandEntryListener
		extends ThreadedListener
	{
		@Override
		protected boolean isValidKeyCode( int keyCode )
		{
			return keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_ENTER;
		}

		@Override
		protected void execute()
		{
			if ( this.isAction() )
			{
				this.submitCommand();
				return;
			}

			int keyCode = getKeyCode();

			if ( keyCode == KeyEvent.VK_UP )
			{
				if ( CommandDisplayPanel.this.commandIndex <= 0 )
				{
					return;
				}

				CommandDisplayPanel.this.entryField.setText( (String) CommandDisplayPanel.this.commandHistory.get( --CommandDisplayPanel.this.commandIndex ) );
			}
			else if ( keyCode == KeyEvent.VK_DOWN )
			{
				if ( CommandDisplayPanel.this.commandIndex + 1 >= CommandDisplayPanel.this.commandHistory.size() )
				{
					return;
				}

				CommandDisplayPanel.this.entryField.setText( (String) CommandDisplayPanel.this.commandHistory.get( ++CommandDisplayPanel.this.commandIndex ) );
			}
			else if ( keyCode == KeyEvent.VK_ENTER )
			{
				this.submitCommand();
			}
		}

		private void submitCommand()
		{
			String command = CommandDisplayPanel.this.entryField.getText().trim();
			CommandDisplayPanel.this.entryField.setText( "" );

			CommandDisplayPanel.this.commandHistory.add( command );

			CommandDisplayPanel.this.commandIndex = CommandDisplayPanel.this.commandHistory.size();
			CommandDisplayFrame.executeCommand( command );
		}
	}
}
