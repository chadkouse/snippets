// Copyright (c) 2000  Dustin Sallings <dustin@spy.net>
//
// $Id: ObjectPoolCleaner.java,v 1.1 2000/07/01 11:05:56 dustin Exp $

package net.spy.pool;

import java.util.*;
import net.spy.SpyConfig;

/**
 * This is a utility class used by ObjectPool to do periodic pool
 * management type things.
 */
public class ObjectPoolCleaner extends Thread {

	ObjectPool op=null;

	public ObjectPoolCleaner(ObjectPool op) {
		super();
		this.op=op;
		this.setDaemon(true);
		this.start();
	}

	public void run() {
		while(true) {
			try {
				// Prune every once in a while.
				sleep(10*1000);

				op.prune();
			} catch(Exception e) {
				// We may prune too often...
			}
		}
	}
}
