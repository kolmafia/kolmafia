/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia.webui;

import java.net.URLEncoder;
import java.util.ArrayList;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.EffectPool;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.MoodManager;

import net.sourceforge.kolmafia.request.BasementRequest;

import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

public class BasementDecorator
{
	public static final void decorate( final StringBuffer buffer )
	{
		addBasementButtons( buffer );

		if ( buffer.indexOf( "Got Silk?" ) != -1 )
		{
			BasementDecorator.addBasementChoiceSpoilers( buffer, "Moxie", "Muscle" );
			return;
		}

		if ( buffer.indexOf( "Save the Dolls" ) != -1 )
		{
			BasementDecorator.addBasementChoiceSpoilers( buffer, "Mysticality", "Moxie" );
			return;
		}

		if ( buffer.indexOf( "Take the Red Pill" ) != -1 )
		{
			BasementDecorator.addBasementChoiceSpoilers( buffer, "Muscle", "Mysticality" );
			return;
		}

		addBasementSpoilers( buffer );
	}

	private static final void addBasementButtons( final StringBuffer buffer )
	{
		if ( !Preferences.getBoolean( "relayAddsCustomCombat" ) )
		{
			return;
		}

		int insertionPoint = buffer.indexOf( "<tr" );
		if ( insertionPoint != -1 )
		{
			StringBuffer actionBuffer = new StringBuffer();
			actionBuffer.append( "<tr><td align=left>" );

			BasementDecorator.addBasementButton( "action", buffer, actionBuffer, "auto", true );
			BasementDecorator.addBasementButton( "rebuff", buffer, actionBuffer, "rebuff", false );
			BasementDecorator.addBasementButton( "", buffer, actionBuffer, "refresh", true );

			actionBuffer.append( "</td></tr><tr><td><font size=1>&nbsp;</font></td></tr>" );
			buffer.insert( insertionPoint, actionBuffer.toString() );
		}
	}

	private static final void addBasementButton( final String parameter, final StringBuffer response,
		final StringBuffer buffer, final String action, final boolean isEnabled )
	{
		buffer.append( "<input type=\"button\" onClick=\"" );

		if ( parameter.startsWith( "rebuff" ) )
		{
			buffer.append( "runBasementScript(); void(0);" );
		}
		else
		{
			buffer.append( "document.location.href='basement.php" );

			if ( parameter.equals( "action" ) )
			{
				buffer.append( "?action=" );
				buffer.append( BasementRequest.getBasementAction( response.toString() ) );
			}

			buffer.append( "'; void(0);" );
		}

		buffer.append( "\" value=\"" );
		buffer.append( action );
		buffer.append( "\"" + ( isEnabled ? "" : " disabled" ) + ">&nbsp;" );
	}

	public static final void addBasementSpoilers( final StringBuffer buffer )
	{
		if ( !BasementRequest.checkBasement( false, buffer.toString() ) )
		{
			return;
		}

		buffer.insert(
			buffer.indexOf( "</head>" ), "<script language=\"Javascript\" src=\"/basement.js\"></script></head>" );

		StringBuffer changes = new StringBuffer();
		changes.append( "<table>" );
		changes.append( "<tr><td><select id=\"gear\" style=\"width: 400px\"><option value=\"none\">- change your equipment -</option>" );

		// Add outfits

		SpecialOutfit outfit;
		for ( int i = 0; i < EquipmentManager.getCustomOutfits().size(); ++i )
		{
			outfit = (SpecialOutfit) EquipmentManager.getCustomOutfits().get( i );

			changes.append( "<option value=\"outfit+" );

			try
			{
				changes.append( URLEncoder.encode( outfit.getName(), "UTF-8" ) );
			}
			catch ( Exception e )
			{
				changes.append( StaticEntity.globalStringReplace( outfit.getName(), " ", "+" ) );
			}

			changes.append( "\">outfit " );
			changes.append( outfit.getName().substring( 8 ) );
			changes.append( "</option>" );
		}

		for ( int i = 0; i < BasementRequest.POSSIBLE_FAMILIARS.length; ++i )
		{
			if ( !KoLCharacter.getFamiliarList().contains( BasementRequest.POSSIBLE_FAMILIARS[ i ] ) )
			{
				continue;
			}

			changes.append( "<option value=\"familiar+" );
			changes.append( StaticEntity.globalStringReplace(
				BasementRequest.POSSIBLE_FAMILIARS[ i ].getRace(), " ", "+" ) );
			changes.append( "\">familiar " );
			changes.append( BasementRequest.POSSIBLE_FAMILIARS[ i ].getRace() );
			changes.append( "</option>" );
		}

		changes.append( "</select></td><td>&nbsp;</td><td valign=top align=left><input type=\"button\" value=\"exec\" onClick=\"changeBasementGear();\"></td></tr>" );

		// Add effects

		ArrayList listedEffects = BasementRequest.getStatBoosters();

		if ( !listedEffects.isEmpty() )
		{
			String computeFunction =
				"computeNetBoost(" + BasementRequest.getBasementTestCurrent() + "," + BasementRequest.getBasementTestValue() + ");";

			String modifierName = Modifiers.getModifierName( BasementRequest.getActualStatNeeded() );
			modifierName = StaticEntity.globalStringDelete( modifierName, "Maximum " ).toLowerCase();

			changes.append( "<tr><td><select onchange=\"" );
			changes.append( computeFunction );
			changes.append( "\" id=\"potion\" style=\"width: 400px\" multiple size=5>" );

			if ( KoLCharacter.getCurrentHP() < KoLCharacter.getMaximumHP() )
			{
				if ( KoLCharacter.hasSkill( "Cannelloni Cocoon" ) )
				{
					changes.append( "<option value=0>cast Cannelloni Cocoon (hp restore)</option>" );
				}
				else
				{
					changes.append( "<option value=0>use 1 scroll of drastic healing (hp restore)</option>" );
				}
			}

			if ( KoLCharacter.getCurrentMP() < KoLCharacter.getMaximumMP() )
			{
				changes.append( "<option value=0" );

				if ( KoLCharacter.getFullness() == KoLCharacter.getFullnessLimit() )
				{
					changes.append( " disabled" );
				}

				changes.append( ">eat 1 Jumbo Dr. Lucifer (mp restore)</option>" );
			}

			for ( int i = 0; i < listedEffects.size(); ++i )
			{
				StatBooster booster = (StatBooster) listedEffects.get( i );
				BasementDecorator.appendBasementEffect( changes, booster );
			}

			changes.append( "</select></td><td>&nbsp;</td><td valign=top align=left>" );
			changes.append( "<input type=\"button\" value=\"exec\" onClick=\"changeBasementEffects();\">" );
			changes.append( "<br/><br/><font size=-1><nobr id=\"changevalue\">" );
			changes.append( BasementRequest.getBasementTestCurrent() );
			changes.append( "</nobr><br/><nobr id=\"changetarget\">" );
			changes.append( BasementRequest.getBasementTestValue() );
			changes.append( "</nobr></td></tr>" );
		}

		changes.append( "</table>" );
		buffer.insert( buffer.indexOf( "</center><blockquote>" ), changes.toString() );

		String checkString = BasementRequest.getRequirement();
		buffer.insert( buffer.lastIndexOf( "</b>" ) + 4, "<br/>" );
		buffer.insert( buffer.lastIndexOf( "<img" ), "<table><tr><td>" );
		buffer.insert(
			buffer.indexOf( ">", buffer.lastIndexOf( "<img" ) ) + 1,
			"</td><td>&nbsp;&nbsp;&nbsp;&nbsp;</td><td><font id=\"spoiler\" size=2>" + checkString + "</font></td></tr></table>" );
	}

	private static final void addBasementChoiceSpoilers( final StringBuffer buffer, final String choice1,
		final String choice2 )
	{
		String text = buffer.toString();

		// Update level string and such for the session log.
		BasementRequest.checkBasement( false, text );

		buffer.setLength( 0 );

		int index1 = 0, index2;

		// Add first choice spoiler
		index2 = text.indexOf( "</form>", index1 );
		buffer.append( text.substring( index1, index2 ) );
		buffer.append( "<br><font size=-1>(" + choice1 + ")</font><br/></form>" );
		index1 = index2 + 7;

		// Add second choice spoiler
		index2 = text.indexOf( "</form>", index1 );
		buffer.append( text.substring( index1, index2 ) );
		buffer.append( "<br><font size=-1>(" + choice2 + ")</font><br/></form>" );
		index1 = index2 + 7;

		// Append remainder of buffer
		buffer.append( text.substring( index1 ) );
	}

	private static final void appendBasementEffect( final StringBuffer changes, final StatBooster effect )
	{
		changes.append( "<option value=" );
		changes.append( effect.getEffectiveBoost() );

		if ( effect.disabled() )
		{
			changes.append( " disabled" );
		}

		changes.append( ">" );

		if ( !effect.itemAvailable() )
		{
			changes.append( "acquire and " );
		}

		changes.append( effect.getAction() );
		changes.append( " (" );

		String effectName = effect.getName();

		if ( effect.getComputedBoost() == 0.0f )
		{
			if ( effectName.equals( EffectPool.ASTRAL_SHELL ) )
			{
				changes.append( "damage absorption/element resist" );
			}
			else if ( effect.isStatEqualizer() )
			{
				changes.append( "stat equalizer" );
			}
			else if ( effect.isDamageAbsorption() )
			{
				changes.append( "damage absorption" );
			}
			else if ( effect.isElementalImmunity() )
			{
				changes.append( "element immunity" );
			}
			else
			{
				changes.append( "element resist" );
			}
		}
		else
		{
			changes.append( "+" );
			changes.append( KoLConstants.COMMA_FORMAT.format( effect.getEffectiveBoost() ) );
		}

		changes.append( ")</option>" );
	}

	public static class StatBooster
		implements Comparable
	{
		private final String name, action;
		private final int computedBoost;
		private final int effectiveBoost;
		private AdventureResult item;
		private boolean itemAvailable;
		private int spleen;
		private int inebriety;
		private boolean isDamageAbsorption;
		private boolean isElementalImmunity;
		private boolean isStatEqualizer;

		private static boolean rigatoni = false;
		private static boolean hardigness = false;
		private static boolean wisdom = false;
		private static boolean ugnderstanding = false;
		private static boolean moxieControlsMP = false;

		private static final AdventureResult MOXIE_MAGNET = new AdventureResult( 519, 1 );
		private static final AdventureResult TRAVOLTAN_TROUSERS = new AdventureResult( 1792, 1 );

		public StatBooster( final String name )
		{
			this.name = name;

			this.computedBoost = (int) Math.ceil( this.computeBoost() );
			this.effectiveBoost = this.computedBoost > 0.0f ? this.computedBoost : 0 - this.computedBoost;

			this.action =
				this.computedBoost < 0 ? "uneffect " + name : MoodManager.getDefaultAction( "lose_effect", name );

			this.item = null;
			this.itemAvailable = true;
			this.spleen = 0;
			this.inebriety = 0;
			this.isDamageAbsorption = this.name.equals( EffectPool.ASTRAL_SHELL) || this.name.equals( EffectPool.GHOSTLY_SHELL);
			this.isElementalImmunity = BasementRequest.isElementalImmunity( this.name );
			this.isStatEqualizer = this.name.equals( EffectPool.EXPERT_OILINESS) || this.name.equals( EffectPool.SLIPPERY_OILINESS) || this.name.equals( EffectPool.STABILIZING_OILINESS);

			if ( this.action.startsWith( "use" ) || this.action.startsWith( "chew" ) || this.action.startsWith( "drink" ) )
			{
				int index = this.action.indexOf( " " ) + 1;
				this.item = KoLmafiaCLI.getFirstMatchingItem( this.action.substring( index ).trim(), false );
				if ( this.item != null )
				{
					this.itemAvailable = InventoryManager.hasItem( this.item );
					this.spleen = ItemDatabase.getSpleenHit( item.getName() );
					this.inebriety = ItemDatabase.getInebriety( item.getName() );
				}
			}
		}

		public static final boolean moxieControlsMP()
		{
			// With Moxie Magnet, uses Moxie, not Mysticality
			if ( KoLCharacter.hasEquipped( MOXIE_MAGNET ) )
				return true;

			// Ditto if Travoltan trousers and Mox > Mys
			if ( KoLCharacter.hasEquipped( TRAVOLTAN_TROUSERS ) )
				return KoLCharacter.getAdjustedMoxie() > KoLCharacter.getAdjustedMysticality();

			return false;
		}

		public final boolean isDamageAbsorption()
		{
			return this.isDamageAbsorption;
		}

		public final boolean isElementalImmunity()
		{
			return this.isElementalImmunity;
		}

		public final boolean isStatEqualizer()
		{
			return this.isStatEqualizer;
		}

		public static void checkSkills()
		{
			StatBooster.rigatoni = KoLCharacter.hasSkill( "Spirit of Rigatoni" );
			StatBooster.hardigness = KoLCharacter.hasSkill( "Gnomish Hardigness" );
			StatBooster.wisdom = KoLCharacter.hasSkill( "Wisdom of the Elder Tortoises" );
			StatBooster.ugnderstanding = KoLCharacter.hasSkill( "Cosmic Ugnderstanding" );
			StatBooster.moxieControlsMP = moxieControlsMP();
		}

		public boolean equals( final Object o )
		{
			return o instanceof StatBooster && this.name.equals( ( (StatBooster) o ).name );
		}

		public int compareTo( final Object o )
		{
			if ( this.effectiveBoost == 0.0f )
			{
				if ( ( (StatBooster) o ).effectiveBoost != 0.0f )
				{
					return -1;
				}
				if ( this.isElementalImmunity )
				{
					return -1;
				}
				if ( ( (StatBooster) o ).isElementalImmunity )
				{
					return 1;
				}
				return this.name.compareToIgnoreCase( ( (StatBooster) o ).name );
			}

			if ( ( (StatBooster) o ).effectiveBoost == 0.0f )
			{
				return 1;
			}

			if ( this.effectiveBoost != ( (StatBooster) o ).effectiveBoost )
			{
				return this.effectiveBoost > ( (StatBooster) o ).effectiveBoost ? -1 : 1;
			}

			return this.name.compareToIgnoreCase( ( (StatBooster) o ).name );
		}

		public String getName()
		{
			return name;
		}

		public AdventureResult getItem()
		{
			return item;
		}

		public boolean itemAvailable()
		{
			return itemAvailable;
		}

		public int getSpleen()
		{
			return spleen;
		}

		public int getInebriety()
		{
			return inebriety;
		}

		public boolean disabled()
		{
			if ( item == null )
			{
				return false;
			}

			if ( this.spleen > 0 && ( KoLCharacter.getSpleenUse() + this.spleen ) > KoLCharacter.getSpleenLimit() )
			{
				return true;
			}

			if ( this.inebriety > 0 && ( KoLCharacter.getInebriety() + this.inebriety ) > KoLCharacter.getInebrietyLimit() )
			{
				return true;
			}

			return false;
		}

		public int getComputedBoost()
		{
			return computedBoost;
		}

		public int getEffectiveBoost()
		{
			return effectiveBoost;
		}

		public String getAction()
		{
			return action;
		}

		public float computeBoost()
		{
			Modifiers m = Modifiers.getModifiers( this.name );
			if ( m == null )
			{
				return 0.0f;
			}

			if ( BasementRequest.getActualStatNeeded() == Modifiers.HP )
			{
				return StatBooster.boostMaxHP( m );
			}

			if ( BasementRequest.getActualStatNeeded() == Modifiers.MP )
			{
				return StatBooster.boostMaxMP( m );
			}

			float base = StatBooster.getEqualizedStat( BasementRequest.getPrimaryBoost() );
			float boost =
				m.get( BasementRequest.getSecondaryBoost() ) + m.get( BasementRequest.getPrimaryBoost() ) * base / 100.0f;

			return boost;
		}

		public static float getEqualizedStat( final int mod )
		{
			float currentStat = 0.0f;

			switch ( mod )
			{
			case Modifiers.MUS_PCT:
				currentStat = KoLCharacter.getBaseMuscle();
				break;
			case Modifiers.MYS_PCT:
				currentStat = KoLCharacter.getBaseMysticality();
				break;
			case Modifiers.MOX_PCT:
				currentStat = KoLCharacter.getBaseMoxie();
				break;
			default:
				return 0.0f;
			}

			if ( KoLConstants.activeEffects.contains( BasementRequest.MUS_EQUAL ) )
			{
				currentStat = Math.max( KoLCharacter.getBaseMuscle(), currentStat );
			}

			if ( KoLConstants.activeEffects.contains( BasementRequest.MYS_EQUAL ) )
			{
				currentStat = Math.max( KoLCharacter.getBaseMysticality(), currentStat );
			}

			if ( KoLConstants.activeEffects.contains( BasementRequest.MOX_EQUAL ) )
			{
				currentStat = Math.max( KoLCharacter.getBaseMoxie(), currentStat );
			}

			return currentStat;
		}

		public static float boostMaxHP( final Modifiers m )
		{
			float addedMuscleFixed = m.get( Modifiers.MUS );
			float addedMusclePercent = m.get( Modifiers.MUS_PCT );
			float addedHealthFixed = m.get( Modifiers.HP );

			if ( addedMuscleFixed == 0.0f && addedMusclePercent == 0.0f && addedHealthFixed == 0.0f )
			{
				return 0.0f;
			}

			float muscleBase = StatBooster.getEqualizedStat( Modifiers.MUS_PCT );
			float muscleBonus = addedMuscleFixed + (float) Math.floor( addedMusclePercent * muscleBase / 100.0f );

			if ( KoLCharacter.isMuscleClass() )
			{
				muscleBonus *= 1.5f;
			}

			if ( StatBooster.rigatoni )
			{
				muscleBonus *= 1.25f;
			}

			if ( StatBooster.hardigness )
			{
				muscleBonus *= 1.05f;
			}

			return addedHealthFixed + muscleBonus;
		}

		public static float boostMaxMP( final Modifiers m )
		{
			int statModifier;
			int statPercentModifier;

			if ( StatBooster.moxieControlsMP )
			{
				statModifier = Modifiers.MOX;
				statPercentModifier = Modifiers.MOX_PCT;
			}
			else
			{
				statModifier = Modifiers.MYS;
				statPercentModifier = Modifiers.MYS_PCT;
			}

			float addedStatFixed = m.get( statModifier );
			float addedStatPercent = m.get( statPercentModifier );
			float addedManaFixed = m.get( Modifiers.MP );

			if ( addedStatFixed == 0.0f && addedStatPercent == 0.0f && addedManaFixed == 0.0f )
			{
				return 0.0f;
			}

			float statBase = StatBooster.getEqualizedStat( statPercentModifier );
			float manaBonus = addedStatFixed + (float) Math.floor( addedStatPercent * statBase / 100.0f );

			if ( KoLCharacter.isMysticalityClass() )
			{
				manaBonus *= 1.5f;
			}

			if ( StatBooster.wisdom )
			{
				manaBonus *= 1.5f;
			}

			if ( StatBooster.ugnderstanding )
			{
				manaBonus *= 1.05f;
			}

			return addedManaFixed + manaBonus;
		}
	}
}
