/**
 * Copyright (c) 2003, Spellcast development team
 * http://spellcast.dev.java.net/
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
 *  [3] Neither the name "Spellcast development team" nor the names of
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

package net.java.dev.spellcast.utilities;

// layout
import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import java.awt.BorderLayout;
import java.awt.GridLayout;

// event listeners
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

// containers
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;

public abstract class ActionVerifyPanel extends JRootPane
{
	private boolean contentSet;
	private JComponent eastContainer;
	private VerifyButtonPanel buttonPanel;
	private Dimension labelSize, fieldSize;

	private static final Dimension DEFAULT_LABEL_SIZE = new Dimension( 100, 20 );
	private static final Dimension DEFAULT_FIELD_SIZE = new Dimension( 165, 20 );

	public ActionVerifyPanel( String confirmedText, String cancelledText )
	{	this( confirmedText, cancelledText, DEFAULT_LABEL_SIZE, DEFAULT_FIELD_SIZE );
	}

	public ActionVerifyPanel( String confirmedText, String cancelledText, Dimension labelSize, Dimension fieldSize )
	{
		contentSet = false;
		this.labelSize = labelSize;
		this.fieldSize = fieldSize;
		buttonPanel = new VerifyButtonPanel( confirmedText, cancelledText );
	}

	protected void setContent( VerifiableElement [] elements )
	{	setContent( elements, null, null, null );
	}

	protected void setContent( VerifiableElement [] elements, JPanel [] extras, JPanel westPanel, JPanel eastPanel )
	{
		if ( contentSet )
			return;

		JPanel container = new JPanel();

		container.setLayout( new BorderLayout( 10, 10 ) );
		container.add( Box.createVerticalStrut( 2 ), BorderLayout.NORTH );

		// add the west container
		container.add( constructWestContainer( elements, westPanel ), BorderLayout.WEST );

		// add the extras panel
		JPanel extrasPanel = constructExtrasPanel( extras );
		if ( extrasPanel != null )
			container.add( extrasPanel, BorderLayout.CENTER );

		// construct the east container, which usually consists of only the
		// button panel, if an east panel is not specified; if one happens
		// to be specified, then it appears above the button panel

		JPanel eastContainer = new JPanel();
		eastContainer.setLayout( new BorderLayout() );

		if ( eastPanel != null )
		{
			eastContainer.add( eastPanel, BorderLayout.NORTH );
			eastContainer.add( Box.createVerticalStrut( 10 ), BorderLayout.CENTER );
			eastContainer.add( buttonPanel, BorderLayout.SOUTH );
		}
		else
			eastContainer.add( buttonPanel, BorderLayout.NORTH );

		container.add( eastContainer, BorderLayout.EAST );

		setLayout( new BorderLayout() );
		add( container, BorderLayout.CENTER );
		contentSet = true;
	}

	private JPanel constructWestContainer( VerifiableElement [] elements, JPanel westPanel )
	{
		JPanel westContainer = new JPanel();
		westContainer.setLayout( new BorderLayout() );

		if ( westPanel != null )
		{
			westContainer.add( westPanel, BorderLayout.NORTH );
			westContainer.add( Box.createVerticalStrut( 10 ), BorderLayout.CENTER );
		}

		if ( elements != null && elements.length > 0 )
		{
			JPanel elementsContainer = new JPanel();
			elementsContainer.setLayout( new BoxLayout( elementsContainer, BoxLayout.X_AXIS ) );

			elementsContainer.add( constructLabelPanel( elements ) );
			elementsContainer.add( Box.createHorizontalStrut( 10 ) );
			elementsContainer.add( constructFieldPanel( elements ) );

			westContainer.add( elementsContainer,
				westPanel == null ? BorderLayout.NORTH : BorderLayout.SOUTH );
		}

		return westContainer;
	}

	private JPanel constructLabelPanel( VerifiableElement [] elements )
	{
		JPanel labelPanel = new JPanel();
		labelPanel.setLayout( new BoxLayout( labelPanel, BoxLayout.Y_AXIS ) );
		labelPanel.add( Box.createVerticalStrut( 5 ) );

		JLabel label;
		for ( int i = 0; i < elements.length; ++i )
		{
			label = elements[i].getLabel();

			JComponentUtilities.setComponentSize( label, labelSize );
			labelPanel.add( label );
			labelPanel.add( Box.createVerticalStrut( 5 ) );
		}

		return labelPanel;
	}

	private JPanel constructFieldPanel( VerifiableElement [] elements )
	{
		JPanel fieldPanel = new JPanel();
		fieldPanel.setLayout( new BoxLayout( fieldPanel, BoxLayout.Y_AXIS ) );
		fieldPanel.add( Box.createVerticalStrut( 5 ) );

		JComponent inputField;

		for ( int i = 0; i < elements.length; ++i )
		{
			inputField = elements[i].getInputField();

			if ( inputField instanceof JTextField )
			{
				JComponentUtilities.setComponentSize( inputField, fieldSize );
				fieldPanel.add( inputField );
			}
			else
			{
				JPanel containerPanel = new JPanel();
				containerPanel.setLayout( new BoxLayout( containerPanel, BoxLayout.X_AXIS ) );
				containerPanel.add( inputField );
				JComponentUtilities.setComponentSize( containerPanel, fieldSize );
				fieldPanel.add( containerPanel );
			}
			fieldPanel.add( Box.createVerticalStrut( 5 ) );
		}

		return fieldPanel;
	}

	private JPanel constructExtrasPanel( JPanel [] extras )
	{
		if ( extras == null || extras.length < 1 )
			return null;

		JPanel extrasPanel = new JPanel();
		extrasPanel.setLayout( new BorderLayout() );

		if ( extras != null )
		{
			if ( extras.length > 0 )
				extrasPanel.add( extras[0], BorderLayout.NORTH );

			if ( extras.length > 1 )
				extrasPanel.add( extras[ extras.length - 1 ], BorderLayout.SOUTH );

			if ( extras.length > 2 )
			{
				JPanel centerPanel = new JPanel();
				centerPanel.setLayout( new BoxLayout( extrasPanel, BoxLayout.Y_AXIS ) );

				for ( int i = 1; i < extras.length - 1; ++i )
				{
					centerPanel.add( extras[i] );
					centerPanel.add( Box.createVerticalStrut( 5 ) );
				}

				extrasPanel.add( centerPanel, BorderLayout.CENTER );
			}
		}

		return extrasPanel;
	}

	public void setEnabled( boolean isEnabled )
	{	buttonPanel.setEnabled( isEnabled );
	}

	protected abstract void actionConfirmed();
	protected abstract void actionCancelled();

	private class VerifyButtonPanel extends JPanel
	{
		private JButton confirmedButton, cancelledButton;

		public VerifyButtonPanel( String confirmedText, String cancelledText )
		{
 			setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );

			// add the "confirmed" button
			confirmedButton = new JButton( confirmedText );
			confirmedButton.addActionListener(
				new ActionListener() {
					public void actionPerformed( ActionEvent e ) {
						actionConfirmed();
					}
				} );

			addButton( confirmedButton );
			ActionVerifyPanel.this.setDefaultButton( confirmedButton );
			add( Box.createVerticalStrut( 4 ) );

			// add the "cancelled" button
			cancelledButton = new JButton( cancelledText );
			cancelledButton.addActionListener(
				new ActionListener() {
					public void actionPerformed( ActionEvent e ) {
						actionCancelled();
					}
				} );
			addButton( cancelledButton );
		}

		private void addButton( JButton buttonToAdd )
		{
			JPanel container = new JPanel();
			container.setLayout( new GridLayout() );
			container.add( buttonToAdd );
			container.setMaximumSize( new Dimension( Integer.MAX_VALUE, 24 ) );
			add( container );
		}

		public void setEnabled( boolean isEnabled )
		{	confirmedButton.setEnabled( isEnabled );
		}
	}

	protected final class VerifiableElement
	{
		private JLabel label;
		private JComponent inputField;

		public VerifiableElement( String label, JComponent inputField )
		{
			this.label = new JLabel( label, JLabel.RIGHT );
			this.inputField = inputField;
		}

		public JLabel getLabel()
		{	return label;
		}

		public JComponent getInputField()
		{	return inputField;
		}
	}
}
