package ca.caseybanner.chief;

import ca.caseybanner.chief.commands.ConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The main entry point for the Chief bot
 *
 * Created by kcbanner on 7/24/2014.
 */
class Chief {

	private static final Logger logger = LogManager.getLogger(Chief.class);
	private static final String PROPERTIES_FILENAME = "config/chief.properties";
	
    public static void main(String[] args) {
		
		logger.info("Chief is starting");
		
        Properties properties = new Properties();		
        try {
            InputStream in = new FileInputStream(new File(PROPERTIES_FILENAME));
            properties.load(in);
        } catch (IOException e) {
			logger.error("Error reading " + PROPERTIES_FILENAME, e);
            System.exit(1);
        }				
		
		try {
			Bot bot = new Bot(properties);
			bot.start();
			bot.waitForExit();
		} catch (ConfigurationException e) {
			logger.error("Configuration error: {}", e.getMessage());
            System.exit(1);
		}

		logger.info("Chief exiting");
		System.exit(0);
		
    }

}
