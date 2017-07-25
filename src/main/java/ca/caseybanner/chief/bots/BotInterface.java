package ca.caseybanner.chief.bots;

import ca.caseybanner.chief.Command;
import java.util.List;

/**
 * Interface that all bots must use.
 * 
 * @author gcl
 */
public interface BotInterface {
	public void exit();
	public List<Command> getCommands();
}
