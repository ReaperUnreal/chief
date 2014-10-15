package ca.caseybanner.chief.commands;

import ca.caseybanner.chief.Bot;
import ca.caseybanner.chief.Command;
import ca.caseybanner.chief.SynchronousCommand;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.UrlEncodedContent;
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
public class MemeCommand extends SynchronousCommand {

	private static final Logger logger = LogManager.getLogger(MemeCommand.class);
	
	private static final Pattern PATTERN = Pattern.compile(
			"^meme\\s+(?<command>plz|list)((?<args>(\\s*\"[\\w\\s]*\")*.*))?$");
	private static final Pattern ARG_PATTERN = Pattern.compile("(?:\"(?<arg>[^\"]*)\")+");
	
	private static final String BASE_URL = "https://api.imgflip.com/";
	private static final String LIST_URL = "get_memes";
	private static final String IMAGE_URL = "caption_image";	
	
	private final JacksonFactory jsonFactory;
	private final HttpRequestFactory requestFactory;	
	
	private String username;
	private String password;
	
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
	 * Response from the caption_image endpoint
	 */
	public static class CaptionRequest {
	
		@Key
		final String template_id;
		
		@Key
		final String username;
		
		@Key
		final String password;
		
		@Key
		final String text0;

		@Key
		final String text1;

		public CaptionRequest(
				String template_id, String username, String password, String text0, String text1) {
			this.template_id = template_id;
			this.username = username;
			this.password = password;
			this.text0 = text0;
			this.text1 = text1;
		}
		
	}
	
	public static class CaptionResponse {
		
		@Key
		boolean success;	
		
		@Key
		CaptionData data;
		
		@Key
		String error_message;
		
	}
	
	public static class CaptionData {
	
		@Key
		String url;				
		
		@Key
		String page_url;
	
	}
	
	/**
	 * Constructor
	 * @see ca.caseybanner.chief.Command
	 */
	public MemeCommand(Bot bot) {
		super(bot);
		
		jsonFactory = new JacksonFactory();			
		requestFactory = new NetHttpTransport().createRequestFactory(
				(HttpRequest request) -> request.setParser(new JsonObjectParser(jsonFactory)));

		memes = null;
	}	
	
	/**
	 * Setter for password
	 * 
	 * @param password plaintext password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Setter for username
	 * 
	 * @param username plaintext username
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	@Override
	public void configurationComplete() throws ConfigurationException {

		if (password == null || username == null) {
			throw new ConfigurationException("Both username and password must be specified");
		}
		
	}
	
	@Override
	public String getUsage() {
		return "meme list [query] - list memes, optional filter on [query]\n" +
				"meme plz \"<name>\" \"<top line>\" \"<bottom line>\" - generate the meme that matches <name>, with <top line> and <bottom line> text\n";
		
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
	
	/**
	 * Generate a meme.
	 * 
	 * @param topText the text to appear on the top of the image
	 * @param bottomText the text to appear on the bottom of the image
	 * @return meme URL
	 */
	private Optional<String> generateMeme(String templateId, String topText, String bottomText) {
		
		logger.info("Generating meme: \"{}\" \"{}\" \"{}\"", templateId, topText, bottomText);
		
		GenericUrl url;
		
		try {
			url = new GenericUrl(BASE_URL + IMAGE_URL);

			HttpContent content = new UrlEncodedContent(
					new CaptionRequest(templateId, username, password, topText, bottomText));
			
			HttpRequest request = requestFactory.buildPostRequest(url, content);			
			HttpResponse response = request.execute();		

			CaptionResponse captionResponse = response.parseAs(CaptionResponse.class);
			
			if (captionResponse.success) {
				return Optional.of(captionResponse.data.url);
			} else {
				logger.error("Error making API query: {}", captionResponse.error_message);				
				return Optional.empty();						
			}
		} catch (IOException e) {
			logger.error("Error making API query: {}", e);
		}
		
		return Optional.empty();						
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
				return Optional.of("Send that command to me in a PM instead");
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
			memes.stream().filter(
				meme -> ! memeQuery.isPresent() || meme.name.toLowerCase().contains(memeQuery.get()))
					.forEach(meme -> builder.append(meme.name).append("\n"));

			return Optional.of(builder.toString());
		} else if (command.equals("plz")) {
			
			// Parse the args
			
			String args = matcher.group("args");
			if (args == null) {
				return Optional.of("Please provide at least a meme name");
			}
			
			Matcher argMatcher = ARG_PATTERN.matcher(args);
			
			String memeName;
			String topText = "";
			String bottomText = "";	
			
			// Get the meme name
			
			if (argMatcher.find()) {
				memeName = argMatcher.group("arg");
				
				// Get the top text

				if (argMatcher.find()) {
					topText = argMatcher.group("arg");
					
					// Get the bottom text

					if (argMatcher.find()) {
						bottomText = argMatcher.group("arg");
					}
				}
			} else {
				return Optional.of("Please provide at least a meme name");				
			}			
			
			if (topText.length() == 0 && bottomText.length() == 0) {
				return Optional.of("Please specify at least top or bottom text");
			}
			
			// Search through all the memes
			
			for (Meme meme : memes) {
				if (meme.name.equals(memeName)) {
					return generateMeme(meme.id, topText, bottomText);
				}
			}
			
			return Optional.of("I couldn't find that meme, please specify it " +
					"exactly as `meme list` returned it");
			
		}
		
		return Optional.empty();		
	}
	
	
	
}
