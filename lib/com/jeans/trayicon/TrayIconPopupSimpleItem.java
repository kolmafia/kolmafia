/***
 * Windows Tray Icon
 * -----------------
 *
 * Written by Jan Struyf
 *
 *  jan.struyf@cs.kuleuven.ac.be
 *  http://jeans.studentenweb.org/java/trayicon/trayicon.html
 *
 * Please mail me if you
 *	- 've found bugs
 *	- like this program
 *	- don't like a particular feature
 *	- would like something to be modified
 *
 * I always give it my best shot to make a program useful and solid, but
 * remeber that there is absolutely no warranty for using this program as
 * stated in the following terms:
 *
 * THERE IS NO WARRANTY FOR THIS PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE
 * LAW. THE COPYRIGHT HOLDER AND/OR OTHER PARTIES WHO MAY HAVE MODIFIED THE
 * PROGRAM, PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER
 * EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  THE ENTIRE RISK AS
 * TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU.  SHOULD THE
 * PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING,
 * REPAIR OR CORRECTION.
 *
 * IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW WILL ANY COPYRIGHT HOLDER,
 * OR ANY OTHER PARTY WHO MAY MODIFY AND/OR REDISTRIBUTE THE PROGRAM,
 * BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR
 * CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR INABILITY TO USE THE
 * PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA OR DATA BEING RENDERED
 * INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE
 * PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER
 * PARTY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * May the Force be with you... Just compile it & use it!
 */

package com.jeans.trayicon;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Vector;

// Menu item for Tray Icon
public class TrayIconPopupSimpleItem
	implements TrayIconPopupItem
{

	// Menu item's name
	protected String m_Item;
	// Menu item's id (used by native code)
	protected int m_MenuId;
	// Enable / Disable menu item
	protected boolean m_Enabled;
	// Set item as default
	protected boolean m_Default;
	// Owner of this menu item
	protected WindowsTrayIcon m_TrayIcon;
	// Action m_Listeners for menu item
	private Vector m_Listeners;

	/**
	 * Create a new menu item Param item = name of new item
	 */
	public TrayIconPopupSimpleItem( final String item )
	{
		this.m_Item = item;
		this.m_Enabled = true;
	}

	/**
	 * Return the name of this item
	 */
	public String getName()
	{
		return this.m_Item;
	}

	/**
	 * Add an ActionLister to this menu item Just like with java.awt.Button or javax.swing.JButton Param listener = your
	 * listener
	 */
	public void addActionListener( final ActionListener listener )
	{
		if ( this.m_Listeners == null )
		{
			this.m_Listeners = new Vector();
		}
		this.m_Listeners.addElement( listener );
	}

	/*******************************************************************************************************************
	 * * Next section is for inter use only -- or for hackers :O) * *
	 ******************************************************************************************************************/

	/**
	 * Return submenu depth - used by WindowsTrayIcon.setPopup()/initPopup()
	 */
	public int getNbLevels()
	{
		return 0;
	}

	/**
	 * Enable/Disable item Param enable = enable/disable item?
	 */
	public void setEnabled( final boolean enable )
	{
		this.m_Enabled = enable;
		if ( this.m_TrayIcon != null )
		{
			this.m_TrayIcon.modifyPopup( this.m_MenuId, WindowsTrayIcon.POPUP_MODE_ENABLE, this.m_Enabled );
		}
	}

	/**
	 * Set item as default Param def = set item as default?
	 */
	public void setDefault( final boolean def )
	{
		this.m_Default = def;
		if ( this.m_TrayIcon != null )
		{
			this.m_TrayIcon.modifyPopup( this.m_MenuId, WindowsTrayIcon.POPUP_MODE_DEFAULT, this.m_Default );
		}
	}

	/**
	 * Callback when user selects menu item (find it by comparing menu id's) Param menuId = the id of the selected item
	 */
	public boolean onSelected( final int menuId )
	{
		boolean selected = menuId == this.m_MenuId;
		if ( selected && this.m_Listeners != null )
		{
			ActionEvent evt = new ActionEvent( this, 0, "" );
			for ( Enumeration e = this.m_Listeners.elements(); e.hasMoreElements(); )
			{
				ActionListener listener = (ActionListener) e.nextElement();
				listener.actionPerformed( evt );
			}
		}
		return selected;
	}

	/**
	 * Create menu in native library - used by WindowsTrayIcon.setPopup() Param trayicon = the owner of this menu Param
	 * id = the icon's id Param level = the level (submenu depth)
	 */
	public void setTrayIcon( final WindowsTrayIcon trayicon, final int id, final int level )
	{
		int extra = this.m_Enabled ? WindowsTrayIcon.POPUP_MODE_ENABLE : 0;
		if ( this.m_Default )
		{
			extra |= WindowsTrayIcon.POPUP_MODE_DEFAULT;
		}
		this.m_MenuId = WindowsTrayIcon.subPopup( id, level, this.m_Item, WindowsTrayIcon.POPUP_TYPE_ITEM, extra );
		this.m_TrayIcon = trayicon;
	}
}
