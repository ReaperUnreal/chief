package ca.caseybanner.chief;

import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

/**
 * Created by kcbanner on 7/24/2014.
 */
public class Bot implements ChatManagerListener {

	private static final Logger logger = LogManager.getLogger(Bot.class);
	
	private final Lock lock;
	private final Condition running;
	private final XMPPConnection connection;
	private final String username;
	private final String password;
	
	/**
	 * Bot constructor
	 * 
	 * @param username
	 * @param password
	 * @param host
	 * @param port
	 */
    public Bot(String username, String password, String host, int port) {

        ConnectionConfiguration config = new ConnectionConfiguration(host, port);

		this.username = username;
		this.password = password;
		
		connection = new XMPPTCPConnection(config);               
		lock = new ReentrantLock();
		running = lock.newCondition();
		
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
	 * Connect to the server
	 */
	public void start() {		

		try {
			connection.connect();
			connection.login(username, password, "Chief Bot");
			
			logger.info("Bot online: connected to domain {}", connection.getConnectionID());
			
			Roster roster = connection.getRoster();
			roster.setSubscriptionMode(Roster.SubscriptionMode.accept_all);
			
			ChatManager chatManager = ChatManager.getInstanceFor(connection);
			chatManager.addChatListener(this);
			
			// Join the rooms we are configured for
			
			/*			
			MultiUserChat muc = new MultiUserChat(connection, "test@conference.localhost");
			muc.join("Chief Bot");
			
			muc.addMessageListener(new PacketListener() {
				public void processPacket(Packet packet) throws SmackException.NotConnectedException {
					logger.trace("MUC Packet from: {}", packet.getFrom());
				}				
			});			
			*/
			
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
		} catch (InterruptedException ex) {
			
		} catch (SmackException.NotConnectedException ex) {
			
		} finally {
			lock.unlock();
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
		chat.addMessageListener(new BotMessageListener(this));
		
	}

}
