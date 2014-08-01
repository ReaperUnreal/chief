package ca.caseybanner.chief;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
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

	/**
	 * Bot constructor
	 * 
	 * @param nickname
	 * @param username
	 * @param password
	 * @param host
	 * @param conferenceHost
	 * @param port
	 */
	public Bot(String nickname, String username, String password, String host, String conferenceHost, int port) {
		
		ConnectionConfiguration config = new ConnectionConfiguration(host, port);
		
		this.nickname = nickname;
		this.username = username;
		this.password = password;
		this.conferenceHost = conferenceHost;
		
		chatsByParticpant = new ConcurrentHashMap<>();
		multiUserChatsByRoom = new ConcurrentHashMap<>();		
		connection = new XMPPTCPConnection(config);
		
		lock = new ReentrantLock();
		running = lock.newCondition();
		
		// TODO: Create some command processor thing
		// 1. Save quotes
		// 2. Watch for pull requests (allow people to subscribe)
		// 3. Get random gif (once per hour per user)		
		
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
	 *
	 * @param chat
	 * @param message
	 */
	@Override
	public void processMessage(Chat chat, Message message) {		
		logger.trace("Message from `{}`: {}", chat.getParticipant(), message.getBody());
		
		// TODO: Pass actions 
		
		if (message.getBody().equals("!exit")) {
			exit();
		}
		
	}
	
	private void processRoomMessage(MultiUserChat chat, Packet packet) {
	
		if (packet instanceof Message) {
			Message message = (Message) packet;
			logger.trace("MUC Message {}: {}", message.getFrom(), message.getBody());			
		} else if (packet instanceof Presence) {
			Presence presence = (Presence) packet;
			logger.trace("MUC Presence {}: {}", chat.getRoom(), presence.getType().toString());						
		} else {
			logger.trace("MUC Packet:", packet.toXML());			
		}
		
	}

}
