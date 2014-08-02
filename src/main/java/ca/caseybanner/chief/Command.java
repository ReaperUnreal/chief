/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.caseybanner.chief;

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
	 * @param message
	 * @param matcher
	 * @return 
	 */
	public abstract Optional<String> processMessage(String from, String message, Matcher matcher);
	
}
