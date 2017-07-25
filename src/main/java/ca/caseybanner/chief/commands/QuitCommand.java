/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.caseybanner.chief.commands;

import ca.caseybanner.chief.Bot;
import ca.caseybanner.chief.SynchronousCommand;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author kcbanner
 */
public class QuitCommand extends SynchronousCommand {

	private static final Pattern PATTERN = Pattern.compile("^quit$");

	public QuitCommand(Bot bot) {
		super(bot);
	}

	@Override
	public String getUsage() {
		return "quit - causes the bot to shutdown";
	}

	@Override
	public Pattern getPattern() {
		return PATTERN;
	}

	@Override
	public Optional<String> processMessage(
			String from, String message, Matcher matcher, boolean fromRoom) {

		getBot().exit();
		return Optional.empty();
	}

	@Override
	public boolean isAdminOnly() {
		return true;
	}

	@Override
	public void configurationComplete() throws ConfigurationException {
	}

}
