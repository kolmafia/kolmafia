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
import java.util.Enumeration;
import java.util.Iterator;
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
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.session.MoodManager;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.panel.ScrollablePanel;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterComboBox;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;
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
		"Equipment suggestions are not yet implemented.  When they are, there will be keywords for specifying which slots are allowed, constraining the type or handedness of your weapon, etc." +
		"<h3>Assumptions</h3>" +
		"All suggestions are based on the assumption that you will be adventuring in the currently selected location, with all your current effects, prior to the next rollover (since some things depend on the moon phases).  For best results, make sure the proper location is selected before maximizing.  This is especially true in The Sea and clan dungeons, which have many location-specific modifiers." +
		"<p>" +
		"Among effects, stat equalizer potions have a major effect on the suggested boosts, since they change the relative importance of additive and percentage stat boosts.  Likewise, elemental phials make certain resistance boosts pointless.  If you plan to use an equalizer or phial while adventuring, please use them first so that the suggestions take them into account." +
		"<h3>GUI Use</h3>" +
		"If the Max Price field is zero or blank, the limit will be the smaller of your available meat, or your autoBuyPriceLimit (default 20,000).  The other options should be self-explanatory." +
		"<p>" +
		"You can select multiple boosts, and the title of the list will indicate the net effect of applying them all - note that this isn't always just the sum of their individual effects." +
		"<h3>CLI Use</h3>" +
		"The Modifier Maximizer can be invoked from the gCLI or a script via <b>maximize <i>expression</i></b>, and will behave as if you'd selected Equipment: on-hand only, Max Price: don't check, and turned off the Include option.  The best equipment will automatically be equipped (once equipment suggestions are implemented, and you haven't specified the to-be-defined keyword to prevent this), but you'll still need to visit the GUI to apply effect boosts - there are too many factors in choosing between the available boosts for that to be safely automated." +
		"<h3>Limitations &amp; Bugs</h3>" +
		"This is still a work-in-progress, so don't expect ANYTHING to work perfectly at the moment.  However, here are some details that are especially broken:" +
		"<br>\u2022 Elemental resistance doesn't take into account the immunities and vulnerabilities given by phials." +
		"<br>\u2022 Items that can be installed at your campground for a bonus (such as Hobopolis bedding) aren't considered." +
		"<br>\u2022 Your song limit isn't considered when recommending buffs, nor are any daily casting limits." +
		"<br>\u2022 Weapon Damage, Ranged Damage, and Spell Damage are calculated assuming 100 points of base damage - in other words, additive and percentage boosts are considered to have exactly equal worth.  It's possible that Weapon and Ranged damage might use a better estimate of the base damage in the future, but for Spell Damage, the proper base depends on which spell you end up using." +
		"<br>\u2022 HP &amp; MP aren't currently calculated in the same oddball way the game does.  Boosters should be properly ranked, but the displayed boost amount won't exactly match the number of points you gain by using them (and therefore, the min/max limit keywords aren't very useful with these modifiers)." +
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
	
	public static void maximize( int equipLevel, int maxPrice, int priceLevel, boolean includeAll )
	{
		KoLmafia.forceContinue();
		MaximizerFrame.eval = new Evaluator( (String)
			MaximizerFrame.expressionSelect.getSelectedItem() );
		if ( !KoLmafia.permitsContinue() ) return;	// parsing error

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
			MaximizerFrame.boosts.add( new Boost( "", "(Equipment suggestions not implemented yet)", 0, null, 0.0f ) );
		}
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
						text = "(no direct source of " + name + ")";
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
						int buy = price > 0 ? Math.min( count, KoLCharacter.getAvailableMeat() / price) : 0;
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
				"equip all", "execute", list );
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
		private float[] weight, min, max;

		private static final Pattern KEYWORD_PATTERN = Pattern.compile( "\\G\\s*(\\+|-|)([\\d.]*)\\s*((?:[^-+,0-9]|(?<! )[-+0-9])+),?\\s*" );
		// Groups: 1=sign 2=weight 3=keyword
		
		public Evaluator( String expr )
		{
			this.weight = new float[ Modifiers.FLOAT_MODIFIERS ];
			this.min = new float[ Modifiers.FLOAT_MODIFIERS ];
			this.max = new float[ Modifiers.FLOAT_MODIFIERS ];
			Arrays.fill( min, Float.NEGATIVE_INFINITY );
			Arrays.fill( max, Float.POSITIVE_INFINITY );
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
			
			int mus = KoLCharacter.getBaseMuscle();
			int mys = KoLCharacter.getBaseMysticality();
			int mox = KoLCharacter.getBaseMoxie();
			String equalize = mods.getString( Modifiers.EQUALIZE );
			if ( equalize.startsWith( "Mus" ) )
			{
				mys = mox = mus;
			}
			else if ( equalize.startsWith( "Mys" ) )
			{
				mus = mox = mys;
			}
			else if ( equalize.startsWith( "Mox" ) )
			{
				mus = mys = mox;
			}
		
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
					val += mus + Math.ceil( mods.get( Modifiers.MUS_PCT ) * mus / 100.0f );
					break;
				case Modifiers.MYS:
					val += mys + Math.ceil( mods.get( Modifiers.MYS_PCT ) * mys / 100.0f );
					break;
				case Modifiers.MOX:
					val += mox + Math.ceil( mods.get( Modifiers.MOX_PCT ) * mox / 100.0f );
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
				case Modifiers.HP:	// Incorrect - actual formula is more complex
					val += Math.ceil( mods.get( Modifiers.HP_PCT ) * mus / 100.0f );
					break;
				case Modifiers.MP:	// Incorrect - actual formula is more complex
					val += Math.ceil( mods.get( Modifiers.MP_PCT ) * mys / 100.0f );
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
				}
				if ( val < min ) this.failed = true;
				score += weight * Math.min( val, max );
			}
			return score;
		}
		
		public float getTiebreaker( Modifiers mods )
		{
			return 0.0f;
		}
	
	}
	
	private static class Spec
	{
		private int MCD;
		private ArrayList equipment;
		private ArrayList effects;
		private FamiliarData familiar;
		private boolean calculated = false;
		private Modifiers mods;
		private float score;
		
		public boolean failed = false;
		
		public Spec()
		{
			this.MCD = KoLCharacter.getMindControlLevel();
			this.equipment = new ArrayList();
			// copy from EqMgr
			
			this.effects = new ArrayList();
			this.effects.addAll( KoLConstants.activeEffects );
			while ( this.effects.size() > 0 )
			{	// Strip out intrinsic effects - those granted by equipment
				// will be added from Intrinsic Effect modifiers.
				// This assumes that no intrinsic that is granted by anything
				// other than equipment has any real effect.
				int pos = this.effects.size() - 1;
				if ( ((AdventureResult) this.effects.get( pos )).getCount() >
					Integer.MAX_VALUE / 2 )
				{
					this.effects.remove( pos );
				}
				else break;
			}
			this.familiar = KoLCharacter.currentFamiliar;
		}
		
		public void setMindControlLevel( int MCD )
		{
			this.MCD = MCD;
		}
		
		public void addEffect( AdventureResult effect )
		{
			this.effects.add( effect );
		}
	
		public void removeEffect( AdventureResult effect )
		{
			this.effects.remove( effect );
		}
		
		public Modifiers calculate()
		{
			this.mods = KoLCharacter.recalculateAdjustments(
				false,
				this.MCD,
				this.equipment,
				this.effects,
				this.familiar,
				true );
			this.calculated = true;
			return this.mods;
		}
	
		public float getScore()
		{
			if ( !this.calculated ) this.calculate();
			this.score = MaximizerFrame.eval.getScore( this.mods );
			this.failed = MaximizerFrame.eval.failed;
			return this.score;
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
