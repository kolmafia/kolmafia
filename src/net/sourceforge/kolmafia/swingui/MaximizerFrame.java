/**
 * Copyright (c) 2005-2009, KoLmafia development team
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.TreeMap;
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
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.Speculation;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.MoodManager;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.panel.ScrollablePanel;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterComboBox;
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
	public static final JComboBox expressionSelect = new JComboBox( "mainstat|mus|mys|mox|familiar weight|HP|MP|ML|DA|DR|+combat|-combat|initiative|exp|meat drop|item drop|2.0 meat, 1.0 item|weapon damage|ranged damage|spell damage|adv|hot res|cold res|spooky res|stench res|sleaze res|all res|ML, 0.001 slime res".split( "\\|") );
	private SmartButtonGroup equipmentSelect, mallSelect;
	private AutoHighlightTextField maxPriceField;
	private JCheckBox includeAll;
	private final ShowDescriptionList boostList;
	private JLabel listTitle = null;
	
	private static Spec best;
	private static int bestChecked;
	private static long bestUpdate;
	
	private static final String TIEBREAKER = "10 familiar weight, 1 initiative, 5 exp, 1 item, 1 meat, 0.1 DA 1000 max, 1 DR, 0.5 all res, -10 mana cost, 1.0 mus, 0.5 mys, 1.0 mox, 1.5 mainstat, 1 HP, 1 MP, 1 weapon damage, 1 ranged damage, 1 spell damage, 1 cold damage, 1 hot damage, 1 sleaze damage, 1 spooky damage, 1 stench damage, 1 cold spell damage, 1 hot spell damage, 1 sleaze spell damage, 1 spooky spell damage, 1 stench spell damage, 1 critical, -1 fumble, 1 HP regen max, 3 MP regen max, 1 critical hit percent, 0.1 food drop, 0.1 booze drop, 0.1 hat drop, 0.1 weapon drop, 0.1 offhand drop, 0.1 shirt drop, 0.1 pants drop, 0.1 accessory drop";
	
	private static final String HELP_STRING = "<html><table width=750><tr><td>" +
		"<h3>General</h3>" +
		"The specification of what attributes to maximize is made by a comma-separated list of keywords, each possibly preceded by a numeric weight.  Commas can be omitted if the next item starts with a +, -, or digit.  Using just a +, or omitting the weight entirely, is equivalent to a weight of 1.  Likewise, using just a - is equivalent to a weight of -1.  Non-integer weights can be used, but may not be meaningful with all keywords." +
		"<h3>Modifiers</h3>" +
		"The full name of any numeric modifier (as shown by the <b>modref</b> CLI command) is a valid keyword, requesting that its value be maximized.  If multiple modifiers are given, their weights specify their relative importance.  Negative weights mean that smaller values are more desirable for that modifier." +
		"<p>" +
		"Shorter forms are allowed for many commonly used modifiers.  They can be abbreviated down to just the bold letters:" +
		"<br><b>mus</b>, <b>mys</b>, <b>mox</b>, <b>main</b>stat, <b>HP</b>, <b>MP</b>, <b>ML</b>, <b>DA</b>, <b>DR</b>, <b>com</b>bat rate, <b>item</b> drop, <b>meat</b> drop, <b>exp</b>erience, <b>adv</b>entures" +
		"<br>Also, resistance (of any type) can be abbreviated as <b>res</b>.  <b>all res</b>istance is a shortcut for giving the same weight to all five basic elements." +
		"<p>" +
		"Note that many modifiers come in pairs: a base value, plus a percentage boost (such as Moxie and Moxie Percent), or a penalty value.  In general, you only need to specify the base modifier, and any related modifiers will automatically be taken into account." +
		"<h3>Limits</h3>" +
		"Any modifier keyword can be followed by one or both of these special keywords:" +
		"<br><b>min</b> - The weight specifies the minimum acceptable value for the preceding modifier.  If the value is lower, the results will be flagged as a failure." +
		"<br><b>max</b> - The weight specifies the largest useful value for the preceding modifier.  Larger values will be ignored in the score calculation, allowing other specified modifiers to be boosted instead." +
		"<br>Note that the limit keywords won't quite work as expected for a modifier that you're trying to minimize." +
		"<h3>Equipment</h3>" +
		"Slot names can be used as keywords:" +
		"<br><b>hat</b>, <b>weapon</b>, <b>offhand</b>, <b>shirt</b>, <b>pants</b>, <b>acc1</b>, <b>acc2</b>, <b>acc3</b>, <b>familiar</b> (familiar is not yet supported; stickers and fake hands are not currently planned.)" +
		"<br>With positive weights, only the specified slots will be considered for maximization.  With negative weights, all but the specified slots will be considered." +
		"<br><b>empty</b> - With positive weight, consider only slots that are currently empty; with negative weight, only those that aren't empty.  Either way, <b>+<i>slot</i></b> and <b>-<i>slot</i></b> can be used to further refine the selected slots." +
		"<br><b>hand</b>ed - With a weight of 1, only 1-handed weapons will be considered.  With a larger weight, only weapons with at least that handedness will be considered." +
		"<br><b>melee</b> - With positive weight, only melee weapons will be considered.  With negative weight, only ranged weapons will be considered." +
		"<br><b>type <i>text</i></b> - Only weapons with a type containing <i>text</i> are considered; for example, <b>type club</b> if you plan to do some Seal Clubbing." +
		"<br><b>shield</b> - With positive weight, only shields will be considered for your off-hand.  Implies <b>1 handed</b>." +
		"<br><b>tie</b>breaker - With negative weight, disables the use of a tiebreaker function that tries to choose equipment with generally beneficial attributes, even if not explicitly requested.  There are only a few cases where this would be desirable: maximizing <b>+combat</b> or <b>-combat</b> (since there's usually only one item that can help), <b>adv</b> and/or <b>PvP fights</b> at rollover, and <b>familiar weight</b> when facing the Naughty Sorceress familiars." +
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
		"This is still a work-in-progress, so don't expect ANYTHING to work perfectly at the moment.  However, here are some details that are especially broken:" +
		"<br>\u2022 Items that can be installed at your campground for a bonus (such as Hobopolis bedding) aren't considered." +
		"<br>\u2022 Your song limit isn't considered when recommending buffs, nor are any daily casting limits." +
		"<br>\u2022 Mutually exclusive effects aren't handled properly." +
		"<br>\u2022 Weapon Damage, Ranged Damage, and Spell Damage are calculated assuming 100 points of base damage - in other words, additive and percentage boosts are considered to have exactly equal worth.  It's possible that Weapon and Ranged damage might use a better estimate of the base damage in the future, but for Spell Damage, the proper base depends on which spell you end up using." +
		"<br>\u2022 Effects which vary in power based on how many turns are left (love songs, Mallowed Out, etc.) are handled poorly.  If you don't have the effect, they'll be suggested based on the results you'd get from having a single turn of it.  If you have the effect already, extending it to raise the power won't even be considered.  Similar problems occur with effects that are based on how full or drunk you currently are." +
		"</td></tr></table></html>";

	public MaximizerFrame()
	{
		super( "Modifier Maximizer" );

		this.framePanel.add( new MaximizerPanel(), BorderLayout.NORTH );

		this.boostList = new ShowDescriptionList( MaximizerFrame.boosts, 12 );
		this.boostList.addListSelectionListener( this );

		this.framePanel.add( new BoostsPanel( this.boostList ), BorderLayout.CENTER );
		if ( this.eval != null )
		{
			this.valueChanged( null );
		}
	}

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
	}
	
	public void maximize()
	{
		MaximizerFrame.maximize( this.equipmentSelect.getSelectedIndex(),
			InputFieldUtilities.getValue( this.maxPriceField ),
			this.mallSelect.getSelectedIndex(),
			this.includeAll.isSelected() );
		this.valueChanged( null );
	}
	
	public static boolean maximize( int equipLevel, int maxPrice, int priceLevel, boolean includeAll )
	{
		KoLmafia.forceContinue();
		MaximizerFrame.eval = new Evaluator( (String)
			MaximizerFrame.expressionSelect.getSelectedItem() );
		if ( !KoLmafia.permitsContinue() ) return false;	// parsing error

		float current = MaximizerFrame.eval.getScore(
			KoLCharacter.getCurrentModifiers() );
		if ( maxPrice <= 0 )
		{
			maxPrice = Math.min( Preferences.getInteger( "autoBuyPriceLimit" ),
				KoLCharacter.getAvailableMeat() );
		}
	
		RequestThread.openRequestSequence();
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
			MaximizerFrame.boosts.add( new Boost( "", "(familiar items aren't considered yet)", -1, null, 0.0f ) );
			MaximizerFrame.best = new Spec();
			MaximizerFrame.bestChecked = 0;
			MaximizerFrame.bestUpdate = System.currentTimeMillis() + 5000;
			try
			{
				MaximizerFrame.eval.enumerateEquipment( equipLevel, maxPrice, priceLevel );
			}
			catch ( MaximizerInterruptedException e )
			{
				KoLmafia.forceContinue();
				MaximizerFrame.boosts.add( new Boost( "", "<font color=red>(interrupted, optimality not guaranteed)</font>", -1, null, 0.0f ) );
			}
			Spec.showProgress();
			
			for ( int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot )
			{	
				String slotname = EquipmentRequest.slotNames[ slot ];
				AdventureResult item = MaximizerFrame.best.equipment[ slot ];
				AdventureResult curr = EquipmentManager.getEquipment( slot );
				if ( curr.equals( item ) )
				{
					if ( slot >= EquipmentManager.STICKER1 ||
						curr.equals( EquipmentRequest.UNEQUIP ) ||
						equipLevel == -1 )
					{
						continue;
					}
					MaximizerFrame.boosts.add( new Boost( "", "keep " + slotname + ": " + item.getName(), -1, item, 0.0f ) );
					continue;
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
					if ( ((count >> Evaluator.INITIAL_SHIFT) & Evaluator.SUBTOTAL_MASK) != 0 )
					{	
						// on hand;
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
					if ( !KoLmafia.permitsContinue() ) equipLevel = 1;
				}
				else
				{
					MaximizerFrame.boosts.add( boost );
				}
			}
		}
		
		current = MaximizerFrame.eval.getScore(
			KoLCharacter.getCurrentModifiers() );
		boolean failed = MaximizerFrame.eval.failed;

		Iterator i = Modifiers.getAllModifiers();
		while ( i.hasNext() )
		{
			String name = (String) i.next();
			if ( !EffectDatabase.contains( name ) )
			{
				continue;
			}

			float delta;
			Spec spec = new Spec();
			AdventureResult effect = new AdventureResult( name, 1, true );
			name = effect.getName();
			boolean hasEffect = KoLConstants.activeEffects.contains( effect );

			String cmd, text;
			AdventureResult item = null;
			int price = 0;
			if ( !hasEffect )
			{
				spec.addEffect( effect );
				delta = spec.getScore() - current;
				if ( delta <= 0.0f ) continue;
				cmd = MoodManager.getDefaultAction( "lose_effect", name );
				if ( cmd.length() == 0 )
				{
					if ( includeAll )
					{
						text = EffectDatabase.getActionNote( name );
						if ( text != null )
						{
							if ( text.indexOf( "BM" ) != -1 &&
								!KoLCharacter.inBadMoon() )
							{
								continue;	// no use displaying this in non-BM
							}
							text = "(get " + name + " via " + text + ")";
						}
						else
						{
							text = "(no direct source of " + name + ")";
						}
					}
					else continue;
				}
				else
				{
					text = cmd;
				}
			}
			else
			{
				spec.removeEffect( effect );
				delta = spec.getScore() - current;
				if ( delta <= 0.0f ) continue;
				cmd = MoodManager.getDefaultAction( "gain_effect", name );
				if ( cmd.length() == 0 )
				{
					if ( includeAll )
					{
						text = "(find some way to remove " + name + ")";
					}
					else continue;
				}
				else
				{
					text = cmd;
					if ( cmd.toLowerCase().indexOf( name.toLowerCase() ) == -1 )
					{
						text = text + " (to remove " + name + ")";
					}
				}
			}
			
			if ( cmd.startsWith( "use " ) || cmd.startsWith( "chew " ) ||
				cmd.startsWith( "drink " ) || cmd.startsWith( "eat " ) )
			{
				item = ItemFinder.getFirstMatchingItem(
					cmd.substring( cmd.indexOf( " " ) + 1 ).trim(), false );
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
			else if ( cmd.startsWith( "concert " ) )
			{
				if ( Preferences.getBoolean( "concertVisited" ) )
				{
					cmd = "";
				}
			}
			else if ( cmd.startsWith( "telescope " ) )
			{
				if ( Preferences.getBoolean( "telescopeLookedHigh" ) )
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
			}
			
			if ( item != null )
			{
				String iname = item.getName();
				
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
							if ( !KoLCharacter.canInteract() )
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
							price = StoreManager.getMallPrice( item );
						}
					}
					if ( price > maxPrice ) continue;
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
			MaximizerFrame.boosts.add( new Boost( cmd, text, effect, hasEffect,
				item, delta ) );
		}
		if ( MaximizerFrame.boosts.size() == 0 )
		{
			MaximizerFrame.boosts.add( new Boost( "", "(nothing useful found)", 0, null, 0.0f ) );
		}
		MaximizerFrame.boosts.sort();
		RequestThread.closeRequestSequence();
		return equipLevel == -1 && failed;
	}
	
	public static class MaximizerInterruptedException
		extends Exception
	{
	}

	private class MaximizerPanel
		extends GenericPanel
	{
		public MaximizerPanel()
		{
			super( "update", "help", new Dimension( 80, 20 ), new Dimension( 420, 20 ) );

			MaximizerFrame.expressionSelect.setEditable( true );
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

		public void actionConfirmed()
		{
			MaximizerFrame.this.maximize();
		}

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
		public boolean failed;
		private Evaluator tiebreaker;
		private float[] weight, min, max;
		private int dump = 0;
		
		private int[] slots = new int[ EquipmentManager.ALL_SLOTS ];
		String weaponType = null;
		int hands = 0;
		int melee = 0;
		boolean requireShield = false;
		boolean noTiebreaker = false;

		private static final Pattern KEYWORD_PATTERN = Pattern.compile( "\\G\\s*(\\+|-|)([\\d.]*)\\s*((?:[^-+,0-9]|(?<! )[-+0-9])+),?\\s*" );
		// Groups: 1=sign 2=weight 3=keyword
		
		// The counts of possible equipment items are overloaded:
		// The total available for use is in the low bits, masked by TOTAL_MASK.
		// Amount from individual sources are in bitfields shifted by one of
		// the XXX_SHIFT amounts, masked by SUBTOTAL_MASK.
		// BUYABLE_FLAG is set if further items can be bought from the Mall.
		// AUTOMATIC_FLAG is set if the item must be considered regardless
		// of its ranking.
		public static final int TOTAL_MASK = 0xFF;
		public static final int SUBTOTAL_MASK = 0x0F;
		public static final int INITIAL_SHIFT = 8;
		public static final int CREATABLE_SHIFT = 12;
		public static final int NPCBUYABLE_SHIFT = 16;
		public static final int FOLDABLE_SHIFT = 20;
		public static final int PULLABLE_SHIFT = 24;
		public static final int BUYABLE_FLAG = 1 << 28;
		public static final int AUTOMATIC_FLAG = 1 << 29;
		
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
		
		private static final int[] MAX_USEFUL = new int[ EquipmentManager.ALL_SLOTS ];
		static {
			Arrays.fill( MAX_USEFUL, 1 );
			MAX_USEFUL[ EquipmentManager.HAT ] = 2;	// Mad Hatrack
			MAX_USEFUL[ Evaluator.WEAPON_1H ] = 3;	// Dual-wield + Hand
			MAX_USEFUL[ EquipmentManager.ACCESSORY1 ] = 3;
		}
		
		private Evaluator()
		{
		}
		
		public Evaluator( String expr )
		{
			this.tiebreaker = new Evaluator();
			this.weight = new float[ Modifiers.FLOAT_MODIFIERS ];
			this.tiebreaker.weight = new float[ Modifiers.FLOAT_MODIFIERS ];
			this.tiebreaker.min = new float[ Modifiers.FLOAT_MODIFIERS ];
			this.tiebreaker.max = new float[ Modifiers.FLOAT_MODIFIERS ];
			Arrays.fill( this.tiebreaker.min, Float.NEGATIVE_INFINITY );
			Arrays.fill( this.tiebreaker.max, Float.POSITIVE_INFINITY );
			this.tiebreaker.parse( MaximizerFrame.TIEBREAKER );
			this.min = (float[]) this.tiebreaker.min.clone();
			this.max = (float[]) this.tiebreaker.max.clone();
			this.parse( expr );
		}
		
		private void parse( String expr )
		{
			expr = expr.trim().toLowerCase();
			Matcher m = KEYWORD_PATTERN.matcher( expr );
			int pos = 0;
			int index = -1;
			while ( pos < expr.length() )
			{
				if ( !m.find() )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
						"Unable to interpret: " + expr.substring( pos ) );
					return;
				}
				pos = m.end();
				float weight = StringUtilities.parseFloat(
					m.end( 2 ) == m.start( 2 ) ? m.group( 1 ) + "1"
						: m.group( 1 ) + m.group( 2 ) );
				
				String keyword = m.group( 3 ).trim();
				if ( keyword.equals( "min" ) )
				{
					if ( index >= 0 )
					{
						this.min[ index ] = weight;
						continue;
					}
					else
					{
						KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
							"'min' without preceding modifier" );
						return;
					}
				}
				else if ( keyword.equals( "max" ) )
				{
					if ( index >= 0 )
					{
						this.max[ index ] = weight;
						continue;
					}
					else
					{
						KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
							"'max' without preceding modifier" );
						return;
					}
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
						this.slots[ EquipmentManager.OFFHAND ] = -1;
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
					this.melee = (int) weight;
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
					index = Modifiers.ADVENTURES;
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
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
					"Unrecognized keyword: " + keyword );
				return;
			}
		}
		
		public float getScore( Modifiers mods )
		{
			this.failed = false;
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
					val += 100.0f + Math.min( 0.0f, mods.get( Modifiers.MEATDROP_PENALTY ) );
					break;
				case Modifiers.ITEMDROP:
					val += 100.0f + Math.min( 0.0f, mods.get( Modifiers.ITEMDROP_PENALTY ) );
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
				}
				if ( val < min ) this.failed = true;
				score += weight * Math.min( val, max );
			}
			return score;
		}
		
		public float getTiebreaker( Modifiers mods )
		{
			if ( this.noTiebreaker ) return 0.0f;
			return this.tiebreaker.getScore( mods );
		}
		
		public AdventureResult checkItem( int id, int equipLevel, int maxPrice )
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
			
			if ( KoLCharacter.canInteract() )
			{	// consider Mall buying
				if ( count == 0 && ItemDatabase.isTradeable( id ) )
				{	// but only if none are otherwise available
					count = 1 | Evaluator.BUYABLE_FLAG;
				}
			}
			else if ( !KoLCharacter.isHardcore() )
			{	// consider pulling
				int pull = ItemPool.get( id, 0 ).getCount( KoLConstants.storage );
				count += (pull << Evaluator.PULLABLE_SHIFT) | pull;
			}
			return ItemPool.get( id, count );
		}
		
		public AdventureResult validateItem( AdventureResult item, int maxPrice, int priceLevel )
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
			// outfit pieces, synergies, hobo power, brimstone, etc.
			ArrayList[] automatic = new ArrayList[ EquipmentManager.ALL_SLOTS ];
			// Items with a positive score
			ArrayList[] ranked = new ArrayList[ EquipmentManager.ALL_SLOTS ];
			// Items with zero score (in case no positive scores were found)
			ArrayList[] neutral = new ArrayList[ EquipmentManager.ALL_SLOTS ];
			for ( int i = 0; i < EquipmentManager.ALL_SLOTS; ++i )
			{
				automatic[ i ] = new ArrayList();
				ranked[ i ] = new ArrayList();
				neutral[ i ] = new ArrayList();
			}
			
			float nullScore = this.getScore( new Modifiers() );

			BooleanArray usefulOutfits = new BooleanArray();
			TreeMap outfitPieces = new TreeMap();
			for ( int i = 1; i < EquipmentDatabase.normalOutfits.size(); ++i )
			{
				SpecialOutfit outfit = EquipmentDatabase.normalOutfits.get( i );
				if ( outfit == null ) continue;
				Modifiers mods = Modifiers.getModifiers( outfit.getName() );
				if ( mods == null )	continue;
				float delta = this.getScore( mods ) - nullScore;
				if ( delta <= 0.0f ) continue;
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
			boolean brimstoneUseful = false;
			boolean slimeHateUseful = false;
			boolean stickersUseful = false;
			{
				Modifiers mods = Modifiers.getModifiers( "_hoboPower" );
				if ( mods != null &&
					this.getScore( mods ) - nullScore > 0.0f )
				{
					hoboPowerUseful = true;
				}
			}
			{
				Modifiers mods = Modifiers.getModifiers( "_brimstone" );
				if ( mods != null &&
					this.getScore( mods ) - nullScore > 0.0f )
				{
					brimstoneUseful = true;
				}
			}
			{
				Modifiers mods = Modifiers.getModifiers( "_slimeHate" );
				if ( mods != null &&
					this.getScore( mods ) - nullScore > 0.0f )
				{
					slimeHateUseful = true;
				}
			}
			{
				Modifiers mods = Modifiers.getModifiers( "_stickers" );
				if ( mods != null &&
					this.getScore( mods ) - nullScore > 0.0f )
				{
					stickersUseful = true;
				}
			}
				
			int id = 0;
			while ( (id = EquipmentDatabase.nextEquipmentItemId( id )) != -1 )
			{
				if ( !EquipmentManager.canEquip( id ) ) continue;
				int slot = EquipmentManager.itemIdToEquipmentType( id );
				if ( slot < 0 || slot >= EquipmentManager.ALL_SLOTS ) continue;
				AdventureResult item = this.checkItem( id, equipLevel, maxPrice );
				int count = item.getCount();
				if ( count == 0 ) continue;
				String name = item.getName();
				
				ArrayList[] dest = null;
				int auxSlot = -1;
			gotItem:
				{
					switch ( slot )
					{
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
							if ( !type.equals( "chefstaff" ) &&
								!this.requireShield )
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
						if ( id == ItemPool.SPECIAL_SAUCE_GLOVE &&
							KoLCharacter.getClassType().equals( KoLCharacter.SAUCEROR )
							&& !KoLCharacter.hasSkill( "Spirit of Rigatoni" ) ) 
						{
							item = item.getInstance( count | Evaluator.AUTOMATIC_FLAG );
							dest = automatic;
							break gotItem;
						}
						if ( hoboPowerUseful && name.startsWith( "Hodgman's" ) )
						{
							Modifiers.hoboPower = 100.0f;
							count |= Evaluator.AUTOMATIC_FLAG;
							item = item.getInstance( count );
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
						if ( (count & Evaluator.BUYABLE_FLAG) != 0 ) continue;
						dest = neutral;
						break gotItem;
					}
					
					if ( mods.getBoolean( Modifiers.SINGLE ) )
					{
						count = (count & ~Evaluator.TOTAL_MASK) | 1;
						item = item.getInstance( count );
					}
					
					if ( ( hoboPowerUseful &&
							mods.get( Modifiers.HOBO_POWER ) > 0.0f ) ||
						( brimstoneUseful &&
							mods.getRawBitmap( Modifiers.BRIMSTONE ) != 0 ) ||
						( slimeHateUseful &&
							mods.get( Modifiers.SLIME_HATES_IT ) > 0.0f ) ||
						( stickersUseful &&
							EquipmentManager.isStickerWeapon( item ) ) ||
						( (mods.getRawBitmap( Modifiers.SYNERGETIC )
							& usefulSynergies) != 0 ) )
					{
						item = item.getInstance( count | Evaluator.AUTOMATIC_FLAG );
						dest = automatic;
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
					float delta = this.getScore( mods ) - nullScore;
					if ( delta < 0.0f ) continue;
					if ( delta == 0.0f )
					{
						if ( (count & Evaluator.BUYABLE_FLAG) != 0 ) continue;
						dest = neutral;
						break gotItem;
					}
					if ( mods.getBoolean( Modifiers.NONSTACKABLE_WATCH ) )
					{
						item = item.getInstance( count | Evaluator.AUTOMATIC_FLAG );
						slot = Evaluator.WATCHES;
						dest = automatic;
						break gotItem;
					}
					dest = ranked;
				}
				// "break gotItem" goes here
				dest[ slot ].add( item );
				if ( auxSlot != -1 ) dest[ auxSlot ].add( item );
			}
			
			for ( int slot = 0; slot < EquipmentManager.ALL_SLOTS; ++slot )
			{
				if ( this.dump > 0 )
				{
					RequestLogger.printLine( "SLOT " + slot );
				}
				ArrayList list = ranked[ slot ];
				if ( list.size() == 0 )
				{
					list = neutral[ slot ];
				}
				else if ( list.size() < Evaluator.MAX_USEFUL[ slot ] )
				{
					list.addAll( neutral[ slot ] );
				}
				list.addAll( automatic[ slot ] );
				automatic[ slot ].clear();
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
				while ( i.hasPrevious() )
				{	
					AdventureResult item = ((Spec) i.previous()).attachment;
					item = this.validateItem( item, maxPrice, priceLevel );
					int count = item.getCount();
					if ( (count & Evaluator.TOTAL_MASK) == 0 )
					{
						continue;
					}
					if ( (count & Evaluator.AUTOMATIC_FLAG) != 0 ||
						automatic[ slot ].size() < Evaluator.MAX_USEFUL[ slot ] )
					{
						automatic[ slot ].add( item );
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
				for ( int i = 0; i < EquipmentManager.FAMILIAR; ++i )
				{
					if ( this.slots[ i ] >= thresh )
					{
						spec.equipment[ i ] = null;
						anySlots = true;
					}
				}
				if ( anySlots ) break;
			}
			spec.tryAll( usefulOutfits, outfitPieces, automatic );
		}
	}
	
	private static class Spec
	extends Speculation
	implements Comparable, Cloneable
	{
		private boolean scored = false;
		private boolean tiebreakered = false;
		private float score, tiebreaker;
		private int simplicity;
		
		public boolean failed = false;
		public AdventureResult attachment;
		
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
			this.failed = MaximizerFrame.eval.failed;
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
			rv = Float.compare( this.getTiebreaker(), other.getTiebreaker() );
			if ( rv != 0 ) return rv;
			rv = this.simplicity - other.simplicity;
			if ( rv != 0 ) return rv;
			if ( this.attachment != null && other.attachment != null )
			{	// prefer items that you don't have to buy
				return (other.attachment.getCount() & Evaluator.BUYABLE_FLAG) -
					 (this.attachment.getCount() & Evaluator.BUYABLE_FLAG);
			}
			return 0;
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
		
		public void tryAll( BooleanArray usefulOutfits, TreeMap outfitPieces, ArrayList[] possibles )
			throws MaximizerInterruptedException
		{
			this.tryOutfits( usefulOutfits, outfitPieces, possibles );
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
						this.tryAccessories( possibles, 0 );
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
			
			this.tryAccessories( possibles, 0 );
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
					}
					this.restore( mark );
				}
			
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
					this.restore( mark );
				}
			
				this.equipment[ EquipmentManager.HAT ] = EquipmentRequest.UNEQUIP;
			}
			
			this.tryShirts( possibles );
			this.restore( mark );
		}
		
		public void tryShirts( ArrayList[] possibles )
			throws MaximizerInterruptedException
		{
			Object mark = this.mark();
			if ( this.equipment[ EquipmentManager.SHIRT ] == null &&
				KoLCharacter.hasSkill( "Torso Awaregness" ) )
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
					this.restore( mark );
				}
			
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
					this.restore( mark );
				}
			
				this.equipment[ EquipmentManager.PANTS ] = EquipmentRequest.UNEQUIP;
			}
			
			this.tryWeapons( possibles );
			this.restore( mark );
		}
		
		public void tryWeapons( ArrayList[] possibles )
			throws MaximizerInterruptedException
		{
			Object mark = this.mark();
			if ( this.equipment[ EquipmentManager.WEAPON ] == null )
			{
				ArrayList possible = possibles[ EquipmentManager.WEAPON ];
				for ( int pos = 0; pos < possible.size(); ++pos )
				{
					AdventureResult item = (AdventureResult) possible.get( pos );
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
					this.restore( mark );
				}
			
				this.equipment[ EquipmentManager.WEAPON ] = EquipmentRequest.UNEQUIP;
			}
			
			this.tryOffhands( possibles );
			this.restore( mark );
		}
		
		public void tryOffhands( ArrayList[] possibles )
			throws MaximizerInterruptedException
		{
			Object mark = this.mark();
			int weapon = this.equipment[ EquipmentManager.WEAPON ].getItemId();
			boolean requireGlove = false;
			int weaponType = -1;
			if ( EquipmentDatabase.getHands( weapon ) > 1 )
			{
				this.equipment[ EquipmentManager.OFFHAND ] = EquipmentRequest.UNEQUIP;
			}
			else if ( EquipmentDatabase.getItemType( weapon ).equals( "chefstaff" ) &&
				!KoLCharacter.hasSkill( "Spirit of Rigatoni" ) ) 
			{
				if ( !KoLCharacter.getClassType().equals( KoLCharacter.SAUCEROR ) )
				{	// This can't work.
					return;
				}
				requireGlove = true;
			}
			
			if ( !requireGlove &&
				KoLCharacter.hasSkill( "Double-Fisted Skull Smashing" ) )
			{
				weaponType = EquipmentDatabase.getWeaponType( weapon );
			}			
			
			if ( this.equipment[ EquipmentManager.OFFHAND ] == null )
			{
				ArrayList possible;
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
				
				for ( int pos = 0; pos < possible.size(); ++pos )
				{
					AdventureResult item = (AdventureResult) possible.get( pos );
					if ( requireGlove &&
						item.getItemId() != ItemPool.SPECIAL_SAUCE_GLOVE )
					{
						continue;
					}
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
					this.restore( mark );
				}
			
				if ( requireGlove ) return;
				this.equipment[ EquipmentManager.OFFHAND ] = EquipmentRequest.UNEQUIP;
			}
			else if ( requireGlove && this.equipment[ EquipmentManager.OFFHAND ].getItemId() != ItemPool.SPECIAL_SAUCE_GLOVE )
			{
				return;
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
		}
		
		private void trySwap( int slot1, int slot2 )
		{
			AdventureResult item1, item2, eq1, eq2;
			item1 = this.equipment[ slot1 ];
			if ( item1 == null ) item1 = EquipmentRequest.UNEQUIP;
			eq1 = EquipmentManager.getEquipment( slot1 );
			if ( eq1.equals( item1 ) ) return;
			item2 = this.equipment[ slot2 ];
			if ( item2 == null ) item2 = EquipmentRequest.UNEQUIP;
			eq2 = EquipmentManager.getEquipment( slot2 );
			if ( eq2.equals( item2 ) ) return;
			if ( eq1.equals( item2 ) || eq2.equals( item1 ) )
			{
				this.equipment[ slot1 ] = item2;
				this.equipment[ slot2 ] = item1;
			}
		}
		
		public static void showProgress()
		{
			String msg = MaximizerFrame.bestChecked + " combinations checked, best score " + MaximizerFrame.best.getScore();
			//if ( MaximizerFrame.best.tiebreakered )
			//{
			//	msg = msg + " / " + MaximizerFrame.best.getTiebreaker() + " / " +
			//		MaximizerFrame.best.simplicity;
			//}
			KoLmafia.updateDisplay( msg );
		}
	}
	
	public static class Boost
	implements Comparable
	{
		private boolean isEquipment, isShrug;
		private String cmd, text;
		private int slot;
		private float boost;
		private AdventureResult item, effect;
		
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

		public Boost( String cmd, String text, AdventureResult effect, boolean isShrug, AdventureResult item, float boost )
		{
			this( cmd, text, item, boost );
			this.isEquipment = false;
			this.effect = effect;
			this.isShrug = isShrug;
		}

		public Boost( String cmd, String text, int slot, AdventureResult item, float boost )
		{
			this( cmd, text, item, boost );
			this.isEquipment = true;
			this.slot = slot;
		}
		
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
			if ( this.isEquipment ) return 0;	// preserve order of addition
			int rv = Float.compare( other.boost, this.boost );
			if ( rv == 0 ) rv = other.cmd.compareTo( this.cmd );
			if ( rv == 0 ) rv = other.text.compareTo( this.text );
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
				if ( this.slot >= 0 && this.item != null )
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
