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

package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;

import java.io.File;
import java.io.InputStream;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.util.Enumeration;
import java.util.Iterator;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.maximizer.Boost;
import net.sourceforge.kolmafia.maximizer.Maximizer;
import net.sourceforge.kolmafia.maximizer.MaximizerSpeculation;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.panel.ScrollablePanel;

import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;

import net.sourceforge.kolmafia.utilities.ByteBufferUtilities;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MaximizerFrame
	extends GenericFrame
	implements ListSelectionListener
{
	public static final JComboBox expressionSelect = new JComboBox( KoLConstants.maximizerExpressions );
	static
	{	// This has to be done before the constructor runs, since the
		// CLI "maximize" command can set the selected item prior to the
		// frame being instantiated.
		expressionSelect.setEditable( true );
	}
	private SmartButtonGroup equipmentSelect, mallSelect;
	private AutoHighlightTextField maxPriceField;
	private JCheckBox includeAll;
	private final ShowDescriptionList boostList;
	private JLabel listTitle = null;

	private static String HELP_STRING;
	static
	{
		InputStream stream = DataUtilities.getInputStream( KoLConstants.DATA_DIRECTORY, "maximizer-help.html", false );
		byte[] bytes = ByteBufferUtilities.read( stream );
		MaximizerFrame.HELP_STRING = StringUtilities.getEncodedString( bytes, "UTF-8" );
	};

	public MaximizerFrame()
	{
		super( "Modifier Maximizer" );

		JPanel wrapperPanel = new JPanel( new BorderLayout() );
		wrapperPanel.add( new MaximizerPanel(), BorderLayout.NORTH );

		this.boostList = new ShowDescriptionList( Maximizer.boosts, 12 );
		this.boostList.addListSelectionListener( this );

		wrapperPanel.add( new BoostsPanel( this.boostList ), BorderLayout.CENTER );

		this.setCenterComponent( wrapperPanel );

		if ( Maximizer.eval != null )
		{
			this.valueChanged( null );
		}
		else
		{
			if ( Preferences.getInteger( "maximizerMRUSize" ) > 0 )
			{
				KoLConstants.maximizerMList.updateJComboData( expressionSelect );
			}
		}
	}

	@Override
	public JTabbedPane getTabbedPane()
	{
		return null;
	}

	public void valueChanged( final ListSelectionEvent e )
	{
		double current = Maximizer.eval.getScore(
			KoLCharacter.getCurrentModifiers() );
		boolean failed = Maximizer.eval.failed;
		Object[] items = this.boostList.getSelectedValues();

		StringBuffer buff = new StringBuffer( "Current score: " );
		buff.append( KoLConstants.FLOAT_FORMAT.format( current ) );
		if ( failed )
		{
			buff.append( " (FAILED)" );
		}
		buff.append( " \u25CA Predicted: " );
		if ( items.length == 0 )
		{
			buff.append( "---" );
		}
		else
		{
			MaximizerSpeculation spec = new MaximizerSpeculation();
			for ( int i = 0; i < items.length; ++i )
			{
				if ( items[ i ] instanceof Boost )
				{
					((Boost) items[ i ]).addTo( spec );
				}
			}
			double score = spec.getScore();
			buff.append( KoLConstants.FLOAT_FORMAT.format( score ) );
			buff.append( " (" );
			buff.append( KoLConstants.MODIFIER_FORMAT.format( score - current ) );
			if ( spec.failed )
			{
				buff.append( ", FAILED)" );
			}
			else
			{
				buff.append( ")" );
			}
		}
		if ( this.listTitle != null )
		{
			this.listTitle.setText( buff.toString() );
		}
		if ( Preferences.getInteger( "maximizerMRUSize") > 0)
		{
			KoLConstants.maximizerMList.updateJComboData( expressionSelect );
		}
	}

	public void maximize()
	{
		Maximizer.maximize( this.equipmentSelect.getSelectedIndex(),
			InputFieldUtilities.getValue( this.maxPriceField ),
			this.mallSelect.getSelectedIndex(),
			this.includeAll.isSelected() );

		this.valueChanged( null );
	}

	private class MaximizerPanel
		extends GenericPanel
	{
		public MaximizerPanel()
		{
			super( "update", "help", new Dimension( 80, 20 ), new Dimension( 450, 20 ) );

			MaximizerFrame.this.maxPriceField = new AutoHighlightTextField();
			JComponentUtilities.setComponentSize( MaximizerFrame.this.maxPriceField, 80, -1 );
			MaximizerFrame.this.includeAll = new JCheckBox( "effects with no direct source, skills you don't have, etc." );

			JPanel equipPanel = new JPanel( new FlowLayout( FlowLayout.LEADING, 0, 0 ) );
			MaximizerFrame.this.equipmentSelect = new SmartButtonGroup( equipPanel );
			MaximizerFrame.this.equipmentSelect.add( new JRadioButton( "none" ) );
			MaximizerFrame.this.equipmentSelect.add( new JRadioButton( "on hand", true ) );
			MaximizerFrame.this.equipmentSelect.add( new JRadioButton( "creatable/foldable" ) );
			MaximizerFrame.this.equipmentSelect.add( new JRadioButton( "pullable/buyable" ) );

			JPanel mallPanel = new JPanel( new FlowLayout( FlowLayout.LEADING, 0, 0 ) );
			mallPanel.add( MaximizerFrame.this.maxPriceField );
			MaximizerFrame.this.mallSelect = new SmartButtonGroup( mallPanel );
			MaximizerFrame.this.mallSelect.add( new JRadioButton( "don't check", true ) );
			MaximizerFrame.this.mallSelect.add( new JRadioButton( "buyable only" ) );
			MaximizerFrame.this.mallSelect.add( new JRadioButton( "all consumables" ) );

			VerifiableElement[] elements = new VerifiableElement[ 4 ];
			elements[ 0 ] = new VerifiableElement( "Maximize: ", MaximizerFrame.expressionSelect );
			elements[ 1 ] = new VerifiableElement( "Equipment: ", equipPanel );
			elements[ 2 ] = new VerifiableElement( "Max price: ", mallPanel );
			elements[ 3 ] = new VerifiableElement( "Include: ", MaximizerFrame.this.includeAll );

			this.setContent( elements );
		}

		@Override
		public void actionConfirmed()
		{
			MaximizerFrame.this.maximize();
		}

		@Override
		public void actionCancelled()
		{
			//InputFieldUtilities.alert( MaximizerFrame.HELP_STRING );
			JLabel help = new JLabel( MaximizerFrame.HELP_STRING );
			//JComponentUtilities.setComponentSize( help, 750, -1 );
			GenericScrollPane content = new GenericScrollPane( help );
			JComponentUtilities.setComponentSize( content, -1, 500 );
			JOptionPane.showMessageDialog( this, content, "Modifier Maximizer help",
				JOptionPane.PLAIN_MESSAGE );
		}
	}

	private class BoostsPanel
		extends ScrollablePanel
	{
		private final ShowDescriptionList elementList;

		public BoostsPanel( final ShowDescriptionList list )
		{
			super( "Current score: --- \u25CA Predicted: ---",
				"equip all", "exec selected", list );
			this.elementList = (ShowDescriptionList) this.scrollComponent;
			MaximizerFrame.this.listTitle = this.titleComponent;
		}

		@Override
		public void actionConfirmed()
		{
			KoLmafia.forceContinue();
			boolean any = false;
			Iterator i = Maximizer.boosts.iterator();
			while ( i.hasNext() )
			{
				Object boost = i.next();
				if ( boost instanceof Boost )
				{
					boolean did = ((Boost) boost).execute( true );
					if ( !KoLmafia.permitsContinue() ) return;
					any |= did;
				}
			}
			if ( any )
			{
				MaximizerFrame.this.maximize();
			}
		}

		@Override
		public void actionCancelled()
		{
			KoLmafia.forceContinue();
			boolean any = false;
			Object[] boosts = this.elementList.getSelectedValues();
			for ( int i = 0; i < boosts.length; ++i )
			{
				if ( boosts[ i ] instanceof Boost )
				{
					boolean did = ((Boost) boosts[ i ]).execute( false );
					if ( !KoLmafia.permitsContinue() ) return;
					any |= did;
				}
			}
			if ( any )
			{
				MaximizerFrame.this.maximize();
			}
		}
	}

	public static class SmartButtonGroup
		extends ButtonGroup
	{	// A version of ButtonGroup that actually does useful things:
		// * Constructor takes a parent container, adding buttons to
		// the group adds them to the container as well.  This generally
		// removes any need for a temp variable to hold the individual
		// buttons as they're being created.
		// * getSelectedIndex() to determine which button (0-based) is
		// selected.  How could that have been missing???

		private Container parent;

		public SmartButtonGroup( Container parent )
		{
			this.parent = parent;
		}

		@Override
		public void add( AbstractButton b )
		{
			super.add( b );
			parent.add( b );
		}

		public int getSelectedIndex()
		{
			int i = 0;
			Enumeration e = this.getElements();
			while ( e.hasMoreElements() )
			{
				if ( ((AbstractButton) e.nextElement()).isSelected() )
				{
					return i;
				}
				++i;
			}
			return -1;
		}
	}

}
