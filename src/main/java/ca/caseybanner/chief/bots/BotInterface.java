package ca.caseybanner.chief.bots;

import ca.caseybanner.chief.Command;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Interface that all bots must use.
 * 
 * @author gcl
 */
public interface BotInterface {
	public void exit();
	public List<Command> getCommands();
	public void addCommand(Function<BotInterface, Command> commandConstructor);
	public void start();
	public CompletableFuture<Boolean> waitForExit();
}
