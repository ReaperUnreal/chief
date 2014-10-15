/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.caseybanner.chief.commands;

import ca.caseybanner.chief.Bot;
import ca.caseybanner.chief.Command;
import ca.caseybanner.chief.SynchronousCommand;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author kcbanner
 */
public class YouTubeCommand extends SynchronousCommand {

	private static final Logger logger = LogManager.getLogger(YouTubeCommand.class);

	private static final Pattern PATTERN = Pattern.compile("^youtube\\s+(.+)$");
	private final YouTube youtube;	
	private String apiKey;
	
	public YouTubeCommand(Bot bot) {
		super(bot);
		
		youtube = new YouTube.Builder(
				new NetHttpTransport(),
				new JacksonFactory(),
				(HttpRequest request) -> {})
			.setApplicationName("chief-bot")
			.build();
		
		apiKey = null;
		
	}

	@Override
	public void configurationComplete() throws ConfigurationException {

		if (apiKey == null) {
			throw new ConfigurationException("API key must be specified");
		}
		
	}	
	
	/**
	 * Setter for apiKey
	 * @param apiKey YouTube API key
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
	public Optional<String> processMessage(
			String from, String message, Matcher matcher, boolean fromRoom) {
		
		String query = matcher.group(1);				
		
		try {
			YouTube.Search.List search = youtube.search().list("id,snippet");
			search.setKey(this.apiKey);
			search.setQ(query);
			search.setType("video");
			
			search.setFields("items(id/kind,id/videoId,snippet/title,snippet)");
			search.setMaxResults(1L);		
			SearchListResponse searchResponse = search.execute();
			
			List<SearchResult> searchResultList = searchResponse.getItems();
			if (searchResultList != null) {
				SearchResult result = searchResultList.get(0);
				ResourceId id = result.getId();
				
				if (id.getKind().equals("youtube#video")) {
					return Optional.of("http://www.youtube.com/watch?v=" + id.getVideoId());
				}				
            } else {
				return Optional.of("Sorry, no results for `" + query + "`!");
            }						
		} catch (IOException ex) {
			logger.error("Error searching YouTube", ex);
		}
		
		return Optional.empty();
		
	}
	
}
