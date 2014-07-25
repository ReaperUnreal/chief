/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.caseybanner.chief;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.muc.MultiUserChat;

/**
 *
 * @author kcbanner
 */
public class BotRoomListener implements PacketListener {

	private static final Logger logger = LogManager.getLogger(BotRoomListener.class);

	private final Bot bot;
	private final MultiUserChat chat;
	
	public BotRoomListener(Bot bot, MultiUserChat chat) {
		this.bot = bot;
		this.chat = chat;
	}
	
	/**
	 * 
	 * @param packet
	 * @throws org.jivesoftware.smack.SmackException.NotConnectedException 
	 */
	@Override
	public void processPacket(Packet packet) throws SmackException.NotConnectedException {
		
		if (packet instanceof Message) {
			Message message = (Message) packet;
			logger.trace("MUC Message {}: {}: {}", chat.getRoom(), message.getFrom(), message.getBody());
		} else {
			logger.trace("MUC Packet:", packet.toXML());
		}
	
	}
	
}
