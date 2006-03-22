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
import java.awt.CardLayout;
import java.awt.BorderLayout;

import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import com.sun.java.forums.SpringUtilities;

// event listeners
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

// containers
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

public abstract class ActionVerifyPanel extends ActionPanel implements ActionListener, FocusListener
{
	protected JPanel container;

	private boolean contentSet;
	private boolean isCenterPanel;
	private JComponent eastContainer;
	private VerifyButtonPanel buttonPanel;
	private Dimension labelSize, fieldSize;

	private static final Dimension DEFAULT_LABEL_SIZE = new Dimension( 100, 20 );
	private static final Dimension DEFAULT_FIELD_SIZE = new Dimension( 165, 20 );

	public ActionVerifyPanel()
	{	this( null, null, DEFAULT_LABEL_SIZE, DEFAULT_FIELD_SIZE, false );
	}

	public ActionVerifyPanel( String confirmedText )
	{	this( confirmedText, null, null, DEFAULT_LABEL_SIZE, DEFAULT_FIELD_SIZE, false );
	}

	public ActionVerifyPanel( String confirmedText, boolean isCenterPanel )
	{	this( confirmedText, null, null, DEFAULT_LABEL_SIZE, DEFAULT_FIELD_SIZE, isCenterPanel );
	}

	public ActionVerifyPanel( Dimension labelSize, Dimension fieldSize )
	{	this( null, null, labelSize, fieldSize, false );
	}

	public ActionVerifyPanel( Dimension labelSize, Dimension fieldSize, boolean isCenterPanel )
	{	this( null, null, labelSize, fieldSize, isCenterPanel );
	}

	public ActionVerifyPanel( String confirmedText, Dimension labelSize, Dimension fieldSize, boolean isCenterPanel )
	{	this( confirmedText, null, null, labelSize, fieldSize, isCenterPanel );
	}

	public ActionVerifyPanel( String confirmedText, String cancelledText )
	{	this( confirmedText, cancelledText, DEFAULT_LABEL_SIZE, DEFAULT_FIELD_SIZE, false );
	}

	public ActionVerifyPanel( String confirmedText, String cancelledText1, String cancelledText2 )
	{	this( confirmedText, cancelledText1, cancelledText2, DEFAULT_LABEL_SIZE, DEFAULT_FIELD_SIZE, false );
	}

	public ActionVerifyPanel( String confirmedText, String cancelledText, Dimension labelSize, Dimension fieldSize )
	{	this( confirmedText, cancelledText, labelSize, fieldSize, false );
	}

	public ActionVerifyPanel( String confirmedText, String cancelledText1, String cancelledText2, Dimension labelSize, Dimension fieldSize )
	{	this( confirmedText, cancelledText1, cancelledText2, labelSize, fieldSize, false );
	}

	public ActionVerifyPanel( String confirmedText, String cancelledText, Dimension labelSize, Dimension fieldSize, boolean isCenterPanel )
	{	this( confirmedText, cancelledText, cancelledText, labelSize, fieldSize, isCenterPanel );
	}

	public ActionVerifyPanel( String confirmedText, String cancelledText1, String cancelledText2, Dimension labelSize, Dimension fieldSize, boolean isCenterPanel )
	{
		contentSet = false;
		this.labelSize = labelSize;
		this.fieldSize = fieldSize;
		this.isCenterPanel = isCenterPanel;
		this.buttonPanel = confirmedText == null ? null : new VerifyButtonPanel( confirmedText, cancelledText1, cancelledText2 );
	}

	protected void setContent( VerifiableElement [] elements )
	{	setContent( elements, null, null, true, false );
	}

	protected void setContent( VerifiableElement [] elements, boolean isLabelPreceeding )
	{	setContent( elements, null, null, isLabelPreceeding, false );
	}

	protected void setContent( VerifiableElement [] elements, JPanel mainPanel, JPanel eastPanel )
	{	setContent( elements, mainPanel, eastPanel, true, false );
	}

	protected void setContent( VerifiableElement [] elements, JPanel mainPanel, JPanel eastPanel, boolean isLabelPreceeding, boolean bothDisabledOnClick )
	{
		if ( contentSet )
			return;

		container = new JPanel();

		container.setLayout( new BorderLayout( 10, 10 ) );
		container.add( Box.createVerticalStrut( 2 ), BorderLayout.NORTH );

		// add the main container
		container.add( constructMainContainer( elements, mainPanel, isLabelPreceeding ), BorderLayout.CENTER );

		// construct the east container, which usually consists of only the
		// button panel, if an east panel is not specified; if one happens
		// to be specified, then it appears above the button panel

		JPanel eastContainer = new JPanel();
		eastContainer.setLayout( new BorderLayout( 10, 10 ) );

		if ( this.buttonPanel != null )
			eastContainer.add( this.buttonPanel, BorderLayout.NORTH );

		if ( eastPanel != null )
			eastContainer.add( eastPanel, BorderLayout.CENTER );

		container.add( eastContainer, BorderLayout.EAST );

		JPanel cardContainer = new JPanel();
		cardContainer.setLayout( new CardLayout( 10, 10 ) );
		cardContainer.add( container, "" );

		setLayout( new BorderLayout() );
		add( cardContainer, isCenterPanel ? BorderLayout.CENTER : BorderLayout.NORTH );

		contentSet = true;

		if ( bothDisabledOnClick && buttonPanel != null )
			buttonPanel.setBothDisabledOnClick( true );
	}

	private JPanel constructMainContainer( VerifiableElement [] elements, JPanel mainPanel, boolean isLabelPreceeding )
	{
		JPanel mainContainer = new JPanel();
		mainContainer.setLayout( new BorderLayout() );

		if ( mainPanel != null )
			mainContainer.add( mainPanel, BorderLayout.NORTH );

		if ( elements != null && elements.length > 0 )
		{
			// Layout the elements using springs
			// instead of standard panel elements.

			JPanel elementsContainer = new JPanel( new SpringLayout() );

			for ( int i = 0; i < elements.length; ++i )
			{
				if ( isLabelPreceeding )
				{
					JComponentUtilities.setComponentSize( elements[i].getLabel(), labelSize );
					elementsContainer.add( elements[i].getLabel() );
				}

				if ( elements[i].shouldResize() )
					JComponentUtilities.setComponentSize( elements[i].getInputField(), fieldSize );

				elementsContainer.add( elements[i].getInputField() );

				if ( !isLabelPreceeding )
				{
					JComponentUtilities.setComponentSize( elements[i].getLabel(), labelSize );
					elementsContainer.add( elements[i].getLabel() );
				}
			}

			// Construct the compact grid with the
			// SpringUtilities module.

			SpringUtilities.makeCompactGrid( elementsContainer, elements.length, 2, 5, 5, 5, 5 );

			// Add in the original main container
			// that was being planned.

			mainContainer.add( elementsContainer, BorderLayout.CENTER );
		}

		return mainContainer;
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
	{
		if ( buttonPanel != null )
			buttonPanel.setEnabled( isEnabled );
	}

	protected abstract void actionConfirmed();
	protected abstract void actionCancelled();

	public void focusGained( FocusEvent e )
	{
	}

	public void focusLost( FocusEvent e )
	{
		if ( buttonPanel == null && isValid() )
			actionConfirmed();
	}

	public void actionPerformed( ActionEvent e )
	{
		if ( buttonPanel == null && isValid() )
			actionConfirmed();
	}

	private Object [] thisArray = { this };
	private static final Class [] ACTIONS = { ActionListener.class };
	private static final Class [] FOCUSES = { FocusListener.class };

	protected final class VerifiableElement implements Comparable
	{
		private JLabel label;
		private boolean shouldResize;
		private JComponent inputField;

		public VerifiableElement( String label, JComponent inputField )
		{	this( label, JLabel.RIGHT, inputField, !(inputField instanceof JScrollPane) );
		}

		public VerifiableElement( String label, JComponent inputField, boolean shouldResize )
		{	this( label, JLabel.RIGHT, inputField, shouldResize );
		}

		public VerifiableElement( String label, int direction, JComponent inputField )
		{	this( label, direction, inputField, !(inputField instanceof JScrollPane) );
		}

		public VerifiableElement( String label, int direction, JComponent inputField, boolean shouldResize )
		{
			this.label = new JLabel( label, direction );
			this.inputField = inputField;
			this.shouldResize = shouldResize;

			this.label.setLabelFor( inputField );
			this.label.setVerticalAlignment( JLabel.TOP );

			if ( buttonPanel == null )
				addListeners( inputField );
		}

		private void addListeners( JComponent c )
		{
			try
			{
				if ( c ==  null || c instanceof JLabel || c instanceof JButton )
					return;

				else if ( c instanceof JRadioButton || c instanceof JCheckBox || c instanceof JComboBox )
				{
					c.getClass().getMethod( "addActionListener", ACTIONS ).invoke( c, thisArray );
				}

				else if ( c instanceof JTextField || c instanceof JPasswordField )
				{
					c.getClass().getMethod( "addFocusListener", FOCUSES ).invoke( c, thisArray );
				}

				else
				{
					for ( int i = 0; i < c.getComponentCount(); ++i )
						if ( c instanceof JComponent && !(c instanceof JLabel || c instanceof JButton) )
							addListeners( (JComponent) c.getComponent(i) );
				}
			}
			catch ( Exception e )
			{
				e.printStackTrace();
			}
		}

		public JLabel getLabel()
		{	return label;
		}

		public JComponent getInputField()
		{	return inputField;
		}
		
		public boolean shouldResize()
		{	return shouldResize;
		}

		public int compareTo( Object o )
		{
			return (o == null || !(o instanceof VerifiableElement)) ? -1 :
				label.getText().compareTo( ((VerifiableElement)o).label.getText() );
		}
	}
}
