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
			"^lcbo\\s+((?<picture>picture)|(?<store>store)|(?<taste>taste))?(?<query>.+)$");
	
	private static final Pattern STORE_QUERY_PATTERN = Pattern.compile(
			"\\s+at\\s+(?<storequery>.*)$");
	
	private static final int MAX_RESULTS = 1;
	
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
				
		@Key
		String primary_category;
		
		@Key
		String secondary_category;
		
		@Key
		Integer alcohol_content;
		
		@Key
		Integer price_in_cents;
		
		@Key
		Integer regular_price_in_cents;

		@Key
		String style;
				
		@Override
		public String toString() {
			
			StringBuilder builder = new StringBuilder(name);
			builder.append(":\n");
			
			if (! Data.isNull(origin))
				builder.append("  From: ").append(origin).append("\n");
			
			if (! Data.isNull(primary_category)) {
				builder.append("  Style: ").append(primary_category);
				
				if (! Data.isNull(secondary_category)) {
					builder.append(" - ").append(secondary_category);
				}
				
				if (! Data.isNull(style)) {
					builder.append(" - ").append(style);
				}
				
				builder.append("\n");
			}				
			
			if (! Data.isNull(alcohol_content)) {
				builder.append("  Alchohol content: ")
						.append((float) alcohol_content / 100.0)
						.append("%\n");
			}
		
			if (! Data.isNull(packaging))
				builder.append("  Package: ").append(packaging).append("\n");			
			
			if (! Data.isNull(price_in_cents)) {
				builder.append("  Price: $").append(price_in_cents / 100);
				
				if (! Data.isNull(regular_price_in_cents) &&
						regular_price_in_cents.compareTo(price_in_cents) != 0) {
					
					builder.append(" (regular $")
							.append(regular_price_in_cents / 100)
							.append(")");
				}
				
				builder.append("\n");
			}
			
			if (! Data.isNull(producer_name))
				builder.append("  Produced by: ").append(producer_name).append("\n");				
			

			if (! Data.isNull(inventory_count)) {
				builder.append("  Total Inventory: ").append(inventory_count).append(" units");
				
				if (! Data.isNull(inventory_volume_in_milliliters)) {
					builder.append(" (")
							.append(inventory_volume_in_milliliters / 1000)
							.append(" litres)");
				}
				
				builder.append("\n");
			}
			
			builder.append("Store link: ")
					.append("http://www.lcbo.com/lcbo/search?searchTerm=")
					.append(id);
			
			return builder.toString();			
		}
		
		public boolean hasNotes() {
			return ! Data.isNull(tasting_note);
		}
		
		public boolean hasServingSuggestion() {
			return  ! Data.isNull(serving_suggestion);				
		}
		
	}
	
	public static class LCBOProductResponse {
		
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

	public static class LCBOStore {
	
		@Key
		String name;
		
		@Key
		String address_line_1;
		
		@Key
		String address_line_2;
		
		@Key
		String city;
		
		@Key
		String telephone;
		
		@Key
		Integer quantity;
		
	}
	
	public static class LCBOStoreResponse {
		
		@Key
		int status;
		
		@Key	
		String message;
		
		@Key
		LCBOPager pager;
		
		@Key
		LCBOProduct product;
		
		@Key("result")
		List<LCBOStore> results;
		
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
		return "lcbo <query> - search for <query>\n" +
				"lcbo picture <query> - show a picture of <query>\n" +
				"lcbo taste <query> - tasting notes for <query>\n" +				
				"lcbo store <query> [at <storequery>] - search stores that have <query>. Optionally narrow down the stores with <storequery>.";		
	}

	@Override
	public Pattern getPattern() {
		return PATTERN;
	}

	private Optional<LCBOProductResponse> findProduct(String query) {
		
		GenericUrl url;
		
		try {
			url = new GenericUrl(BASE_URL + "/products" + "?q=" + URLEncoder.encode(query, "UTF-8"));

			HttpRequest request = requestFactory.buildGetRequest(url);
			HttpResponse response = request.execute();		
			
			return Optional.of(response.parseAs(LCBOProductResponse.class));
		} catch (IOException ex) {
			logger.error("Error making API query: {}", query, ex);
		}
		
		return Optional.empty();
		
	}
	
	/**
	 * Process a message for querying products
	 * 
	 * @param query
	 * @param isTaste
	 * @param isPicture
	 * @return 
	 */
	private Optional<String> processProductMessage(String query, boolean isTaste, boolean isPicture) {
			
		Optional<LCBOProductResponse> optionalLcboResponse = findProduct(query);

		if (! optionalLcboResponse.isPresent()) {
			return Optional.empty();
		}
			
		final StringBuilder builder = new StringBuilder();
		LCBOProductResponse lcboResponse = optionalLcboResponse.get();
		
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
				if (lcboResponse.results.size() > MAX_RESULTS) {
					builder.append("Results (")
						.append(Integer.toString(lcboResponse.pager.total_record_count))
						.append(" total results, limiting to ")
						.append(Integer.toString(MAX_RESULTS))
						.append("):\n");
				}

				lcboResponse.results.stream().limit(MAX_RESULTS).forEach(product -> {				
					builder.append(product);
				});				
			}

		}

		return Optional.of(builder.toString());
	}
	
	/**
	 * Process a message for querying stores
	 * 
	 * @param query
	 * @return 
	 */
	private Optional<String> processStoreMessage(String query) {
		
		// Split up the query (if it included the at)
		
		Matcher queryMatcher = STORE_QUERY_PATTERN.matcher(query);
		
		String storeQuery = null;
		if (queryMatcher.find()) {
			storeQuery = queryMatcher.group("storequery");
			query = query.substring(0, queryMatcher.start());
		}
		
		logger.trace("Split {}:{}", query, storeQuery);
		
		Optional<LCBOProductResponse> optionalLcboResponse = findProduct(query);

		if (! optionalLcboResponse.isPresent()) {
			return Optional.empty();
		}
		
		final StringBuilder builder = new StringBuilder();
		LCBOProductResponse lcboResponse = optionalLcboResponse.get();
		
		if (lcboResponse.results.isEmpty()) {
			builder.append("I couldn't find that.\n");
		} else {
			LCBOProduct product = lcboResponse.results.get(0);
			
			GenericUrl url;

			try {
				String urlString = BASE_URL +
						"/products/" + product.id +
						"/stores";

				if (storeQuery != null) {
					urlString += "?q=" + URLEncoder.encode(storeQuery, "UTF-8") 
							+ "&order=products_count.desc";
				}
				
				url = new GenericUrl(urlString);
				
				HttpRequest request = requestFactory.buildGetRequest(url);
				HttpResponse response = request.execute();		

				LCBOStoreResponse storeResponse = response.parseAs(LCBOStoreResponse.class);

				if (storeResponse.results.isEmpty()) {
					builder.append("No matching stores found.");					
				} else {
					
					
					builder.append("Stores with ").append(product.name).append(":");
					
					storeResponse.results.stream().sorted((a, b) -> {
						return b.quantity - a.quantity;
					}).forEach(store -> {
						builder.append("\nStore: ")
							.append(store.name)
							.append(" at ")
							.append(store.address_line_1)
							.append(", ")
							.append(store.address_line_2)
							.append(", ")
							.append(store.city)
							.append(" has ")
							.append(store.quantity);
					});
				}
			} catch (IOException ex) {
				logger.error("Error making API query: {}", query, ex);
			}				
			
		}			
			
		return Optional.of(builder.toString());		
	}	
	
	@Override
	public Optional<String> processMessage(
			String from, String message, Matcher matcher, boolean fromRoom) {
		
		String query = matcher.group("query");		
		boolean isTaste = matcher.group("taste") != null;
		boolean isPicture = matcher.group("picture") != null;
		boolean isStore = matcher.group("store") != null;
		
		if (isStore) {
			return processStoreMessage(query);
		} else {
			return processProductMessage(query, isTaste, isPicture);		
		}
		
	}
	
}
