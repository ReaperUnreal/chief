/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.caseybanner.chief.commands;

import ca.caseybanner.chief.Bot;
import ca.caseybanner.chief.Command;
import com.google.api.services.youtube.YouTube;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author kcbanner
 */
public class YouTubeCommand extends Command {

	private static final Pattern PATTERN = Pattern.compile("^youtube\\s+(.+)$");

	private YouTube youtube;		
	private String apiKey;
	
	public YouTubeCommand(Bot bot) {
		super(bot);
	}

	/**
	 * Setter for apiKey
	 * @param apiKey 
	 */
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}	
	
	@Override
	public String getUsage() {
		return "youtube <query> - searches youtube and returns the first result for <query>";
	}

	@Override
	public Pattern getPattern() {
		return PATTERN;
	}

	@Override
	public Optional<String> processMessage(String from, String message, Matcher matcher) {
		
		String query = matcher.group(1);
		
		
		
		return Optional.empty();
		
	}
	
}
