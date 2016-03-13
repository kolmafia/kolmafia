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
import java.awt.LayoutManager;
import java.awt.event.ActionListener;

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
import javax.swing.JTextArea;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import org.jdesktop.swingx.JXCollapsiblePane;

import com.sun.java.forums.SpringUtilities;

public abstract class ActionVerifyPanel
	extends ActionPanel
{
	protected VerifiableElement[] elements;

	protected JPanel container;
	protected JPanel mainContainer;
	protected JPanel eastContainer;

	private String confirmedText, cancelledText1, cancelledText2;
	private ActionListener confirmListener, cancelListener;

	private VerifyButtonPanel buttonPanel;
	private final boolean isCenterPanel;
	private final Dimension left, right;

	private static final Dimension DEFAULT_LEFT = new Dimension( 100, 20 );
	private static final Dimension DEFAULT_RIGHT = new Dimension( 165, 20 );

	public ActionVerifyPanel()
	{
		this( null, null, ActionVerifyPanel.DEFAULT_LEFT, ActionVerifyPanel.DEFAULT_RIGHT, false );
	}

	public ActionVerifyPanel( final String confirmedText )
	{
		this( confirmedText, null, null, ActionVerifyPanel.DEFAULT_LEFT, ActionVerifyPanel.DEFAULT_RIGHT, false );
	}

	public ActionVerifyPanel( final String confirmedText, final boolean isCenterPanel )
	{
		this( confirmedText, null, null, ActionVerifyPanel.DEFAULT_LEFT, ActionVerifyPanel.DEFAULT_RIGHT, isCenterPanel );
	}

	public ActionVerifyPanel( final Dimension left, final Dimension right )
	{
		this( null, null, left, right, false );
	}

	public ActionVerifyPanel( final Dimension left, final Dimension right, final boolean isCenterPanel )
	{
		this( null, null, left, right, isCenterPanel );
	}

	public ActionVerifyPanel( final String confirmedText, final Dimension left, final Dimension right,
		final boolean isCenterPanel )
	{
		this( confirmedText, null, null, left, right, isCenterPanel );
	}

	public ActionVerifyPanel( final String confirmedText, final String cancelledText )
	{
		this( confirmedText, cancelledText, ActionVerifyPanel.DEFAULT_LEFT, ActionVerifyPanel.DEFAULT_RIGHT, false );
	}

	public ActionVerifyPanel( final String confirmedText, final String cancelledText, final boolean isCenterPanel )
	{
		this(
			confirmedText, cancelledText, ActionVerifyPanel.DEFAULT_LEFT, ActionVerifyPanel.DEFAULT_RIGHT,
			isCenterPanel );
	}

	public ActionVerifyPanel( final String confirmedText, final String cancelledText1, final String cancelledText2 )
	{
		this(
			confirmedText, cancelledText1, cancelledText2, ActionVerifyPanel.DEFAULT_LEFT,
			ActionVerifyPanel.DEFAULT_RIGHT, false );
	}

	public ActionVerifyPanel( final String confirmedText, final String cancelledText, final Dimension left,
		final Dimension right )
	{
		this( confirmedText, cancelledText, left, right, false );
	}

	public ActionVerifyPanel( final String confirmedText, final String cancelledText1, final String cancelledText2,
		final Dimension left, final Dimension right )
	{
		this( confirmedText, cancelledText1, cancelledText2, left, right, false );
	}

	public ActionVerifyPanel( final String confirmedText, final String cancelledText, final Dimension left,
		final Dimension right, final boolean isCenterPanel )
	{
		this( confirmedText, cancelledText, cancelledText, left, right, isCenterPanel );
	}

	public ActionVerifyPanel( final String confirmedText, final String cancelledText1, final String cancelledText2,
		final Dimension left, final Dimension right, final boolean isCenterPanel )
	{
		this.contentSet = false;

		this.left = left;
		this.right = right;

		this.isCenterPanel = isCenterPanel;

		this.confirmedText = confirmedText;
		this.cancelledText1 = cancelledText1;
		this.cancelledText2 = cancelledText2;
	}

	protected void setListeners( ActionListener confirmListener, ActionListener cancelListener )
	{
		this.confirmListener = confirmListener;
		this.cancelListener = cancelListener;

		if ( this.confirmedText != null && !this.confirmedText.equals( "" ) )
		{
			this.buttonPanel =
				new VerifyButtonPanel(
					this.confirmedText, this.cancelledText1, this.cancelledText2,
					this.confirmListener, this.cancelListener );
		}
	}

	protected void setContent( final VerifiableElement[] elements )
	{
		this.setContent( elements, false );
	}

	protected void setContent( final VerifiableElement[] elements, final boolean bothDisabledOnClick )
	{
		if ( this.contentSet )
		{
			return;
		}

		this.elements = elements;

		this.container = new JPanel();

		this.container.setLayout( new BorderLayout( 10, 10 ) );
		this.container.add( Box.createVerticalStrut( 2 ), BorderLayout.NORTH );

		// add the main container
		this.container.add( this.constructMainContainer( elements ), BorderLayout.CENTER );

		// construct the east container, which usually consists of only the
		// button panel, if an east panel is not specified; if one happens
		// to be specified, then it appears above the button panel

		this.eastContainer = new JPanel();
		this.eastContainer.setLayout( new BorderLayout( 10, 10 ) );

		if ( this.buttonPanel != null )
		{
			this.eastContainer.add( this.buttonPanel, BorderLayout.NORTH );
		}

		this.container.add( this.eastContainer, BorderLayout.EAST );

		JPanel cardContainer = new JPanel();
		cardContainer.setLayout( new CardLayout( 10, 10 ) );
		cardContainer.add( this.container, "" );

		this.setLayout( new BorderLayout() );
		this.add( cardContainer, this.isCenterPanel ? BorderLayout.CENTER : BorderLayout.NORTH );

		this.contentSet = true;

		if ( this.buttonPanel != null )
		{
			this.buttonPanel.setBothDisabledOnClick( bothDisabledOnClick );
		}
	}

	private JPanel constructMainContainer( final VerifiableElement[] elements )
	{
		JPanel mainContainer = new JPanel();
		mainContainer.setLayout( new BoxLayout( mainContainer, BoxLayout.Y_AXIS ) );

		if ( elements == null || elements.length == 0 )
		{
			return mainContainer;
		}

		// Layout the elements using springs
		// instead of standard panel elements.

		int springCount = 0;
		JPanel currentContainer = null;

		for ( int i = 0; i < elements.length; ++i )
		{
			VerifiableElement element = elements[ i ];
			boolean hideable = element instanceof HideableVerifiableElement;
			boolean isInputPreceding = element.isInputPreceding();
			JLabel label = element.getLabel();
			JComponent inputField = element.getInputField();
			boolean shouldResize = element.shouldResize();

			if ( isInputPreceding &&
			     ( inputField instanceof JCheckBox || inputField instanceof JRadioButton ) )
			{
				if ( springCount > 0 )
				{
					SpringUtilities.makeCompactGrid( currentContainer, springCount, 2, 5, 5, 5, 5 );
					springCount = 0;
					mainContainer.add( currentContainer );
					currentContainer = null;
				}

				if ( currentContainer == null )
				{
					currentContainer = new JPanel();
					currentContainer.setLayout( new BoxLayout( currentContainer, BoxLayout.Y_AXIS ) );
					currentContainer.setAlignmentX( Component.LEFT_ALIGNMENT );
				}

				if ( inputField instanceof JCheckBox )
				{
					( (JCheckBox) inputField ).setText( label.getText() );
				}
				else if ( inputField instanceof JRadioButton )
				{
					( (JRadioButton) inputField ).setText( label.getText() );
				}

				currentContainer.add( inputField );
				currentContainer.add( Box.createVerticalStrut( 5 ) );
			}
			else if ( inputField instanceof JLabel &&
				  label.getText().equals( "" ) )
			{
				if ( currentContainer == null )
				{
					GridLayout layout = new GridLayout( 0, 1, 5, 5 );
					currentContainer = new JPanel( layout );
					currentContainer.setAlignmentX( Component.LEFT_ALIGNMENT );
				}

				if ( springCount == 0 )
				{
					currentContainer.add( inputField );
				}
				else
				{
					JComponentUtilities.setComponentSize( label, isInputPreceding ? this.right : this.left );
					JComponentUtilities.setComponentSize( inputField, isInputPreceding ? this.left : this.right );

					currentContainer.add( isInputPreceding ? inputField : label );
					currentContainer.add( isInputPreceding ? label : inputField );
					++springCount;
				}
			}
			else
			{
				if ( shouldResize )
				{
					JComponentUtilities.setComponentSize( label, isInputPreceding ? this.right : this.left );
					JComponentUtilities.setComponentSize( inputField, isInputPreceding ? this.left : this.right );
				}


				if ( hideable )
				{
					if ( springCount > 0 )
					{
						SpringUtilities.makeCompactGrid( currentContainer, springCount, 2, 5, 5, 5, 5 );
						springCount = 0;
					}

					if ( currentContainer != null )
					{
						mainContainer.add( currentContainer );
						currentContainer = null;
					}

					HideablePanel container = new HideablePanel( new SpringLayout() );
					container.setAlignmentX( Component.LEFT_ALIGNMENT );

					container.add( isInputPreceding ? inputField : label );
					container.add( isInputPreceding ? label : inputField );

					SpringUtilities.makeCompactGrid( container.getContentPane(), 1, 2, 5, 5, 5, 5 );

					mainContainer.add( container );
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

					currentContainer.add( isInputPreceding ? inputField : label );
					currentContainer.add( isInputPreceding ? label : inputField );
				}
			}
		}

		if ( springCount > 0 )
		{
			SpringUtilities.makeCompactGrid( currentContainer, springCount, 2, 5, 5, 5, 5 );
		}

		if ( currentContainer != null )
		{
			mainContainer.add( currentContainer );
		}

		this.mainContainer = mainContainer;

		JPanel holder = new JPanel( new BorderLayout() );
		holder.add( mainContainer, BorderLayout.NORTH );

		return holder;
	}

	public void hideOrShowElements()
	{
		if ( this.elements == null )
		{
			return;
		}
		
		Component[] components = this.mainContainer.getComponents();
		int componentIndex = 0;

		for ( VerifiableElement element : this.elements )
		{
			if ( element instanceof HideableVerifiableElement )
			{
				HideableVerifiableElement hideableElement = (HideableVerifiableElement) element;
				boolean shouldBeHidden = hideableElement.isHidden();
				while ( componentIndex < components.length )
				{
					Component component = components[ componentIndex++ ];
					if ( component instanceof HideablePanel )
					{
						HideablePanel hideablePanel = (HideablePanel) component;
						boolean isHidden = hideablePanel.isCollapsed();
						if ( shouldBeHidden != isHidden )
						{
							hideablePanel.setCollapsed( shouldBeHidden );
						}
					}
				}
			}
		}
	}

	@Override
	public void setEnabled( final boolean isEnabled )
	{
		if ( this.buttonPanel != null )
		{
			this.buttonPanel.setEnabled( isEnabled );
		}
	}

	@Override
	public abstract void actionConfirmed();

	@Override
	public abstract void actionCancelled();

	@Override
	public void dispose()
	{
		if ( this.buttonPanel != null )
		{
			this.buttonPanel.dispose();
		}

		if ( this.elements != null )
		{
			for ( int i = 0; i < this.elements.length; ++i )
			{
				this.elements[ i ].removeListeners( this.elements[ i ].getInputField() );
				this.elements[ i ] = null;
			}
		}
	}

	protected class HideablePanel
		extends JXCollapsiblePane
	{
		public HideablePanel()
		{
			super();
			this.initialize();
		}

		public HideablePanel( LayoutManager layout)
		{
			super();
			this.setLayout( layout );
			this.initialize();
		}

		private void initialize()
		{
			this.setMinimumSize( new Dimension( 0, 0 ) ); // collapse all the way when hidden
			this.setAnimated( false );
		}
	}

	protected class VerifiableElement
		implements Comparable
	{
		private final JLabel label;
		private final boolean shouldResize;
		private final JComponent inputField;
		private final boolean isInputPreceding;

		public VerifiableElement()
		{
			this( "", SwingConstants.RIGHT, new JLabel( " " ), false );
		}

		public VerifiableElement( final JTextArea text )
		{
			this( "", SwingConstants.RIGHT, text, false );
		}

		public VerifiableElement( final JComponent inputField )
		{
			this(
				"", SwingConstants.RIGHT, inputField,
				!( inputField instanceof JScrollPane || inputField instanceof JCheckBox ) );
		}

		public VerifiableElement( final String label, final JComponent inputField )
		{
			this(
				label, SwingConstants.RIGHT, inputField,
				!( inputField instanceof JScrollPane || inputField instanceof JCheckBox ) );
		}

		public VerifiableElement( final String label, final JComponent inputField, final boolean shouldResize )
		{
			this( label, SwingConstants.RIGHT, inputField, shouldResize );
		}

		public VerifiableElement( final String label, final int direction, final JComponent inputField )
		{
			this( label, direction, inputField, !( inputField instanceof JScrollPane || inputField instanceof JCheckBox ) );
		}

		public VerifiableElement( final String label, final int direction, final JComponent inputField,
			final boolean shouldResize )
		{
			this.label = new JLabel( label, direction );
			this.inputField = inputField;
			this.shouldResize = shouldResize;
			this.isInputPreceding = direction == SwingConstants.LEFT;

			this.label.setLabelFor( inputField );
			this.label.setVerticalAlignment( SwingConstants.TOP );

			if ( ActionVerifyPanel.this.buttonPanel == null )
			{
				this.addListeners( inputField );
			}
		}

		public boolean isInputPreceding()
		{
			return this.isInputPreceding;
		}

		public void removeListeners( final JComponent c )
		{
			if ( c == null || c instanceof JLabel || c instanceof JButton )
			{
				return;
			}

			if ( c instanceof JRadioButton )
			{
				( (JRadioButton) c ).removeActionListener( ActionVerifyPanel.this.confirmListener );
				return;
			}

			if ( c instanceof JCheckBox )
			{
				( (JCheckBox) c ).removeActionListener( ActionVerifyPanel.this.confirmListener );
				return;
			}

			if ( c instanceof JComboBox )
			{
				( (JComboBox) c ).removeActionListener( ActionVerifyPanel.this.confirmListener );
				return;
			}

			for ( int i = 0; i < c.getComponentCount(); ++i )
			{
				if ( !( c instanceof JLabel || c instanceof JButton ) )
				{
					this.removeListeners( (JComponent) c.getComponent( i ) );
				}
			}
		}

		private void addListeners( final JComponent c )
		{
			if ( c == null || c instanceof JLabel || c instanceof JButton )
			{
				return;
			}

			if ( c instanceof JRadioButton )
			{
				( (JRadioButton) c ).addActionListener( ActionVerifyPanel.this.confirmListener );
				return;
			}

			if ( c instanceof JCheckBox )
			{
				( (JCheckBox) c ).addActionListener( ActionVerifyPanel.this.confirmListener );
				return;
			}

			if ( c instanceof JComboBox )
			{
				( (JComboBox) c ).addActionListener( ActionVerifyPanel.this.confirmListener );
				return;
			}

			for ( int i = 0; i < c.getComponentCount(); ++i )
			{
				if ( !( c instanceof JLabel || c instanceof JButton ) )
				{
					this.addListeners( (JComponent) c.getComponent( i ) );
				}
			}
		}

		public JLabel getLabel()
		{
			return this.label;
		}

		public JComponent getInputField()
		{
			return this.inputField;
		}

		public boolean shouldResize()
		{
			return this.shouldResize;
		}

		public int compareTo( final Object o )
		{
			return o == null || !( o instanceof VerifiableElement ) ? -1 : this.label.getText().compareTo(
				( (VerifiableElement) o ).label.getText() );
		}
	}

	public abstract class HideableVerifiableElement
		extends VerifiableElement
	{
		public HideableVerifiableElement( final String label, final JComponent inputField )
		{
			super( label, inputField );
		}

		public abstract boolean isHidden();
	}
}
