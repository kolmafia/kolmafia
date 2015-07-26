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

import java.util.Enumeration;
import java.util.Vector;

// Class for Tray Icon popup menu (shown when user right clicks Tray Icon)
// Used for main popup menu and it's submenus
public class TrayIconPopup
	implements TrayIconPopupItem
{

	/**
	 * Create main popup menu (use for WindowsTrayIcon.setPopup())
	 */
	public TrayIconPopup()
	{
	}

	/**
	 * Create sub menu (use for TrayIconPopup.addMenuItem()) Param item = the name of the new submenu
	 */
	public TrayIconPopup( final String item )
	{
		this.mItem = item;
	}

	/**
	 * Add menu item to popup (sub)menu Param item = the item to add (instance of
	 * TrayIconPopup/TrayIconPopupSimpleItem/TrayIconPopupCh..)
	 */
	public void addMenuItem( final TrayIconPopupItem item )
	{
		this.mVector.addElement( item );
	}

	/*******************************************************************************************************************
	 * * Next section is for inter use only -- or for hackers :O) * *
	 ******************************************************************************************************************/

	// Vector containing menu items
	protected Vector mVector = new Vector();
	// Name of popup menu (only needed for submenus)
	protected String mItem;

	/**
	 * Return submenu depth - used by WindowsTrayIcon.setPopup()/initPopup()
	 */
	public int getNbLevels()
	{
		int nb = 0;
		for ( Enumeration e = this.mVector.elements(); e.hasMoreElements(); )
		{
			TrayIconPopupItem item = (TrayIconPopupItem) e.nextElement();
			nb = Math.max( nb, item.getNbLevels() );
		}
		return nb + 1;
	}

	/**
	 * Callback when user selects menu item (find it by comparing menu id's) Param menuId = the id of the selected item
	 */
	public boolean onSelected( final int menuId )
	{
		for ( Enumeration e = this.mVector.elements(); e.hasMoreElements(); )
		{
			TrayIconPopupItem item = (TrayIconPopupItem) e.nextElement();
			if ( item.onSelected( menuId ) )
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Create menu in native library - used by WindowsTrayIcon.setPopup() Param trayicon = the owner of this menu Param
	 * id = the icon's id Param level = the level (submenu depth)
	 */
	public void setTrayIcon( final WindowsTrayIcon trayicon, final int id, final int level )
	{
		int mLevel = level + 1;
		WindowsTrayIcon.subPopup( id, mLevel, this.mItem, WindowsTrayIcon.POPUP_TYPE_INIT_LEVEL, 0 );
		for ( Enumeration e = this.mVector.elements(); e.hasMoreElements(); )
		{
			TrayIconPopupItem item = (TrayIconPopupItem) e.nextElement();
			item.setTrayIcon( trayicon, id, mLevel );
		}
		WindowsTrayIcon.subPopup( id, mLevel, this.mItem, WindowsTrayIcon.POPUP_TYPE_DONE_LEVEL, 0 );
	}
}
