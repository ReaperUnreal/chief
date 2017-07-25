package ca.caseybanner.chief.commands;

import ca.caseybanner.chief.Command;
import ca.caseybanner.chief.HipChatBot;
import java.util.function.Function;

/**
 * Interface for adding a command to a bot.
 * 
 * @author gcl
 */
public interface CommandAdder {
	public void addCommand(Function<HipChatBot, Command> commandConstructor);
}
