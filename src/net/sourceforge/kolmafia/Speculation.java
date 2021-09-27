package net.sourceforge.kolmafia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;

import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.EquipmentRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.textui.command.RetroCapeCommand;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class Speculation
{
	private int MCD;
	public AdventureResult[] equipment;
	private final ArrayList<AdventureResult> effects;
	private FamiliarData familiar, enthroned, bjorned;
	private String edPiece, snowsuit, custom, horsery, boomBox, retroCape, backupCamera;
	protected boolean calculated = false;
	protected Modifiers mods;

	public Speculation()
	{
		this.MCD = KoLCharacter.getMindControlLevel();
		this.equipment = EquipmentManager.allEquipment();
		this.effects = new ArrayList<AdventureResult>();
		this.effects.addAll( KoLConstants.activeEffects );
		// Strip out intrinsic effects granted by equipment - they will
		// be readded if appropriate via Intrinsic Effect modifiers.
		// We used to just strip out all intrinsics, back when non-equipment
		// intrinsics were all just flavor rather than possibly significant.
		for ( int i = this.equipment.length - 1; i >= 0; --i )
		{
			if ( this.equipment[ i ] == null ) continue;
			int itemId = this.equipment[ i ].getItemId();
			Modifiers mods = Modifiers.getItemModifiers( itemId );
			if ( mods == null ) continue;
			String name = mods.getString( Modifiers.INTRINSIC_EFFECT );
			if ( name.length() == 0 ) continue;
			int effectId = EffectDatabase.getEffectId( name );
			this.effects.remove( EffectPool.get( effectId ) );
		}
		this.familiar = KoLCharacter.currentFamiliar;
		this.enthroned = KoLCharacter.currentEnthroned;
		this.bjorned = KoLCharacter.currentBjorned;
		this.edPiece = Preferences.getString( "edPiece" );
		this.snowsuit = Preferences.getString( "snowsuit" );
		this.custom = null;
		this.horsery = Preferences.getString( "_horsery" );
		this.boomBox = Preferences.getString( "boomBoxSong" );
		this.retroCape = Preferences.getString( "retroCapeSuperhero" ) + " " + Preferences.getString( "retroCapeWashingInstructions" );
		this.backupCamera = Preferences.getString( "backupCameraMode" );

	}

	public void setMindControlLevel( int MCD )
	{
		this.MCD = MCD;
	}

	public void setFamiliar( FamiliarData familiar )
	{
		this.familiar = familiar;
	}

	public void setEnthroned( FamiliarData familiar )
	{
		this.enthroned = familiar;
	}

	public void setBjorned( FamiliarData familiar )
	{
		this.bjorned = familiar;
	}

	public void setEdPiece( String edPiece )
	{
		this.edPiece = edPiece;
	}

	public void setRetroCape( String retroCape ) { this.retroCape = retroCape; }

	public void setBackupCamera( String backupCamera ) { this.backupCamera = backupCamera; }

	public void setSnowsuit( String snowsuit )
	{
		this.snowsuit = snowsuit;
	}

	public void setCustom( String custom )
	{
		this.custom = custom;
	}

	public void setHorsery( String horsery )
	{
		this.horsery = horsery;
	}

	public void setBoomBox( String boomBox )
	{
		this.boomBox = boomBox;
	}

	public FamiliarData getEnthroned()
	{
		return this.enthroned;
	}

	public FamiliarData getBjorned()
	{
		return this.bjorned;
	}

	public FamiliarData getFamiliar()
	{
		return this.familiar;
	}

	public String getEdPiece()
	{
		return this.edPiece;
	}

	public String getRetroCape() { return this.retroCape; }

	public String getBackupCamera() { return this.backupCamera; }

	public String getSnowsuit()
	{
		return this.snowsuit;
	}

	public String getCustom()
	{
		return this.custom;
	}

	public String getHorsery()
	{
		return this.horsery;
	}

	public String getBoomBox()
	{
		return this.boomBox;
	}

	public void equip( int slot, AdventureResult item )
	{
		if ( slot < 0 || slot >= EquipmentManager.ALL_SLOTS ) return;
		this.equipment[ slot ] = item;
		if ( slot == EquipmentManager.WEAPON &&
			EquipmentDatabase.getHands( item.getItemId() ) > 1 )
		{
			this.equipment[ EquipmentManager.OFFHAND ] = EquipmentRequest.UNEQUIP;
		}
	}

	public boolean hasEffect( AdventureResult effect )
	{
		return this.effects.contains( effect );
	}

	public void addEffect( AdventureResult effect )
	{
		if ( !this.effects.contains( effect ) )
		{
			this.effects.add( effect );
		}
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
			this.enthroned,
			this.bjorned,
			this.edPiece,
			this.snowsuit,
			this.custom,
			this.horsery,
			this.boomBox,
			this.retroCape,
			this.backupCamera,
			true );
		this.calculated = true;
		return this.mods;
	}

	public Modifiers getModifiers()
	{
		if ( !this.calculated ) this.calculate();
		return this.mods;
	}

	public boolean parse( String text )
	{
		boolean quiet = false;
		String[] pieces = text.toLowerCase().split( "\\s*;\\s*" );
		for ( int i = 0; i < pieces.length; ++i )
		{
			String[] piece = pieces[ i ].split( " ", 2 );
			String cmd = piece[ 0 ];
			String params = piece.length > 1 ? piece[ 1 ] : "";
			
			if ( cmd.equals( "" ) )
			{
				continue;
			}

			if ( cmd.equals( "mcd" ) )
			{
				this.setMindControlLevel( StringUtilities.parseInt( params ) );
			}
			else if ( cmd.equals( "equip" ) )
			{
				piece = params.split( " ", 2 );
				int slot = EquipmentRequest.slotNumber( piece[ 0 ] );
				if ( slot != -1 )
				{
					params = piece[ 1 ];
				}
		
				AdventureResult match = ItemFinder.getFirstMatchingItem( params,
					Match.EQUIP );
				if ( match == null )
				{
					return true;
				}
				if ( slot == -1 )
				{
					slot = EquipmentRequest.chooseEquipmentSlot( match.getItemId() );
		
					// If it can't be equipped, give up
					if ( slot == -1 )
					{
						KoLmafia.updateDisplay( MafiaState.ERROR, "You can't equip a " + match.getName() );
						return true;
					}
				}
				this.equip( slot, match );
			}
			else if ( cmd.equals( "unequip" ) )
			{
				int slot = EquipmentRequest.slotNumber( params );
				if ( slot == -1 )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR,
						"Unknown slot: " + params );
					return true;
				}
				this.equip( slot, EquipmentRequest.UNEQUIP );
			}
			else if ( cmd.equals( "familiar" ) )
			{
				int id = FamiliarDatabase.getFamiliarId( params );
				if ( id == -1 && !params.equals( "none" ) )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR,
						"Unknown familiar: " + params );
					return true;
				}
				FamiliarData fam = KoLCharacter.findFamiliar( id );
				if ( fam == null )
				{
					fam = new FamiliarData( id );
				}
				this.setFamiliar( fam );
			}
			else if ( cmd.equals( "enthrone" ) )
			{
				int id = FamiliarDatabase.getFamiliarId( params );
				if ( id == -1 && !params.equals( "none" ) )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR,
						"Unknown familiar: " + params );
					return true;
				}
				FamiliarData fam = new FamiliarData( id );
				this.setEnthroned( fam );
				this.equip( EquipmentManager.HAT,
					ItemPool.get( ItemPool.HATSEAT ) );
			}
			else if ( cmd.equals( "bjornify" ) )
			{
				int id = FamiliarDatabase.getFamiliarId( params );
				if ( id == -1 && !params.equals( "none" ) )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR,
						"Unknown familiar: " + params );
					return true;
				}
				FamiliarData fam = new FamiliarData( id );
				this.setBjorned( fam );
				this.equip( EquipmentManager.CONTAINER,
					ItemPool.get( ItemPool.BUDDY_BJORN ) );
			}
			else if ( cmd.equals( "edpiece" ) )
			{
				if ( !params.equals( "bear" ) && !params.equals( "owl" ) && 
					!params.equals( "puma" ) && !params.equals( "hyena" ) && 
					!params.equals( "mouse" ) && !params.equals( "weasel" ) )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR,
						"Unknown animal: " + params );
					return true;
				}		
				this.setEdPiece( params );
				this.equip( EquipmentManager.HAT,
					ItemPool.get( ItemPool.CROWN_OF_ED ) );
			}
			else if ( cmd.equals( "retrocape" ) )
			{
				String[] parts = params.split(" " );
				if ( ( !Arrays.asList(RetroCapeCommand.SUPERHEROS).contains( parts[0] ) ) ||
					 ( !Arrays.asList(RetroCapeCommand.WASHING_INSTRUCTIONS).contains( parts[1] ) ))
				{
					KoLmafia.updateDisplay( MafiaState.ERROR,
						"Unknown retro cape configuration: " + params );
					return true;
				}
				this.setRetroCape( params );
				this.equip( EquipmentManager.CONTAINER,
					ItemPool.get( ItemPool.KNOCK_OFF_RETRO_SUPERHERO_CAPE ) );
			}
			else if ( cmd.equals( "backupcamera" ) )
			{
				if ( !params.equals( "meat" ) && !params.equals( "ml" ) &&
						!params.equals( "init" ) )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR,
							"Unknown backup camera setting: " + params );
					return true;
				}
				this.setBackupCamera( params );
				this.equip( EquipmentManager.ACCESSORY3,
						ItemPool.get( ItemPool.BACKUP_CAMERA ) );
			}
			else if ( cmd.equals( "snowsuit" ) )
			{
				if ( !params.equals( "eyebrows" ) && !params.equals( "smirk" ) && 
					!params.equals( "nose" ) && !params.equals( "goatee" ) && 
					!params.equals( "hat" ) )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR,
						"Unknown decoration: " + params );
					return true;
				}		
				this.setSnowsuit( params );
				this.equip( EquipmentManager.FAMILIAR,
					ItemPool.get( ItemPool.SNOW_SUIT ) );
			}
			else if ( cmd.equals( "up" ) )
			{
				List effects = EffectDatabase.getMatchingNames( params );
				if ( effects.isEmpty() )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR,
						"Unknown effect: " + params );
					return true;
				}

				int effectId = EffectDatabase.getEffectId( (String) effects.get( 0 ) );
				AdventureResult effect = EffectPool.get( effectId );
				if ( !this.hasEffect( effect ) )
				{
					this.addEffect( effect );
				}
			}
			else if ( cmd.equals( "uneffect" ) )
			{
				List effects = EffectDatabase.getMatchingNames( params );
				if ( effects.isEmpty() )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR,
						"Unknown effect: " + params );
					return true;
				}

				int effectId = EffectDatabase.getEffectId( (String) effects.get( 0 ) );
				AdventureResult effect = EffectPool.get( effectId );
				this.removeEffect( effect );
			}
			else if ( cmd.equals( "quiet" ) )
			{
				quiet = true;
			}
			else
			{
				KoLmafia.updateDisplay( MafiaState.ERROR,
					"I don't know how to speculate about " + cmd );
				return true;
			}
		}
		return quiet;
	}
}
