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

package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.TreeMap;
import java.util.TreeSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.Speculation;

import net.sourceforge.kolmafia.moods.MoodManager;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.EffectPool.Effect;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.SkateParkRequest;
import net.sourceforge.kolmafia.request.TrendyRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.RabbitHoleManager;
import net.sourceforge.kolmafia.session.StoreManager;

import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.panel.ScrollablePanel;

import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;

import net.sourceforge.kolmafia.utilities.BooleanArray;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MaximizerFrame
	extends GenericFrame
	implements ListSelectionListener
{
	public static final LockableListModel boosts = new LockableListModel();
	public static Evaluator eval;

	private static boolean firstTime = true;

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

	public static String [] maximizationCategories =
	{
		"_hoboPower",
		"_brimstone",
		"_slimeHate",
		"_stickers",
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

	private static Spec best;
	private static int bestChecked;
	private static long bestUpdate;

	private static final String TIEBREAKER = "1 familiar weight, 1 familiar experience, 1 initiative, 5 exp, 1 item, 1 meat, 0.1 DA 1000 max, 1 DR, 0.5 all res, -10 mana cost, 1.0 mus, 0.5 mys, 1.0 mox, 1.5 mainstat, 1 HP, 1 MP, 1 weapon damage, 1 ranged damage, 1 spell damage, 1 cold damage, 1 hot damage, 1 sleaze damage, 1 spooky damage, 1 stench damage, 1 cold spell damage, 1 hot spell damage, 1 sleaze spell damage, 1 spooky spell damage, 1 stench spell damage, 1 critical, -1 fumble, 1 HP regen max, 3 MP regen max, 1 critical hit percent, 0.1 food drop, 0.1 booze drop, 0.1 hat drop, 0.1 weapon drop, 0.1 offhand drop, 0.1 shirt drop, 0.1 pants drop, 0.1 accessory drop, 1 DB combat damage";

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

		this.boostList = new ShowDescriptionList( MaximizerFrame.boosts, 12 );
		this.boostList.addListSelectionListener( this );

		wrapperPanel.add( new BoostsPanel( this.boostList ), BorderLayout.CENTER );

		this.setCenterComponent( wrapperPanel );

		if ( this.eval != null )
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
		float current = MaximizerFrame.eval.getScore(
			KoLCharacter.getCurrentModifiers() );
		boolean failed = MaximizerFrame.eval.failed;
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
			Spec spec = new Spec();
			for ( int i = 0; i < items.length; ++i )
			{
				if ( items[ i ] instanceof Boost )
				{
					((Boost) items[ i ]).addTo( spec );
				}
			}
			float score = spec.getScore();
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
		MaximizerFrame.maximize( this.equipmentSelect.getSelectedIndex(),
			InputFieldUtilities.getValue( this.maxPriceField ),
			this.mallSelect.getSelectedIndex(),
			this.includeAll.isSelected() );

		this.valueChanged( null );
	}

	public static boolean maximize( String maximizerString, int maxPrice, int priceLevel, boolean isSpeculationOnly )
	{
		MaximizerFrame.expressionSelect.setSelectedItem( maximizerString );
		int equipLevel = isSpeculationOnly ? 1 : -1;

		// iECOC has to be turned off before actually maximizing as
		// it would cause all item lookups during the process to just
		// print the item name and return null.

		KoLmafiaCLI.isExecutingCheckOnlyCommand = false;

		MaximizerFrame.maximize( equipLevel, maxPrice, priceLevel, false );

		if ( !KoLmafia.permitsContinue() )
		{
			return false;
		}

		Modifiers mods = MaximizerFrame.best.calculate();
		Modifiers.overrideModifier( "_spec", mods );

		return !MaximizerFrame.best.failed;
	}

	public static void maximize( int equipLevel, int maxPrice, int priceLevel, boolean includeAll )
	{
		KoLmafia.forceContinue();
		String maxMe = (String) MaximizerFrame.expressionSelect.getSelectedItem();
		KoLConstants.maximizerMList.addItem( maxMe );
		MaximizerFrame.eval = new Evaluator( maxMe );

		// parsing error
		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		float current = MaximizerFrame.eval.getScore( KoLCharacter.getCurrentModifiers() );

		if ( maxPrice <= 0 )
		{
			maxPrice = Math.min( Preferences.getInteger( "autoBuyPriceLimit" ),
				KoLCharacter.getAvailableMeat() );
		}

		KoLmafia.updateDisplay( MaximizerFrame.firstTime ?
			"Maximizing (1st time may take a while)..." : "Maximizing..." );
		MaximizerFrame.firstTime = false;

		MaximizerFrame.boosts.clear();
		if ( equipLevel != 0 )
		{
			if ( equipLevel > 1 )
			{
				MaximizerFrame.boosts.add( new Boost( "", "(folding equipment is not considered yet)", -1, null, 0.0f ) );
			}
			MaximizerFrame.best = new Spec();
			MaximizerFrame.best.getScore();
			// In case the current outfit scores better than any tried combination,
			// due to some newly-added constraint (such as +melee):
			MaximizerFrame.best.failed = true;
			MaximizerFrame.bestChecked = 0;
			MaximizerFrame.bestUpdate = System.currentTimeMillis() + 5000;
			try
			{
				MaximizerFrame.eval.enumerateEquipment( equipLevel, maxPrice, priceLevel );
			}
			catch ( MaximizerExceededException e )
			{
				MaximizerFrame.boosts.add( new Boost( "", "(maximum achieved, no further combinations checked)", -1, null, 0.0f ) );
			}
			catch ( MaximizerInterruptedException e )
			{
				KoLmafia.forceContinue();
				MaximizerFrame.boosts.add( new Boost( "", "<font color=red>(interrupted, optimality not guaranteed)</font>", -1, null, 0.0f ) );
			}
			Spec.showProgress();
			
			boolean[] alreadyDone = new boolean[ EquipmentManager.ALL_SLOTS ];
			
			for ( int slot = EquipmentManager.ACCESSORY1; slot <= EquipmentManager.ACCESSORY3; ++slot )
			{
				if ( MaximizerFrame.best.equipment[ slot ].getItemId() == ItemPool.SPECIAL_SAUCE_GLOVE &&
					EquipmentManager.getEquipment( slot ).getItemId() != ItemPool.SPECIAL_SAUCE_GLOVE )
				{
					equipLevel = emitSlot( slot, equipLevel, maxPrice, priceLevel, current );
					alreadyDone[ slot ] = true;
				}
			}

			for ( int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot )
			{
				if ( !alreadyDone[ slot ] )
				{
					equipLevel = emitSlot( slot, equipLevel, maxPrice, priceLevel, current );
				}
			}
		}

		current = MaximizerFrame.eval.getScore(
			KoLCharacter.getCurrentModifiers() );

		Iterator i = Modifiers.getAllModifiers();
		while ( i.hasNext() )
		{
			String name = (String) i.next();
			if ( !EffectDatabase.contains( name ) )
			{
				continue;
			}

			float delta;
			boolean isSpecial = false;
			Spec spec = new Spec();
			AdventureResult effect = new AdventureResult( name, 1, true );
			name = effect.getName();
			boolean hasEffect = KoLConstants.activeEffects.contains( effect );
			Iterator sources;
			String cmd, text;
			int price = 0;
			if ( !hasEffect )
			{
				spec.addEffect( effect );
				delta = spec.getScore() - current;
				if ( (spec.getModifiers().getRawBitmap( Modifiers.MUTEX_VIOLATIONS )
					& ~KoLCharacter.currentRawBitmapModifier( Modifiers.MUTEX_VIOLATIONS )) != 0 )
				{	// This effect creates a mutex problem that the player
					// didn't already have.  In the future, perhaps suggest
					// uneffecting the conflicting effect, but for now just skip.
					continue;
				}
				switch ( MaximizerFrame.eval.checkConstraints(
					Modifiers.getModifiers( name ) ) )
				{
				case -1:
					continue;
				case 0:
					if ( delta <= 0.0f ) continue;
					break;
				case 1:
					isSpecial = true;
				}
				sources = EffectDatabase.getAllActions( name );
				cmd = MoodManager.getDefaultAction( "lose_effect", name );
				if ( !sources.hasNext() )
				{
					if ( includeAll )
					{
						sources = Collections.singletonList(
							"(no known source of " + name + ")" ).iterator();
					}
					else continue;
				}
			}
			else
			{
				spec.removeEffect( effect );
				delta = spec.getScore() - current;
				switch ( MaximizerFrame.eval.checkConstraints(
					Modifiers.getModifiers( name ) ) )
				{
				case 1:
					continue;
				case 0:
					if ( delta <= 0.0f ) continue;
					break;
				case -1:
					isSpecial = true;
				}
				cmd = MoodManager.getDefaultAction( "gain_effect", name );
				if ( cmd.length() == 0 )
				{
					if ( includeAll )
					{
						cmd = "(find some way to remove " + name + ")";
					}
					else continue;
				}
				sources = Collections.singletonList( cmd ).iterator();
			}

			boolean haveVipKey = InventoryManager.getCount( ItemPool.VIP_LOUNGE_KEY ) > 0;
			boolean orFlag = false;
			while ( sources.hasNext() )
			{
				cmd = text = (String) sources.next();
				AdventureResult item = null;

				if ( cmd.startsWith( "#" ) )	// usage note, no command
				{
					if ( includeAll )
					{
						if ( cmd.indexOf( "BM" ) != -1 &&
							!KoLCharacter.inBadMoon() )
						{
							continue;	// no use displaying this in non-BM
						}
						text = (orFlag ? "(...or get " : "(get ")
							+ name + " via " + cmd.substring( 1 ) + ")";
						orFlag = false;
						cmd = "";
					}
					else continue;
				}

				if ( hasEffect &&
					cmd.toLowerCase().indexOf( name.toLowerCase() ) == -1 )
				{
					text = text + " (to remove " + name + ")";
				}

				if ( cmd.startsWith( "(" ) )	// preformatted note
				{
					cmd = "";
					orFlag = false;
				}
				else if ( cmd.startsWith( "use " ) || cmd.startsWith( "chew " ) ||
					cmd.startsWith( "drink " ) || cmd.startsWith( "eat " ) )
				{
					// Hardcoded exception for "Trivia Master", which has a non-standard use command.
					if ( !KoLCharacter.canInteract() && cmd.indexOf( "Trivial Avocations Card" ) != -1 )
					{
						continue;
					}
					else
					{
						item = ItemFinder.getFirstMatchingItem(
							cmd.substring( cmd.indexOf( " " ) + 1 ).trim(), false );
						if ( item == null && cmd.indexOf( "," ) == -1 )
						{
							if ( includeAll )
							{
								text = "(identify & " + cmd + ")";
								cmd = "";
							}
							else continue;
						}
					}
				}
				else if ( cmd.startsWith( "gong " ) )
				{
					item = ItemPool.get( ItemPool.GONG, 1 );
				}
				else if ( cmd.startsWith( "cast " ) )
				{
					if ( !KoLCharacter.hasSkill( UneffectRequest.effectToSkill( name ) ) )
					{
						if ( includeAll )
						{
							text = "(learn to " + cmd + ", or get it from a buffbot)";
							cmd = "";
						}
						else continue;
					}
				}
				else if ( cmd.startsWith( "friars " ) )
				{
					int lfc = Preferences.getInteger( "lastFriarCeremonyAscension" );
					int ka = Preferences.getInteger( "knownAscensions" );
					if ( lfc < ka )
					{
						continue;
					}
					else if ( Preferences.getBoolean( "friarsBlessingReceived" ) )
					{
						cmd = "";
					}
				}
				else if ( cmd.startsWith( "hatter " ) )
				{
					boolean haveEffect = KoLConstants.activeEffects.contains( EffectPool
						.get( Effect.DOWN_THE_RABBIT_HOLE ) );
					boolean havePotion = InventoryManager.hasItem( ItemPool.DRINK_ME_POTION );
					if ( !havePotion && !haveEffect )
					{
						continue;
					}
					else if ( !RabbitHoleManager.hatLengthAvailable( StringUtilities.parseInt( cmd
						.substring( 7 ) ) ) )
					{
						continue;
					}
					else if ( Preferences.getBoolean( "_madTeaParty" ) )
					{
						cmd = "";
					}
				}
				else if ( cmd.startsWith( "summon " ) )
				{
					if ( !Preferences.getString( Quest.MANOR.getPref() ).equals( QuestDatabase.FINISHED ) )
					{
						continue;
					}
					int onHand = InventoryManager.getAccessibleCount( ItemPool.EVIL_SCROLL );
					int creatable = CreateItemRequest.getInstance( ItemPool.EVIL_SCROLL )
						.getQuantityPossible();

					if ( !KoLCharacter.canInteract() && ( onHand + creatable ) < 1 )
					{
						continue;
					}
					else if ( Preferences.getBoolean( "demonSummoned" ) )
					{
						cmd = "";
					}
				}
				else if ( cmd.startsWith( "concert " ) )
				{
					String side = Preferences.getString( "sidequestArenaCompleted" );
					boolean available = false;

					if ( side.equals( "none" ) )
					{
						continue;
					}
					else if ( side.equals( "fratboy" ) )
					{
						available = cmd.indexOf( "Elvish" ) != -1 ||
						            cmd.indexOf( "Winklered" ) != -1 ||
						            cmd.indexOf( "White-boy Angst" ) != -1;
					}
					else if ( side.equals( "hippy" ) )
					{
						available = cmd.indexOf( "Moon" ) != -1 ||
						            cmd.indexOf( "Dilated" ) != -1 ||
						            cmd.indexOf( "Optimist" ) != -1;
					}

					if ( !available )
					{
						continue;
					}
					else if ( Preferences.getBoolean( "concertVisited" ) )
					{
						cmd = "";
					}
				}
				else if ( cmd.startsWith( "telescope " ) )
				{
					if ( Preferences.getInteger( "telescopeUpgrades" ) == 0 )
					{
						if ( includeAll )
						{
							text = "( get a telescope )";
							cmd = "";
						}
						else continue;
					}
					else if ( KoLCharacter.inBadMoon() )
					{
						continue;
					}
					else if ( Preferences.getBoolean( "telescopeLookedHigh" ) )
					{
						cmd = "";
					}
				}
				else if ( cmd.startsWith( "ballpit" ) )
				{
					if ( !KoLCharacter.canInteract() )
					{
						continue;
					}
					else if ( Preferences.getBoolean( "_ballpit" ) )
					{
						cmd = "";
					}
				}
				else if ( cmd.startsWith( "jukebox" ) )
				{
					if ( !KoLCharacter.canInteract() )
					{
						continue;
					}
					else if ( Preferences.getBoolean( "_jukebox" ) )
					{
						cmd = "";
					}
				}
				else if ( cmd.startsWith( "pool " ) )
				{
					if ( KoLCharacter.inBadMoon() )
					{
						continue;
					}
					else if ( KoLCharacter.isTrendy() && !TrendyRequest.isTrendy( "Clan Item", "Pool Table" ) )
					{
						continue;
					}
					else if ( !haveVipKey )
					{
						if ( includeAll )
						{
							text = "( get access to the VIP lounge )";
							cmd = "";
						}
						else continue;
					}
					else if ( Preferences.getInteger( "_poolGames" ) >= 3 )
					{
						cmd = "";
					}
				}
				else if ( cmd.startsWith( "shower " ) )
				{
					if ( KoLCharacter.inBadMoon() )
					{
						continue;
					}
					else if ( KoLCharacter.isTrendy() && !TrendyRequest.isTrendy( "Clan Item", "April Shower" ) )
					{
						continue;
					}
					else if ( !haveVipKey )
					{
						if ( includeAll )
						{
							text = "( get access to the VIP lounge )";
							cmd = "";
						}
						else continue;
					}
					else if ( Preferences.getBoolean( "_aprilShower" ) )
					{
						cmd = "";
					}
				}
				else if ( cmd.startsWith( "swim " ) )
				{
					if ( KoLCharacter.inBadMoon() )
					{
						continue;
					}
					else if ( KoLCharacter.isTrendy() && !TrendyRequest.isTrendy( "Clan Item", "Swimming Pool" ) )
					{
						continue;
					}
					else if ( !haveVipKey )
					{
						if ( includeAll )
						{
							text = "( get access to the VIP lounge )";
							cmd = "";
						}
						else continue;
					}
					else if ( Preferences.getBoolean( "_olympicSwimmingPool" ) )
					{
						cmd = "";
					}
				}
				else if ( cmd.startsWith( "styx " ) )
				{
					if ( !KoLCharacter.inBadMoon() )
					{
						continue;
					}
					else if ( Preferences.getBoolean( "styxPixieVisited" ) )
					{
						cmd = "";
					}
				}
				else if ( cmd.startsWith( "skate " ) )
				{
					String status = Preferences.getString( "skateParkStatus" );
					int buff = SkateParkRequest.placeToBuff( cmd.substring( 6 ) );
					Object [] data = SkateParkRequest.buffToData( buff );
					String buffPref = (String) data[4];
					String buffStatus = (String) data[6];

					if ( !status.equals( buffStatus ) )
					{
						continue;
					}
					else if ( Preferences.getBoolean( buffPref ) )
					{
						cmd = "";
					}
				}
				else if ( cmd.startsWith( "gap" ) )
				{
					AdventureResult pants = EquipmentManager.getEquipment( EquipmentManager.PANTS );
					if ( InventoryManager.getAccessibleCount( ItemPool.GREAT_PANTS ) == 0 )
					{
						if ( includeAll )
						{
							text = "(acquire and equip Greatest American Pants for " + name + ")";
							cmd = "";
						}
						else
						{
							continue;
						}
					}
					else if ( Preferences.getInteger( "_gapBuffs" ) >= 5 )
					{
						cmd = "";
					}
					else if ( pants == null || ( pants.getItemId() != ItemPool.GREAT_PANTS ) )
					{
						text = "(equip Greatest American Pants for " + name + ")";
						cmd = "";
					}
				}

				if ( item != null )
				{
					String iname = item.getName();

					if ( KoLCharacter.inBeecore() &&
						KoLCharacter.getBeeosity( iname ) > 0 )
					{
						continue;
					}

					int full = ItemDatabase.getFullness( iname );
					if ( full > 0 &&
						KoLCharacter.getFullness() + full > KoLCharacter.getFullnessLimit() )
					{
						cmd = "";
					}
					full = ItemDatabase.getInebriety( iname );
					if ( full > 0 &&
						KoLCharacter.getInebriety() + full > KoLCharacter.getInebrietyLimit() )
					{
						cmd = "";
					}
					full = ItemDatabase.getSpleenHit( iname );
					if ( full > 0 && cmd.indexOf( "chew" ) == -1 )
					{
						RequestLogger.printLine( "(Note: extender for " +
							name + " is a spleen item that doesn't use 'chew')" );
					}
					if ( full > 0 &&
						KoLCharacter.getSpleenUse() + full > KoLCharacter.getSpleenLimit() )
					{
						cmd = "";
					}
					if ( !ItemDatabase.meetsLevelRequirement( iname ) )
					{
						if ( includeAll )
						{
							text = "level up & " + text;
							cmd = "";
						}
						else continue;
					}

					if ( cmd.length() > 0 )
					{
						Concoction c = ConcoctionPool.get( item );
						price = c.price;
						int count = Math.max( 0, item.getCount() - c.initial );
						if ( count > 0 )
						{
							int create = Math.min( count, c.creatable );
							count -= create;
							if ( create > 0 )
							{
								text = create > 1 ? "make " + create + " & " + text
									: "make & " + text;
							}
							int buy = price > 0 ? Math.min( count, KoLCharacter.getAvailableMeat() / price ) : 0;
							count -= buy;
							if ( buy > 0 )
							{
								text = buy > 1 ? "buy " + buy + " & " + text
									: "buy & " + text;
								cmd = "buy " + buy + " \u00B6" + item.getItemId() +
									";" + cmd;
							}
							if ( count > 0 )
							{
								if ( !KoLCharacter.canInteract() ||
									!ItemDatabase.isTradeable( item.getItemId() ) )
								{
									continue;
								}
								text = count > 1 ? "acquire " + count + " & " + text
									: "acquire & " + text;
							}
						}
						if ( priceLevel == 2 || (priceLevel == 1 && count > 0) )
						{
							if ( price <= 0 && KoLCharacter.canInteract() &&
								ItemDatabase.isTradeable( item.getItemId() ) )
							{
								if ( MallPriceDatabase.getPrice( item.getItemId() )
									> maxPrice * 2 )
								{
									continue;
								}

								price = StoreManager.getMallPrice( item );
							}
						}
						if ( price > maxPrice ) continue;
					}
					else if ( item.getCount( KoLConstants.inventory ) == 0 )
					{
						continue;
					}
				}

				if ( price > 0 )
				{
					text = text + " (" + KoLConstants.COMMA_FORMAT.format( price ) +
						" meat, " +
						KoLConstants.MODIFIER_FORMAT.format( delta ) + ")";
				}
				else
				{
					text = text + " (" + KoLConstants.MODIFIER_FORMAT.format(
						delta ) + ")";
				}
				if ( orFlag )
				{
					text = "...or " + text;
				}
				MaximizerFrame.boosts.add( new Boost( cmd, text, effect, hasEffect,
					item, delta, isSpecial ) );
				orFlag = true;
			}
		}

		if ( MaximizerFrame.boosts.size() == 0 )
		{
			MaximizerFrame.boosts.add( new Boost( "", "(nothing useful found)", 0, null, 0.0f ) );
		}

		MaximizerFrame.boosts.sort();
	}

	static private int emitSlot( int slot, int equipLevel, int maxPrice, int priceLevel, float current )
	{
		if ( slot == EquipmentManager.FAMILIAR )
		{	// Insert any familiar switch at this point
			FamiliarData fam = MaximizerFrame.best.getFamiliar();
			if ( !fam.equals( KoLCharacter.getFamiliar() ) )
			{
				Spec spec = new Spec();
				spec.setFamiliar( fam );
				float delta = spec.getScore() - current;
				String cmd, text;
				cmd = "familiar " + fam.getRace();
				text = cmd + " (" +
					KoLConstants.MODIFIER_FORMAT.format( delta ) + ")";

				Boost boost = new Boost( cmd, text, fam, delta );
				if ( equipLevel == -1 )
				{	// called from CLI
					boost.execute( true );
					if ( !KoLmafia.permitsContinue() ) equipLevel = 1;
				}
				else
				{
					MaximizerFrame.boosts.add( boost );
				}
			}
		}

		String slotname = EquipmentRequest.slotNames[ slot ];
		AdventureResult item = MaximizerFrame.best.equipment[ slot ];
		AdventureResult curr = EquipmentManager.getEquipment( slot );
		if ( curr.equals( item ) )
		{
			if ( slot >= EquipmentManager.STICKER1 ||
				curr.equals( EquipmentRequest.UNEQUIP ) ||
				equipLevel == -1 )
			{
				return equipLevel;
			}
			MaximizerFrame.boosts.add( new Boost( "", "keep " + slotname + ": " + item.getName(), -1, item, 0.0f ) );
			return equipLevel;
		}
		Spec spec = new Spec();
		spec.equip( slot, item );
		float delta = spec.getScore() - current;
		String cmd, text;
		if ( item == null || item.equals( EquipmentRequest.UNEQUIP ) )
		{
			item = curr;
			cmd = "unequip " + slotname;
			text = cmd + " (" + curr.getName() + ", " +
				KoLConstants.MODIFIER_FORMAT.format( delta ) + ")";
		}
		else
		{
			cmd = "equip " + slotname + " " + item.getName();
			text = cmd + " (";
			int count = item.getCount();
			int price = 0;

			// The "initial" quantity comes from
			// InventoryManager.getAccessibleCount.
			// It can include inventory, closet, and
			// storage.  However, anything that is included should
			// also be supported by retrieveItem(), so we don't need
			// to take any special action here.  Displaying the method
			// that will be used would still be useful, though.
			if ( ((count >> Evaluator.INITIAL_SHIFT) & Evaluator.SUBTOTAL_MASK) != 0 )
			{
				String method = InventoryManager.simRetrieveItem(
					item.getInstance( 1 ) );
				if ( !method.equals( "have" ) )
				{
					text = method + " & " + text;
				}
			}
			else if ( ((count >> Evaluator.CREATABLE_SHIFT) & Evaluator.SUBTOTAL_MASK) != 0 )
			{
				text = "make & " + text;
			}
			else if ( ((count >> Evaluator.NPCBUYABLE_SHIFT) & Evaluator.SUBTOTAL_MASK) != 0 )
			{
				text = "buy & " + text;
				cmd = "buy 1 \u00B6" + item.getItemId() +
						";" + cmd;
				price = ConcoctionPool.get( item ).price;
			}
			else if ( ((count >> Evaluator.FOLDABLE_SHIFT) & Evaluator.SUBTOTAL_MASK) != 0 )
			{
				text = "fold & " + text;
				cmd = "fold \u00B6" + item.getItemId() +
						";" + cmd;
			}
			else if ( ((count >> Evaluator.PULLABLE_SHIFT) & Evaluator.SUBTOTAL_MASK) != 0 )
			{
				text = "pull & " + text;
				cmd = "pull 1 \u00B6" + item.getItemId() +
						";" + cmd;
			}
			else 	// Mall buyable
			{
				text = "acquire & " + text;
				if ( priceLevel > 0 )
				{
					price = StoreManager.getMallPrice( item );
				}
			}

			if ( price > 0 )
			{
				text = text + KoLConstants.COMMA_FORMAT.format( price ) +
					" meat, ";
			}
			text = text + KoLConstants.MODIFIER_FORMAT.format(
				delta ) + ")";
		}

		Boost boost = new Boost( cmd, text, slot, item, delta );
		if ( equipLevel == -1 )
		{	// called from CLI
			boost.execute( true );
			if ( !KoLmafia.permitsContinue() )
			{
				equipLevel = 1;
				MaximizerFrame.boosts.add( boost );
			}
		}
		else
		{
			MaximizerFrame.boosts.add( boost );
		}
		return equipLevel;
	}

	public static class MaximizerInterruptedException
		extends Exception
	{
	}

	public static class MaximizerExceededException
		extends MaximizerInterruptedException
	{
	}

	private class MaximizerPanel
		extends GenericPanel
	{
		public MaximizerPanel()
		{
			super( "update", "help", new Dimension( 80, 20 ), new Dimension( 420, 20 ) );

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
			Iterator i = MaximizerFrame.boosts.iterator();
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

	private static class Evaluator
	{
		public boolean failed, exceeded;
		private Evaluator tiebreaker;
		private float[] weight, min, max;
		private float totalMin, totalMax;
		private int dump = 0;
		private int clownosity = 0;
		private int raveosity = 0;
		private int beeosity = 2;
		private int booleanMask, booleanValue;
		private ArrayList<FamiliarData> familiars;

		private int[] slots = new int[ EquipmentManager.ALL_SLOTS ];
		String weaponType = null;
		int hands = 0;
		int melee = 0;	// +/-2 or higher: require, +/-1: disallow other type
		boolean requireShield = false;
		boolean noTiebreaker = false;
		HashSet<String> posOutfits, negOutfits;
		TreeSet<AdventureResult> posEquip, negEquip;

		private static final Pattern KEYWORD_PATTERN = Pattern.compile( "\\G\\s*(\\+|-|)([\\d.]*)\\s*(\"[^\"]+\"|(?:[^-+,0-9]|(?<! )[-+0-9])+),?\\s*" );
		// Groups: 1=sign 2=weight 3=keyword

		// The counts of possible equipment items are overloaded:
		// The total available for use is in the low bits, masked by TOTAL_MASK.
		// Amount from individual sources are in bitfields shifted by one of
		// the XXX_SHIFT amounts, masked by SUBTOTAL_MASK.
		// BUYABLE_FLAG is set if further items can be bought from the Mall.
		// AUTOMATIC_FLAG is set if the item must be considered regardless
		// of its ranking.
		// CONDITIONAL_FLAG is set for items that may turn out to be unusable
		// due to interactions with other equipment.  They're accurately
		// described by their modifiers, so the special handling of AUTOMATIC
		// isn't appropriate, but on the other hand they should never displace
		// an unconditionally useful item from the shortlist.
		public static final int TOTAL_MASK = 0xFF;
		public static final int SUBTOTAL_MASK = 0x0F;
		public static final int INITIAL_SHIFT = 8;
		public static final int CREATABLE_SHIFT = 12;
		public static final int NPCBUYABLE_SHIFT = 16;
		public static final int FOLDABLE_SHIFT = 20;
		public static final int PULLABLE_SHIFT = 24;
		public static final int BUYABLE_FLAG = 1 << 28;
		public static final int AUTOMATIC_FLAG = 1 << 29;
		public static final int CONDITIONAL_FLAG = 1 << 30;

		// Equipment slots, that aren't the primary slot of any item type,
		// that are repurposed here (rather than making the array bigger).
		// Watches have to be handled specially because only one can be
		// used - otherwise, they'd fill up the list, leaving no room for
		// any non-watches to put in the other two acc slots.
		// 1-handed weapons have to be ranked separately due to the following
		// possibility: all of your best weapons are 2-hand, but you've got
		// a really good off-hand, better than any weapon.  There would
		// otherwise be no suitable weapons to go with that off-hand.
		public static final int OFFHAND_MELEE = EquipmentManager.ACCESSORY2;
		public static final int OFFHAND_RANGED = EquipmentManager.ACCESSORY3;
		public static final int WATCHES = EquipmentManager.STICKER2;
		public static final int WEAPON_1H = EquipmentManager.STICKER3;
		// Slots starting with EquipmentManager.ALL_SLOTS are equipment
		// for other familiars being considered.

		private static int relevantSkill( String skill )
		{
			return KoLCharacter.hasSkill( skill ) ? 1 : 0;
		}

		private int relevantFamiliar( int id )
		{
			if ( KoLCharacter.getFamiliar().getId() == id )
			{
				return 1;
			}
			for ( int i = 0; i < this.familiars.size(); ++i )
			{
				if ( ((FamiliarData) this.familiars.get( i )).getId() == id )
				{
					return 1;
				}
			}
			return 0;
		}

		private int maxUseful( int slot )
		{
			switch ( slot )
			{
			case EquipmentManager.HAT:
				return 1 + this.relevantFamiliar( FamiliarPool.HATRACK );
			case EquipmentManager.PANTS:
				return 1 + this.relevantFamiliar( FamiliarPool.SCARECROW );
			case Evaluator.WEAPON_1H:
				return 1 + relevantSkill( "Double-Fisted Skull Smashing" ) +
					this.relevantFamiliar( FamiliarPool.HAND );
			case EquipmentManager.ACCESSORY1:
				return 3;
			}
			return 1;
		}

		private Evaluator()
		{
			this.totalMin = Float.NEGATIVE_INFINITY;
			this.totalMax = Float.POSITIVE_INFINITY;
		}

		public Evaluator( String expr )
		{
			this();
			Evaluator tiebreaker = new Evaluator();
			this.tiebreaker = tiebreaker;
			this.posOutfits = tiebreaker.posOutfits = new HashSet<String>();
			this.negOutfits = tiebreaker.negOutfits = new HashSet<String>();
			this.posEquip = tiebreaker.posEquip = new TreeSet<AdventureResult>();
			this.negEquip = tiebreaker.negEquip = new TreeSet<AdventureResult>();
			this.familiars = tiebreaker.familiars = new ArrayList<FamiliarData>();
			this.weight = new float[ Modifiers.FLOAT_MODIFIERS ];
			tiebreaker.weight = new float[ Modifiers.FLOAT_MODIFIERS ];
			tiebreaker.min = new float[ Modifiers.FLOAT_MODIFIERS ];
			tiebreaker.max = new float[ Modifiers.FLOAT_MODIFIERS ];
			Arrays.fill( tiebreaker.min, Float.NEGATIVE_INFINITY );
			Arrays.fill( tiebreaker.max, Float.POSITIVE_INFINITY );
			tiebreaker.parse( MaximizerFrame.TIEBREAKER );
			this.min = (float[]) tiebreaker.min.clone();
			this.max = (float[]) tiebreaker.max.clone();
			this.parse( expr );
		}

		private void parse( String expr )
		{
			expr = expr.trim().toLowerCase();
			Matcher m = KEYWORD_PATTERN.matcher( expr );
			boolean hadFamiliar = false;
			int pos = 0;
			int index = -1;

			int equipBeeosity = 0;
			int outfitBeeosity = 0;

			while ( pos < expr.length() )
			{
				if ( !m.find() )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR,
						"Unable to interpret: " + expr.substring( pos ) );
					return;
				}
				pos = m.end();
				float weight = StringUtilities.parseFloat(
					m.end( 2 ) == m.start( 2 ) ? m.group( 1 ) + "1"
						: m.group( 1 ) + m.group( 2 ) );

				String keyword = m.group( 3 ).trim();
				if ( keyword.startsWith( "\"" ) && keyword.endsWith( "\"" ) )
				{
					keyword = keyword.substring( 1, keyword.length() - 1 ).trim();
				}
				if ( keyword.equals( "min" ) )
				{
					if ( index >= 0 )
					{
						this.min[ index ] = weight;
					}
					else
					{
						this.totalMin = weight;
					}
					continue;
				}
				else if ( keyword.equals( "max" ) )
				{
					if ( index >= 0 )
					{
						this.max[ index ] = weight;
					}
					else
					{
						this.totalMax = weight;
					}
					continue;
				}
				else if ( keyword.equals( "dump" ) )
				{
					this.dump = (int) weight;
					continue;
				}
				else if ( keyword.startsWith( "hand" ) )
				{
					this.hands = (int) weight;
					if ( this.hands >= 2 )
					{
						//this.slots[ EquipmentManager.OFFHAND ] = -1;
					}
					continue;
				}
				else if ( keyword.startsWith( "tie" ) )
				{
					this.noTiebreaker = weight < 0.0f;
					continue;
				}
				else if ( keyword.startsWith( "type " ) )
				{
					this.weaponType = keyword.substring( 5 ).trim();
					continue;
				}
				else if ( keyword.equals( "shield" ) )
				{
					this.requireShield = weight > 0.0f;
					this.hands = 1;
					continue;
				}
				else if ( keyword.equals( "melee" ) )
				{
					this.melee = (int) (weight * 2.0f);
					continue;
				}
				else if ( keyword.equals( "empty" ) )
				{
					for ( int i = 0; i < EquipmentManager.ALL_SLOTS; ++i )
					{
						this.slots[ i ] += ((int) weight) *
							( EquipmentManager.getEquipment( i ).equals( EquipmentRequest.UNEQUIP ) ? 1 : -1 );
					}
					continue;
				}
				else if ( keyword.equals( "clownosity" ) )
				{
					this.clownosity = (int) weight;
					continue;
				}
				else if ( keyword.equals( "raveosity" ) )
				{
					this.raveosity = (int) weight;
					continue;
				}
				else if ( keyword.equals( "beeosity" ) )
				{
					this.beeosity = (int) weight;
					continue;
				}
				else if ( keyword.equals( "sea" ) )
				{
					this.booleanMask |= (1 << Modifiers.ADVENTURE_UNDERWATER) |
						(1 << Modifiers.UNDERWATER_FAMILIAR);
					this.booleanValue |= (1 << Modifiers.ADVENTURE_UNDERWATER) |
						(1 << Modifiers.UNDERWATER_FAMILIAR);
					index = -1;
					continue;
				}
				else if ( keyword.startsWith( "equip " ) )
				{
					AdventureResult match = ItemFinder.getFirstMatchingItem(
						keyword.substring( 6 ).trim(), ItemFinder.EQUIP_MATCH );
					if ( match == null )
					{
						return;
					}
					if ( weight > 0.0f )
					{
						this.posEquip.add( match );
						equipBeeosity += KoLCharacter.getBeeosity(
							match.getName() );
					}
					else
					{
						this.negEquip.add( match );
					}
					continue;
				}
				else if ( keyword.startsWith( "outfit" ) )
				{
					keyword = keyword.substring( 6 ).trim();
					if ( keyword.equals( "" ) )
					{	// allow "+outfit" to mean "keep the current outfit on"
						keyword = KoLCharacter.currentStringModifier( Modifiers.OUTFIT );
					}
					SpecialOutfit outfit = EquipmentManager.getMatchingOutfit( keyword );
					if ( outfit == null || outfit.getOutfitId() <= 0 )
					{
						KoLmafia.updateDisplay( MafiaState.ERROR,
							"Unknown or custom outfit: " + keyword );
						return;
					}
					if ( weight > 0.0f )
					{
						this.posOutfits.add( outfit.getName() );
						int bees = 0;
						AdventureResult[] pieces = outfit.getPieces();
						for ( int i = 0; i < pieces.length; ++i )
						{
							bees += KoLCharacter.getBeeosity( pieces[ i ].getName() );
						}
						outfitBeeosity = Math.max( outfitBeeosity, bees );
					}
					else
					{
						this.negOutfits.add( outfit.getName() );
					}
					continue;
				}
				else if ( keyword.startsWith( "switch " ) )
				{
					keyword = keyword.substring( 7 ).trim();
					int id = FamiliarDatabase.getFamiliarId( keyword );
					if ( id == -1 )
					{
						KoLmafia.updateDisplay( MafiaState.ERROR,
							"Unknown familiar: " + keyword );
						return;
					}
					if ( hadFamiliar && weight < 0.0f ) continue;
					FamiliarData fam = KoLCharacter.findFamiliar( id );
					if ( fam == null && weight > 1.0f )
					{	// Allow a familiar to be faked for testing
						fam = new FamiliarData( id );
						fam.setWeight( (int) weight );
					}
					hadFamiliar = fam != null;
					if ( fam != null && !fam.equals( KoLCharacter.getFamiliar() )
						&& fam.canEquip() && !this.familiars.contains( fam ) )
					{
						this.familiars.add( fam );
					}
					continue;
				}

				int slot = EquipmentRequest.slotNumber( keyword );
				if ( slot >= 0 && slot < EquipmentManager.ALL_SLOTS )
				{
					this.slots[ slot ] += (int) weight;
					continue;
				}

				index = Modifiers.findName( keyword );
				if ( index < 0 )
				{	// try generic abbreviations
					if ( keyword.endsWith( " res" ) )
					{
						keyword += "istance";
					}
					else if ( keyword.endsWith( " dmg" ) )
					{
						keyword = keyword.substring( 0, keyword.length() - 3 ) + "damage";
					}
					else if ( keyword.endsWith( " exp" ) )
					{
						keyword = keyword.substring( 0, keyword.length() - 3 ) + "experience";
					}
					index = Modifiers.findName( keyword );
				}

				if ( index >= 0 )
				{	// exact match
				}
				else if ( keyword.equals( "all resistance" ) )
				{
					this.weight[ Modifiers.COLD_RESISTANCE ] = weight;
					this.weight[ Modifiers.HOT_RESISTANCE ] = weight;
					this.weight[ Modifiers.SLEAZE_RESISTANCE ] = weight;
					this.weight[ Modifiers.SPOOKY_RESISTANCE ] = weight;
					this.weight[ Modifiers.STENCH_RESISTANCE ] = weight;
					continue;
				}
				else if ( keyword.equals( "elemental damage" ) )
				{
					this.weight[ Modifiers.COLD_DAMAGE ] = weight;
					this.weight[ Modifiers.HOT_DAMAGE ] = weight;
					this.weight[ Modifiers.SLEAZE_DAMAGE ] = weight;
					this.weight[ Modifiers.SPOOKY_DAMAGE ] = weight;
					this.weight[ Modifiers.STENCH_DAMAGE ] = weight;
					continue;
				}
				else if ( keyword.equals( "hp regen" ) )
				{
					this.weight[ Modifiers.HP_REGEN_MIN ] = weight / 2;
					this.weight[ Modifiers.HP_REGEN_MAX ] = weight / 2;
					continue;
				}
				else if ( keyword.equals( "mp regen" ) )
				{
					this.weight[ Modifiers.MP_REGEN_MIN ] = weight / 2;
					this.weight[ Modifiers.MP_REGEN_MAX ] = weight / 2;
					continue;
				}
				else if ( keyword.equals( "init" ) )
				{
					index = Modifiers.INITIATIVE;
				}
				else if ( keyword.equals( "hp" ) )
				{
					index = Modifiers.HP;
				}
				else if ( keyword.equals( "mp" ) )
				{
					index = Modifiers.MP;
				}
				else if ( keyword.equals( "da" ) )
				{
					index = Modifiers.DAMAGE_ABSORPTION;
				}
				else if ( keyword.equals( "dr" ) )
				{
					index = Modifiers.DAMAGE_REDUCTION;
				}
				else if ( keyword.equals( "ml" ) )
				{
					index = Modifiers.MONSTER_LEVEL;
				}
				else if ( keyword.startsWith( "mus" ) )
				{
					index = Modifiers.MUS;
				}
				else if ( keyword.startsWith( "mys" ) )
				{
					index = Modifiers.MYS;
				}
				else if ( keyword.startsWith( "mox" ) )
				{
					index = Modifiers.MOX;
				}
				else if ( keyword.startsWith( "main" ) )
				{
					switch ( KoLCharacter.getPrimeIndex() )
					{
					case 0:
						index = Modifiers.MUS;
						break;
					case 1:
						index = Modifiers.MYS;
						break;
					case 2:
						index = Modifiers.MOX;
						break;
					}
				}
				else if ( keyword.startsWith( "com" ) )
				{
					index = Modifiers.COMBAT_RATE;
					if ( Modifiers.currentZone.indexOf( "the sea" ) != -1 )
					{
						this.weight[ Modifiers.UNDERWATER_COMBAT_RATE ] = weight;
					}
				}
				else if ( keyword.startsWith( "item" ) )
				{
					index = Modifiers.ITEMDROP;
				}
				else if ( keyword.startsWith( "meat" ) )
				{
					index = Modifiers.MEATDROP;
				}
				else if ( keyword.startsWith( "adv" ) )
				{
					this.noTiebreaker = true;
					this.beeosity = 999;
					index = Modifiers.ADVENTURES;
				}
				else if ( keyword.startsWith( "fites" ) )
				{
					this.noTiebreaker = true;
					this.beeosity = 999;
					index = Modifiers.PVP_FIGHTS;
				}
				else if ( keyword.startsWith( "exp" ) )
				{
					index = Modifiers.EXPERIENCE;
				}

				if ( index >= 0 )
				{
					this.weight[ index ] = weight;
					continue;
				}

				int boolIndex = Modifiers.findBooleanName( keyword );
				if ( boolIndex >= 0 )
				{
					this.booleanMask |= 1 << boolIndex;
					if ( weight > 0.0f )
					{
						this.booleanValue |= 1 << boolIndex;
					}
					index = -1;	// min/max not valid at this point
					continue;
				}

				KoLmafia.updateDisplay( MafiaState.ERROR,
					"Unrecognized keyword: " + keyword );
				return;
			}

			this.beeosity = Math.max( Math.max( this.beeosity,
				equipBeeosity ), outfitBeeosity );

			// Make sure indirect sources have at least a little weight;
			float fudge = this.weight[ Modifiers.EXPERIENCE ] * 0.0001f;
			this.weight[ Modifiers.MONSTER_LEVEL ] += fudge;
			this.weight[ Modifiers.MUS_EXPERIENCE ] += fudge;
			this.weight[ Modifiers.MYS_EXPERIENCE ] += fudge;
			this.weight[ Modifiers.MOX_EXPERIENCE ] += fudge;
			this.weight[ Modifiers.MUS_EXPERIENCE_PCT ] += fudge;
			this.weight[ Modifiers.MYS_EXPERIENCE_PCT ] += fudge;
			this.weight[ Modifiers.MOX_EXPERIENCE_PCT ] += fudge;
			this.weight[ Modifiers.VOLLEYBALL_WEIGHT ] += fudge;
			this.weight[ Modifiers.SOMBRERO_WEIGHT ] += fudge;
			this.weight[ Modifiers.VOLLEYBALL_EFFECTIVENESS ] += fudge;
			this.weight[ Modifiers.SOMBRERO_EFFECTIVENESS ] += fudge;
			this.weight[ Modifiers.SOMBRERO_BONUS ] += fudge;

			fudge = this.weight[ Modifiers.ITEMDROP ] * 0.0001f;
			this.weight[ Modifiers.FOODDROP ] += fudge;
			this.weight[ Modifiers.BOOZEDROP ] += fudge;
			this.weight[ Modifiers.HATDROP ] += fudge;
			this.weight[ Modifiers.WEAPONDROP ] += fudge;
			this.weight[ Modifiers.OFFHANDDROP ] += fudge;
			this.weight[ Modifiers.SHIRTDROP ] += fudge;
			this.weight[ Modifiers.PANTSDROP ] += fudge;
			this.weight[ Modifiers.ACCESSORYDROP ] += fudge;
			this.weight[ Modifiers.CANDYDROP ] += fudge;
			this.weight[ Modifiers.FAIRY_WEIGHT ] += fudge;
			this.weight[ Modifiers.FAIRY_EFFECTIVENESS ] += fudge;
			this.weight[ Modifiers.SPORADIC_ITEMDROP ] += fudge;
			this.weight[ Modifiers.PICKPOCKET_CHANCE ] += fudge;

			fudge = this.weight[ Modifiers.MEATDROP ] * 0.0001f;
			this.weight[ Modifiers.LEPRECHAUN_WEIGHT ] += fudge;
			this.weight[ Modifiers.LEPRECHAUN_EFFECTIVENESS ] += fudge;
			this.weight[ Modifiers.SPORADIC_MEATDROP ] += fudge;
			this.weight[ Modifiers.MEAT_BONUS ] += fudge;
		}

		public float getScore( Modifiers mods )
		{
			this.failed = false;
			this.exceeded = false;
			int[] predicted = mods.predict();

			float score = 0.0f;
			for ( int i = 0; i < Modifiers.FLOAT_MODIFIERS; ++i )
			{
				float weight = this.weight[ i ];
				float min = this.min[ i ];
				if ( weight == 0.0f && min == Float.NEGATIVE_INFINITY ) continue;
				float val = mods.get( i );
				float max = this.max[ i ];
				switch ( i )
				{
				case Modifiers.MUS:
					val = predicted[ Modifiers.BUFFED_MUS ];
					break;
				case Modifiers.MYS:
					val = predicted[ Modifiers.BUFFED_MYS ];
					break;
				case Modifiers.MOX:
					val = predicted[ Modifiers.BUFFED_MOX ];
					break;
				case Modifiers.FAMILIAR_WEIGHT:
					val += mods.get( Modifiers.HIDDEN_FAMILIAR_WEIGHT );
					if ( mods.get( Modifiers.FAMILIAR_WEIGHT_PCT ) < 0.0f )
					{
						val *= 0.5f;
					}
					break;
				case Modifiers.MANA_COST:
					val += mods.get( Modifiers.STACKABLE_MANA_COST );
					break;
				case Modifiers.INITIATIVE:
					val += Math.min( 0.0f, mods.get( Modifiers.INITIATIVE_PENALTY ) );
					break;
				case Modifiers.MEATDROP:
					val += 100.0f + Math.min( 0.0f, mods.get( Modifiers.MEATDROP_PENALTY ) ) + mods.get( Modifiers.SPORADIC_MEATDROP ) + mods.get( Modifiers.MEAT_BONUS ) / 10000.0f;
					break;
				case Modifiers.ITEMDROP:
					val += 100.0f + Math.min( 0.0f, mods.get( Modifiers.ITEMDROP_PENALTY ) ) + mods.get( Modifiers.SPORADIC_ITEMDROP );
					break;
				case Modifiers.HP:
					val = predicted[ Modifiers.BUFFED_HP ];
					break;
				case Modifiers.MP:
					val = predicted[ Modifiers.BUFFED_MP ];
					break;
				case Modifiers.WEAPON_DAMAGE:
					// Incorrect - needs to estimate base damage
					val += mods.get( Modifiers.WEAPON_DAMAGE_PCT );
					break;
				case Modifiers.RANGED_DAMAGE:
					// Incorrect - needs to estimate base damage
					val += mods.get( Modifiers.RANGED_DAMAGE_PCT );
					break;
				case Modifiers.SPELL_DAMAGE:
					// Incorrect - base damage depends on spell used
					val += mods.get( Modifiers.SPELL_DAMAGE_PCT );
					break;
				case Modifiers.COLD_RESISTANCE:
				case Modifiers.HOT_RESISTANCE:
				case Modifiers.SLEAZE_RESISTANCE:
				case Modifiers.SPOOKY_RESISTANCE:
				case Modifiers.STENCH_RESISTANCE:
					if ( mods.getBoolean( i - Modifiers.COLD_RESISTANCE + Modifiers.COLD_IMMUNITY ) )
					{
						val = 100.0f;
					}
					else if ( mods.getBoolean( i - Modifiers.COLD_RESISTANCE + Modifiers.COLD_VULNERABILITY ) )
					{
						val -= 100.0f;
					}
					break;
				case Modifiers.EXPERIENCE:
					val = mods.get( Modifiers.MUS_EXPERIENCE + KoLCharacter.getPrimeIndex() );
					break;
				}
				if ( val < min ) this.failed = true;
				score += weight * Math.min( val, max );
			}
			if ( score < this.totalMin ) this.failed = true;
			if ( score >= this.totalMax ) this.exceeded = true;
			if ( !this.failed && this.clownosity > 0 &&
				mods.getBitmap( Modifiers.CLOWNOSITY ) < this.clownosity )
			{
				this.failed = true;
			}
			if ( !this.failed && this.raveosity > 0 &&
				mods.getBitmap( Modifiers.RAVEOSITY ) < this.raveosity )
			{
				this.failed = true;
			}
			if ( !this.failed && this.booleanMask != 0 &&
				(mods.getRawBitmap( 0 ) & this.booleanMask) != this.booleanValue )
			{
				this.failed = true;
			}
			return score;
		}

		public void checkEquipment( Modifiers mods, AdventureResult[] equipment,
			int beeosity )
		{
			boolean outfitSatisfied = false;
			boolean equipSatisfied = this.posOutfits.isEmpty();
			if ( !this.failed && !this.posEquip.isEmpty() )
			{
				equipSatisfied = true;
				Iterator i = this.posEquip.iterator();
				while ( i.hasNext() )
				{
					AdventureResult item = (AdventureResult) i.next();
					if ( !KoLCharacter.hasEquipped( equipment, item ) )
					{
						equipSatisfied = false;
						break;
					}
				}
			}
			if ( !this.failed )
			{
				String outfit = mods.getString( Modifiers.OUTFIT );
				if ( this.negOutfits.contains( outfit ) )
				{
					this.failed = true;
				}
				else
				{
					outfitSatisfied = this.posOutfits.contains( outfit );
				}
			}
			// negEquip is not checked, since enumerateEquipment should make it
			// impossible for such items to be chosen.
			if ( !outfitSatisfied && !equipSatisfied )
			{
				this.failed = true;
			}
			if ( beeosity > this.beeosity )
			{
				this.failed = true;
			}
		}

		public float getTiebreaker( Modifiers mods )
		{
			if ( this.noTiebreaker ) return 0.0f;
			return this.tiebreaker.getScore( mods );
		}

		public int checkConstraints( Modifiers mods )
		{
			// Return value:
			//	-1: item violates a constraint, don't use it
			//	0: item not relevant to any constraints
			//	1: item meets a constraint, give it special handling
			if ( mods == null ) return 0;
			int bools = mods.getRawBitmap( 0 ) & this.booleanMask;
			if ( (bools & ~this.booleanValue) != 0 ) return -1;
			if ( bools != 0 ) return 1;
			return 0;
		}

		private AdventureResult checkItem( int id, int equipLevel, int maxPrice, int priceLevel )
		{
			int count = Math.min( Evaluator.SUBTOTAL_MASK,
				InventoryManager.getAccessibleCount( id ) );
			count |= count << Evaluator.INITIAL_SHIFT;
			if ( (count & Evaluator.TOTAL_MASK) >= 3 || equipLevel < 2 )
			{
				return ItemPool.get( id, count );
			}

			Concoction c = ConcoctionPool.get( id );
			int create = Math.min( Evaluator.SUBTOTAL_MASK, c.creatable );
			count += (create << Evaluator.CREATABLE_SHIFT) | create;

			int buy = 0;
			if ( c.price > 0 )
			{
				buy = Math.min( Evaluator.SUBTOTAL_MASK, maxPrice / c.price );
				count += (buy << Evaluator.NPCBUYABLE_SHIFT) | buy;
			}

			// TODO: check foldability

			if ( (count & Evaluator.TOTAL_MASK) >= 3 || equipLevel < 3 )
			{
				return ItemPool.get( id, count );
			}

			if ( KoLCharacter.isTrendy() && !TrendyRequest.isTrendy( "Items", ItemPool.get( id, 0 ).getName() ) )
			{	// Trendy characters can't buy or pull untrendy items
				count = 0;
			}
			else if ( KoLCharacter.canInteract() )
			{	// consider Mall buying
				if ( count == 0 && ItemDatabase.isTradeable( id ) )
				{	// but only if none are otherwise available
					if ( priceLevel == 0 ||
						MallPriceDatabase.getPrice( id ) < maxPrice * 2 )
					{
						count = 1 | Evaluator.BUYABLE_FLAG;
					}
				}
			}
			else if ( !KoLCharacter.isHardcore() )
			{	// consider pulling
				int pull = ItemPool.get( id, 0 ).getCount( KoLConstants.storage );
				count += (pull << Evaluator.PULLABLE_SHIFT) | pull;
			}
			return ItemPool.get( id, count );
		}

		private AdventureResult validateItem( AdventureResult item, int maxPrice, int priceLevel )
		{
			if ( priceLevel <= 0 ) return item;
			int count = item.getCount();
			if ( (count & Evaluator.BUYABLE_FLAG) == 0 ) return item;
			int price = StoreManager.getMallPrice( item );
			if ( price <= 0 || price > maxPrice )
			{
				return item.getInstance( count - 1 );
			}
			return item;
		}

		public void enumerateEquipment( int equipLevel, int maxPrice, int priceLevel )
			throws MaximizerInterruptedException
		{
			// Items automatically considered regardless of their score -
			// synergies, hobo power, brimstone, etc.
			ArrayList<AdventureResult>[] automatic = new ArrayList[ EquipmentManager.ALL_SLOTS + this.familiars.size() ];
			// Items to be considered based on their score
			ArrayList<AdventureResult>[] ranked = new ArrayList[ EquipmentManager.ALL_SLOTS + this.familiars.size() ];
			for ( int i = ranked.length - 1; i >= 0; --i )
			{
				automatic[ i ] = new ArrayList<AdventureResult>();
				ranked[ i ] = new ArrayList<AdventureResult>();
			}

			float nullScore = this.getScore( new Modifiers() );

			BooleanArray usefulOutfits = new BooleanArray();
			TreeMap<AdventureResult, AdventureResult> outfitPieces = new TreeMap<AdventureResult, AdventureResult>();
			for ( int i = 1; i < EquipmentDatabase.normalOutfits.size(); ++i )
			{
				SpecialOutfit outfit = EquipmentDatabase.normalOutfits.get( i );
				if ( outfit == null ) continue;
				if ( this.negOutfits.contains( outfit.getName() ) ) continue;
				if ( this.posOutfits.contains( outfit.getName() ) )
				{
					usefulOutfits.set( i, true );
					continue;
				}

				Modifiers mods = Modifiers.getModifiers( outfit.getName() );
				if ( mods == null )	continue;

				switch ( this.checkConstraints( mods ) )
				{
				case -1:
					continue;
				case 0:
					float delta = this.getScore( mods ) - nullScore;
					if ( delta <= 0.0f ) continue;
					break;
				}
				usefulOutfits.set( i, true );
			}

			int usefulSynergies = 0;
			Iterator syn = Modifiers.getSynergies();
			while ( syn.hasNext() )
			{
				Modifiers mods = Modifiers.getModifiers( (String) syn.next() );
				int value = ((Integer) syn.next()).intValue();
				if ( mods == null )	continue;
				float delta = this.getScore( mods ) - nullScore;
				if ( delta > 0.0f ) usefulSynergies |= value;
			}

			boolean hoboPowerUseful = false;
			{
				Modifiers mods = Modifiers.getModifiers( "_hoboPower" );
				if ( mods != null &&
					this.getScore( mods ) - nullScore > 0.0f )
				{
					hoboPowerUseful = true;
				}
			}

			boolean brimstoneUseful = false;
			{
				Modifiers mods = Modifiers.getModifiers( "_brimstone" );
				if ( mods != null &&
					this.getScore( mods ) - nullScore > 0.0f )
				{
					brimstoneUseful = true;
				}
			}

			boolean slimeHateUseful = false;
			{
				Modifiers mods = Modifiers.getModifiers( "_slimeHate" );
				if ( mods != null &&
					this.getScore( mods ) - nullScore > 0.0f )
				{
					slimeHateUseful = true;
				}
			}

			boolean stickersUseful = false;
			{
				Modifiers mods = Modifiers.getModifiers( "_stickers" );
				if ( mods != null &&
					this.getScore( mods ) - nullScore > 0.0f )
				{
					stickersUseful = true;
				}
			}

			// This relies on the special sauce glove having a lower ID
			// than any chefstaff.
			boolean gloveAvailable = false;
			
			int id = 0;
			while ( (id = EquipmentDatabase.nextEquipmentItemId( id )) != -1 )
			{
				int slot = EquipmentManager.itemIdToEquipmentType( id );
				if ( slot < 0 || slot >= EquipmentManager.ALL_SLOTS ) continue;
				AdventureResult preItem = ItemPool.get( id, 1 );
				String name = preItem.getName();
				AdventureResult item = null;
				if ( this.negEquip.contains( preItem ) ) continue;
				if ( KoLCharacter.inBeecore() &&
					KoLCharacter.getBeeosity( name ) > this.beeosity )
				{	// too beechin' all by itself!
					continue;
				}
				boolean famCanEquip = KoLCharacter.getFamiliar().canEquip( preItem );
				if ( famCanEquip && slot != EquipmentManager.FAMILIAR )
				{
					item = this.checkItem( id, equipLevel, maxPrice, priceLevel );
					if ( item.getCount() != 0 )
					{
						ranked[ EquipmentManager.FAMILIAR ].add( item );
					}
				}
				for ( int f = this.familiars.size() - 1; f >= 0; --f )
				{
					FamiliarData fam = (FamiliarData) this.familiars.get( f );
					if ( !fam.canEquip( preItem ) ) continue;
					if ( item == null )
					{
						item = this.checkItem( id, equipLevel, maxPrice, priceLevel );
					}
					if ( item.getCount() != 0 )
					{
						ranked[ EquipmentManager.ALL_SLOTS + f ].add( item );
					}
				}

				if ( !EquipmentManager.canEquip( id ) ) continue;
				if ( item == null )
				{
					item = this.checkItem( id, equipLevel, maxPrice, priceLevel );
				}
				int count = item.getCount();
				if ( count == 0 ) continue;

				int auxSlot = -1;
			gotItem:
				{
					switch ( slot )
					{
					case EquipmentManager.FAMILIAR:
						if ( !famCanEquip ) continue;
						break;

					case EquipmentManager.WEAPON:
						int hands = EquipmentDatabase.getHands( id );
						if ( this.hands == 1 && hands != 1 )
						{
							continue;
						}
						if ( this.hands > 1 && hands < this.hands )
						{
							continue;
						}
						int stat = EquipmentDatabase.getWeaponType( id );
						if ( this.melee > 0 && stat != KoLConstants.MELEE )
						{
							continue;
						}
						if ( this.melee < 0 && stat != KoLConstants.RANGED )
						{
							continue;
						}
						String type = EquipmentDatabase.getItemType( id );
						if ( this.weaponType != null && type.indexOf( this.weaponType ) == -1 )
						{
							continue;
						}
						if ( hands == 1 )
						{
							slot = Evaluator.WEAPON_1H;
							if ( type.equals( "chefstaff" ) )
							{	// Don't allow chefstaves to displace other
								// 1H weapons from the shortlist if you can't
								// equip them anyway.
								if ( !KoLCharacter.hasSkill( "Spirit of Rigatoni" ) &&
									!(KoLCharacter.getClassType().equals( KoLCharacter.SAUCEROR ) && gloveAvailable) )
								{
									continue;
								}
								// In any case, don't put this in an aux slot.
							}
							else if ( !this.requireShield )
							{
								switch ( stat )
								{
								case KoLConstants.MELEE:
									auxSlot = Evaluator.OFFHAND_MELEE;
									break;
								case KoLConstants.RANGED:
									auxSlot = Evaluator.OFFHAND_RANGED;
									break;
								}
							}
						}
						break;

					case EquipmentManager.OFFHAND:
						if ( this.requireShield &&
							!EquipmentDatabase.getItemType( id ).equals( "shield" ) )
						{
							continue;
						}
						if ( hoboPowerUseful && name.startsWith( "Hodgman's" ) )
						{
							Modifiers.hoboPower = 100.0f;
							count |= Evaluator.AUTOMATIC_FLAG;
							item = item.getInstance( count );
						}
						break;
						
					case EquipmentManager.ACCESSORY1:
						if ( id == ItemPool.SPECIAL_SAUCE_GLOVE &&
							KoLCharacter.getClassType().equals( KoLCharacter.SAUCEROR )
							&& !KoLCharacter.hasSkill( "Spirit of Rigatoni" ) )
						{
							item = this.validateItem( item, maxPrice, priceLevel );
							count = item.getCount();
							if ( (count & Evaluator.TOTAL_MASK) == 0 )
							{
								continue;
							}
							item = item.getInstance( count | Evaluator.AUTOMATIC_FLAG );
							gloveAvailable = true;
							break gotItem;
						}
						break;
					}

					if ( usefulOutfits.get( EquipmentDatabase.getOutfitWithItem( id ) ) )
					{
						item = this.validateItem( item, maxPrice, priceLevel );
						count = item.getCount();
						if ( (count & Evaluator.TOTAL_MASK) == 0 )
						{
							continue;
						}
						outfitPieces.put( item, item );
					}

					if ( KoLCharacter.hasEquipped( item ) )
					{	// Make sure the current item in each slot is considered
						// for keeping, unless it's actively harmful.
						count |= Evaluator.AUTOMATIC_FLAG;
						item = item.getInstance( count );
					}

					Modifiers mods = Modifiers.getModifiers( name );
					if ( mods == null )	// no enchantments
					{
						mods = new Modifiers();
					}

					if ( mods.getBoolean( Modifiers.SINGLE ) )
					{
						count = (count & ~Evaluator.TOTAL_MASK) | 1;
						item = item.getInstance( count );
					}

					if ( this.posEquip.contains( item ) )
					{
						item = item.getInstance( count | Evaluator.AUTOMATIC_FLAG );
						break gotItem;
					}

					switch ( this.checkConstraints( mods ) )
					{
					case -1:
						continue;
					case 1:
						item = item.getInstance( count | Evaluator.AUTOMATIC_FLAG );
						break gotItem;
					}

					if ( ( hoboPowerUseful &&
							mods.get( Modifiers.HOBO_POWER ) > 0.0f ) ||
						( brimstoneUseful &&
							mods.getRawBitmap( Modifiers.BRIMSTONE ) != 0 ) ||
						( slimeHateUseful &&
							mods.get( Modifiers.SLIME_HATES_IT ) > 0.0f ) ||
						( stickersUseful &&
							EquipmentManager.isStickerWeapon( item ) ) ||
						( this.clownosity > 0 &&
							mods.getRawBitmap( Modifiers.CLOWNOSITY ) != 0 ) ||
						( this.raveosity > 0 &&
							mods.getRawBitmap( Modifiers.RAVEOSITY ) != 0 ) ||
						( (mods.getRawBitmap( Modifiers.SYNERGETIC )
							& usefulSynergies) != 0 ) )
					{
						item = item.getInstance( count | Evaluator.AUTOMATIC_FLAG );
						break gotItem;
					}

					String intrinsic = mods.getString( Modifiers.INTRINSIC_EFFECT );
					if ( intrinsic.length() > 0 )
					{
						Modifiers newMods = new Modifiers();
						newMods.add( mods );
						newMods.add( Modifiers.getModifiers( intrinsic ) );
						mods = newMods;
					}
					if ( mods.getBoolean( Modifiers.NONSTACKABLE_WATCH ) )
					{
						slot = Evaluator.WATCHES;
					}
					float delta = this.getScore( mods ) - nullScore;
					if ( delta < 0.0f ) continue;
					if ( delta == 0.0f )
					{
						if ( KoLCharacter.hasEquipped( item ) ) break gotItem;
						if ( ((count >> Evaluator.INITIAL_SHIFT) & Evaluator.SUBTOTAL_MASK) == 0 ) continue;
						if ( (count & Evaluator.AUTOMATIC_FLAG) != 0 ) continue;
					}

					if ( mods.getBoolean( Modifiers.UNARMED ) ||
						mods.getRawBitmap( Modifiers.MUTEX ) != 0 )
					{	// This item may turn out to be unequippable, so don't
						// count it towards the shortlist length.
						item = item.getInstance( count | Evaluator.CONDITIONAL_FLAG );
					}
				}
				// "break gotItem" goes here
				ranked[ slot ].add( item );
				if ( auxSlot != -1 ) ranked[ auxSlot ].add( item );
			}

			for ( int slot = 0; slot < ranked.length; ++slot )
			{
				if ( this.dump > 0 )
				{
					RequestLogger.printLine( "SLOT " + slot );
				}
				ArrayList list = ranked[ slot ];
				ListIterator i = list.listIterator();
				while ( i.hasNext() )
				{
					AdventureResult item = (AdventureResult) i.next();
					Spec spec = new Spec();
					spec.attachment = item;
					int useSlot = slot;
					switch ( slot )
					{
					case Evaluator.OFFHAND_MELEE:
					case Evaluator.OFFHAND_RANGED:
						useSlot = EquipmentManager.OFFHAND;
						break;
					case Evaluator.WATCHES:
						useSlot = EquipmentManager.ACCESSORY1;
						break;
					case Evaluator.WEAPON_1H:
						useSlot = EquipmentManager.WEAPON;
						break;
					}
					if ( slot >= EquipmentManager.ALL_SLOTS )
					{
						spec.setFamiliar( (FamiliarData) this.familiars.get(
							slot - EquipmentManager.ALL_SLOTS ) );
						useSlot = EquipmentManager.FAMILIAR;
					}
					Arrays.fill( spec.equipment, EquipmentRequest.UNEQUIP );
					spec.equipment[ useSlot ] = item;
					i.set( spec );
				}
				Collections.sort( list );
				if ( this.dump > 1 )
				{
					RequestLogger.printLine( list.toString() );
				}
				i = list.listIterator( list.size() );
				int total = 0;
				int beeotches = 0;
				int beeosity = 0;
				int b;
				int useful = this.maxUseful( slot );
				while ( i.hasPrevious() )
				{
					AdventureResult item = ((Spec) i.previous()).attachment;
					item = this.validateItem( item, maxPrice, priceLevel );
					int count = item.getCount();
					boolean auto = (count & Evaluator.AUTOMATIC_FLAG) != 0;
					boolean cond = (count & Evaluator.CONDITIONAL_FLAG) != 0;
					count &= TOTAL_MASK;
					if ( count == 0 )
					{
						continue;
					}
					if ( KoLCharacter.inBeecore() &&
						(b = KoLCharacter.getBeeosity( item.getName() )) > 0 )
					{	// This item is a beeotch!
						// Don't count it towards the number of items desired
						// in this slot's shortlist, since it may turn out to be
						// advantageous to use up all our allowed beeosity on
						// other slots.
						if ( auto )
						{
							automatic[ slot ].add( item );
							beeotches += count;
							beeosity += b * count;
						}
						else if ( total < useful && beeotches < useful &&
							beeosity < this.beeosity )
						{
							automatic[ slot ].add( item );
							beeotches += count;
							beeosity += b * count;
						}
					}
					else if ( auto )
					{
						automatic[ slot ].add( item );
						total += count;
					}
					else if ( total < useful )
					{
						automatic[ slot ].add( item );
						if ( !cond )
						{
							total += count;
						}
					}
				}
				if ( this.dump > 0 )
				{
					RequestLogger.printLine( automatic[ slot ].toString() );
				}
			}

			automatic[ EquipmentManager.ACCESSORY1 ].addAll( automatic[ Evaluator.WATCHES ] );
			automatic[ EquipmentManager.WEAPON ].addAll( automatic[ Evaluator.WEAPON_1H ] );
			automatic[ Evaluator.OFFHAND_MELEE ].addAll( automatic[ EquipmentManager.OFFHAND ] );
			automatic[ Evaluator.OFFHAND_RANGED ].addAll( automatic[ EquipmentManager.OFFHAND ] );

			Spec spec = new Spec();
			// The threshold in the slots array that indicates that a slot
			// should be considered will be either >= 1 or >= 0, depending
			// on whether inclusive or exclusive slot specs were used.
			for ( int thresh = 1; ; --thresh )
			{
				if ( thresh < 0 ) return;	// no slots enabled
				boolean anySlots = false;
				for ( int i = 0; i <= EquipmentManager.FAMILIAR; ++i )
				{
					if ( this.slots[ i ] >= thresh )
					{
						spec.equipment[ i ] = null;
						anySlots = true;
					}
				}
				if ( anySlots ) break;
			}

			if ( spec.equipment[ EquipmentManager.OFFHAND ] != null )
			{
				this.hands = 1;
				automatic[ EquipmentManager.WEAPON ] = automatic[ Evaluator.WEAPON_1H ];

				Iterator i = outfitPieces.keySet().iterator();
				while ( i.hasNext() )
				{
					id = ((AdventureResult) i.next()).getItemId();
					if ( EquipmentManager.itemIdToEquipmentType( id ) == EquipmentManager.WEAPON &&
						EquipmentDatabase.getHands( id ) > 1 )
					{
						i.remove();
					}
				}
			}

			spec.tryAll( this.familiars, usefulOutfits, outfitPieces, automatic );
		}
	}

	private static class Spec
	extends Speculation
	implements Comparable, Cloneable
	{
		private boolean scored = false;
		private boolean tiebreakered = false;
		private boolean exceeded;
		private float score, tiebreaker;
		private int simplicity;
		private int beeosity;

		public boolean failed = false;
		public AdventureResult attachment;

		@Override
		public Object clone()
		{
			try
			{
				Spec copy = (Spec) super.clone();
				copy.equipment = (AdventureResult[]) this.equipment.clone();
				return copy;
			}
			catch ( CloneNotSupportedException e )
			{
				return null;
			}
		}

		@Override
		public String toString()
		{
			if ( this.attachment != null )
			{
				return this.attachment.getInstance( (int) this.getScore() ).toString();
			}
			return super.toString();
		}

		public float getScore()
		{
			if ( this.scored ) return this.score;
			if ( !this.calculated ) this.calculate();
			this.score = MaximizerFrame.eval.getScore( this.mods );
			if ( KoLCharacter.inBeecore() )
			{
				this.beeosity = KoLCharacter.getBeeosity( this.equipment );
			}
			MaximizerFrame.eval.checkEquipment( this.mods, this.equipment,
				this.beeosity );
			this.failed = MaximizerFrame.eval.failed;
			if ( (this.mods.getRawBitmap( Modifiers.MUTEX_VIOLATIONS )
				& ~KoLCharacter.currentRawBitmapModifier( Modifiers.MUTEX_VIOLATIONS )) != 0 )
			{	// We're speculating about something that would create a
				// mutex problem that the player didn't already have.
				this.failed = true;
			}
			this.exceeded = MaximizerFrame.eval.exceeded;
			this.scored = true;
			return this.score;
		}

		public float getTiebreaker()
		{
			if ( this.tiebreakered ) return this.tiebreaker;
			if ( !this.calculated ) this.calculate();
			this.tiebreaker = MaximizerFrame.eval.getTiebreaker( this.mods );
			this.tiebreakered = true;
			this.simplicity = 0;
			for ( int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot )
			{
				AdventureResult item = this.equipment[ slot ];
				if ( item == null ) item = EquipmentRequest.UNEQUIP;
				if ( EquipmentManager.getEquipment( slot ).equals( item ) )
				{
					this.simplicity += 2;
				}
				else if ( item.equals( EquipmentRequest.UNEQUIP ) )
				{
					this.simplicity += slot == EquipmentManager.WEAPON ? -1 : 1;
				}
			}
			return this.tiebreaker;
		}

		public int compareTo( Object o )
		{
			if ( !(o instanceof Spec) ) return 1;
			Spec other = (Spec) o;
			int rv = Float.compare( this.getScore(), other.getScore() );
			if ( this.failed != other.failed ) return this.failed ? -1 : 1;
			if ( rv != 0 ) return rv;
			rv = other.beeosity - this.beeosity;
			if ( rv != 0 ) return rv;
			rv = Float.compare( this.getTiebreaker(), other.getTiebreaker() );
			if ( rv != 0 ) return rv;
			rv = this.simplicity - other.simplicity;
			if ( rv != 0 ) return rv;
			if ( this.attachment != null && other.attachment != null )
			{	// prefer items that you don't have to buy
				rv = (other.attachment.getCount() & Evaluator.BUYABLE_FLAG) -
					 (this.attachment.getCount() & Evaluator.BUYABLE_FLAG);
				if ( rv != 0 ) return rv;
				if ( KoLCharacter.inBeecore() )
				{	// prefer fewer Bs
					rv = KoLCharacter.getBeeosity( other.attachment.getName() ) -
						KoLCharacter.getBeeosity( this.attachment.getName() );
				}
			}
			return rv;
		}

		// Remember which equipment slots were null, so that this
		// state can be restored later.
		public Object mark()
		{
			return this.equipment.clone();
		}

		public void restore( Object mark )
		{
			System.arraycopy( mark, 0, this.equipment, 0, EquipmentManager.ALL_SLOTS );
		}

		public void tryAll( ArrayList familiars, BooleanArray usefulOutfits, TreeMap outfitPieces, ArrayList[] possibles )
			throws MaximizerInterruptedException
		{
			this.tryOutfits( usefulOutfits, outfitPieces, possibles );
			for ( int i = 0; i < familiars.size(); ++i )
			{
				this.setFamiliar( (FamiliarData) familiars.get( i ) );
				possibles[ EquipmentManager.FAMILIAR ] =
					possibles[ EquipmentManager.ALL_SLOTS + i ];
				this.tryOutfits( usefulOutfits, outfitPieces, possibles );
			}
		}

		public void tryOutfits( BooleanArray usefulOutfits, TreeMap outfitPieces, ArrayList[] possibles )
			throws MaximizerInterruptedException
		{
			Object mark = this.mark();
			for ( int outfit = usefulOutfits.size() - 1; outfit >= 0; --outfit )
			{
				if ( !usefulOutfits.get( outfit ) ) continue;
				AdventureResult[] pieces = EquipmentDatabase.getOutfit( outfit ).getPieces();
			pieceloop:
				for ( int idx = pieces.length - 1; ; --idx )
				{
					if ( idx == -1 )
					{	// all pieces successfully put on
						this.tryFamiliarItems( possibles );
						break;
					}
					AdventureResult item = (AdventureResult) outfitPieces.get( pieces[ idx ] );
					if ( item == null ) break;	// not available
					int count = item.getCount() & Evaluator.TOTAL_MASK;
					int slot = EquipmentManager.itemIdToEquipmentType( item.getItemId() );

					switch ( slot )
					{
					case EquipmentManager.HAT:
					case EquipmentManager.PANTS:
					case EquipmentManager.SHIRT:
						if ( item.equals( this.equipment[ slot ] ) )
						{	// already worn
							continue pieceloop;
						}
						if ( item.equals( this.equipment[ EquipmentManager.FAMILIAR ] ) )
						{
							--count;
						}
						break;
					case EquipmentManager.WEAPON:
					case EquipmentManager.OFFHAND:
						if ( item.equals( this.equipment[ EquipmentManager.WEAPON ] ) ||
							item.equals( this.equipment[ EquipmentManager.OFFHAND ] ) )
						{	// already worn
							continue pieceloop;
						}
						if ( item.equals( this.equipment[ EquipmentManager.FAMILIAR ] ) )
						{
							--count;
						}
						break;
					case EquipmentManager.ACCESSORY1:
						if ( item.equals( this.equipment[ EquipmentManager.ACCESSORY1 ] ) ||
							item.equals( this.equipment[ EquipmentManager.ACCESSORY2 ] ) ||
							item.equals( this.equipment[ EquipmentManager.ACCESSORY3 ] ) )
						{	// already worn
							continue pieceloop;
						}
						if ( item.equals( this.equipment[ EquipmentManager.FAMILIAR ] ) )
						{
							--count;
						}
						if ( this.equipment[ EquipmentManager.ACCESSORY3 ] == null )
						{
							slot = EquipmentManager.ACCESSORY3;
						}
						else if ( this.equipment[ EquipmentManager.ACCESSORY2 ] == null )
						{
							slot = EquipmentManager.ACCESSORY2;
						}
						break;
					default:
						break pieceloop;	// don't know how to wear that
					}

					if ( count <= 0 ) break;	// none available
					if ( this.equipment[ slot ] != null ) break; // slot taken
					this.equipment[ slot ] = item;
				}
				this.restore( mark );
			}

			this.tryFamiliarItems( possibles );
		}

		public void tryFamiliarItems( ArrayList[] possibles )
			throws MaximizerInterruptedException
		{
			Object mark = this.mark();
			if ( this.equipment[ EquipmentManager.FAMILIAR ] == null )
			{
				ArrayList possible = possibles[ EquipmentManager.FAMILIAR ];
				boolean any = false;
				for ( int pos = 0; pos < possible.size(); ++pos )
				{
					AdventureResult item = (AdventureResult) possible.get( pos );
					int count = item.getCount() & Evaluator.TOTAL_MASK;
					if ( item.equals( this.equipment[ EquipmentManager.OFFHAND ] ) )
					{
						--count;
					}
					if ( item.equals( this.equipment[ EquipmentManager.WEAPON ] ) )
					{
						--count;
					}
					if ( item.equals( this.equipment[ EquipmentManager.HAT ] ) )
					{
						--count;
					}
					if ( item.equals( this.equipment[ EquipmentManager.PANTS ] ) )
					{
						--count;
					}
					if ( count <= 0 ) continue;
					this.equipment[ EquipmentManager.FAMILIAR ] = item;
					this.tryContainers( possibles );
					any = true;
					this.restore( mark );
				}

				if ( any ) return;
				this.equipment[ EquipmentManager.FAMILIAR ] = EquipmentRequest.UNEQUIP;
			}

			this.tryContainers( possibles );
			this.restore( mark );
		}

		public void tryContainers( ArrayList[] possibles )
			throws MaximizerInterruptedException
		{
			Object mark = this.mark();
			if ( this.equipment[ EquipmentManager.CONTAINER ] == null )
			{
				ArrayList possible = possibles[ EquipmentManager.CONTAINER ];
				boolean any = false;
				for ( int pos = 0; pos < possible.size(); ++pos )
				{
					AdventureResult item = (AdventureResult) possible.get( pos );
					int count = item.getCount() & Evaluator.TOTAL_MASK;
					//if ( item.equals( this.equipment[ EquipmentManager.FAMILIAR ] ) )
					//{
					//	--count;
					//}
					//if ( count <= 0 ) continue;
					this.equipment[ EquipmentManager.CONTAINER ] = item;
					this.tryAccessories( possibles, 0 );
					any = true;
					this.restore( mark );
				}

				if ( any ) return;
				this.equipment[ EquipmentManager.CONTAINER ] = EquipmentRequest.UNEQUIP;
			}

			this.tryAccessories( possibles, 0 );
			this.restore( mark );
		}

		public void tryAccessories( ArrayList[] possibles, int pos )
			throws MaximizerInterruptedException
		{
			Object mark = this.mark();
			int free = 0;
			if ( this.equipment[ EquipmentManager.ACCESSORY1 ] == null ) ++free;
			if ( this.equipment[ EquipmentManager.ACCESSORY2 ] == null ) ++free;
			if ( this.equipment[ EquipmentManager.ACCESSORY3 ] == null ) ++free;
			if ( free > 0 )
			{
				ArrayList possible = possibles[ EquipmentManager.ACCESSORY1 ];
				boolean any = false;
				for ( ; pos < possible.size(); ++pos )
				{
					AdventureResult item = (AdventureResult) possible.get( pos );
					int count = item.getCount() & Evaluator.TOTAL_MASK;
					if ( item.equals( this.equipment[ EquipmentManager.ACCESSORY1 ] ) )
					{
						--count;
					}
					if ( item.equals( this.equipment[ EquipmentManager.ACCESSORY2 ] ) )
					{
						--count;
					}
					if ( item.equals( this.equipment[ EquipmentManager.ACCESSORY3 ] ) )
					{
						--count;
					}
					if ( count <= 0 ) continue;
					for ( count = Math.min( free, count ); count > 0; --count )
					{
						if ( this.equipment[ EquipmentManager.ACCESSORY1 ] == null )
						{
							this.equipment[ EquipmentManager.ACCESSORY1 ] = item;
						}
						else if ( this.equipment[ EquipmentManager.ACCESSORY2 ] == null )
						{
							this.equipment[ EquipmentManager.ACCESSORY2 ] = item;
						}
						else if ( this.equipment[ EquipmentManager.ACCESSORY3 ] == null )
						{
							this.equipment[ EquipmentManager.ACCESSORY3 ] = item;
						}
						else
						{
							System.out.println( "no room left???" );
							break;	// no room left - shouldn't happen
						}

						this.tryAccessories( possibles, pos + 1 );
						any = true;
					}
					this.restore( mark );
				}

				if ( any ) return;

				if ( this.equipment[ EquipmentManager.ACCESSORY1 ] == null )
				{
					this.equipment[ EquipmentManager.ACCESSORY1 ] = EquipmentRequest.UNEQUIP;
				}
				if ( this.equipment[ EquipmentManager.ACCESSORY2 ] == null )
				{
					this.equipment[ EquipmentManager.ACCESSORY2 ] = EquipmentRequest.UNEQUIP;
				}
				if ( this.equipment[ EquipmentManager.ACCESSORY3 ] == null )
				{
					this.equipment[ EquipmentManager.ACCESSORY3 ] = EquipmentRequest.UNEQUIP;
				}
			}

			this.trySwap( EquipmentManager.ACCESSORY1, EquipmentManager.ACCESSORY2 );
			this.trySwap( EquipmentManager.ACCESSORY2, EquipmentManager.ACCESSORY3 );
			this.trySwap( EquipmentManager.ACCESSORY3, EquipmentManager.ACCESSORY1 );

			this.tryHats( possibles );
			this.restore( mark );
		}

		public void tryHats( ArrayList[] possibles )
			throws MaximizerInterruptedException
		{
			Object mark = this.mark();
			if ( this.equipment[ EquipmentManager.HAT ] == null )
			{
				ArrayList possible = possibles[ EquipmentManager.HAT ];
				boolean any = false;
				for ( int pos = 0; pos < possible.size(); ++pos )
				{
					AdventureResult item = (AdventureResult) possible.get( pos );
					int count = item.getCount() & Evaluator.TOTAL_MASK;
					if ( item.equals( this.equipment[ EquipmentManager.FAMILIAR ] ) )
					{
						--count;
					}
					if ( count <= 0 ) continue;
					this.equipment[ EquipmentManager.HAT ] = item;
					this.tryShirts( possibles );
					any = true;
					this.restore( mark );
				}

				if ( any ) return;
				this.equipment[ EquipmentManager.HAT ] = EquipmentRequest.UNEQUIP;
			}

			this.tryShirts( possibles );
			this.restore( mark );
		}

		public void tryShirts( ArrayList[] possibles )
			throws MaximizerInterruptedException
		{
			Object mark = this.mark();
			if ( this.equipment[ EquipmentManager.SHIRT ] == null )
			{
				boolean any = false;
				if ( KoLCharacter.hasSkill( "Torso Awaregness" )  )
				{
					ArrayList possible = possibles[ EquipmentManager.SHIRT ];
					for ( int pos = 0; pos < possible.size(); ++pos )
					{
						AdventureResult item = (AdventureResult) possible.get( pos );
						int count = item.getCount() & Evaluator.TOTAL_MASK;
						//if ( item.equals( this.equipment[ EquipmentManager.FAMILIAR ] ) )
						//{
						//	--count;
						//}
						//if ( count <= 0 ) continue;
						this.equipment[ EquipmentManager.SHIRT ] = item;
						this.tryPants( possibles );
						any = true;
						this.restore( mark );
					}
				}

				if ( any ) return;
				this.equipment[ EquipmentManager.SHIRT ] = EquipmentRequest.UNEQUIP;
			}

			this.tryPants( possibles );
			this.restore( mark );
		}

		public void tryPants( ArrayList[] possibles )
			throws MaximizerInterruptedException
		{
			Object mark = this.mark();
			if ( this.equipment[ EquipmentManager.PANTS ] == null )
			{
				ArrayList possible = possibles[ EquipmentManager.PANTS ];
				boolean any = false;
				for ( int pos = 0; pos < possible.size(); ++pos )
				{
					AdventureResult item = (AdventureResult) possible.get( pos );
					int count = item.getCount() & Evaluator.TOTAL_MASK;
					if ( item.equals( this.equipment[ EquipmentManager.FAMILIAR ] ) )
					{
						--count;
					}
					if ( count <= 0 ) continue;
					this.equipment[ EquipmentManager.PANTS ] = item;
					this.tryWeapons( possibles );
					any = true;
					this.restore( mark );
				}

				if ( any ) return;
				this.equipment[ EquipmentManager.PANTS ] = EquipmentRequest.UNEQUIP;
			}

			this.tryWeapons( possibles );
			this.restore( mark );
		}

		public void tryWeapons( ArrayList[] possibles )
			throws MaximizerInterruptedException
		{
			Object mark = this.mark();
			boolean chefstaffable = KoLCharacter.hasSkill( "Spirit of Rigatoni" );
			if ( !chefstaffable && KoLCharacter.getClassType().equals( KoLCharacter.SAUCEROR ) )
			{
				chefstaffable =
					this.equipment[ EquipmentManager.ACCESSORY1 ].getItemId() == ItemPool.SPECIAL_SAUCE_GLOVE ||
					this.equipment[ EquipmentManager.ACCESSORY2 ].getItemId() == ItemPool.SPECIAL_SAUCE_GLOVE ||
					this.equipment[ EquipmentManager.ACCESSORY3 ].getItemId() == ItemPool.SPECIAL_SAUCE_GLOVE;
			}
			if ( this.equipment[ EquipmentManager.WEAPON ] == null )
			{
				ArrayList possible = possibles[ EquipmentManager.WEAPON ];
				boolean any = false;
				for ( int pos = 0; pos < possible.size(); ++pos )
				{
					AdventureResult item = (AdventureResult) possible.get( pos );
					if ( !chefstaffable &&
						EquipmentDatabase.getItemType( item.getItemId() ).equals( "chefstaff" ) )
					{
						continue;
					}
					int count = item.getCount() & Evaluator.TOTAL_MASK;
					if ( item.equals( this.equipment[ EquipmentManager.OFFHAND ] ) )
					{
						--count;
					}
					if ( item.equals( this.equipment[ EquipmentManager.FAMILIAR ] ) )
					{
						--count;
					}
					if ( count <= 0 ) continue;
					this.equipment[ EquipmentManager.WEAPON ] = item;
					this.tryOffhands( possibles );
					any = true;
					this.restore( mark );
				}

				// if ( any && <no unarmed items in shortlists> ) return;
				if ( MaximizerFrame.eval.melee < -1 || MaximizerFrame.eval.melee > 1 )
				{
					return;
				}
				this.equipment[ EquipmentManager.WEAPON ] = EquipmentRequest.UNEQUIP;
			}
			else if ( !chefstaffable && 
				EquipmentDatabase.getItemType( this.equipment[ EquipmentManager.WEAPON ].getItemId() ).equals( "chefstaff" ) )
			{
				return;
			}

			this.tryOffhands( possibles );
			this.restore( mark );
		}

		public void tryOffhands( ArrayList[] possibles )
			throws MaximizerInterruptedException
		{
			Object mark = this.mark();
			int weapon = this.equipment[ EquipmentManager.WEAPON ].getItemId();
			if ( EquipmentDatabase.getHands( weapon ) > 1 )
			{
				this.equipment[ EquipmentManager.OFFHAND ] = EquipmentRequest.UNEQUIP;
			}

			if ( this.equipment[ EquipmentManager.OFFHAND ] == null )
			{
				ArrayList possible;
				int weaponType = -1;
				if ( KoLCharacter.hasSkill( "Double-Fisted Skull Smashing" ) )
				{
					weaponType = EquipmentDatabase.getWeaponType( weapon );
				}
				switch ( weaponType )
				{
				case KoLConstants.MELEE:
					possible = possibles[ Evaluator.OFFHAND_MELEE ];
					break;
				case KoLConstants.RANGED:
					possible = possibles[ Evaluator.OFFHAND_RANGED ];
					break;
				default:
					possible = possibles[ EquipmentManager.OFFHAND ];
				}
				boolean any = false;

				for ( int pos = 0; pos < possible.size(); ++pos )
				{
					AdventureResult item = (AdventureResult) possible.get( pos );
					int count = item.getCount() & Evaluator.TOTAL_MASK;
					if ( item.equals( this.equipment[ EquipmentManager.WEAPON ] ) )
					{
						--count;
					}
					if ( item.equals( this.equipment[ EquipmentManager.FAMILIAR ] ) )
					{
						--count;
					}
					if ( count <= 0 ) continue;
					this.equipment[ EquipmentManager.OFFHAND ] = item;
					this.tryOffhands( possibles );
					any = true;
					this.restore( mark );
				}

				if ( any && weapon > 0 ) return;
				this.equipment[ EquipmentManager.OFFHAND ] = EquipmentRequest.UNEQUIP;
			}

			// doit
			this.calculated = false;
			this.scored = false;
			this.tiebreakered = false;
			if ( this.compareTo( MaximizerFrame.best ) > 0 )
			{
				MaximizerFrame.best = (Spec) this.clone();
			}
			MaximizerFrame.bestChecked++;
			long t = System.currentTimeMillis();
			if ( t > MaximizerFrame.bestUpdate )
			{
				Spec.showProgress();
				MaximizerFrame.bestUpdate = t + 5000;
			}
			this.restore( mark );
			if ( !KoLmafia.permitsContinue() )
			{
				throw new MaximizerInterruptedException();
			}
			if ( this.exceeded )
			{
				throw new MaximizerExceededException();
			}
		}

		private static int getMutex( AdventureResult item )
		{
			Modifiers mods = Modifiers.getModifiers( item.getName() );
			if ( mods == null )
			{
				return 0;
			}
			return mods.getRawBitmap( Modifiers.MUTEX );
		}

		private void trySwap( int slot1, int slot2 )
		{
			// If we are suggesting an accessory that's already being worn,
			// make sure we suggest the same slot (to minimize server hits).
			AdventureResult item1, item2, eq1, eq2;
			item1 = this.equipment[ slot1 ];
			if ( item1 == null ) item1 = EquipmentRequest.UNEQUIP;
			eq1 = EquipmentManager.getEquipment( slot1 );
			if ( eq1.equals( item1 ) ) return;
			item2 = this.equipment[ slot2 ];
			if ( item2 == null ) item2 = EquipmentRequest.UNEQUIP;
			eq2 = EquipmentManager.getEquipment( slot2 );
			if ( eq2.equals( item2 ) ) return;

			// The same thing applies to mutually exclusive accessories -
			// putting the new one in an earlier slot would cause an error
			// when the equipment is being changed.
			int imutex1, imutex2, emutex1, emutex2;
			imutex1 = getMutex( item1 );
			emutex1 = getMutex( eq1 );
			if ( (imutex1 & emutex1) != 0 ) return;
			imutex2 = getMutex( item2 );
			emutex2 = getMutex( eq2 );
			if ( (imutex2 & emutex2) != 0 ) return;

			if ( eq1.equals( item2 ) || eq2.equals( item1 ) ||
				(imutex1 & emutex2) != 0 || (imutex2 & emutex1) != 0 )
			{
				this.equipment[ slot1 ] = item2;
				this.equipment[ slot2 ] = item1;
			}
		}

		public static void showProgress()
		{
			StringBuffer msg = new StringBuffer();
			msg.append( MaximizerFrame.bestChecked );
			msg.append( " combinations checked, best score " );
			msg.append( MaximizerFrame.best.getScore() );
			if ( MaximizerFrame.best.failed )
			{
				msg.append( " (FAIL)" );
			}
			//if ( MaximizerFrame.best.tiebreakered )
			//{
			//	msg = msg + " / " + MaximizerFrame.best.getTiebreaker() + " / " +
			//		MaximizerFrame.best.simplicity;
			//}
			KoLmafia.updateDisplay( msg.toString() );
		}
	}

	public static class Boost
	implements Comparable
	{
		private boolean isEquipment, isShrug, priority;
		private String cmd, text;
		private int slot;
		private float boost;
		private AdventureResult item, effect;
		private FamiliarData fam;

		private Boost( String cmd, String text, AdventureResult item, float boost )
		{
			this.cmd = cmd;
			this.text = text;
			this.item = item;
			this.boost = boost;
			if ( cmd.length() == 0 )
			{
				this.text = "<html><font color=gray>" +
					text.replaceAll( "&", "&amp;" ) +
					"</font></html>";
			}
		}

		public Boost( String cmd, String text, AdventureResult effect, boolean isShrug, AdventureResult item, float boost, boolean priority )
		{
			this( cmd, text, item, boost );
			this.isEquipment = false;
			this.effect = effect;
			this.isShrug = isShrug;
			this.priority = priority;
		}

		public Boost( String cmd, String text, int slot, AdventureResult item, float boost )
		{
			this( cmd, text, item, boost );
			this.isEquipment = true;
			this.slot = slot;
		}

		public Boost( String cmd, String text, FamiliarData fam, float boost )
		{
			this( cmd, text, (AdventureResult) null, boost );
			this.isEquipment = true;
			this.fam = fam;
			this.slot = -1;
		}

		@Override
		public String toString()
		{
			return this.text;
		}

		public int compareTo( Object o )
		{
			if ( !(o instanceof Boost) ) return -1;
			Boost other = (Boost) o;

			if ( this.isEquipment != other.isEquipment )
			{
				return this.isEquipment ? -1 : 1;
			}
			if ( this.priority != other.priority )
			{
				return this.priority ? -1 : 1;
			}
			if ( this.isEquipment ) return 0;	// preserve order of addition
			int rv = Float.compare( other.boost, this.boost );
			return rv;
		}

		public boolean execute( boolean equipOnly )
		{
			if ( equipOnly && !this.isEquipment ) return false;
			if ( this.cmd.length() == 0 ) return false;
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( this.cmd );
			return true;
		}

		public void addTo( Spec spec )
		{
			if ( this.isEquipment )
			{
				if ( this.fam != null )
				{
					spec.setFamiliar( fam );
				}
				else if ( this.slot >= 0 && this.item != null )
				{
					spec.equip( slot, this.item );
				}
			}
			else if ( this.effect != null )
			{
				if ( this.isShrug )
				{
					spec.removeEffect( this.effect );
				}
				else
				{
					spec.addEffect( this.effect );
				}
			}
		}

		public AdventureResult getItem()
		{
			if ( this.effect != null ) return this.effect;
			return this.item;
		}
	}

}
