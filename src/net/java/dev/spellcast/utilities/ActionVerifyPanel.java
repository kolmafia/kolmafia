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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;

import com.sun.java.forums.SpringUtilities;

public abstract class ActionVerifyPanel extends ActionPanel
{
	protected VerifiableElement [] elements;

	protected JPanel container;
	protected JPanel eastContainer;

	private VerifyButtonPanel buttonPanel;
	private boolean isCenterPanel;
	private Dimension left, right;

	private static final Dimension DEFAULT_LEFT = new Dimension( 100, 20 );
	private static final Dimension DEFAULT_RIGHT = new Dimension( 165, 20 );

	public ActionVerifyPanel()
	{	this( null, null, DEFAULT_LEFT, DEFAULT_RIGHT, false );
	}

	public ActionVerifyPanel( String confirmedText )
	{	this( confirmedText, null, null, DEFAULT_LEFT, DEFAULT_RIGHT, false );
	}

	public ActionVerifyPanel( String confirmedText, boolean isCenterPanel )
	{	this( confirmedText, null, null, DEFAULT_LEFT, DEFAULT_RIGHT, isCenterPanel );
	}

	public ActionVerifyPanel( Dimension left, Dimension right )
	{	this( null, null, left, right, false );
	}

	public ActionVerifyPanel( Dimension left, Dimension right, boolean isCenterPanel )
	{	this( null, null, left, right, isCenterPanel );
	}

	public ActionVerifyPanel( String confirmedText, Dimension left, Dimension right, boolean isCenterPanel )
	{	this( confirmedText, null, null, left, right, isCenterPanel );
	}

	public ActionVerifyPanel( String confirmedText, String cancelledText )
	{	this( confirmedText, cancelledText, DEFAULT_LEFT, DEFAULT_RIGHT, false );
	}

	public ActionVerifyPanel( String confirmedText, String cancelledText, boolean isCenterPanel )
	{	this( confirmedText, cancelledText, DEFAULT_LEFT, DEFAULT_RIGHT, isCenterPanel );
	}

	public ActionVerifyPanel( String confirmedText, String cancelledText1, String cancelledText2 )
	{	this( confirmedText, cancelledText1, cancelledText2, DEFAULT_LEFT, DEFAULT_RIGHT, false );
	}

	public ActionVerifyPanel( String confirmedText, String cancelledText, Dimension left, Dimension right )
	{	this( confirmedText, cancelledText, left, right, false );
	}

	public ActionVerifyPanel( String confirmedText, String cancelledText1, String cancelledText2, Dimension left, Dimension right )
	{	this( confirmedText, cancelledText1, cancelledText2, left, right, false );
	}

	public ActionVerifyPanel( String confirmedText, String cancelledText, Dimension left, Dimension right, boolean isCenterPanel )
	{	this( confirmedText, cancelledText, cancelledText, left, right, isCenterPanel );
	}

	public ActionVerifyPanel( String confirmedText, String cancelledText1, String cancelledText2, Dimension left, Dimension right, boolean isCenterPanel )
	{
		contentSet = false;

		this.left = left;
		this.right = right;

		this.isCenterPanel = isCenterPanel;

		this.buttonPanel = confirmedText == null || confirmedText.equals( "" ) ? null :
			new VerifyButtonPanel( confirmedText, cancelledText1, cancelledText2 );
	}

	protected void setContent( VerifiableElement [] elements )
	{	setContent( elements, false );
	}

	protected void setContent( VerifiableElement [] elements, boolean bothDisabledOnClick )
	{
		if ( contentSet )
			return;

		this.elements = elements;

		container = new JPanel();

		container.setLayout( new BorderLayout( 10, 10 ) );
		container.add( Box.createVerticalStrut( 2 ), BorderLayout.NORTH );

		// add the main container
		container.add( constructMainContainer( elements ), BorderLayout.CENTER );

		// construct the east container, which usually consists of only the
		// button panel, if an east panel is not specified; if one happens
		// to be specified, then it appears above the button panel

		this.eastContainer = new JPanel();
		this.eastContainer.setLayout( new BorderLayout( 10, 10 ) );

		if ( this.buttonPanel != null )
			eastContainer.add( this.buttonPanel, BorderLayout.NORTH );

		container.add( eastContainer, BorderLayout.EAST );

		JPanel cardContainer = new JPanel();
		cardContainer.setLayout( new CardLayout( 10, 10 ) );
		cardContainer.add( container, "" );

		setLayout( new BorderLayout() );
		add( cardContainer, isCenterPanel ? BorderLayout.CENTER : BorderLayout.NORTH );

		contentSet = true;

		if ( buttonPanel != null )
			buttonPanel.setBothDisabledOnClick( bothDisabledOnClick );
	}

	private JPanel constructMainContainer( VerifiableElement [] elements )
	{
		JPanel mainContainer = new JPanel();
		mainContainer.setLayout( new BoxLayout( mainContainer, BoxLayout.Y_AXIS ) );

		if ( elements == null || elements.length == 0 )
			return mainContainer;

		// Layout the elements using springs
		// instead of standard panel elements.

		int springCount = 0;
		JPanel currentContainer = null;

		for ( int i = 0; i < elements.length; ++i )
		{
			if ( elements[i].isInputPreceding() && (elements[i].getInputField() instanceof JCheckBox || elements[i].getInputField() instanceof JRadioButton) )
			{
				if ( currentContainer == null )
				{
					currentContainer = new JPanel();
					currentContainer.setLayout( new BoxLayout( currentContainer, BoxLayout.Y_AXIS ) );
					currentContainer.setAlignmentX( Component.LEFT_ALIGNMENT );
				}
				else if ( springCount > 0 )
				{
					SpringUtilities.makeCompactGrid( currentContainer, springCount, 2, 5, 5, 5, 5 );
					springCount = 0;
					mainContainer.add( currentContainer );

					currentContainer = new JPanel();
					currentContainer.setLayout( new BoxLayout( currentContainer, BoxLayout.Y_AXIS ) );
					currentContainer.setAlignmentX( Component.LEFT_ALIGNMENT );
				}

				if ( elements[i].getInputField() instanceof JCheckBox )
					((JCheckBox)elements[i].getInputField()).setText( elements[i].getLabel().getText() );
				else if ( elements[i].getInputField() instanceof JRadioButton )
					((JRadioButton)elements[i].getInputField()).setText( elements[i].getLabel().getText() );

				currentContainer.add( elements[i].getInputField() );
				currentContainer.add( Box.createVerticalStrut( 5 ) );
			}
			else if ( elements[i].getInputField() instanceof JLabel && elements[i].getLabel().getText().equals( "" ) )
			{
				if ( currentContainer == null )
				{
					currentContainer = new JPanel( new GridLayout( 0, 1, 5, 5 ) );
					currentContainer.setAlignmentX( Component.LEFT_ALIGNMENT );
					currentContainer.add( elements[i].getInputField() );
				}
				else if ( springCount == 0 )
				{
					currentContainer.add( elements[i].getInputField() );
				}
				else if ( elements[i].isInputPreceding() )
				{
					JComponentUtilities.setComponentSize( elements[i].getLabel(), right );
					JComponentUtilities.setComponentSize( elements[i].getInputField(), left );

					currentContainer.add( elements[i].getInputField() );
					currentContainer.add( elements[i].getLabel() );
					++springCount;
				}
				else
				{
					JComponentUtilities.setComponentSize( elements[i].getLabel(), left );
					JComponentUtilities.setComponentSize( elements[i].getInputField(), right );

					currentContainer.add( elements[i].getLabel() );
					currentContainer.add( elements[i].getInputField() );
					++springCount;
				}
			}
			else
			{
				if ( currentContainer == null )
				{
					currentContainer = new JPanel( new SpringLayout() );
					currentContainer.setAlignmentX( Component.LEFT_ALIGNMENT );
				}
				else if ( springCount == 0 )
				{
					mainContainer.add( currentContainer );
					currentContainer = new JPanel( new SpringLayout() );
					currentContainer.setAlignmentX( Component.LEFT_ALIGNMENT );
				}

				++springCount;

				if ( elements[i].shouldResize() )
				{
					JComponentUtilities.setComponentSize( elements[i].getLabel(),
						elements[i].isInputPreceding() ? right : left );

					JComponentUtilities.setComponentSize( elements[i].getInputField(),
						elements[i].isInputPreceding() ? left : right );
				}

				if ( elements[i].isInputPreceding() )
				{
					currentContainer.add( elements[i].getInputField() );
					currentContainer.add( elements[i].getLabel() );
				}
				else
				{
					currentContainer.add( elements[i].getLabel() );
					currentContainer.add( elements[i].getInputField() );
				}
			}
		}

		if ( springCount > 0 )
			SpringUtilities.makeCompactGrid( currentContainer, springCount, 2, 5, 5, 5, 5 );

		if ( currentContainer != null )
			mainContainer.add( currentContainer );

		JPanel holder = new JPanel( new BorderLayout() );
		holder.add( mainContainer, BorderLayout.NORTH );

		return holder;
	}

	public void setEnabled( boolean isEnabled )
	{
		if ( buttonPanel != null )
			buttonPanel.setEnabled( isEnabled );
	}

	public abstract void actionConfirmed();
	public abstract void actionCancelled();

	public void dispose()
	{
		for ( int i = 0; i < this.elements.length; ++i )
		{
			this.elements[i].removeListeners( elements[i].getInputField() );
			this.elements[i] = null;
		}

		super.dispose();
	}

	protected final class VerifiableElement implements Comparable
	{
		private JLabel label;
		private boolean shouldResize;
		private JComponent inputField;
		private boolean isInputPreceding;

		public VerifiableElement()
		{	this( "", JLabel.RIGHT, new JLabel( " " ), false );
		}

		public VerifiableElement( JComponent inputField )
		{	this( "", JLabel.RIGHT, inputField, !(inputField instanceof JScrollPane || inputField instanceof JCheckBox) );
		}

		public VerifiableElement( String label, JComponent inputField )
		{	this( label, JLabel.RIGHT, inputField, !(inputField instanceof JScrollPane || inputField instanceof JCheckBox) );
		}

		public VerifiableElement( String label, JComponent inputField, boolean shouldResize )
		{	this( label, JLabel.RIGHT, inputField, shouldResize );
		}

		public VerifiableElement( String label, int direction, JComponent inputField )
		{	this( label, direction, inputField, !(inputField instanceof JScrollPane || inputField instanceof JCheckBox) );
		}

		public VerifiableElement( String label, int direction, JComponent inputField, boolean shouldResize )
		{
			this.label = new JLabel( label, direction );
			this.inputField = inputField;
			this.shouldResize = shouldResize;
			this.isInputPreceding = direction == JLabel.LEFT;

			this.label.setLabelFor( inputField );
			this.label.setVerticalAlignment( JLabel.TOP );

			if ( buttonPanel == null )
				addListeners( inputField );
		}

		public boolean isInputPreceding()
		{	return isInputPreceding;
		}

		public void removeListeners( JComponent c )
		{
			if ( c ==  null || c instanceof JLabel || c instanceof JButton )
				return;

			if ( c instanceof JRadioButton )
			{
				((JRadioButton)c).removeActionListener( CONFIRM_LISTENER );
				return;
			}

			if ( c instanceof JCheckBox )
			{
				((JCheckBox)c).removeActionListener( CONFIRM_LISTENER );
				return;
			}

			if ( c instanceof JComboBox )
			{
				((JComboBox)c).removeActionListener( CONFIRM_LISTENER );
				return;
			}

			for ( int i = 0; i < c.getComponentCount(); ++i )
				if ( c instanceof JComponent && !(c instanceof JLabel || c instanceof JButton) )
					removeListeners( (JComponent) c.getComponent(i) );
		}

		private void addListeners( JComponent c )
		{
			if ( c ==  null || c instanceof JLabel || c instanceof JButton )
				return;

			if ( c instanceof JRadioButton )
			{
				((JRadioButton)c).addActionListener( CONFIRM_LISTENER );
				return;
			}

			if ( c instanceof JCheckBox )
			{
				((JCheckBox)c).addActionListener( CONFIRM_LISTENER );
				return;
			}

			if ( c instanceof JComboBox )
			{
				((JComboBox)c).addActionListener( CONFIRM_LISTENER );
				return;
			}

			for ( int i = 0; i < c.getComponentCount(); ++i )
				if ( c instanceof JComponent && !(c instanceof JLabel || c instanceof JButton) )
					addListeners( (JComponent) c.getComponent(i) );
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
