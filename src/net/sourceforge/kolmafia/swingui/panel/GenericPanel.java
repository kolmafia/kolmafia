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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;

import java.io.File;
import java.io.IOException;

import java.lang.ref.WeakReference;

import java.util.HashMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import javax.swing.text.JTextComponent;

import net.java.dev.spellcast.utilities.ActionVerifyPanel;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.UtilityConstants;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterComboBox;
import net.sourceforge.kolmafia.swingui.widget.CollapsibleTextArea;

public abstract class GenericPanel
	extends ActionVerifyPanel
{
	protected ConfirmedListener CONFIRM_LISTENER = new ConfirmedListener();
	protected CancelledListener CANCEL_LISTENER = new CancelledListener();

	protected HashMap listenerMap;

	public JPanel southContainer;
	public JPanel actionStatusPanel;
	public StatusLabel actionStatusLabel;

	public GenericPanel()
	{
		super();
		this.setListeners( CONFIRM_LISTENER, CANCEL_LISTENER );
		StaticEntity.registerPanel( this );
	}

	public GenericPanel( final Dimension left, final Dimension right )
	{
		super( left, right );
		this.setListeners( CONFIRM_LISTENER, CANCEL_LISTENER );
		StaticEntity.registerPanel( this );
	}

	public GenericPanel( final Dimension left, final Dimension right, final boolean isCenterPanel )
	{
		super( left, right, isCenterPanel );
		this.setListeners( CONFIRM_LISTENER, CANCEL_LISTENER );
		StaticEntity.registerPanel( this );
	}

	public GenericPanel( final String confirmedText )
	{
		super( confirmedText );
		this.setListeners( CONFIRM_LISTENER, CANCEL_LISTENER );
		StaticEntity.registerPanel( this );
	}

	public GenericPanel( final String confirmedText, final boolean isCenterPanel )
	{
		super( confirmedText, isCenterPanel );
		this.setListeners( CONFIRM_LISTENER, CANCEL_LISTENER );
		StaticEntity.registerPanel( this );
	}

	public GenericPanel( final String confirmedText, final String cancelledText )
	{
		super( confirmedText, cancelledText );
		this.setListeners( CONFIRM_LISTENER, CANCEL_LISTENER );
		StaticEntity.registerPanel( this );
	}

	public GenericPanel( final String confirmedText, final String cancelledText, final boolean isCenterPanel )
	{
		super( confirmedText, cancelledText, isCenterPanel );
		this.setListeners( CONFIRM_LISTENER, CANCEL_LISTENER );
		StaticEntity.registerPanel( this );
	}

	public GenericPanel( final String confirmedText, final Dimension left, final Dimension right,
		final boolean isCenterPanel )
	{
		super( confirmedText, left, right, isCenterPanel );
		this.setListeners( CONFIRM_LISTENER, CANCEL_LISTENER );
		StaticEntity.registerPanel( this );
	}

	public GenericPanel( final String confirmedText, final String cancelledText1, final String cancelledText2 )
	{
		super( confirmedText, cancelledText1, cancelledText2 );
		this.setListeners( CONFIRM_LISTENER, CANCEL_LISTENER );
		StaticEntity.registerPanel( this );
	}

	public GenericPanel( final String confirmedText, final String cancelledText, final Dimension left, final Dimension right )
	{
		super( confirmedText, cancelledText, left, right );
		this.setListeners( CONFIRM_LISTENER, CANCEL_LISTENER );
		StaticEntity.registerPanel( this );
	}

	public GenericPanel( final String confirmedText, final String cancelledText1, final String cancelledText2,
		final Dimension left, final Dimension right )
	{
		super( confirmedText, cancelledText1, cancelledText2, left, right );
		this.setListeners( CONFIRM_LISTENER, CANCEL_LISTENER );
		StaticEntity.registerPanel( this );
	}

	public GenericPanel( final String confirmedText, final String cancelledText, final Dimension left,
		final Dimension right, final boolean isCenterPanel )
	{
		super( confirmedText, cancelledText, left, right, isCenterPanel );
		this.setListeners( CONFIRM_LISTENER, CANCEL_LISTENER );
		StaticEntity.registerPanel( this );
	}

	public GenericPanel( final String confirmedText, final String cancelledText1, final String cancelledText2,
		final Dimension left, final Dimension right, final boolean isCenterPanel )
	{
		super( confirmedText, cancelledText1, cancelledText2, left, right, isCenterPanel );
		this.setListeners( CONFIRM_LISTENER, CANCEL_LISTENER );
		StaticEntity.registerPanel( this );
	}

	@Override
	public void setContent( final VerifiableElement[] elements, final boolean bothDisabledOnClick )
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
		{
			return;
		}

		ActionConfirmListener listener = new ActionConfirmListener();
		for ( int i = 0; i < this.elements.length; ++i )
		{
			this.addListener( this.elements[ i ].getInputField(), listener );
		}
	}

	private void addListener( final Object component, final ActionConfirmListener listener )
	{
		if ( this.listenerMap == null )
		{
			this.listenerMap = new HashMap();
		}

		if ( component instanceof JTextField )
		{
			( (JTextField) component ).addKeyListener( listener );
			this.listenerMap.put( component, new WeakReference( listener ) );
		}

		if ( component instanceof AutoFilterComboBox )
		{
			JTextComponent editor = (JTextComponent) ( (AutoFilterComboBox) component ).getEditor().getEditorComponent();

			editor.addKeyListener( listener );
			this.listenerMap.put( editor, new WeakReference( listener ) );
		}
		else if ( component instanceof JComboBox && ((JComboBox) component).isEditable() )
		{
			JTextComponent editor = (JTextComponent) ( (JComboBox) component ).getEditor().getEditorComponent();

			editor.addKeyListener( listener );
			this.listenerMap.put( editor, new WeakReference( listener ) );
		}
	}

	@Override
	public void dispose()
	{
		if ( this.listenerMap == null )
		{
			super.dispose();
			return;
		}

		Object[] keys = this.listenerMap.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			WeakReference ref = (WeakReference) this.listenerMap.get( keys[ i ] );
			if ( ref == null )
			{
				continue;
			}

			Object listener = ref.get();
			if ( listener == null )
			{
				continue;
			}

			this.removeListener( keys[ i ], (ActionConfirmListener) listener );
		}

		this.listenerMap.clear();
		this.listenerMap = null;

		this.southContainer = null;
		this.actionStatusPanel = null;
		this.actionStatusLabel = null;

		super.dispose();
	}

	private void removeListener( final Object component, final ActionConfirmListener listener )
	{
		if ( component instanceof JTextField )
		{
			( (JTextField) component ).removeKeyListener( listener );
		}

		if ( component instanceof AutoFilterComboBox )
		{
			JTextComponent editor = (JTextComponent) ( (AutoFilterComboBox) component ).getEditor().getEditorComponent();

			editor.removeKeyListener( listener );
		}
		else if ( component instanceof JComboBox && ((JComboBox) component).isEditable() )
		{
			JTextComponent editor = (JTextComponent) ( (JComboBox) component ).getEditor().getEditorComponent();

			editor.removeKeyListener( listener );
		}
	}

	@Override
	public void setEnabled( final boolean isEnabled )
	{
		super.setEnabled( isEnabled );
		if ( this.elements == null || this.elements.length == 0 )
		{
			return;
		}

		for ( int i = 0; i < this.elements.length; ++i )
		{
			if ( this.elements[ i ] == null )
			{
				continue;
			}

			JComponent inputField = this.elements[ i ].getInputField();

			if ( inputField == null )
			{
				continue;
			}

			inputField.setEnabled( isEnabled );
		}
	}

	public void setStatusMessage( final String message )
	{
		if ( this.actionStatusLabel != null )
		{
			this.actionStatusLabel.setStatusMessage( message );
		}
	}

	public void addStatusLabel()
	{
		if ( !this.shouldAddStatusLabel() )
		{
			return;
		}

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
		{
			return false;
		}

		boolean shouldAddStatusLabel = this.elements != null && this.elements.length != 0;
		for ( int i = 0; shouldAddStatusLabel && i < this.elements.length; ++i )
		{
			shouldAddStatusLabel &= !( this.elements[ i ].getInputField() instanceof JScrollPane );
		}

		return shouldAddStatusLabel;
	}

	private class StatusLabel
		extends JLabel
	{
		public StatusLabel()
		{
			super( " ", SwingConstants.CENTER );
		}

		public void setStatusMessage( final String message )
		{
			String label = this.getText();

			// If the current text or the string you're using is
			// null, then do nothing.

			if ( message == null || label == null || message.length() == 0 )
			{
				return;
			}

			// If the string which you're trying to set is blank,
			// then you don't have to update the status message.

			this.setText( message );
		}
	}

	/**
	 * This internal class is used to process the request for selecting a
	 * file using the file dialog.
	 */

	public class ScriptSelectPanel
		extends FileSelectPanel
	{
		public ScriptSelectPanel( final JComponent textField )
		{
			super( textField, true );
			this.setPath( KoLConstants.SCRIPT_LOCATION );
		}
	}

	public class FileSelectPanel
		extends JPanel
		implements ActionListener, FocusListener
	{
		private final JTextComponent textField;
		private final JButton fileButton;
		private File path = null;
		private JLabel label;

		public FileSelectPanel( final JComponent textField, final boolean button )
		{
			this.setLayout( new BorderLayout( 0, 0 ) );

			if ( textField instanceof JTextComponent )
			{
				this.textField = (JTextComponent) textField;
			}
			else if ( textField instanceof CollapsibleTextArea )
			{
				this.textField = ( (CollapsibleTextArea) textField ).getArea();
				this.label = ( (CollapsibleTextArea) textField ).getLabel();
			}
			else
			{
				this.textField = new JTextField();
			}

			this.textField.addFocusListener( this );
			this.add( textField, BorderLayout.CENTER );

			if ( button )
			{
				this.fileButton = new JButton( "..." );
				JComponentUtilities.setComponentSize( this.fileButton, 20, 20 );
				this.fileButton.addActionListener( this );
				this.add( this.fileButton, BorderLayout.EAST );
			}
			else
			{
				this.fileButton = null;
			}
		}

		@Override
		public void setEnabled( final boolean isEnabled )
		{
			this.textField.setEnabled( isEnabled );
			if ( this.fileButton != null )
			{
				this.fileButton.setEnabled( isEnabled );
			}
		}

		public void setPath( final File path )
		{
			this.path = path;
		}

		public String getText()
		{
			return this.textField.getText();
		}

		public void setText( String text )
		{
			try
			{
				text = this.getRelativePath( text );
			}
			catch ( IOException e )
			{

			}

			this.textField.setText( text );
		}

		private String getRelativePath( final String text )
			throws IOException
		{
			File root = UtilityConstants.ROOT_LOCATION;
			String rootPath = root.getCanonicalPath();

			if ( rootPath.endsWith( File.separator ) )
			{
				rootPath = rootPath.substring( 0, rootPath.length() - 1 );
			}

			if ( rootPath.equals( "" ) )
			{
				return text;
			}

			if ( text.toLowerCase().startsWith( rootPath.toLowerCase() ) )
			{
				return text.substring( rootPath.length() + 1 );
			}

			File rootParent = root.getParentFile();

			if ( rootParent == null )
			{
				return text;
			}

			String rootParentPath = rootParent.getCanonicalPath();

			if ( rootParentPath.endsWith( File.separator ) )
			{
				rootParentPath = rootParentPath.substring( 0, rootParentPath.length() - 1 );
			}

			if ( rootParentPath.equals( "" ) )
			{
				return text;
			}

			if ( text.toLowerCase().startsWith( rootParentPath.toLowerCase() ) )
			{
				return ".." + File.separator + text.substring( rootParentPath.length() + 1 );
			}

			File rootParentParent = rootParent.getParentFile();

			if ( rootParentParent == null )
			{
				return text;
			}

			String rootParentParentPath = rootParentParent.getCanonicalPath();

			if ( rootParentParentPath.endsWith( File.separator ) )
			{
				rootParentParentPath = rootParentParentPath.substring( 0, rootParentParentPath.length() - 1 );
			}

			if ( rootParentParentPath.equals( "" ) )
			{
				return text;
			}

			if ( text.toLowerCase().startsWith( rootParentParentPath.toLowerCase() ) )
			{
				return ".." + File.separator + ".." + File.separator +
					text.substring( rootParentParentPath.length() + 1 );
			}

			return text;
		}

		public void focusLost( final FocusEvent e )
		{
			GenericPanel.this.actionConfirmed();
		}

		public void focusGained( final FocusEvent e )
		{
		}

		public void actionPerformed( final ActionEvent e )
		{
			if ( this.path != null )
			{
				try
				{
					JFileChooser chooser = new JFileChooser( this.path.getCanonicalPath() );
					chooser.showOpenDialog( null );

					if ( chooser.getSelectedFile() == null )
					{
						return;
					}

					this.setText( chooser.getSelectedFile().getCanonicalPath() );
				}
				catch ( IOException e1 )
				{

				}
			}

			GenericPanel.this.actionConfirmed();
		}

		public JLabel getLabel()
		{
			return this.label;
		}
	}

	public class ActionConfirmListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			if ( getKeyCode() != KeyEvent.VK_ENTER )
			{
				return;
			}

			GenericPanel.this.actionConfirmed();
		}
	}

	private class ConfirmedListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			if ( GenericPanel.this.contentSet )
			{
				GenericPanel.this.actionConfirmed();
			}
		}
	}

	private class CancelledListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			if ( GenericPanel.this.contentSet )
			{
				GenericPanel.this.actionCancelled();
			}
		}
	}
}
