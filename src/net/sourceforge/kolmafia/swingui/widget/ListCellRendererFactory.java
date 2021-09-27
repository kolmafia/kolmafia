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
import net.sourceforge.kolmafia.KoLGUIConstants;
import net.sourceforge.kolmafia.Modifiers;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase.QueuedConcoction;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CafeRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;

import net.sourceforge.kolmafia.session.EncounterManager.RegisteredEncounter;
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

			if ( value instanceof RegisteredEncounter )
			{
                String buffer = "<html><nobr>" +
                        value.toString() +
                        "</nobr></html>";
                ( (JLabel) defaultComponent ).setText( buffer );
				return defaultComponent;
			}

			return defaultComponent;
		}

		private Component getRenderer( Component defaultComponent, PurchaseRequest value, boolean isSelected )
		{
			StringBuilder buffer = new StringBuilder();
			String color = null;
			if ( isSelected )
			{
				setForeground( UIManager.getColor( "List.selectionForeground" ) );
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
			StringBuilder stringForm = new StringBuilder();

			if ( !ar.isItem() )
			{
				// HTMLify it so that character entities get displayed nicely
				stringForm.append( "<html><nobr>" );
				stringForm.append( ar.toString() );
				stringForm.append( "</nobr></html>" );
				( (JLabel) defaultComponent ).setText( stringForm.toString() );
				return defaultComponent;
			}

			stringForm.append( "<html><nobr>" );

			String name = ar.getName();
			String color = null;

			if ( isSelected )
			{
				setForeground( UIManager.getColor( "List.selectionForeground" ) );
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
			StringBuilder stringForm = new StringBuilder();
			boolean isHTML = false;

			String name = icr.getName();
			String color = null;

			if ( isSelected )
			{
				setForeground( UIManager.getColor( "List.selectionForeground" ) );
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

			stringForm.append( "<html><nobr>" );

			String color = null;

			if ( isSelected )
			{
				setForeground( UIManager.getColor( "List.selectionForeground" ) );
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
			case ItemPool.MUNCHIES_PILL:
				stringForm.append( "+1-3 adv from next food" );
				break;

			case ItemPool.SUSHI_DOILY:
				stringForm.append( "+3 adv from next sushi (automatically used from inventory)" );
				break;

			case ItemPool.GRAINS_OF_SALT:
				stringForm.append( "+3 adv from next food (automatically used from inventory)" );
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

			case ItemPool.JAR_OF_SWAMP_HONEY:
				stringForm.append( "+10-15 mus/mys/mox from next food (automatically used from inventory)" );
				break;

			case ItemPool.MAYONEX:
				stringForm.append( "adv from next food converted to BMC" );
				break;

			case ItemPool.MAYODIOL:
				stringForm.append( "1 full from next food converted to drunk" );
				break;

			case ItemPool.MAYOSTAT:
				stringForm.append( "return some of next food" );
				break;

			case ItemPool.MAYOZAPINE:
				stringForm.append( "x2 stat gain from next food" );
				break;

			case ItemPool.MAYOFLEX:
				stringForm.append( "+1 adv from next food" );
				break;

			default:
				Integer fullness = ConsumablesDatabase.getRawFullness( name );
				Integer inebriety = ConsumablesDatabase.getRawInebriety( name );
				Integer spleenhit = ConsumablesDatabase.getRawSpleenHit( name );
				String spacer = "";
				boolean potion = true;
	
				if ( fullness != null )
				{
					stringForm.append( spacer );
					spacer = " ";
					stringForm.append( fullness );
					stringForm.append( " full" );
					potion = false;
				}
				if ( inebriety != null )
				{
					stringForm.append( spacer );
					spacer = " ";
					stringForm.append( inebriety );
					stringForm.append( " drunk" );
					potion = false;
				}
				if ( spleenhit != null )
				{
					stringForm.append( spacer );
					spacer = " ";
					stringForm.append( spleenhit );
					stringForm.append( " spleen" );
					potion = false;
				}
	
				if ( !potion )
				{
					this.appendRange( stringForm, ConsumablesDatabase.getAdventureRange( name ), "adv" );
	
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
	
					this.appendRange( stringForm, ConsumablesDatabase.getMuscleRange( name ), "mus" );
					this.appendRange( stringForm, ConsumablesDatabase.getMysticalityRange( name ), "mys" );
					this.appendRange( stringForm, ConsumablesDatabase.getMoxieRange( name ), "mox" );
				}
				else
				{
					String effectName = item.getEffectName();
					int effectId = EffectDatabase.getEffectId( effectName );
					int effectDuration = (int) Modifiers.getNumericModifier( "Item", name, "Effect Duration" );
					Modifiers mods = Modifiers.getEffectModifiers( effectId );
					String effectModifiers = (mods == null ) ? null : mods.getString( "Evaluated Modifiers" );
					AdventureResult effect = EffectPool.get( effectId, 0 );
					int active = effect.getCount( KoLConstants.activeEffects );
					stringForm.append( effectDuration );
					stringForm.append( " " );
					stringForm.append( effectName );
					if ( active > 0 )
					{
						stringForm.append( " (" );
						stringForm.append( active );
						stringForm.append( " active)" );
					}
					if ( effectModifiers != null )
					{
						stringForm.append( " (" );
						stringForm.append( effectModifiers );
						stringForm.append( ")" );
					}
				}

				// Display notes whether the item is a consumable or not
				String notes = ConsumablesDatabase.getNotes( name );
				if ( notes != null && notes.length() > 0 )
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

			defaultComponent.setFont( KoLGUIConstants.DEFAULT_FONT );
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
				if ( item.getMixingMethod() == CraftingType.CLIPART && Preferences.getBoolean( "showNoSummonOnly" ) )
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
				// The speakeasy doesn't give you a discount.
				int price = item.speakeasy ? item.getPrice() : CafeRequest.discountedPrice( item.getPrice() );
				stringForm.append( price );
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

			StringBuilder stringForm = new StringBuilder();

			stringForm.append( "<html><nobr>" );

			String color = null;

			if ( isSelected )
			{
				setForeground( UIManager.getColor( "List.selectionForeground" ) );
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

			int power = EquipmentDatabase.getPower( ar.getItemId() );

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
			StringBuilder stringForm = new StringBuilder();
			boolean isHTML = false;

			stringForm.append( icr.getName() );

			int power = EquipmentDatabase.getPower( icr.getItemId() );

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
			
			String color = null;

			if ( isSelected )
			{
				setForeground( UIManager.getColor( "List.selectionForeground" ) );
			}
			else 
			{
				color = ColorFactory.getCreationColor( icr, true );
			}

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
			if ( !( value instanceof AdventureResult ) )
			{
				return super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
			}

			if ( isSelected )
			{
				GearChangeFrame.showModifiers( value, false );
			}

			AdventureResult ar = (AdventureResult) value;
			int equipmentType = ItemDatabase.getConsumptionType( ar.getItemId() );

			int power = EquipmentDatabase.getPower( ar.getItemId() );
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
					stringForm = "<font color=gray>" + ar.getName() + "</font>";
				}
			}
			else
			{
				if ( equipmentType == KoLConstants.EQUIP_ACCESSORY )
				{
					int count;
					Modifiers mods = Modifiers.getItemModifiers( ar.getItemId() );
					if ( mods != null && mods.getBoolean( Modifiers.SINGLE ) )
					{
						count = 1;
					}
					else
					{
						count = ar.getCount( KoLConstants.inventory );
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
					stringForm = "<font color=gray>" + stringForm + "</font>";
				}
			}

			JLabel defaultComponent =
				(JLabel) super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
			defaultComponent.setText( "<html>" + stringForm + "</html>" );
			return defaultComponent;
		}
	}

	public static final DefaultListCellRenderer getFamiliarEquipmentRenderer()
	{
		return new FamiliarEquipmentRenderer();
	}

	private static class FamiliarEquipmentRenderer
		extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent( final JList list, final Object value, final int index,
			final boolean isSelected, final boolean cellHasFocus )
		{
			if ( !( value instanceof AdventureResult ) )
			{
				return super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
			}

			if ( isSelected )
			{
				GearChangeFrame.showModifiers( value, true );
			}

			AdventureResult ar = (AdventureResult) value;
			String stringForm = ar.getName();

			String effect = Modifiers.getFamiliarEffect( ar.getName() );
			if ( effect != null )
			{
				stringForm +=  " (" + effect + ")";
			}

			if ( KoLCharacter.getFamiliar() == null || !KoLCharacter.getFamiliar().canEquip( ar ) )
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
			if ( !( value instanceof FamiliarData ) )
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

			String color = null;

			if ( isSelected )
			{
				setForeground( UIManager.getColor( "List.selectionForeground" ) );
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

			String name = ar.getName();
			stringForm.append( name );

			if ( color != null )
			{
				stringForm.append( "</font>" );
			}

			int power = EquipmentDatabase.getPower( ar.getItemId() );

			if ( power > 0 )
			{
				stringForm.append( " (+" );
				stringForm.append( power );
				stringForm.append( ")" );
			}
			else
			{
				Integer fullness = ConsumablesDatabase.getRawFullness( ar.getName() );
				Integer inebriety = ConsumablesDatabase.getRawInebriety( ar.getName() );
				
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
						ConsumablesDatabase.getAdventureRange( ar.getName() ), "adv" );
	
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

            String stringForm = ar.getName() +
                    " (" +
                    KoLConstants.COMMA_FORMAT.format( ar.getCount() ) +
                    ")";
            ( (JLabel) defaultComponent ).setText( stringForm );
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

			StringBuilder stringForm = new StringBuilder();
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
					KoLConstants.pulverizeQueue.get( index ).getCount() );
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
