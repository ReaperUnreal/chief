/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.caseybanner.chief.commands;

import ca.caseybanner.chief.Bot;
import ca.caseybanner.chief.Command;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 *
 * @author kcbanner
 */
public class HelpCommmand extends Command {
	
	private static final Pattern PATTERN = Pattern.compile("^help");
	private static final String USAGE_HEADER = "Chief Bot Usage:\n";
	
	public HelpCommmand(Bot bot) {
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
	public Optional<String> processMessage(String from, String message) {
		final StringBuilder builder = new StringBuilder(USAGE_HEADER);		
		
		getBot().getCommands().stream().forEach(command -> {
			builder.append(command.getUsage());
			builder.append("\n");
		});
		
		return Optional.of(builder.toString());
	}
	
	
}
