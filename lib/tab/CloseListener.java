/*
 * David Bismut, davidou@mageos.com
 * Intern, SETLabs, Infosys Technologies Ltd. May 2004 - Jul 2004
 * Ecole des Mines de Nantes, France
 */

package tab;

import java.awt.event.MouseEvent;
import java.util.EventListener;

public interface CloseListener
	extends EventListener
{
	public void closeOperation( MouseEvent e, int overTabIndex );
}
