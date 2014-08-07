/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.caseybanner.chief.commands;

import ca.caseybanner.chief.Bot;
import ca.caseybanner.chief.Command;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Key;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A command that talks to the Imgflip API to generate memes
 * 
 * @author kcbanner
 */
public class MemeCommand extends Command {

	private static final Logger logger = LogManager.getLogger(MemeCommand.class);
	
	private static final Pattern PATTERN = Pattern.compile(
			"^meme\\s+(?<command>plz|list)((?<args>(\\s*\"[\\w\\s]*\")*.*))?$");
	private static final String BASE_URL = "https://api.imgflip.com/";
	private static final String LIST_URL = "get_memes";
	private static final String IMAGE_URL = "caption_image";	
	
	private final JacksonFactory jsonFactory;
	private final HttpRequestFactory requestFactory;	
	
	/**
	 * Cached meme list
	 */
	private List<Meme> memes;
	
	/**
	 * Response from the get_memes endpoint
	 */
	public static class ListResponse {
			
		@Key
		boolean success;	
		
		@Key
		ListData data;
		
		@Key
		String error_message;
		
	}
	
	public static class ListData {
	
		@Key
		List<Meme> memes;
		
	}
	
	public static class Meme {
		
		@Key
		String id;
		
		@Key
		String name;
		
		@Key
		String url;
		
		@Key
		Integer width;
		
		@Key
		Integer height;
		
	}
	
	/**
	 * Constructor
	 * 
	 * @param bot 
	 */
	public MemeCommand(Bot bot) {
		super(bot);
		
		jsonFactory = new JacksonFactory();			
		requestFactory = new NetHttpTransport().createRequestFactory((HttpRequest request) -> {
			request.setParser(new JsonObjectParser(jsonFactory));
		});
			
		memes = null;
		
	}	
	
	@Override
	public String getUsage() {
		return "meme list [query] - list memes, optional filter on [query]\n" +
				"meme plz \"<name>\" \"[top line]\" \"[bottom line]\" - generate the meme that matches <name>, with [top line] and [bottom line] text\n";
		
	}

	@Override
	public Pattern getPattern() {
		return PATTERN;
	}
	
	/**
	 * Loads the meme list from imgflip.
	 */
	private boolean loadMemeList() {
		
		GenericUrl url;
		
		try {
			url = new GenericUrl(BASE_URL + LIST_URL);
			
			HttpRequest request = requestFactory.buildGetRequest(url);
			HttpResponse response = request.execute();		

			ListResponse listResponse = response.parseAs(ListResponse.class);
			
			if (listResponse.success) {
				memes = listResponse.data.memes;
				logger.trace("{} memes loaded", memes.size());
				return true;
			} else {
				logger.error("Error making API query: {}", listResponse.error_message);				
				return false;
			}
		} catch (IOException e) {
			logger.error("Error making API query: {}", e);
		}
		
		return false;
	}
	
	@Override
	public Optional<String> processMessage(
			String from, String message, Matcher matcher, boolean fromRoom) {

		String command = matcher.group("command");
		
		// If we don't have the meme list, get it first
		
		if (memes == null) {
			if (! loadMemeList()) {
				return Optional.of("Couldn't load meme list, please check error logs");
			}
		}
		
		if (command.equals("list")) {
			
			// The list can be big, so don't spam rooms
			
			if (fromRoom) {
				return Optional.of("send that command to me in a PM instead");
			}

			// Check for an optional query

			final Optional<String> memeQuery;
			if (matcher.group("args") != null) {				
				logger.trace("Args was \"{}\"", matcher.group("args"));	
				memeQuery = Optional.of(matcher.group("args").trim().toLowerCase());
			} else {
				memeQuery = Optional.empty();
			}
			
			StringBuilder builder = new StringBuilder("Found these memes:\n");
			memes.stream().filter(meme -> {

				// Filter the matches, if there was a query
				
				return ! memeQuery.isPresent() ||
						(memeQuery.isPresent() &&
						meme.name.toLowerCase().contains(memeQuery.get()));				
			}).forEach(meme -> {
				builder.append(meme.name)
						.append("\n");
			});

			return Optional.of(builder.toString());
		} else if (command.equals("plz")) {
			
			
			
		}
		
		return Optional.empty();		
	}
	
	
	
}
