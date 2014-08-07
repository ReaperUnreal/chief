/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.caseybanner.chief;

import ca.caseybanner.chief.commands.ConfigurationException;
import java.util.Optional;
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
	 * @param bot 
	 */
	public Command(Bot bot) {
		this.bot = bot;
	}
	
	/**
	 * Getter for the bot this command belongs to
	 * 
	 * @return 
	 */
	protected Bot getBot() {
		return bot;
	}
	
	/**
	 * Return a usage string for this command
	 * 
	 * @return 
	 */
	public abstract String getUsage();
	
	/**
	 * Return a Pattern used to determine if a message is accepted by this command.
	 * 
	 * @return 
	 */
	public abstract Pattern getPattern();	

	/**
	 * Process a message and optionally return a response. 
	 * 
	 * @param from the nickname the chat was from
	 * @param message the message itself
	 * @param matcher the matcher created by pattern returned by getPattern(), 
	 *	               already run on the message
	 * @param fromRoom whether or not this message come from a room
	 * @return 
	 */
	public abstract Optional<String> processMessage(
			String from, String message, Matcher matcher, boolean fromRoom);
	
	/**
	 * Called when all configuration properties have been set.
	 * @throws ca.caseybanner.chief.commands.ConfigurationException
	 */
	public void configurationComplete() throws ConfigurationException {
	
	};
	
	/**
	 * Returns true if this is an admin only command.
	 * 
	 * If this returns true, the command will only respond to JIDs listed in the admins list.
	 * This includes JIDs of rooms, so entire rooms can be admins if you want.
	 * 
	 * @return 
	 */
	public boolean isAdminOnly() {
		return false;
	};
	
}
