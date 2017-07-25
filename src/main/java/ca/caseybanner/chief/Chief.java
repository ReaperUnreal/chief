package ca.caseybanner.chief;

import ca.caseybanner.chief.commands.HelpCommand;
import ca.caseybanner.chief.commands.QuitCommand;
import ca.caseybanner.chief.util.DynamicURLClassLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;

/**
 * The main entry point for the Chief bot
 * <p/>
 * Created by kcbanner on 7/24/2014.
 */
class Chief {

	private static final Logger logger = LogManager.getLogger(Chief.class);
	private static final String PROPERTIES_FILENAME = "config/chief.properties";
	
	private static List<Function<HipChatBot, Command>> loadCommands(Properties properties) {
		List<Function<HipChatBot, Command>> commandConstructors = new ArrayList<>();
		
		// Load external jars
		URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		DynamicURLClassLoader classLoader = new DynamicURLClassLoader(urlClassLoader);
		Arrays.stream(properties.getProperty("resources", "").split("\\s*,\\s*"))
				.filter(path -> !path.isEmpty()).forEach(path -> {
			try {
				File resourceFile = new File(path);
				logger.trace(resourceFile.getAbsolutePath());
				classLoader.addURL(resourceFile.toURI().toURL());
			} catch (MalformedURLException e) {
				logger.error("Invalid resource path: " + path, e);
			}
		});
		
		// add the default commands
		commandConstructors.add(HelpCommand::new);
		commandConstructors.add(QuitCommand::new);
		String[] commandClassnames = properties.getProperty("commands", "").split("\\s*,\\s*");
		Arrays.stream(commandClassnames)
				.filter(classname -> !classname.isEmpty()).forEach(classname -> {
			try {
				@SuppressWarnings("unchecked")
				Class<? extends Command> clazz = (Class<? extends Command>)classLoader.loadClass(classname);
				Constructor<? extends Command> cons = clazz.getConstructor(HipChatBot.class);
				commandConstructors.add(bot -> {
					try {
						return cons.newInstance(bot);
					} catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
						logger.error("Error creating command " + classname, e);
						return null;
					}
				});
				logger.info("Added command " + classname);
			} catch (ClassNotFoundException e) {
				logger.error("Cannot find class " + classname, e);
			} catch (NoSuchMethodException e) {
				logger.error("Failed to find the correct method in command " + classname, e);
			} catch (SecurityException e) {
				logger.error("Security exception creating " + classname, e);
			}
		});
		
		return commandConstructors;
	}

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
		
		List<Function<HipChatBot, Command>> commands = loadCommands(properties);

		if (HipChatBot.hasRequiredProperties(properties)) {
			logger.info("Creating HipChat Bot");
			HipChatBot hipChatBot = new HipChatBot(properties);
			commands.forEach(hipChatBot::addCommand);
			hipChatBot.start();
			hipChatBot.waitForExit();
		} else {
			logger.info("Cannot create HipChat Bot, properties not present.");
		}

		logger.info("Chief exiting");
		System.exit(0);

	}

}
