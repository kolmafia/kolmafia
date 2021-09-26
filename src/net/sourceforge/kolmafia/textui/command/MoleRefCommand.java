package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestLogger;

public class MoleRefCommand
	extends AbstractCommand
{
	public MoleRefCommand()
	{
		this.usage = " - Path of the Mole spoilers.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		RequestLogger.printLine( "<table border=2>" + "<tr><td>9</td><td rowspan=6></td><td rowspan=3></td><td>+30% all stats</td></tr>" + "<tr><td>8</td><td rowspan=5>+10 fam weight</td></tr>" + "<tr><td>7</td></tr>" + "<tr><td>6</td><td>MP</td></tr>" + "<tr><td>5</td><td rowspan=6>food</td></tr>" + "<tr><td>4</td></tr>" + "<tr><td>3</td><td>HP</td><td rowspan=7>+3 stats/fight</td></tr>" + "<tr><td>2</td><td rowspan=5>+meat</td></tr>" + "<tr><td>1</td></tr>" + "<tr><td>0</td></tr>" + "<tr><td>-1</td><td rowspan=5>booze</td></tr>" + "<tr><td>-2</td></tr>" + "<tr><td>-3</td><td>stats</td></tr>" + "<tr><td>-4</td><td rowspan=6></td><td rowspan=5>regenerate</td></tr>" + "<tr><td>-5</td></tr>" + "<tr><td>-6</td><td>-3MP/skill</td></tr>" + "<tr><td>-7</td><td rowspan=3></td></tr>" + "<tr><td>-8</td></tr>" + "<tr><td>-9</td><td>+30 ML</td></tr>" + "</table><br>" );
	}
}
