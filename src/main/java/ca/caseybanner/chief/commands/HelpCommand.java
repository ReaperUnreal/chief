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
public class HelpCommand extends SynchronousCommand {

	private static final Pattern PATTERN = Pattern.compile("^help");
	private static final String USAGE_HEADER = "Chief Bot Usage:\n";

	public HelpCommand(Bot bot) {
		super(bot);
	}

	@Override
	public String getUsage() {
		return "help - displays usage information";
	}

	@Override
	public Pattern getPattern() {
		return PATTERN;
	}

	@Override
	public Optional<String> processMessage(
			String from, String message, Matcher matcher, boolean fromRoom) {
		final StringBuilder builder = new StringBuilder(USAGE_HEADER);

		getBot().getCommands().stream().forEach(command -> {
			builder.append(command.getUsage());
			builder.append("\n");
		});

		return Optional.of(builder.toString());
	}


}
