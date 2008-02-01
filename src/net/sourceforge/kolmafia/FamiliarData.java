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

package net.sourceforge.kolmafia;

import java.awt.Component;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingConstants;

import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.request.EquipmentRequest;

import net.sourceforge.kolmafia.persistence.FamiliarDatabase;

public class FamiliarData
	implements Comparable
{
	public static final FamiliarData NO_FAMILIAR = new FamiliarData( -1 );

	private static final Pattern REGISTER_PATTERN =
		Pattern.compile( "<img src=\"http://images\\.kingdomofloathing\\.com/([^\"]*?)\" class=hand onClick='fam\\((\\d+)\\)'>.*?<b>(.*?)</b>.*?\\d+-pound (.*?) \\(([\\d,]+) (exp|experience)?, .*? kills?\\)(.*?)<(/tr|form)" );

	private final int id;

	private int weight;
	private final String name, race;
	private AdventureResult item;

	public FamiliarData( final int id )
	{
		this( id, "", 1, EquipmentRequest.UNEQUIP );
	}

	public FamiliarData( final int id, final String name, final int weight, final AdventureResult item )
	{
		this.id = id;
		this.name = name;
		this.race = id == -1 ? "(none)" : FamiliarDatabase.getFamiliarName( id );

		this.weight = weight;
		this.item = item;
	}

	private FamiliarData( final Matcher dataMatcher )
	{
		this.id = StaticEntity.parseInt( dataMatcher.group( 2 ) );
		this.race = dataMatcher.group( 4 );

		FamiliarDatabase.registerFamiliar( this.id, this.race );
		FamiliarDatabase.setFamiliarImageLocation( this.id, dataMatcher.group( 1 ) );

		int kills = StaticEntity.parseInt( dataMatcher.group( 5 ) );
		this.weight = Math.max( Math.min( 20, (int) Math.sqrt( kills ) ), 1 );

		this.name = dataMatcher.group( 3 );

		String itemData = dataMatcher.group( 7 );

		this.item = parseFamiliarItem( this.id, itemData );
	}

	private static final Object[][] FAMILIAR_ITEM_DATA =
	{
		{
			"tamo.gif",
			new AdventureResult( "lucky Tam O'Shanter", 1, false ),
		},
		{
			"omat.gif",
			new AdventureResult( "lucky Tam O'Shatner", 1, false ),
		},
		{
			"maypole.gif",
			new AdventureResult( "miniature gravy-covered maypole", 1, false ),
		},
		{
			"waxlips.gif",
			new AdventureResult( "wax lips", 1, false ),
		},
		{
			"pitchfork.gif",
			new AdventureResult( "annoying pitchfork", 1, false ),
		},
		{
			"lnecklace.gif",
			new AdventureResult( "lead necklace", 1, false ),
		},
		{
			"ratbal.gif",
			new AdventureResult( "rat head balloon", 1, false ),
		},
		{
			"punkin.gif",
			new AdventureResult( "plastic pumpkin bucket", 1, false ),
		},
		{
			"ffdop.gif",
			new AdventureResult( "flaming familiar doppelg&auml;nger", 1, false ),
		},
		{
			"maybouquet.gif",
			new AdventureResult( "Mayflower bouquet", 1, false ),
		},
		{
			"anthoe.gif",
			new AdventureResult( "ant hoe", 1, false ),
		},
		{
			"antrake.gif",
			new AdventureResult( "ant rake", 1, false ),
		},
		{
			"antfork.gif",
			new AdventureResult( "ant pitchfork", 1, false ),
		},
		{
			"antsickle.gif",
			new AdventureResult( "ant sickle", 1, false ),
		},
		{
			"antpick.gif",
			new AdventureResult( "ant pick", 1, false ),
		},
		{
			"fishscaler.gif",
			new AdventureResult( "oversized fish scaler", 1, false ),
		},
		{
			"origamimag.gif",
			new AdventureResult( "origami &quot;gentlemen's&quot; magazine", 1, false ),
		},
		// Crimbo P. R. E. S. S. I. E. items
		{
			"whitebow.gif",
			new AdventureResult( "metallic foil bow", 1, false ),
		},
		{
			"radar.gif",
			new AdventureResult( "metallic foil radar dish", 1, false ),
		},
		// Pet Rock items
		{
			"monocle.gif",
			new AdventureResult( "pet rock &quot;Snooty&quot; disguise", 1, false ),
		},
		{
			"groucho.gif",
			new AdventureResult( "pet rock &quot;Groucho&quot; disguise", 1, false ),
		},
	};

	private static final AdventureResult parseFamiliarItem( final int id, final String text )
	{
		if ( text.indexOf( "<img" ) == -1 )
		{
			return EquipmentRequest.UNEQUIP;
		}

		for ( int i = 0; i < FAMILIAR_ITEM_DATA.length; ++i )
		{
			String img = (String) FAMILIAR_ITEM_DATA[i][0];
			if ( text.indexOf( img ) != -1 )
			{
				return (AdventureResult) FAMILIAR_ITEM_DATA[i][1];
			}
		}

		// Default familiar equipment
		return new AdventureResult( FamiliarDatabase.getFamiliarItem( id ), 1, false );
	}

	public static final void registerFamiliarData( final String searchText )
	{
		// Assume he has no familiar
		FamiliarData firstFamiliar = null;

		Matcher familiarMatcher = FamiliarData.REGISTER_PATTERN.matcher( searchText );
		while ( familiarMatcher.find() )
		{
			FamiliarData examinedFamiliar = KoLCharacter.addFamiliar( new FamiliarData( familiarMatcher ) );

			// First in the list might be equipped
			if ( firstFamiliar == null )
			{
				firstFamiliar = examinedFamiliar;
			}
		}

		// On the other hand, he may have familiars but none are equipped.
		if ( firstFamiliar == null || searchText.indexOf( "You do not currently have a familiar" ) != -1 )
		{
			firstFamiliar = FamiliarData.NO_FAMILIAR;
		}

		KoLCharacter.setFamiliar( firstFamiliar );
		KoLCharacter.setEquipment( KoLCharacter.FAMILIAR, firstFamiliar.getItem() );
	}

	public int getId()
	{
		return this.id;
	}

	public void setItem( final AdventureResult item )
	{
		this.item = item;
	}

	public AdventureResult getItem()
	{
		return this.item == null ? EquipmentRequest.UNEQUIP : this.item;
	}

	public void setWeight( final int weight )
	{
		this.weight = weight;
	}

	public int getWeight()
	{
		return this.weight;
	}

	public int getModifiedWeight()
	{
		int weight = this.weight + KoLCharacter.getFamiliarWeightAdjustment();
		float percent = KoLCharacter.getFamiliarWeightPercentAdjustment() / 100.0f;

		if ( percent != 0.0f )
		{
			weight = (int) Math.floor( weight + weight * percent );
		}

		return Math.max( 1, weight );
	}

	public static final int itemWeightModifier( final int itemId )
	{
		switch ( itemId )
		{
		case -1: // bogus item id
		case 0: // another bogus item id
		case 856: // shock collar
		case 857: // moonglasses
		case 1040: // lucky Tam O'Shanter
		case 1102: // targeting chip
		case 1116: // annoying pitchfork
		case 1152: // miniature gravy-covered maypole
		case 1260: // wax lips
		case 1264: // tiny nose-bone fetish
		case 1419: // teddy bear sewing kit
		case 1489: // miniature dormouse
		case 1537: // weegee sqouija
		case 1539: // lucky Tam O'Shatner
		case 1623: // badger badge
		case 1928: // tuning fork
		case 2084: // can of starch
		case 2147: // evil teddy bear sewing kit
		case 2191: // giant book of ancient carols
		case 2225: // flaming familiar doppelg&auml;nger
		case 2570: // ant hoe
		case 2571: // ant rake
		case 2572: // ant pitchfork
		case 2573: // ant sickle
		case 2574: // ant pick
		case 2846: // plastic bib
		case 3087: // teddy borg sewing kit
		case 3097: // oversized fish scaler
			return 0;

		case 865: // lead necklace
			return 3;

		case 1971: // plastic pumpkin bucket
		case 2541: // Mayflower bouquet
			return 5;

		case 1218: // rat head balloon
			return -3;

		case 1243: // toy six-seater hovercraft
			return -5;

		case 1305: // tiny makeup kit
		case 2710: // cracker
			return 15;

		case 1526: // pet rock "Snooty" disguise
		case 1678: // pet rock "Groucho" disguise
			return 11;

		default:
			return 5;
		}
	}

	public String getName()
	{
		return this.name;
	}

	public String getRace()
	{
		return this.race;
	}

	public boolean trainable()
	{
		if ( this.id == -1 )
		{
			return false;
		}

		int skills[] = FamiliarDatabase.getFamiliarSkills( this.id );

		// If any skill is greater than 0, we can train in that event
		for ( int i = 0; i < skills.length; ++i )
		{
			if ( skills[ i ] > 0 )
			{
				return true;
			}
		}

		return false;
	}

	public String toString()
	{
		return this.id == -1 ? "(none)" : this.race + " (" + this.getModifiedWeight() + " lbs)";
	}

	public boolean equals( final Object o )
	{
		return o != null && o instanceof FamiliarData && this.id == ( (FamiliarData) o ).id;
	}

	public int compareTo( final Object o )
	{
		return o == null || !( o instanceof FamiliarData ) ? 1 : this.compareTo( (FamiliarData) o );
	}

	public int compareTo( final FamiliarData fd )
	{
		return this.race.compareToIgnoreCase( fd.race );
	}

	/**
	 * Returns whether or not the familiar can equip the given familiar item.
	 */

	public boolean canEquip( final AdventureResult item )
	{
		if ( item == null )
		{
			return false;
		}

		if ( item == EquipmentRequest.UNEQUIP )
		{
			return true;
		}

		switch ( item.getItemId() )
		{
		case -1:
			return false;

		case 865: // lead necklace
		case 1040: // lucky Tam O'Shanter
		case 1116: // annoying pitchfork
		case 1152: // miniature gravy-covered maypole
		case 1218: // rat head balloon
		case 1260: // wax lips
		case 1539: // lucky Tam O'Shatner
		case 1971: // plastic pumpkin bucket
		case 2225: // flaming familiar doppelg&auml;nger
		case 2541: // Mayflower bouquet
		case 2570: // ant hoe
		case 2571: // ant rake
		case 2572: // ant pitchfork
		case 2573: // ant sickle
		case 2574: // ant pick
		case 3097: // oversized fish scaler
			return this.id != 54;

		case 1526: // pet rock "Snooty" disguise
		case 1678: // pet rock "Groucho" disguise
			return this.id == 45 || this.id == 63 || this.id == 78;

		case 3043: // metallic foil bow
		case 3044: // metallic foil radar dish
			return this.id == 77;

		default:
			return item.getName().equals( FamiliarDatabase.getFamiliarItem( this.id ) );
		}
	}

	public boolean isCombatFamiliar()
	{
		if ( FamiliarDatabase.isCombatType( this.id ) )
		{
			return true;
		}

		if ( this.id == 66 )
		{
			return KoLCharacter.getEquipment( KoLCharacter.WEAPON ).getName().endsWith( "whip" ) || KoLCharacter.getEquipment(
				KoLCharacter.OFFHAND ).getName().endsWith( "whip" );
		}

		return false;
	}

	public static final DefaultListCellRenderer getRenderer()
	{
		return new FamiliarRenderer();
	}

	private static class FamiliarRenderer
		extends DefaultListCellRenderer
	{
		public Component getListCellRendererComponent( final JList list, final Object value, final int index,
			final boolean isSelected, final boolean cellHasFocus )
		{
			JLabel defaultComponent =
				(JLabel) super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );

			if ( value == null || !( value instanceof FamiliarData ) || ( (FamiliarData) value ).id == -1 )
			{
				defaultComponent.setIcon( JComponentUtilities.getImage( "debug.gif" ) );
				defaultComponent.setText( KoLConstants.VERSION_NAME + ", the 0 lb. \"No Familiar Plz\" Placeholder" );

				defaultComponent.setVerticalTextPosition( SwingConstants.CENTER );
				defaultComponent.setHorizontalTextPosition( SwingConstants.RIGHT );
				return defaultComponent;
			}

			FamiliarData familiar = (FamiliarData) value;
			defaultComponent.setIcon( FamiliarDatabase.getFamiliarImage( familiar.id ) );
			defaultComponent.setText( familiar.getName() + ", the " + familiar.getWeight() + " lb. " + familiar.getRace() );

			defaultComponent.setVerticalTextPosition( SwingConstants.CENTER );
			defaultComponent.setHorizontalTextPosition( SwingConstants.RIGHT );

			return defaultComponent;
		}
	}
}
