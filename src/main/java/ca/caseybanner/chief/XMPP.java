/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.caseybanner.chief;

/**
 * XMPP utilities
 * 
 * @author kcbanner
 */
class XMPP {
	
	/**
	 * Returns the JID without the resource portion
	 * 
	 * @param jid input JID
	 * @return JID with the trailing resource portion removed
	 */
	public static String getPlainJID(String jid) {
		
		// The spec says the only / can be after the domain
		
		return jid.split("/")[0];
		
	}
	
}
