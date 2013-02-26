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

package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;

import java.util.Enumeration;
import java.util.Iterator;


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

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class MaximizerFrame
	extends GenericFrame
	implements ListSelectionListener
{
	private static String [] expressions =
	{
		"mainstat",
		"mus",
		"mys",
		"mox",
		"familiar weight",
		"HP",
		"MP",
		"ML",
		"DA",
		"DR",
		"+combat -tie",
		"-combat -tie",
		"initiative",
		"exp",
		"meat drop",
		"item drop",
		"2.0 meat, 1.0 item",
		"item, sea",
		"weapon dmg",
		"ranged dmg",
		"elemental dmg",
		"spell dmg",
		"adv",
		"pvp fights",
		"hot res",
		"cold res",
		"spooky res",
		"stench res",
		"sleaze res",
		"all res",
		"mp regen",
		"ML, 0.001 slime res",
		"4 clownosity",
		"7 raveosity",
		"+four songs",
	};

	public static final JComboBox expressionSelect = new JComboBox( expressions );
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

	private static final String HELP_STRING = "<html><table width=750><tr><td>" +
		"<h3>General</h3>" +
		"The specification of what attributes to maximize is made by a comma-separated list of keywords, each possibly preceded by a numeric weight.  Commas can be omitted if the next item starts with a +, -, or digit.  Using just a +, or omitting the weight entirely, is equivalent to a weight of 1.  Likewise, using just a - is equivalent to a weight of -1.  Non-integer weights can be used, but may not be meaningful with all keywords." +
		"<h3>Numeric Modifiers</h3>" +
		"The full name of any numeric modifier (as shown by the <b>modref</b> CLI command) is a valid keyword, requesting that its value be maximized.  If multiple modifiers are given, their weights specify their relative importance.  Negative weights mean that smaller values are more desirable for that modifier." +
		"<p>" +
		"Shorter forms are allowed for many commonly used modifiers.  They can be abbreviated down to just the bold letters:" +
		"<br><b>mus</b>, <b>mys</b>, <b>mox</b>, <b>main</b>stat, <b>HP</b>, <b>MP</b>, <b>ML</b>, <b>DA</b>, <b>DR</b>, <b>com</b>bat rate, <b>item</b> drop, <b>meat</b> drop, <b>exp</b>erience, <b>adv</b>entures" +
		"<br>Also, resistance (of any type) can be abbreviated as <b>res</b>, and damage can be abbreviated as <b>dmg</b>.  <b>all res</b>istance is a shortcut for giving the same weight to all five basic elements.  Likewise, <b>elemental dmg</b> is a shortcut for the five elemental damage types." +
		"<p>" +
		"Note that many modifiers come in pairs: a base value, plus a percentage boost (such as Moxie and Moxie Percent), or a penalty value.  In general, you only need to specify the base modifier, and any related modifiers will automatically be taken into account." +
		"<h3>Limits</h3>" +
		"Any numeric modifier keyword can be followed by one or both of these special keywords:" +
		"<br><b>min</b> - The weight specifies the minimum acceptable value for the preceding modifier.  If the value is lower, the results will be flagged as a failure." +
		"<br><b>max</b> - The weight specifies the largest useful value for the preceding modifier.  Larger values will be ignored in the score calculation, allowing other specified modifiers to be boosted instead." +
		"<br>Note that the limit keywords won't quite work as expected for a modifier that you're trying to minimize." +
		"<br>If <b>min</b> or <b>max</b> is specified at the start of the expression, it applies to the total score (the sum of each modifier value times its weight).  A global <b>max</b> may allow equipment maximization to finish faster, since no further combinations will be considered once the specified value is reached." +
		"<h3>Other Modifiers</h3>" +
		"Boolean modifiers can also be used as keywords.  With positive weight, the modifier is required to be true; with negative weight, it is required to be false.  There is one shortcut available: <b>sea</b> requires both Adventure Underwater and Underwater Familiar to be true." +
		"<p>" +
		"The only bitmap modifiers that currently appear useful for maximization are Clownosity and Raveosity, so they are allowed as a special case.  The weight specifies the required minimum value; only one value is actually meaningful for each keyword, so <b>4 clownosity</b> and <b>7 raveosity</b> are the only useful forms." +
		"<p>" +
		"String modifiers are not currently meaningful for maximization." +
		"<p>" +
		"The 'Bees Hate You' challenge path adds a <b>beeosity</b> keyword, which specifies the maximum number of 'B's allowed in the names of your equipment items (each of which causes 10% of your maximum HP in damage at the start of every combat).  The default is 2 at the moment.  The value you specify will automatically be increased if you use a <b>+equip</b> or <b>+outfit</b> keyword (described below) that requires more 'B's to satisfy." +
		"<h3>Equipment</h3>" +
		"Slot names can be used as keywords:" +
		"<br><b>hat</b>, <b>weapon</b>, <b>offhand</b>, <b>shirt</b>, <b>pants</b>, <b>acc1</b>, <b>acc2</b>, <b>acc3</b>, <b>familiar</b> (stickers and fake hands are not currently planned.)" +
		"<br>With positive weights, only the specified slots will be considered for maximization.  With negative weights, all but the specified slots will be considered." +
		"<br><b>empty</b> - With positive weight, consider only slots that are currently empty; with negative weight, only those that aren't empty.  Either way, <b>+<i>slot</i></b> and <b>-<i>slot</i></b> can be used to further refine the selected slots." +
		"<br><b>hand</b>ed - With a weight of 1, only 1-handed weapons will be considered.  With a larger weight, only weapons with at least that handedness will be considered." +
		"<br><b>melee</b> - With positive weight, only melee weapons will be considered.  With negative weight, only ranged weapons will be considered." +
		"<br><b>type <i>text</i></b> - Only weapons with a type containing <i>text</i> are considered; for example, <b>type club</b> if you plan to do some Seal Clubbing." +
		"<br><b>shield</b> - With positive weight, only shields will be considered for your off-hand.  Implies <b>1 handed</b>." +
		"<br><b>equip <i>item</i></b> - The specified item is required (positive weight) or forbidden (negative weight).  Multiple uses of <b>+equip</b> require all of the items to be equipped." +
		"<br><b>outfit <i>name</i></b> - The specified standard outfit is required or forbidden.  If the name is omitted, the currently equipped outfit is used.  Multiple uses of <b>+outfit</b> are satisfied by any one of the outfits (since you can't be wearing more than one at a time)." +
		"<br>If both <b>+equip</b> and <b>+outfit</b> are used together, either one will satisfy the condition - all of the items, or one of the outfits.  This special case is needed to be able to specify the conditions for adventuring in the Pirate Cove." +
		"<br><b>tie</b>breaker - With negative weight, disables the use of a tiebreaker function that tries to choose equipment with generally beneficial attributes, even if not explicitly requested.  There are only a few cases where this would be desirable: maximizing <b>+combat</b> or <b>-combat</b> (since there's usually only one item that can help), <b>adv</b> and/or <b>PvP fights</b> at rollover, and <b>familiar weight</b> when facing the Naughty Sorceress familiars." +
		"<h3>Familiars</h3>" +
		"By default, the Modifier Maximizer does not recommend familiars, since there are many possible factors in choosing one beyond those that can be expressed via modifiers.  However, you can request that specific familiars be compared with your current one:" +
		"<br><b>switch <i>familiar</i></b> - With positive weight, the familiar is added to the list to be considered (unless the player lacks that familiar, or is already using it, in which case there is no effect).  With negative weight, the familiar is added to the list only if the player lacks the previously specified familiar.  For example, <b>switch hobo monkey, -switch leprechaun</b> will only consider the leprechaun if the player doesn't have the monkey." +
		"<h3>Assumptions</h3>" +
		"All suggestions are based on the assumption that you will be adventuring in the currently selected location, with all your current effects, prior to the next rollover (since some things depend on the moon phases).  For best results, make sure the proper location is selected before maximizing.  This is especially true in The Sea and clan dungeons, which have many location-specific modifiers." +
		"<p>" +
		"Among effects, stat equalizer potions have a major effect on the suggested boosts, since they change the relative importance of additive and percentage stat boosts.  Likewise, elemental phials make certain resistance boosts pointless.  If you plan to use an equalizer or phial while adventuring, please use them first so that the suggestions take them into account." +
		"<h3>GUI Use</h3>" +
		"If the Max Price field is zero or blank, the limit will be the smaller of your available meat, or your autoBuyPriceLimit (default 20,000).  The other options should be self-explanatory." +
		"<p>" +
		"You can select multiple boosts, and the title of the list will indicate the net effect of applying them all - note that this isn't always just the sum of their individual effects." +
		"<h3>CLI Use</h3>" +
		"The Modifier Maximizer can be invoked from the gCLI or a script via <b>maximize <i>expression</i></b>, and will behave as if you'd selected Equipment: on-hand only, Max Price: don't check, and turned off the Include option.  The best equipment will automatically be equipped (unless you invoked the command as <b>maximize? <i>expression</i></b>), but you'll still need to visit the GUI to apply effect boosts - there are too many factors in choosing between the available boosts for that to be safely automated.  An error will be generated if the equipment changes weren't sufficient to fulfill all <b>min</b> keywords in the expression." +
		"<h3>Limitations &amp; Bugs</h3>" +
		"This is still a work-in-progress, so don't expect everything to work perfectly at the moment.  However, here are some details that are especially broken:" +
		"<br>\u2022 Items that can be installed at your campground for a bonus (such as Hobopolis bedding) aren't considered." +
		"<br>\u2022 Your song limit isn't considered when recommending buffs, nor are any daily casting limits." +
		"<br>\u2022 If more than one of a given item is being recommended, and some of the required quantity isn't already in your inventory (in your closet, perhaps), trying to equip them fails.  After all the items from inventory are equipped, further attempts will steal one of those items from its slot rather than considering other retrieval options." +
		"<br>\u2022 Weapon Damage, Ranged Damage, and Spell Damage are calculated assuming 100 points of base damage - in other words, additive and percentage boosts are considered to have exactly equal worth.  It's possible that Weapon and Ranged damage might use a better estimate of the base damage in the future, but for Spell Damage, the proper base depends on which spell you end up using." +
		"<br>\u2022 Effects which vary in power based on how many turns are left (love songs, Mallowed Out, etc.) are handled poorly.  If you don't have the effect, they'll be suggested based on the results you'd get from having a single turn of it.  If you have the effect already, extending it to raise the power won't even be considered.  Similar problems occur with effects that are based on how full or drunk you currently are." +
		"</td></tr></table></html>";

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
			if ( Preferences.getInteger( "maximizerMRULength" ) > 0 )
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
		if ( Preferences.getInteger( "maximizerMRULength") > 0)
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
