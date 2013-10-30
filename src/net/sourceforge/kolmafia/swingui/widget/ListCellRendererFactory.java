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

package net.sourceforge.kolmafia.swingui.widget;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.UIManager;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.Modifiers;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase.QueuedConcoction;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CafeRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.swingui.GearChangeFrame;

public class ListCellRendererFactory
{
	public static final DefaultListCellRenderer getDefaultRenderer()
	{
		return new DefaultRenderer();
	}

	private static class DefaultRenderer
		extends DefaultListCellRenderer
	{
		public DefaultRenderer()
		{
			this.setOpaque( true );
		}

		public boolean allowHighlight()
		{
			return false;
		}

		@Override
		public final Component getListCellRendererComponent( final JList list, final Object value, final int index,
			final boolean isSelected, final boolean cellHasFocus )
		{
			Component defaultComponent =
				super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );

			if ( value == null )
			{
				return defaultComponent;
			}
			
			if ( isSelected )
			{
				GearChangeFrame.showModifiers( value, false );
			}

			if ( value instanceof AdventureResult )
			{
				return this.getRenderer( defaultComponent, (AdventureResult) value, isSelected );
			}
			
			if ( value instanceof PurchaseRequest )
			{
				return this.getRenderer( defaultComponent, (PurchaseRequest) value, isSelected );
			}

			if ( value instanceof CreateItemRequest )
			{
				return this.getRenderer( defaultComponent, (CreateItemRequest) value, isSelected );
			}

			if ( value instanceof Concoction )
			{
				return this.getRenderer( defaultComponent, (Concoction) value,
					list.getWidth(), isSelected );
			}

			if ( value instanceof QueuedConcoction )
			{
				return this.getRenderer( defaultComponent, (QueuedConcoction) value,
					list.getWidth(), isSelected );
			}

			return defaultComponent;
		}

		private Component getRenderer( Component defaultComponent, PurchaseRequest value, boolean isSelected )
		{
			StringBuilder buffer = new StringBuilder();
			String color = null;
			if ( isSelected )
			{
				setForeground( UIManager.getColor( "textHighlightText" ) );
			}
			else
			{
				color = value.color();
			}

			buffer.append( "<html><nobr>" );
			if ( color != null )
			{
				buffer.append( "<font color=\"" );
				buffer.append( color );
				buffer.append( "\">" );
			}

			buffer.append( value.getItem() );
			buffer.append( " (" );

			if ( value.getQuantity() == PurchaseRequest.MAX_QUANTITY )
			{
				buffer.append( "unlimited" );
			}
			else if ( value.getQuantity() < 0 )
			{
				buffer.append( "unknown" );
			}
			else
			{
				buffer.append( KoLConstants.COMMA_FORMAT.format( value.getQuantity() ) );

				if ( value.getLimit() < value.getQuantity() )
				{
					buffer.append( " limit " );
					buffer.append( KoLConstants.COMMA_FORMAT.format( value.getLimit() ) );
				}
			}

			buffer.append( " @ " );
			buffer.append( value.getPriceString() );
			buffer.append( "): " );
			buffer.append( value.getShopName() );

			if ( color != null )
			{
				buffer.append( "</font>" );
			}

			buffer.append( "</nobr></html>" );

			( (JLabel) defaultComponent ).setText( buffer.toString() );
			return defaultComponent;
		}

		public Component getRenderer( final Component defaultComponent, final AdventureResult ar, final boolean isSelected )
		{
			if ( !ar.isItem() )
			{
				return defaultComponent;
			}

			StringBuffer stringForm = new StringBuffer();

			stringForm.append( "<html><nobr>" );

			String name = ar.getName();
			String color = null;

			if ( isSelected )
			{
				setForeground( UIManager.getColor( "textHighlightText" ) );
			}
			else 
			{
				color = ColorFactory.getItemColor ( ar );
			}

			if ( color != null )
			{
				stringForm.append( "<font color=" );
				stringForm.append( color );
				stringForm.append( ">" );
			}

			stringForm.append( name );

			if ( color != null )
			{
				stringForm.append( "</font>" );
			}

			int itemId = ar.getItemId();

			if ( itemId > 0 )
			{
				int value = ItemDatabase.getPriceById( itemId );

				if ( value <= 0 )
				{
					stringForm.append( " (no-sell)" );
				}
				else
				{
					stringForm.append( " (" );
					stringForm.append( value );
					stringForm.append( " meat)" );
				}
			}

			stringForm.append( " (" );
			stringForm.append( KoLConstants.COMMA_FORMAT.format( ar.getCount() ) );
			stringForm.append( ")" );

			stringForm.append( "</nobr></html>" );

			( (JLabel) defaultComponent ).setText( stringForm.toString() );
			return defaultComponent;
		}

		public Component getRenderer( final Component defaultComponent, final CreateItemRequest icr, final boolean isSelected )
		{
			StringBuffer stringForm = new StringBuffer();
			boolean isHTML = false;

			String name = icr.getName();
			String color = null;

			if ( isSelected )
			{
				setForeground( UIManager.getColor( "textHighlightText" ) );
			}
			else 
			{
				color = ColorFactory.getCreationColor( icr );
			}

			if ( color != null )
			{
				stringForm.append( "<font color=" );
				stringForm.append( color );
				stringForm.append( ">" );
				isHTML = true;
			}

			stringForm.append( name );

			if ( color != null )
			{
				stringForm.append( "</font>" );
			}

			int itemId = icr.getItemId();

			if ( itemId > 0 )
			{
				int value = ItemDatabase.getPriceById( itemId );

				if ( value <= 0 )
				{
					stringForm.append( " (no-sell)" );
				}
				else
				{
					stringForm.append( " (" );
					stringForm.append( value );
					stringForm.append( " meat)" );
				}
			}
			
			int costForOne = icr.concoction.getAdventuresNeeded( 1, true );
			int maxPossible = icr.getQuantityPossible();
			int costForMax = icr.concoction.getAdventuresNeeded( maxPossible, true );
			boolean futureCreationMoreExpensive = costForMax  > costForOne * maxPossible;

			if ( costForOne > 0 )
			{
				stringForm.append( " (" );
				stringForm.append( costForOne );
				if ( futureCreationMoreExpensive )
				{
					stringForm.append( "+" );
				}
				stringForm.append( " adv)" );
			}

			stringForm.append( " (" );
			stringForm.append( KoLConstants.COMMA_FORMAT.format( icr.getQuantityPossible() ) );
			if ( icr.getQuantityPullable() > 0 )
			{
				stringForm.append( ", " );
				stringForm.append( KoLConstants.COMMA_FORMAT.format(
					icr.getQuantityPullable() ) );
				stringForm.append( " pullable" );
			}
			stringForm.append( ")" );

			if ( icr.getQuantityPossible() == 0 && icr.getQuantityPullable() > 0 )
			{
				stringForm.insert( 0, "<i>" );
				stringForm.append( "</i>" );
				isHTML = true;
			}

			if ( isHTML )
			{
				stringForm.insert( 0, "<html>" );
				stringForm.append( "</html>" );
			}

			( (JLabel) defaultComponent ).setText( stringForm.toString() );
			return defaultComponent;
		}

		public Component getRenderer( final Component defaultComponent, final QueuedConcoction item,
					      final int listWidth, final boolean isSelected )
		{
			return this.getRenderer( defaultComponent, item.getConcoction(), listWidth, isSelected, item.getCount() );
		}

		public Component getRenderer( final Component defaultComponent, final Concoction item,
					      final int listWidth, final boolean isSelected )
		{
			return this.getRenderer( defaultComponent, item, listWidth, isSelected, -1 );
		}

		private Component getRenderer( final Component defaultComponent, final Concoction item,
					       final int listWidth, final boolean isSelected, int count )
		{
			StringBuffer stringForm = new StringBuffer();
			String name = item.getName();
			boolean meetsRequirement = ItemDatabase.meetsLevelRequirement( name );

			stringForm.append( "<html><nobr>" );

			String color = null;

			if ( isSelected )
			{
				setForeground( UIManager.getColor( "textHighlightText" ) );
			}
			else
			{
				color = ColorFactory.getConcoctionColor( item );
			}

			if ( color != null )
			{
				stringForm.append( "<font color=" );
				stringForm.append( color );
				stringForm.append( ">" );
			}

			stringForm.append( "<b>" );
			stringForm.append( item.toString() );

			boolean pulling;

			stringForm.append( " (" );
			if ( count > 0 )
			{
				stringForm.append( count );
				pulling = item.queuedPulls != 0;
			}
			else
			{
				pulling = this.appendAmount( stringForm, item );
			}
			stringForm.append( ")" );
			stringForm.append( "</b></nobr><br><nobr>&nbsp;" );
			
			switch ( item.getItemId() )
			{
			case ItemPool.DISTENTION_PILL:
				stringForm.append( "increase fullness by 1" );
				break;

			case ItemPool.MUNCHIES_PILL:
				stringForm.append( "+1-3 adv from next food eaten" );
				break;

			case ItemPool.SUSHI_DOILY:
				stringForm.append( "+3 adv from next sushi eaten" );
				break;

			case ItemPool.GRAINS_OF_SALT:
				stringForm.append( "+3 adv from next food eaten" );
				break;

			case ItemPool.SCRATCHS_FORK:
				stringForm.append( "+30% adv/mus/mys/mox from next food, +50% from salad" );
				break;

			case ItemPool.DIVINE_FLUTE:
				stringForm.append( "+(7*adv)+(0-15) MP from next drink" );
				break;

			case ItemPool.FROSTYS_MUG:
				stringForm.append( "+30% adv/mus/mys/mox from next drink, +50% from beer" );
				break;

			case ItemPool.CRIMBCO_MUG:
				stringForm.append( "does something to next drink" );
				break;

			case ItemPool.BGE_SHOTGLASS:
				stringForm.append( "+3 adv from next drink" );
				break;

			case ItemPool.FUDGE_SPORK:
				stringForm.append( "+3 adv from next food, 10 sugar rush" );
				break;

			default:
				Integer fullness = ItemDatabase.getRawFullness( name );
				Integer inebriety = ItemDatabase.getRawInebriety( name );
				Integer spleenhit = ItemDatabase.getRawSpleenHit( name );
	
				if ( fullness != null )
				{
					stringForm.append( fullness );
					stringForm.append( " full" );
				}
				if ( inebriety != null )
				{
					stringForm.append( inebriety );
					stringForm.append( " drunk" );
				}
				if ( spleenhit != null )
				{
					stringForm.append( spleenhit );
					stringForm.append( " spleen" );
				}
	
				this.appendRange( stringForm, ItemDatabase.getAdventureRange( name ), "adv" );
	
				if ( Preferences.getBoolean( "showGainsPerUnit" ) )
				{
					if ( fullness != null && fullness.intValue() > 0 )
					{
						stringForm.append( " / full" );
					}
					else if ( inebriety != null && inebriety.intValue() > 0 )
					{
						stringForm.append( " / drunk" );
					}
					else if ( spleenhit != null && spleenhit.intValue() > 0 )
					{
						stringForm.append( " / spleen" );
					}
				}
	
				this.appendRange( stringForm, ItemDatabase.getMuscleRange( name ), "mus" );
				this.appendRange( stringForm, ItemDatabase.getMysticalityRange( name ), "mys" );
				this.appendRange( stringForm, ItemDatabase.getMoxieRange( name ), "mox" );
				String notes = ItemDatabase.getNotes( name );
				if ( notes != null )
				{
					stringForm.append( ", " );
					stringForm.append( notes );
				}
			}

			if ( color != null )
			{
				stringForm.append( "</font>" );
			}
			
			if ( pulling )
			{
				stringForm.insert( 6, "<i>" );
				stringForm.append( "</i>" );
			}

			stringForm.append( "</nobr></html>" );

			defaultComponent.setFont( KoLConstants.DEFAULT_FONT );
			String text = stringForm.toString();
			( (JLabel) defaultComponent ).setText( text );
			if ( defaultComponent.getPreferredSize().width > listWidth )
			{
				((JComponent) defaultComponent).setToolTipText( text );
			}
			else
			{
				((JComponent) defaultComponent).setToolTipText( null );
			}
			return defaultComponent;
		}

		public boolean appendAmount( final StringBuffer stringForm, final Concoction item )
		{
			if ( item.getItem() != null )
			{
				boolean turnFreeOnly = Preferences.getBoolean( "showTurnFreeOnly" );
				int modified = ( turnFreeOnly ? item.getTurnFreeAvailable() : item.getAvailable() );
				int initial = item.getItem().getCount( KoLConstants.inventory );
				
				// hack for clip art summons: if "no-summon" is checked, only show on-hand items
				if ( item != null && item.getMixingMethod() == CraftingType.CLIPART && Preferences.getBoolean( "showNoSummonOnly" ) )
				{
					modified = initial;
				}

				if ( item.price > 0 )
				{
					stringForm.append( item.price );
					stringForm.append( " meat, " );
					stringForm.append( initial );
					stringForm.append( " current" );
					return false;
				}

				PurchaseRequest purchaseRequest = item.getPurchaseRequest();
				if ( purchaseRequest != null )
				{
					AdventureResult cost = purchaseRequest.getCost();
					int count = cost.getCount();
					String currency = purchaseRequest.getCurrency( count );
					stringForm.append( count );
					stringForm.append( " " );
					stringForm.append( currency );
					stringForm.append( ", " );
					stringForm.append( modified );
					stringForm.append( " possible, " );
					stringForm.append( initial );
					stringForm.append( " current" );
					return false;
				}

				stringForm.append( modified );
				stringForm.append( " possible, " );
				stringForm.append( initial );
				stringForm.append( " current" );
				int costForOne = item.getAdventuresNeeded( 1, true );
				int maxPossible = modified;
				int costForMax = item.getAdventuresNeeded( maxPossible, true );
				boolean futureCreationMoreExpensive = costForMax > costForOne * maxPossible;

				if ( costForOne > 0 )
				{
					stringForm.append( ", " );
					stringForm.append( costForOne );
					if ( futureCreationMoreExpensive )
					{
						stringForm.append( "+" );
					}
					stringForm.append( " adv" );
				}
			}
			else if ( item.getPrice() > 0 )
			{
				stringForm.append( CafeRequest.discountedPrice( item.getPrice() ) );
				stringForm.append( " meat" );
				return false;
			}
			else if ( item.special )
			{
				stringForm.append( item.initial );
				return false;
			}
			return item.initial + item.creatable <= 0;
		}

		protected void appendRange( final StringBuffer stringForm, final double range, final String suffix )
		{
			String val = ( range == 0.0f ) ? "+0.00" : KoLConstants.ROUNDED_MODIFIER_FORMAT.format( range );
			this.appendRange( stringForm, val, suffix );
		}

		protected void appendRange( final StringBuffer stringForm, final String range, final String suffix )
		{
			if ( range.equals( "+0.0" ) && !suffix.equals( "adv" ) )
			{
				return;
			}

			stringForm.append( ", " );
			stringForm.append( range );
			stringForm.append( " " );
			stringForm.append( suffix );
		}
	}

	public static final DefaultListCellRenderer getEquipmentPowerRenderer()
	{
		return new EquipmentPowerRenderer();
	}

	private static class EquipmentPowerRenderer
		extends DefaultRenderer
	{
		public EquipmentPowerRenderer()
		{
			this.setOpaque( true );
		}

		@Override
		public boolean allowHighlight()
		{
			return true;
		}

		@Override
		public Component getRenderer( final Component defaultComponent, final AdventureResult ar, final boolean isSelected )
		{
			if ( !ar.isItem() )
			{
				return defaultComponent;
			}

			StringBuffer stringForm = new StringBuffer();

			stringForm.append( "<html><nobr>" );

			String color = null;

			if ( isSelected )
			{
				setForeground( UIManager.getColor( "textHighlightText" ) );
			}
			else
			{
				color = ColorFactory.getItemColor( ar );
			}

			if ( color != null )
			{
				stringForm.append( "<font color=" );
				stringForm.append( color );
				stringForm.append( ">" );
			}

			stringForm.append( ar.getName() );

			if ( color != null )
			{
				stringForm.append( "</font>" );
			}

			int power = EquipmentDatabase.getPower( ar.getName() );

			if ( power > 0 )
			{
				stringForm.append( " (+" );
				stringForm.append( power );
				stringForm.append( ")" );
			}

			stringForm.append( " (" );
			stringForm.append( KoLConstants.COMMA_FORMAT.format( ar.getCount() ) );
			stringForm.append( ")" );

			stringForm.append( "</nobr></html>" );

			( (JLabel) defaultComponent ).setText( stringForm.toString() );
			return defaultComponent;
		}

		@Override
		public Component getRenderer( final Component defaultComponent, final CreateItemRequest icr, final boolean isSelected )
		{
			StringBuffer stringForm = new StringBuffer();
			boolean isHTML = false;

			stringForm.append( icr.getName() );

			int power = EquipmentDatabase.getPower( icr.getName() );

			if ( power > 0 )
			{
				stringForm.append( " (+" );
				stringForm.append( power );
				stringForm.append( ")" );
			}

			stringForm.append( " (" );
			stringForm.append( KoLConstants.COMMA_FORMAT.format( icr.getQuantityPossible() ) );
			if ( icr.getQuantityPullable() > 0 )
			{
				stringForm.append( ", " );
				stringForm.append( KoLConstants.COMMA_FORMAT.format(
					icr.getQuantityPullable() ) );
				stringForm.append( " pullable" );
			}
			stringForm.append( ")" );
			
			if ( icr.getQuantityPossible() == 0 && icr.getQuantityPullable() > 0 )
			{
				stringForm.insert( 0, "<i>" );
				stringForm.append( "</i>" );
				isHTML = true;
			}
			
			String color = ColorFactory.getCreationColor( icr, true );

			if ( color != null )
			{
				stringForm.insert( 0, "<font color=" + color + ">" );
				stringForm.append( "</font>" );
				isHTML = true;
			}

			if ( isHTML )
			{
				stringForm.insert( 0, "<html>" );
				stringForm.append( "</html>" );
			}

			( (JLabel) defaultComponent ).setText( stringForm.toString() );
			return defaultComponent;
		}
	}

	public static final DefaultListCellRenderer getCreationQueueRenderer()
	{
		return new CreationQueueRenderer();
	}

	private static class CreationQueueRenderer
		extends DefaultRenderer
	{
		public CreationQueueRenderer()
		{
			this.setOpaque( true );
		}

		@Override
		public boolean allowHighlight()
		{
			return false;
		}
	}

	public static final DefaultListCellRenderer getNameOnlyRenderer()
	{
		return new NameOnlyRenderer();
	}

	private static class NameOnlyRenderer
		extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent( final JList list, final Object value, final int index,
			final boolean isSelected, final boolean cellHasFocus )
		{
			return super.getListCellRendererComponent(
				list, value instanceof AdventureResult ? ( (AdventureResult) value ).getName() : value, index,
				isSelected, cellHasFocus );
		}
	}

	public static final DefaultListCellRenderer getUsableEquipmentRenderer()
	{
		return new UsableEquipmentRenderer();
	}

	private static class UsableEquipmentRenderer
		extends DefaultListCellRenderer
	{
		public UsableEquipmentRenderer()
		{
			this.setOpaque( true );
		}

		@Override
		public Component getListCellRendererComponent( final JList list, final Object value, final int index,
			final boolean isSelected, final boolean cellHasFocus )
		{
			if ( value == null || !( value instanceof AdventureResult ) )
			{
				return super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
			}

			if ( isSelected )
			{
				GearChangeFrame.showModifiers( value, false );
			}

			AdventureResult ar = (AdventureResult) value;
			int equipmentType = ItemDatabase.getConsumptionType( ar.getName() );

			int power = EquipmentDatabase.getPower( ar.getName() );
			String stringForm = null;

			if ( equipmentType == KoLConstants.EQUIP_FAMILIAR || ar.equals( EquipmentRequest.UNEQUIP ) )
			{
				if ( ar.equals( EquipmentRequest.UNEQUIP ) )
				{
					stringForm = ar.getName();
				}
				else if ( KoLCharacter.getFamiliar() != null && KoLCharacter.getFamiliar().canEquip( ar ) )
				{
					stringForm = ar.getName();
				}
				else
				{
					stringForm = "<html><font color=gray>" + ar.getName() + "</font></html>";
				}
			}
			else
			{
				if ( equipmentType == KoLConstants.EQUIP_ACCESSORY )
				{
					int count = ar.getCount( KoLConstants.inventory );
					if ( ar.equals( EquipmentManager.getEquipment( EquipmentManager.ACCESSORY1 ) ) )
					{
						++count;
					}
					if ( ar.equals( EquipmentManager.getEquipment( EquipmentManager.ACCESSORY2 ) ) )
					{
						++count;
					}
					if ( ar.equals( EquipmentManager.getEquipment( EquipmentManager.ACCESSORY3 ) ) )
					{
						++count;
					}
					stringForm = ar.getName() + " (" + count + " max)";
				}
				else if ( power > 0 )
				{
					stringForm = ar.getName() + " (+" + KoLConstants.COMMA_FORMAT.format( power ) + ")";
				}
				else
				{
					stringForm = ar.getName();
				}

				// Gray out any equipment that the player cannot currently equip
				// inside of an equipment filter.

				if ( !EquipmentManager.canEquip( ar.getName() ) )
				{
					stringForm = "<html><font color=gray>" + stringForm + "</font></html>";
				}
			}

			JLabel defaultComponent =
				(JLabel) super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
			defaultComponent.setText( stringForm );
			return defaultComponent;
		}
	}

	public static final DefaultListCellRenderer getFamiliarEquipmentRenderer()
	{
		return new FamiliarEquipmentRenderer();
	}

	private static class FamiliarEquipmentRenderer
		extends UsableEquipmentRenderer
	{
		@Override
		public Component getListCellRendererComponent( final JList list, final Object value, final int index,
			final boolean isSelected, final boolean cellHasFocus )
		{
			if ( value == null || !( value instanceof AdventureResult ) )
			{
				return super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
			};

			if ( isSelected )
			{
				GearChangeFrame.showModifiers( value, true );
			}

			AdventureResult ar = (AdventureResult) value;
			String effect = Modifiers.getFamiliarEffect( ar.getName() );
			if ( effect == null )
			{
				return super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
			};

			String stringForm = ar.getName() + " (" + effect + ")";
			if ( KoLCharacter.getFamiliar() == null || 
				!KoLCharacter.getFamiliar().canEquip( ar ) )
			{
				stringForm = "<html><font color=gray>" + stringForm + "</font></html>";
			}

			JLabel defaultComponent =
				(JLabel) super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
			defaultComponent.setText( stringForm );
			return defaultComponent;
		}
	}

	public static final DefaultListCellRenderer getFamiliarRenderer()
	{
		return new FamiliarRenderer();
	}

	private static class FamiliarRenderer
		extends DefaultListCellRenderer
	{
		public FamiliarRenderer()
		{
			this.setOpaque( true );
		}

		@Override
		public Component getListCellRendererComponent( final JList list, final Object value, final int index,
			final boolean isSelected, final boolean cellHasFocus )
		{
			if ( value == null || !( value instanceof FamiliarData ) )
			{
				return super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
			}

			if ( isSelected )
			{
				GearChangeFrame.showModifiers( value, false );
			}

			return super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
		}
	}

	public static final DefaultListCellRenderer getStorageRenderer()
	{
		return new StorageRenderer();
	}

	private static class StorageRenderer
		extends DefaultRenderer
	{
		public StorageRenderer()
		{
			this.setOpaque( true );
		}

		@Override
		public boolean allowHighlight()
		{
			return true;
		}

		@Override
		public Component getRenderer( final Component defaultComponent, final AdventureResult ar, final boolean isSelected )
		{
			if ( !ar.isItem() )
			{
				return defaultComponent;
			}

			StringBuffer stringForm = new StringBuffer();

			stringForm.append( "<html><nobr>" );

			String name = ar.getName();
			String color = null;


			if ( isSelected )
			{
				setForeground( UIManager.getColor( "textHighlightText" ) );
			}
			else
			{
				color = ColorFactory.getStorageColor( ar );
			}

			if ( color != null )
			{
				stringForm.append( "<font color=" );
				stringForm.append( color );
				stringForm.append( ">" );
			}

			stringForm.append( name );

			if ( color != null )
			{
				stringForm.append( "</font>" );
			}

			int power = EquipmentDatabase.getPower( ar.getName() );

			if ( power > 0 )
			{
				stringForm.append( " (+" );
				stringForm.append( power );
				stringForm.append( ")" );
			}
			else
			{
				Integer fullness = ItemDatabase.getRawFullness( ar.getName() );
				Integer inebriety = ItemDatabase.getRawInebriety( ar.getName() );
				
				if ( fullness != null || inebriety != null )
				{
					stringForm.append( " (" );
					if ( fullness != null )
					{
						stringForm.append( fullness );
					}
					else 	//if ( inebriety != null )
					{
						stringForm.append( inebriety );
					}
	
					this.appendRange( stringForm,
						ItemDatabase.getAdventureRange( ar.getName() ), "adv" );
	
					if ( Preferences.getBoolean( "showGainsPerUnit" ) )
					{
						if ( fullness != null && fullness.intValue() > 0 )
						{
							stringForm.append( "/full" );
						}
						else if ( inebriety != null && inebriety.intValue() > 0 )
						{
							stringForm.append( "/drunk" );
						}
					}
					stringForm.append( ")" );
				}
			}

			stringForm.append( " (" );
			stringForm.append( KoLConstants.COMMA_FORMAT.format( ar.getCount() ) );
			stringForm.append( ")" );

			stringForm.append( "</nobr></html>" );

			( (JLabel) defaultComponent ).setText( stringForm.toString() );
			return defaultComponent;
		}
	}

	public static final DefaultListCellRenderer getFreePullsRenderer()
	{
		return new FreePullsRenderer();
	}

	private static class FreePullsRenderer
		extends DefaultRenderer
	{
		public FreePullsRenderer()
		{
			this.setOpaque( true );
		}

		@Override
		public boolean allowHighlight()
		{
			return true;
		}

		@Override
		public Component getRenderer( final Component defaultComponent, final AdventureResult ar, final boolean isSelected )
		{
			if ( !ar.isItem() )
			{
				return defaultComponent;
			}

			StringBuffer stringForm = new StringBuffer();
			stringForm.append( ar.getName() );
			stringForm.append( " (" );
			stringForm.append( KoLConstants.COMMA_FORMAT.format( ar.getCount() ) );
			stringForm.append( ")" );

			( (JLabel) defaultComponent ).setText( stringForm.toString() );
			return defaultComponent;
		}
	}

	public static final DefaultListCellRenderer getPulverizationRenderer()
	{
		return new PulverizationRenderer();
	}

	private static class PulverizationRenderer
		extends DefaultRenderer
	{
		public PulverizationRenderer()
		{
			this.setOpaque( true );
		}

		@Override
		public boolean allowHighlight()
		{
			return true;
		}

		@Override
		public Component getRenderer( final Component defaultComponent, final AdventureResult ar, final boolean isSelected )
		{
			if ( !ar.isItem() )
			{
				return defaultComponent;
			}

			StringBuffer stringForm = new StringBuffer();
			stringForm.append( ar.getName() );

			int pulver = EquipmentDatabase.getPulverization( ar.getItemId() );
			boolean HTML = false;

			if ( pulver > 0 )
			{
				stringForm.append( " => " );
				stringForm.append( ItemDatabase.getItemName( pulver ) );
			}
			else if ( pulver < -1 )
			{
				stringForm.append( " => " );
				if ( (pulver & EquipmentDatabase.ELEM_TWINKLY) != 0 )
				{
					stringForm.append( "Tw" );
				}
				if ( (pulver & EquipmentDatabase.ELEM_HOT) != 0 )
				{
					stringForm.append( "<font color=red>Ho</font>" );
					HTML = true;
				}
				if ( (pulver & EquipmentDatabase.ELEM_COLD) != 0 )
				{
					stringForm.append( "<font color=blue>Co</font>" );
					HTML = true;
				}
				if ( (pulver & EquipmentDatabase.ELEM_STENCH) != 0 )
				{
					stringForm.append( "<font color=green>St</font>" );
					HTML = true;
				}
				if ( (pulver & EquipmentDatabase.ELEM_SPOOKY) != 0 )
				{
					stringForm.append( "<font color=gray>Sp</font>" );
					HTML = true;
				}
				if ( (pulver & EquipmentDatabase.ELEM_SLEAZE) != 0 )
				{
					stringForm.append( "<font color=purple>Sl</font>" );
					HTML = true;
				}

				if ( (pulver & EquipmentDatabase.YIELD_1C) != 0 )
				{
					stringForm.append( "C" );
				}
				
				if ( (pulver & EquipmentDatabase.MALUS_UPGRADE ) != 0 )
				{
					stringForm.append( " upgrade" );
				}
				else if ( (pulver & EquipmentDatabase.YIELD_3W) != 0 )
				{
					stringForm.append( " 3W" );
				}
				else if ( (pulver & EquipmentDatabase.YIELD_1W3N_2W) != 0 )
				{
					stringForm.append( "  1W+3N or 2W" );
				}
				else if ( (pulver & EquipmentDatabase.YIELD_4N_1W) != 0 )
				{
					stringForm.append( " 4N or 1W" );
				}
				else if ( (pulver & EquipmentDatabase.YIELD_3N) != 0 )
				{
					stringForm.append( " 3N" );
				}
				else if ( (pulver & EquipmentDatabase.YIELD_1N3P_2N) != 0 )
				{
					stringForm.append( " 1N+3P or 2N" );
				}
				else if ( (pulver & EquipmentDatabase.YIELD_4P_1N) != 0 )
				{
					stringForm.append( " 4P or 1N" );
				}
				else if ( (pulver & EquipmentDatabase.YIELD_3P) != 0 )
				{
					stringForm.append( " 3P" );
				}
				else if ( (pulver & EquipmentDatabase.YIELD_2P) != 0 )
				{
					stringForm.append( " 2P" );
				}
				else if ( (pulver & EquipmentDatabase.YIELD_1P) != 0 )
				{
					stringForm.append( " 1P" );
				}

				if ( (pulver & EquipmentDatabase.YIELD_UNCERTAIN) != 0 )
				{
					stringForm.append( "?" );
				}
			}
			else
			{	// this should have been filtered out of the list
				stringForm.append( " [NOT PULVERIZABLE]" );
			}

			stringForm.append( " (" );
			stringForm.append( KoLConstants.COMMA_FORMAT.format( ar.getCount() ) );
			stringForm.append( ")" );
			
			int index = KoLConstants.pulverizeQueue.indexOf( ar );
			if ( index != -1 )
			{
				stringForm.append( ", " );
				stringForm.append(
					((AdventureResult)KoLConstants.pulverizeQueue.get( index )).getCount() );
				stringForm.append( " queued" );
			}

			if ( HTML )
			{
				stringForm.insert( 0, "<html>" );
				stringForm.append( "</html>" );
			}
			( (JLabel) defaultComponent ).setText( stringForm.toString() );
			return defaultComponent;
		}
	}
}
