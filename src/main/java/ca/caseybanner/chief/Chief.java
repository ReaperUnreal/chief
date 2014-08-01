package ca.caseybanner.chief;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by kcbanner on 7/24/2014.
 */
public class Chief {

	private static final Logger logger = LogManager.getLogger(Chief.class);
	
    public static void main(String[] args) {

		// TODO: Load a configuration file
		
		logger.info("Chief is starting");
		
		Bot bot = new Bot("Chief Bot", "bot", "test", "localhost", "conference.localhost", 5222);		
		bot.start();			
		bot.waitForExit();

		logger.info("Chief exiting");
		System.exit(0);
		
    }

}
