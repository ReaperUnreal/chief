package ca.caseybanner.chief;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by kcbanner on 7/24/2014.
 */
public class Chief {

	private static final Logger logger = LogManager.getLogger(Chief.class);
	private static final String PROPERTIES_FILENAME = "chief.properties";
	
    public static void main(String[] args) {
		
		logger.info("Chief is starting");
		
        Properties properties = new Properties();		
        try {			
            InputStream in = Chief.class.getResourceAsStream("/" + PROPERTIES_FILENAME);
            properties.load(in);
        } catch (IOException e) {
			logger.error("Error reading " + PROPERTIES_FILENAME, e);
            System.exit(1);
        }				
		
		Bot bot = new Bot(properties);
		bot.start();			
		bot.waitForExit();

		logger.info("Chief exiting");
		System.exit(0);
		
    }

}
