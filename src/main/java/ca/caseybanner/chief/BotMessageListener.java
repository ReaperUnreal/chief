/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.caseybanner.chief;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.packet.Message;

/**
 * Bot message handler
 * 
 * @author kcbanner
 */
public class BotMessageListener implements MessageListener {

	private static final Logger logger = LogManager.getLogger(BotMessageListener.class);	
	private final Bot bot;
	
	public BotMessageListener(Bot bot) {
		this.bot = bot;
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
			bot.exit();
		}
		
	}
	
}
