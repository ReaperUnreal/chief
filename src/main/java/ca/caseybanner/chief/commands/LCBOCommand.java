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
import com.google.api.client.util.Data;
import com.google.api.client.util.Key;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A command to interface with the LCBO API 
 * 
 * @author kcbanner
 */
public class LCBOCommand extends Command {

	private static final Logger logger = LogManager.getLogger(LCBOCommand.class);			
	
	private static final String BASE_URL = "http://lcboapi.com";
	private static final Pattern PATTERN = Pattern.compile(
			"^lcbo\\s+((?<picture>picture)|(?<taste>taste))?(?<query>.+)$");
	
	private static final int MAX_RESULTS = 3;
	
	private final JacksonFactory jsonFactory;
	private final HttpRequestFactory requestFactory;

	public static class LCBOPager {
		
		@Key
		Integer records_per_page;

		@Key		
		Integer total_record_count;

		@Key
		Integer current_page_record_count;
		
		@Key
		boolean is_first_page;
		
		@Key
		boolean is_final_page;
		
		@Key
		Integer current_page;
		
		@Key
		String current_page_path;
		
		@Key
		Integer next_page;
		
		@Key
		String next_page_path;
		
		@Key
		Integer previous_page;
				
		@Key
		String previous_page_path;
		
		@Key
		Integer total_pages;
		
		@Key
		String total_pages_path;	
	}
		
	public static class LCBOProduct {		
		
		@Key
		int id;
		
		@Key
		String name;
		
		@Key
		String origin;
		
		@Key("package")
		String packaging;
		
		@Key
		String producer_name;
		
		@Key
		String serving_suggestion;
		
		@Key
		String tasting_note;
		
		@Key
		String image_url;

		@Key
		String image_thumb_url;
		
		@Key
		Integer inventory_count;
		
		@Key
		Integer inventory_volume_in_milliliters;
				
		
		@Override
		public String toString() {
			
			StringBuilder builder = new StringBuilder(name);
			builder.append(":\n");
			
			if (! Data.isNull(origin))
				builder.append("  From: ").append(origin).append("\n");
			if (! Data.isNull(producer_name))
				builder.append("  Produced by: ").append(producer_name).append("\n");				
			if (! Data.isNull(packaging))
				builder.append("  Package: ").append(packaging).append("\n");

			if (! Data.isNull(inventory_count)) {
				builder.append("  Total Inventory: ").append(inventory_count).append(" units");
				
				if (! Data.isNull(inventory_volume_in_milliliters)) {
					builder.append(" (")
							.append(inventory_volume_in_milliliters / 1000)
							.append(" litres)");
				}
				
				builder.append("\n");
			}
			
			return builder.toString();
			
		}
		
		public boolean hasNotes() {
			return ! Data.isNull(tasting_note);
		}
		
		public boolean hasServingSuggestion() {
			return  ! Data.isNull(serving_suggestion);				
		}
		
	}
	
	public static class LCBOResponse {
		
		@Key
		int status;
		
		@Key	
		String message;
		
		@Key
		LCBOPager pager;
		
		@Key
		String suggestion;
		
		@Key("result")
		List<LCBOProduct> results;
		
	}
	
	public LCBOCommand(Bot bot) {
		
		super(bot);
		
		jsonFactory = new JacksonFactory();			
		requestFactory = new NetHttpTransport().createRequestFactory((HttpRequest request) -> {
			request.setParser(new JsonObjectParser(jsonFactory));
		});
		
	}
	
	@Override
	public String getUsage() {
		return "";
	}

	@Override
	public Pattern getPattern() {
		return PATTERN;
	}

	@Override
	public Optional<String> processMessage(String from, String message, Matcher matcher) {
		
		String query = matcher.group("query");
		
		boolean isTaste = matcher.group("taste") != null;
		boolean isPicture = matcher.group("picture") != null;
		
		GenericUrl url;
		
		try {
			url = new GenericUrl(BASE_URL + "/products" + "?q=" + URLEncoder.encode(query, "UTF-8"));

			HttpRequest request = requestFactory.buildGetRequest(url);
			HttpResponse response = request.execute();		
			
			LCBOResponse lcboResponse = response.parseAs(LCBOResponse.class);

			final StringBuilder builder = new StringBuilder();
			if (lcboResponse.results.isEmpty()) {
				builder.append("I couldn't find that.\n");
				
				if (lcboResponse.suggestion != null) {
					builder.append(" May I suggest trying \"")
							.append(lcboResponse.suggestion)
							.append("\"?");
				}				
			} else {				
				if (isTaste) {
					LCBOProduct product = lcboResponse.results.get(0);

					builder.append("Tasting notes for ").append(product.name).append(": ");

					if (product.hasNotes()) {						
						builder.append(product.tasting_note);
					} else {
						builder.append("none!");
					}
					
					if (product.hasServingSuggestion()) {
						builder.append("\nServing suggestions: ")
								.append(product.serving_suggestion);
					}
				} else if (isPicture) {
					LCBOProduct product = lcboResponse.results.get(0);
					
					if (! Data.isNull(product.image_url)) {						
						builder.append(product.image_url);
					} else if (! Data.isNull(product.image_thumb_url)) {						
						builder.append(product.image_thumb_url);
					} else {				
						builder.append("No picture for ").append(product.name);
					}
				} else {
					builder.append("Results");

					if (lcboResponse.results.size() > MAX_RESULTS) {
						builder.append(" (")
							.append(Integer.toString(lcboResponse.pager.total_record_count))
							.append(" total results, limiting to ")
							.append(Integer.toString(MAX_RESULTS))
							.append(")");
					}

					builder.append(":\n");				
					lcboResponse.results.stream().limit(MAX_RESULTS).forEach(product -> {				
						builder.append(product);
						builder.append("\n");
					});				
				}
								
			}

			return Optional.of(builder.toString());
		} catch (IOException ex) {
			logger.error("Error making API query: {}", query, ex);
		}
		
		return Optional.empty();
	}
	
}