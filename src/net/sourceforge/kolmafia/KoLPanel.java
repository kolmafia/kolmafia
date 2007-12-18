/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.lang.ref.WeakReference;
import java.util.HashMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import net.java.dev.spellcast.utilities.ActionVerifyPanel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public abstract class KoLPanel extends ActionVerifyPanel implements KoLConstants
{
	protected HashMap listenerMap;

	public JPanel southContainer;
	public JPanel actionStatusPanel;
	public StatusLabel actionStatusLabel;

	public KoLPanel( Dimension left, Dimension right )
	{
		super( left, right );
		StaticEntity.registerPanel( this );
	}

	public KoLPanel( Dimension left, Dimension right, boolean isCenterPanel )
	{
		super( left, right, isCenterPanel );
		StaticEntity.registerPanel( this );
	}

	public KoLPanel( String confirmedText )
	{
		super( confirmedText );
		StaticEntity.registerPanel( this );
	}

	public KoLPanel( String confirmedText, boolean isCenterPanel )
	{
		super( confirmedText, isCenterPanel );
		StaticEntity.registerPanel( this );
	}

	public KoLPanel( String confirmedText, String cancelledText )
	{
		super( confirmedText, cancelledText );
		StaticEntity.registerPanel( this );
	}

	public KoLPanel( String confirmedText, String cancelledText, boolean isCenterPanel )
	{
		super( confirmedText, cancelledText, isCenterPanel );
		StaticEntity.registerPanel( this );
	}

	public KoLPanel( String confirmedText, Dimension left, Dimension right, boolean isCenterPanel )
	{
		super( confirmedText, left, right, isCenterPanel );
		StaticEntity.registerPanel( this );
	}

	public KoLPanel( String confirmedText, String cancelledText1, String cancelledText2 )
	{
		super( confirmedText, cancelledText1, cancelledText2 );
		StaticEntity.registerPanel( this );
	}

	public KoLPanel( String confirmedText, String cancelledText, Dimension left, Dimension right )
	{
		super( confirmedText, cancelledText, left, right );
		StaticEntity.registerPanel( this );
	}

	public KoLPanel( String confirmedText, String cancelledText1, String cancelledText2, Dimension left, Dimension right )
	{
		super( confirmedText, cancelledText1, cancelledText2, left, right );
		StaticEntity.registerPanel( this );
	}

	public KoLPanel( String confirmedText, String cancelledText, Dimension left, Dimension right, boolean isCenterPanel )
	{
		super( confirmedText, cancelledText, left, right, isCenterPanel );
		StaticEntity.registerPanel( this );
	}

	public KoLPanel( String confirmedText, String cancelledText1, String cancelledText2, Dimension left, Dimension right, boolean isCenterPanel )
	{
		super( confirmedText, cancelledText1, cancelledText2, left, right, isCenterPanel );
		StaticEntity.registerPanel( this );
	}

	public void setContent( VerifiableElement [] elements, boolean bothDisabledOnClick )
	{
		super.setContent( elements, bothDisabledOnClick );

		// In addition to setting the content on these, also
		// add a return-key listener to each of the input fields.

		this.elements = elements;

		this.addListeners();
		this.addStatusLabel();
	}

	public void addListeners()
	{
		if ( this.elements == null )
			return;

		ActionConfirmListener listener = new ActionConfirmListener();
		for ( int i = 0; i < this.elements.length; ++i )
			addListener( elements[i].getInputField(), listener );
	}

	private void addListener( Object component, ActionConfirmListener listener )
	{
		if ( listenerMap == null )
			listenerMap = new HashMap();

		if ( component instanceof JTextField )
		{
			((JTextField)component).addKeyListener( listener );
			listenerMap.put( component, new WeakReference( listener ) );
		}

		if ( component instanceof MutableComboBox )
		{
			JTextComponent editor = (JTextComponent) ((MutableComboBox)component).getEditor().getEditorComponent();

			editor.addKeyListener( listener );
			listenerMap.put( editor, new WeakReference( listener ) );
		}
	}

	public void dispose()
	{
		StaticEntity.unregisterPanel( this );

		if ( this.listenerMap == null )
		{
			super.dispose();
			return;
		}

		Object [] keys = this.listenerMap.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			WeakReference ref = (WeakReference) listenerMap.get( keys[i] );
			if ( ref == null )
				continue;

			Object listener = ref.get();
			if ( listener == null )
				continue;

			removeListener( keys[i], (ActionConfirmListener) listener );
		}

		this.listenerMap.clear();
		this.listenerMap = null;

		this.southContainer = null;
		this.actionStatusPanel = null;
		this.actionStatusLabel = null;

		super.dispose();
	}

	private void removeListener( Object component, ActionConfirmListener listener )
	{
		if ( component instanceof JTextField )
			((JTextField)component).removeKeyListener( listener );
	}

	public void setEnabled( boolean isEnabled )
	{
		super.setEnabled( isEnabled );
		if ( this.elements == null || this.elements.length == 0 )
			return;

		if ( this.elements[0].getInputField().isEnabled() == isEnabled )
			return;

		for ( int i = 0; i < this.elements.length; ++i )
			this.elements[i].getInputField().setEnabled( isEnabled );
	}

	public void setStatusMessage( String message )
	{
		if ( this.actionStatusLabel != null )
			this.actionStatusLabel.setStatusMessage( message );
	}

	public void addStatusLabel()
	{
		if ( !shouldAddStatusLabel() )
			return;

		JPanel statusContainer = new JPanel();
		statusContainer.setLayout( new BoxLayout( statusContainer, BoxLayout.Y_AXIS ) );

		this.actionStatusPanel = new JPanel( new BorderLayout() );
		this.actionStatusLabel = new StatusLabel();
		this.actionStatusPanel.add( this.actionStatusLabel, BorderLayout.SOUTH );

		statusContainer.add( this.actionStatusPanel );
		statusContainer.add( Box.createVerticalStrut( 20 ) );

		this.southContainer = new JPanel( new BorderLayout() );
		this.southContainer.add( statusContainer, BorderLayout.NORTH );
		this.container.add( this.southContainer, BorderLayout.SOUTH );
	}

	public boolean shouldAddStatusLabel()
	{
		if ( this.elements == null )
			return false;

		boolean shouldAddStatusLabel = this.elements != null && this.elements.length != 0;
		for ( int i = 0; shouldAddStatusLabel && i < this.elements.length; ++i )
			shouldAddStatusLabel &= !(this.elements[i].getInputField() instanceof JScrollPane);

		return shouldAddStatusLabel;
	}

	private class StatusLabel extends JLabel
	{
		public StatusLabel()
		{	super( " ", JLabel.CENTER );
		}

		public void setStatusMessage( String message )
		{
			String label = this.getText();

			// If the current text or the string you're using is
			// null, then do nothing.

			if ( message == null || label == null || message.length() == 0 )
				return;

			// If the string which you're trying to set is blank,
			// then you don't have to update the status message.

			this.setText( message );
		}
	}


	/**
	 * This internal class is used to process the request for selecting
	 * a script using the file dialog.
	 */

	public class ScriptSelectPanel extends JPanel implements ActionListener, FocusListener
	{
		private AutoHighlightField scriptField;
		private JButton scriptButton;

		public ScriptSelectPanel( AutoHighlightField scriptField )
		{
			this.setLayout( new BorderLayout( 0, 0 ) );

			scriptField.addFocusListener( this );
			this.add( scriptField, BorderLayout.CENTER );
			this.scriptButton = new JButton( "..." );

			JComponentUtilities.setComponentSize( this.scriptButton, 20, 20 );
			this.scriptButton.addActionListener( this );
			this.add( this.scriptButton, BorderLayout.EAST );

			this.scriptField = scriptField;
		}

		public void setEnabled( boolean isEnabled )
		{
			this.scriptField.setEnabled( isEnabled );
			this.scriptButton.setEnabled( isEnabled );
		}

		public String getText()
		{	return this.scriptField.getText();
		}

		public void setText( String text )
		{	this.scriptField.setText( text );
		}

		public void focusLost( FocusEvent e )
		{	KoLPanel.this.actionConfirmed();
		}

		public void focusGained( FocusEvent e )
		{
		}

		public void actionPerformed( ActionEvent e )
		{
			JFileChooser chooser = new JFileChooser( SCRIPT_LOCATION.getAbsolutePath() );
			chooser.showOpenDialog( null );

			if ( chooser.getSelectedFile() == null )
				return;

			this.scriptField.setText( chooser.getSelectedFile().getAbsolutePath() );
			KoLPanel.this.actionConfirmed();
		}
	}

	public class ActionConfirmListener extends KeyAdapter
	{
		public void keyReleased( KeyEvent e )
		{
			if ( e.isConsumed() )
				return;

			if ( e.getKeyCode() != KeyEvent.VK_ENTER )
				return;

			KoLPanel.this.actionConfirmed();
			e.consume();
		}
	}
}
