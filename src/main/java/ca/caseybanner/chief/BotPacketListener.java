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

/**
 *
 * @author kcbanner
 */
public class BotPacketListener implements PacketListener {

	private static final Logger logger = LogManager.getLogger(BotPacketListener.class);
	
	/**
	 * 
	 * @param packet
	 * @throws org.jivesoftware.smack.SmackException.NotConnectedException 
	 */
	@Override
	public void processPacket(Packet packet) throws SmackException.NotConnectedException {
		
		if (packet instanceof Message) {
			Message message = (Message) packet;
			logger.trace("MUC Message: {}: {}", message.getFrom(), message.getBody());
		} else {
			logger.trace("MUC Packet:", packet.toXML());
		}
	
	}
	
}
