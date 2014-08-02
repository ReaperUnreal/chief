/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.caseybanner.chief;

/**
 *
 * @author kcbanner
 */
public class XMPP {
	
	/**
	 * Returns the JID without the resource portion
	 * 
	 * @param jid
	 * @return 
	 */
	public static String getPlainJID(String jid) {
		
		// The spec says the only / can be after the domain
		
		return jid.split("/")[0];
		
	}
	
}
