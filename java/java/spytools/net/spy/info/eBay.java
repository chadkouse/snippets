// Copyright (c) 2000  Dustin Sallings <dustin@spy.net>
//
// $Id: eBay.java,v 1.1 2000/04/03 06:00:46 dustin Exp $

package net.spy.info;

import java.util.*;

import net.spy.*;
import net.spy.net.*;

/**
 * Get eBay info.
 */

public class eBay extends Info {

	String item_number=null;

	/**
	 * Get an eBay object.
	 *
	 * @param item_number the item number to look up
	 */
	public eBay(String item_number) {
		super();
		this.item_number = item_number;
	}

	public String toString() {
		String ret="";
		try {
			parseInfo();

			if(!error) {
				ret+=(String)hinfo.get("Description") + "\r\n"
					+ (String)hinfo.get("Currently") + "\r\n"
					+ (String)hinfo.get("High bid") + "\r\n"
					+ (String)hinfo.get("Time left");
			}
		} catch(Exception e) {
			// Who cares.
		}
		return(ret);
	}

	protected void parseInfo() throws Exception {
		if(hinfo==null) {
			hinfo=new Hashtable();
			hinfo.put("item_number", item_number);
			getInfo();
			String lines[]=SpyUtil.split("\n", info);
			int section=0;
			for(int i=0; i<lines.length; i++) {
				if(lines[i].startsWith("Currently")) {
					i++;
					hinfo.put("Currently", lines[i]);
					section++;
				} else if(lines[i].startsWith("Time left")) {
					i++;
					hinfo.put("Time left", lines[i]);
					section++;
				} else if(lines[i].startsWith("High bid")) {
					i++;
					hinfo.put("High bid", lines[i]);
					section++;
				} else if(lines[i].startsWith("Bidding")) {
					i++;
					hinfo.put("Description", lines[i]);
					section++;
				}
			}
			if(section==0) {
				String error_string="Unable to get eBay info.  "
					+ "Invalid item number?";
				hinfo.put("ERROR", error_string);
			} else {
				error=false;
			}
		} // if there's a need to find it at all.
	}

	protected void getInfo() throws Exception {
		if(info==null) {
			String url=
				"http://cgi.ebay.com/aw-cgi/eBayISAPI.dll?ViewItem&item=";
			url += item_number;
			hinfo.put("URL", url);
			HTTPFetch f = new HTTPFetch(url);
			info=f.getStrippedData();
		}
	}

	public static void main(String args[]) throws Exception {
		eBay e = new eBay(args[0]);
		System.out.println("Info:\n" + e);
		System.out.println("Info (XML):\n" + e.toXML());
	}
}