package net.sourceforge.kolmafia.maximizer;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLmafiaCLI;

public class Boost
implements Comparable<Boost>
{
	private boolean isEquipment, isShrug, priority;
	private final String cmd;
    private String text;
	private int slot;
	private final double boost;
	private final AdventureResult item;
    private AdventureResult effect;
	private FamiliarData fam, enthroned, bjorned;
	private String edPiece, snowsuit, horse, retroCape, backupCamera;

	public Boost( String cmd, String text, AdventureResult item, double boost )
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

	public Boost( String cmd, String text, AdventureResult effect, boolean isShrug, AdventureResult item, double boost, boolean priority )
	{
		this( cmd, text, item, boost );
		this.isEquipment = false;
		this.effect = effect;
		this.isShrug = isShrug;
		this.priority = priority;
	}

	public Boost( String cmd, String text, int slot, AdventureResult item, double boost )
	{
		this( cmd, text, item, boost );
		this.isEquipment = true;
		this.slot = slot;
	}

	public Boost( String cmd, String text, String horse, double boost )
	{
		this( cmd, text, (AdventureResult) null, boost );
		this.isEquipment = false;
		this.horse = horse;
	}

	public Boost( String cmd, String text, int slot, AdventureResult item, double boost, FamiliarData enthroned, FamiliarData bjorned, String edPiece, String snowsuit, String retroCape, String backupCamera )
	{
		this( cmd, text, item, boost );
		this.isEquipment = true;
		this.slot = slot;
		this.enthroned = enthroned;
		this.bjorned = bjorned;
		this.edPiece = edPiece;
		this.snowsuit = snowsuit;
		this.retroCape = retroCape;
		this.backupCamera = backupCamera;
	}

	public Boost( String cmd, String text, FamiliarData fam, double boost )
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

	public int compareTo( Boost o )
	{
		if ( !(o instanceof Boost) ) return -1;
		Boost other = o;

		if ( this.isEquipment != other.isEquipment )
		{
			return this.isEquipment ? -1 : 1;
		}
		if ( this.priority != other.priority )
		{
			return this.priority ? -1 : 1;
		}
		if ( this.isEquipment ) return 0;	// preserve order of addition
		int rv = Double.compare( other.boost, this.boost );
		return rv;
	}

	public boolean execute( boolean equipOnly )
	{
		if ( equipOnly && !this.isEquipment ) return false;
		if ( this.cmd.length() == 0 ) return false;
		KoLmafiaCLI.DEFAULT_SHELL.executeLine( this.cmd );
		return true;
	}

	public void addTo( MaximizerSpeculation spec )
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
				if ( this.enthroned != null )
				{
					spec.setEnthroned( this.enthroned );
				}
				if ( this.bjorned != null )
				{
					spec.setBjorned( this.bjorned );
				}
				if ( this.edPiece != null )
				{
					spec.setEdPiece( this.edPiece );
				}
				if ( this.retroCape != null )
				{
					spec.setRetroCape( this.retroCape );
				}
				if ( this.backupCamera != null )
				{
					spec.setBackupCamera( this.backupCamera );
				}
				if ( this.snowsuit != null )
				{
					spec.setSnowsuit( this.snowsuit );
				}
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
		else if ( this.horse != null )
		{
			spec.setHorsery( this.horse );
		}
	}
	
	public AdventureResult getItem( )
	{
		return getItem( true );
	}

	public AdventureResult getItem( boolean preferEffect )
	{
		if ( this.effect != null && preferEffect ) return this.effect;
		return this.item;
	}

	public double getBoost()
	{
		return this.boost;
	}

	public String getCmd()
	{
		return this.cmd;
	}

	public int getSlot()
	{
		return this.slot;
	}

	public boolean isEquipment()
	{
		return this.isEquipment;
	}
}
