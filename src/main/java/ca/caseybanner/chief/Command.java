/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.caseybanner.chief;

import ca.caseybanner.chief.commands.ConfigurationException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author kcbanner
 */
public abstract class Command {

	private final Bot bot;
	
	/**
	 * Constructor
	 * 
	 * @param bot the Bot this command belongs to
	 */
	protected Command(Bot bot) {
		this.bot = bot;
	}
	
	/**
	 * Getter for the bot this command belongs to
	 * 
	 * @return the Bot instance
	 */
	protected Bot getBot() {
		return bot;
	}
	
	/**
	 * Return a usage string for this command
	 * 
	 * @return usage string to be displayed to the user
	 */
	public abstract String getUsage();
	
	/**
	 * Return a Pattern used to determine if a message is accepted by this command.
	 * 
	 * @return pattern used to match against incoming messages
	 */
	public abstract Pattern getPattern();

    /**
     * Possibly async processing.  If the optional is present it should contain the text result to be
	 * sent as a message representing the result of the command.
     *
     * @param from
     * @param message
     * @param matcher
     * @param fromRoom
     * @return
     */
    public abstract CompletableFuture<Optional<String>> processAsyncMessage(
            String from, String message, Matcher matcher, boolean fromRoom);
	
	/**
	 * Called when all configuration properties have been set.
	 * @throws ca.caseybanner.chief.commands.ConfigurationException
	 */
	public void configurationComplete() throws ConfigurationException {
	
	}
	
	/**
	 * Returns true if this is an admin only command.
	 * 
	 * If this returns true, the command will only respond to JIDs listed in the admins list.
	 * This includes JIDs of rooms, so entire rooms can be admins if you want.
	 * 
	 * @return true if this command is only to be allowed to be used by admins
	 */
	public boolean isAdminOnly() {
		return false;
	}

    /**
     * Helper to convert an optional<string> into a completed future with that value
     *
     * @param value
     * @return
     */
    protected static CompletableFuture<Optional<String>> toFuture(Optional<String> value) {
        return CompletableFuture.completedFuture(value);
    }

    /**
     * Helper to create a completed future from a string value
     *
     * @param value
     * @return
     */
	protected static CompletableFuture<Optional<String>> toFuture(String value) {
        return toFuture(Optional.of(value));
    }
	
}
