// Copyright (c) 1999 Dustin Sallings <dustin@spy.net>
// $Id: Example.java,v 1.2 1999/09/15 08:00:56 dustin Exp $

import java.util.*;
import java.lang.*;

public class Example {
	public static void main(String args[]) {
		RHash r = new RHash("//dhcp-104/RObjectServer");
		Hashtable h;

		h = (Hashtable)r.get("myhash");
		// If we didn't get one, make one...
		if(h == null) {
			h = new Hashtable();
		}
		if(args.length > 1) {
			h.put(args[0], args[1]);
			r.put("myhash", h);
		}

		System.out.println(h.toString());

		// Make sure everything's good to go.
		System.runFinalization();

		// Dump the list of threads that are running before we exit.
		Thread.currentThread().getThreadGroup().getParent().list();

		System.exit(0);
	}
}
