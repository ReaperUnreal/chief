package ca.caseybanner.chief;

import ca.caseybanner.chief.commands.HelpCommmand;
import ca.caseybanner.chief.commands.QuitCommand;
import ca.caseybanner.chief.commands.YouTubeCommand;
import com.google.api.services.youtube.YouTube;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;

/**
 * Created by kcbanner on 7/24/2014.
 */
public class Bot implements ChatManagerListener, MessageListener {

	private static final Logger logger = LogManager.getLogger(Bot.class);
	
	private final Lock lock;
	private final Condition running;
	private final XMPPConnection connection;
	private final String username;
	private final String password;
	private final String conferenceHost;
	private final String nickname;	

	private final Pattern JID_PATTERN = Pattern.compile("^\\d+_");

	private final ConcurrentHashMap<String, Chat> chatsByParticpant;
	private final ConcurrentHashMap<String, MultiUserChat> multiUserChatsByRoom;
	
	private final List<Command> commands;
	private final Properties properties;

	/**
	 * Bot constructor
	 * 
	 * @param properties 
	 */
	public Bot(Properties properties) {
		
		this.properties = properties;
		
		// TODO: Throw errors when required properties are missing
		
		String host = properties.getProperty("host");
		int port = Integer.parseInt(properties.getProperty("port"));
		
		ConnectionConfiguration config = new ConnectionConfiguration(host, port);
		
		this.nickname = properties.getProperty("nickname", "Chief Bot");		
		this.username = properties.getProperty("username");
		this.password = properties.getProperty("password");	
		this.conferenceHost = properties.getProperty("conferenceHost");
		
		chatsByParticpant = new ConcurrentHashMap<>();
		multiUserChatsByRoom = new ConcurrentHashMap<>();		
		connection = new XMPPTCPConnection(config);		
		
		lock = new ReentrantLock();
		running = lock.newCondition();

		commands = new ArrayList<>();		
		
		// Add some default commands
		
		commands.add(new QuitCommand(this));
		commands.add(new HelpCommmand(this));
		
		// TODO: Load plugins based on properties file
		
		addCommand(YouTubeCommand::new);
		
		// TODO: Create some command processor thing
		// 1. Save quotes
		// 2. Watch for pull requests (allow people to subscribe)
		// 3. Get random gif (once per hour per user)
		
	}
	
	/**
	 * Constructs and adds a command
	 * 
	 * @param commandConstructor 
	 */
	private void addCommand(Function<Bot, Command> commandConstructor) {
		
		Command command = commandConstructor.apply(this);

		// Load any properties prefixed with this classname and apply them
		
		String typeName = command.getClass().getTypeName();	
		properties.stringPropertyNames().stream().filter(propertyName -> {
			
			// Filter properties that are prefixed with the full type name,
			// a period, and then at least one character
			
			return propertyName.indexOf(typeName) == 0 && 
					propertyName.length() > typeName.length() + 1 &&
					propertyName.charAt(typeName.length()) == '.';
		}).forEach(propertyName -> {			
			String fieldName = propertyName.substring(typeName.length() + 1);
			
			// Construct the name of the setter for this field
			
			StringBuilder setterNameBuilder = new StringBuilder("set");
			setterNameBuilder.append(fieldName.substring(0, 1).toUpperCase());				
			if (fieldName.length() > 1) {			
				setterNameBuilder.append(fieldName.substring(1));
			}
			
			String setterName = setterNameBuilder.toString();
			
			try {				
				
				// Call the setter with the property value
				
				Method method = command.getClass().getMethod(setterName, String.class);
				method.invoke(command, properties.getProperty(propertyName));
			} catch (NoSuchMethodException | SecurityException ex) {
				logger.error(
					"Could not find configuration setter {} on {}",
					setterName,
					command.getClass().getTypeName());				
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
				logger.error(
					"Error calling configuration setter {} on {}",
					setterName,
					command.getClass().getTypeName(),
					ex);				
			}
			
		});
				
				
		
	}
	
	/**
	 * Getter for the list of commands.
	 * 
	 * @return 
	 */
	public List<Command> getCommands() {
		return Collections.unmodifiableList(commands);
	}
	
	/**
	 * Connect to the server
	 */
	public void start() {		

		try {
			connection.connect();
			connection.login(username, password, "Chief Bot");
			
			logger.info("Bot online: connected to domain {}", connection.getConnectionID());
	
			// Roster setup
			
			Roster roster = connection.getRoster();
			roster.setSubscriptionMode(Roster.SubscriptionMode.accept_all);
			
			ChatManager chatManager = ChatManager.getInstanceFor(connection);
			chatManager.addChatListener(this);		
			
			joinRoom("test");
			
		} catch (XMPPException e) {
			throw new RuntimeException("XMPP Error", e);
		} catch (SmackException e) {
			throw new RuntimeException("Smack Error", e);
		} catch (IOException e) {
			throw new RuntimeException("IO Error", e);
		}
		
	}
	
	/**
	 * Waits until the bot exits before returning
	 */
	public void waitForExit() {

		lock.lock();
		try {		
			running.await();			
			connection.disconnect();
		} catch (InterruptedException | SmackException.NotConnectedException ex) {
			
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Tell the bot to shutdown. 
	 * This will cause the call to waitForExit to return.
	 */
	public void exit() {
		lock.lock();
		
		try {
			running.signal();
		} finally {
			lock.unlock();
		}
		
	}

	/**
	 * Returns if a name looks like a HipChat JID
	 * 
	 * @param name
	 * @return 
	 */
	private boolean isHipchatJID(String name) {
		Matcher matcher = JID_PATTERN.matcher(name);
		return matcher.matches();
	}
	
	/**
	 * Join a room
	 * 
	 * @param room
	 * @param nickname
	 * @return 
	 */
	private boolean joinRoom(String room) {
			
		if (multiUserChatsByRoom.containsKey(room)) {

			// Already joined this room

			return true;
		}

		// TODO: Save the fact that we joined this room, to rejoin it on connect
		
		// Join the room, request 0 lines of history
		
		MultiUserChat muc = new MultiUserChat(connection, room + "@" + conferenceHost);

		DiscussionHistory history = new DiscussionHistory();
		history.setMaxStanzas(0);

		try {		
			muc.join(nickname, "", history, SmackConfiguration.getDefaultPacketReplyTimeout());
			muc.addMessageListener((Packet packet) -> {				
				processRoomMessage(muc, packet);
			});
			
			multiUserChatsByRoom.put(room, muc);

			return true;
		} catch (XMPPException.XMPPErrorException |
				SmackException.NoResponseException |
				SmackException.NotConnectedException e) {
			logger.error("Error joining room", e);			
			return false;
		}
		
	}
	
	/**
	 * Called when a new chat is created with the bot
	 * 
	 * @param chat
	 * @param createdLocally 
	 */
	@Override
	public void chatCreated(Chat chat, boolean createdLocally) {
		
		logger.trace("Chat started with: {}", chat.getParticipant());
		chat.addMessageListener(this);
		
		chatsByParticpant.put(chat.getParticipant(), chat);
		
	}		
	
	/**
	 * Process a message and return an optional response
	 * 
	 * @param from
	 * @param message
	 * @return 
	 */
	public Optional<String> handleMessage(String from, Message message, boolean fromRoom) {
		
		// TODO: Do some processing on the user and pass metadata to commands
		
		String body = message.getBody();		
		for (Command command : commands) {
			
			// Return the result of the first matching command
			
			Matcher matcher = command.getPattern().matcher(body);
			if (matcher.matches()) {
				return command.processMessage(from, message.getBody(), matcher);
			}
		}
		
		// No commands matched, if this is a single user chat then help them out
		
		if (! fromRoom) {
			return Optional.of("I didn't understand that. Type `help` for usage information.");			
		}
		
		return Optional.empty();
		
	}
	
	/**
	 * Send a message to a chat
	 * 
	 * @param chat
	 * @param message 
	 */
	private void sendMessage(Chat chat, String message) {
		try {
			chat.sendMessage(message);
		} catch (XMPPException | SmackException.NotConnectedException ex) {
			logger.error("Error sending message to {}", chat.getParticipant(), ex);
		}
	}

	/**
	 * Send a message to a multi user chat
	 * 
	 * @param chat
	 * @param message 
	 */
	private void sendMessage(MultiUserChat chat, String message) {
		try {
			chat.sendMessage(message);
		} catch (XMPPException | SmackException.NotConnectedException ex) {
			logger.error("Error sending message to room {}", chat.getNickname(), ex);
		}
	}
	
	/**
	 *
	 * @param chat
	 * @param message
	 */
	@Override
	public void processMessage(Chat chat, Message message) {		

		if (message.getBody() != null) {		
			logger.trace("Message from `{}`: {}", chat.getParticipant(), message.getBody());
			Optional<String> response = handleMessage(chat.getParticipant(), message, false);

			response.ifPresent(responseString -> {
				sendMessage(chat, responseString);		
			});
		}
		
	}
	
	private void processRoomMessage(MultiUserChat chat, Packet packet) {
	
		if (packet instanceof Message) {
			Message message = (Message) packet;
			logger.trace("MUC Message {}: {}", message.getFrom(), message.getBody());	
			Optional<String> response = handleMessage(message.getFrom(), message, true);			
			
			response.ifPresent(responseString -> {
				sendMessage(chat, responseString);		
			});
		} else if (packet instanceof Presence) {
			Presence presence = (Presence) packet;
			logger.trace("MUC Presence {}: {}", chat.getRoom(), presence.getType().toString());						
		} else {
			logger.trace("MUC Packet:", packet.toXML());			
		}
		
	}

}
