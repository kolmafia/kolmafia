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

import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.JFileChooser;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.BorderLayout;

import java.lang.ref.WeakReference;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.ActionVerifyPanel;

/**
 * An internal class used as the basis for content panels.  This
 * class builds upon the <code>ActionVerifyPanel</code> by adding
 * a <code>setStatusMessage()</code> method.
 */

public abstract class KoLPanel extends ActionVerifyPanel implements KoLConstants
{
	private VerifiableElement [] elements;

	protected JPanel southContainer;
	protected JPanel actionStatusPanel;
	protected StatusLabel actionStatusLabel;

	protected KoLPanel( Dimension left, Dimension right )
	{
		super( left, right );
		existingPanels.add( new WeakReference( this ) );
	}

	protected KoLPanel( Dimension left, Dimension right, boolean isCenterPanel )
	{
		super( left, right, isCenterPanel );
		existingPanels.add( new WeakReference( this ) );
	}

	protected KoLPanel( String confirmedText )
	{
		super( confirmedText );
		existingPanels.add( new WeakReference( this ) );
	}

	protected KoLPanel( String confirmedText, boolean isCenterPanel )
	{
		super( confirmedText, isCenterPanel );
		existingPanels.add( new WeakReference( this ) );
	}

	protected KoLPanel( String confirmedText, String cancelledText )
	{
		super( confirmedText, cancelledText );
		existingPanels.add( new WeakReference( this ) );
	}

	protected KoLPanel( String confirmedText, String cancelledText, boolean isCenterPanel )
	{
		super( confirmedText, cancelledText, isCenterPanel );
		existingPanels.add( new WeakReference( this ) );
	}

	protected KoLPanel( String confirmedText, Dimension left, Dimension right, boolean isCenterPanel )
	{
		super( confirmedText, left, right, isCenterPanel );
		existingPanels.add( new WeakReference( this ) );
	}

	protected KoLPanel( String confirmedText, String cancelledText1, String cancelledText2 )
	{
		super( confirmedText, cancelledText1, cancelledText2 );
		existingPanels.add( new WeakReference( this ) );
	}

	protected KoLPanel( String confirmedText, String cancelledText, Dimension left, Dimension right )
	{
		super( confirmedText, cancelledText, left, right );
		existingPanels.add( new WeakReference( this ) );
	}

	protected KoLPanel( String confirmedText, String cancelledText1, String cancelledText2, Dimension left, Dimension right )
	{
		super( confirmedText, cancelledText1, cancelledText2, left, right );
		existingPanels.add( new WeakReference( this ) );
	}

	protected KoLPanel( String confirmedText, String cancelledText, Dimension left, Dimension right, boolean isCenterPanel )
	{
		super( confirmedText, cancelledText, left, right, isCenterPanel );
		existingPanels.add( new WeakReference( this ) );
	}

	protected KoLPanel( String confirmedText, String cancelledText1, String cancelledText2, Dimension left, Dimension right, boolean isCenterPanel )
	{
		super( confirmedText, cancelledText1, cancelledText2, left, right, isCenterPanel );
		existingPanels.add( new WeakReference( this ) );
	}

	protected void setContent( VerifiableElement [] elements, JPanel mainPanel, JPanel eastPanel, boolean bothDisabledOnClick )
	{
		super.setContent( elements, mainPanel, eastPanel, bothDisabledOnClick );

		// In addition to setting the content on these, also
		// add a return-key listener to each of the input fields.

		ActionConfirmListener listener = new ActionConfirmListener();
		this.elements = elements;

		if ( elements != null )
		{
			for ( int i = 0; i < elements.length; ++i )
			{
				if ( elements[i].getInputField() instanceof MutableComboBox )
					((MutableComboBox)elements[i].getInputField()).getEditor().getEditorComponent().addKeyListener( listener );
				else if ( elements[i].getInputField() instanceof JComboBox )
					((JComboBox)elements[i].getInputField()).addKeyListener( listener );
				else if ( elements[i].getInputField() instanceof JTextField )
					((JTextField)elements[i].getInputField()).addKeyListener( listener );
			}

			if ( shouldAddStatusLabel( elements ) )
			{
				JPanel statusContainer = new JPanel();
				statusContainer.setLayout( new BoxLayout( statusContainer, BoxLayout.Y_AXIS ) );

				actionStatusPanel = new JPanel( new BorderLayout() );
				actionStatusLabel = new StatusLabel();
				actionStatusPanel.add( actionStatusLabel, BorderLayout.SOUTH );

				statusContainer.add( actionStatusPanel );
				statusContainer.add( Box.createVerticalStrut( 20 ) );

				southContainer = new JPanel( new BorderLayout() );
				southContainer.add( statusContainer, BorderLayout.NORTH );
				container.add( southContainer, BorderLayout.SOUTH );
			}
		}
	}

	public void setEnabled( boolean isEnabled )
	{
		super.setEnabled( isEnabled );
		if ( elements == null || elements.length == 0 )
			return;

		if ( elements[0].getInputField().isEnabled() == isEnabled )
			return;

		for ( int i = 0; i < elements.length; ++i )
			elements[i].getInputField().setEnabled( isEnabled );
	}

	protected class ActionConfirmListener extends KeyAdapter implements Runnable
	{
		public void keyReleased( KeyEvent e )
		{
			if ( e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB )
				(new Thread( this )).start();
		}

		public void run()
		{
			for ( int i = 0; i < elements.length; ++i )
				if ( elements[i].getInputField() instanceof MutableComboBox )
					((MutableComboBox)elements[i].getInputField()).forceAddition();

			actionConfirmed();
		}
	}

	private class StatusLabel extends JLabel
	{
		public StatusLabel()
		{	super( " ", JLabel.CENTER );
		}

		public void setStatusMessage( int displayState, String s )
		{
			String label = getText();

			// If the current text or the string you're using is
			// null, then do nothing.

			if ( s == null || label == null )
				return;

			// If the string which you're trying to set is blank,
			// then you don't have to update the status message.

			if ( !s.equals( "" ) )
				setText( s );
		}
	}

	protected boolean shouldAddStatusLabel( VerifiableElement [] elements )
	{
		boolean shouldAddStatusLabel = elements != null && elements.length != 0;
		for ( int i = 0; shouldAddStatusLabel && i < elements.length; ++i )
			shouldAddStatusLabel &= !(elements[i].getInputField() instanceof JScrollPane);

		return shouldAddStatusLabel;
	}

	/**
	 * This internal class is used to process the request for selecting
	 * a script using the file dialog.
	 */

	protected class ScriptSelectPanel extends JPanel implements ActionListener
	{
		private JTextField scriptField;
		private JButton scriptButton;

		public ScriptSelectPanel( JTextField scriptField )
		{
			setLayout( new BorderLayout( 0, 0 ) );

			add( scriptField, BorderLayout.CENTER );
			scriptButton = new JButton( "..." );

			JComponentUtilities.setComponentSize( scriptButton, 20, 20 );
			scriptButton.addActionListener( this );
			add( scriptButton, BorderLayout.EAST );

			this.scriptField = scriptField;
		}

		public void setEnabled( boolean isEnabled )
		{
			scriptField.setEnabled( isEnabled );
			scriptButton.setEnabled( isEnabled );
		}

		public String getText()
		{	return scriptField.getText();
		}

		public void setText( String text )
		{	scriptField.setText( text );
		}

		public void actionPerformed( ActionEvent e )
		{
			JFileChooser chooser = new JFileChooser( SCRIPT_DIRECTORY.getAbsolutePath() );
			chooser.showOpenDialog( null );

			if ( chooser.getSelectedFile() == null )
				return;

			scriptField.setText( chooser.getSelectedFile().getAbsolutePath() );
		}
	}

	public void setStatusMessage( String s )
	{	setStatusMessage( ENABLE_STATE, s );
	}

	public void setStatusMessage( int displayState, String s )
	{
		if ( actionStatusLabel != null && !s.trim().equals( "" ) )
			actionStatusLabel.setStatusMessage( displayState, s.trim() );
	}
}
