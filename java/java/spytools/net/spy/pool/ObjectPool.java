// Copyright (c) 2000  Dustin Sallings <dustin@spy.net>
//
// $Id: ObjectPool.java,v 1.17 2000/11/02 22:19:44 dustin Exp $

package net.spy.pool;

import java.util.*;
import net.spy.SpyConfig;

/**
 * ObjectPool is the entry point for all object pooling facilities in
 * net.spy.pool.*.  ObjectPools have a shared reference to a pool, so there
 * is exactly one set of pools per JVM.  This can be very useful in
 * consolidating applications' pools into one.
 * <p>
 * Pools are referenced by name, so as long as two pools have two different
 * names, they will be used independently.
 * <p>
 * When creating a pool, you must have a PoolFiller that will populate the
 * pool with objects when it needs them.
 * <p>
 * The following is an example demonstrating how to instantiate a JDBC pool
 * using JDBCPoolFiller:
 * <pre>
 * SpyConfig conf=new SpyConfig("pool.conf");
 * ObjectPool op=new ObjectPool(conf);
 * JDBCPoolFiller pf=new JDBCPoolFiller("db", conf);
 * op.createPool("db", pf);
 * </pre>
 */

public class ObjectPool extends Object {
	protected Exception objectException=null;
	protected SpyConfig conf=null;
	// This is static so we can check up on it.
	protected static ObjectPoolCleaner cleaner=null;
	protected static String CLEANER_MUTEX="CLEANER_MUTEX";
	// This is static because we want everyone to see the same pools, of
	// course.
	protected static Hashtable pools=null;
	protected static String POOL_MUTEX="POOL_MUTEX";

	public ObjectPool(SpyConfig conf) {
		super();
		this.conf=conf;

		initialize();
	}

	/**
	 * Create a new object pool.
	 *
	 * @param name The name of the object pool.
	 * @param pf The PoolFiller object that will be used to create new
	 * objects within the pool.
	 *
	 * @exception PoolException when bad things happen
	 */
	public void createPool(String name, PoolFiller pf)
		throws PoolException {

		synchronized(pools) {
			// Make sure we don't already have a this pool
			if(hasPool(name)) {
				throw new PoolException("There's already a pool called "
					+ name);
			}

			// Grab a PoolContainer
			PoolContainer pc=new PoolContainer(name, pf);

			// add it to our pool list
			pools.put(name, pc);
		}
	}

	/**
	 * Destory a pool.
	 *
	 * @param name The pool to destroy.
	 * @exception PoolException if there's a problem removing the pool
	 */
	public void destroyPool(String name) throws PoolException {
		synchronized (pools) {
			getPool(name);
			pools.remove(name);
		}
	}

	/**
	 * Find out if the ObjectPool contains the named pool.
	 *
	 * @param name the name of the pool we're looking for
	 */
	public boolean hasPool(String name) {
		boolean ret=false;
		synchronized (pools) {
			ret=pools.containsKey(name);
		}
		return(ret);
	}

	/**
	 * Get an object from a pool.
	 *
	 * @param name The pool from which we'll get our object.
	 *
	 * @return a PooledObject object.
	 *
	 * @exception PoolException if it can't get an object
	 */
	public PooledObject getObject(String name) throws PoolException {
		PooledObject ret=null;
		PoolContainer pc=null;
		synchronized (pools) {
			pc=getPool(name);
		}
		ret=pc.getObject();
		return(ret);
	}

	/**
	 * Get a count of the number of object pools.
	 */
	public int numPools() {
		int n=0;
		synchronized (pools) {
			n=pools.size();
		}
		return(n);
	}

	/**
	 * Dump out the object pools.
	 */
	public String toString() {
		String out="";
		Vector v=new Vector();
		synchronized (pools) {
			for(Enumeration e=pools.elements(); e.hasMoreElements(); ) {
				v.addElement(e.nextElement());
			}
		}
		// This is broken out to get out of the lock fast...
		for(Enumeration e=v.elements(); e.hasMoreElements(); ) {
			out+=e.nextElement();
		}
		return(out);
	}

	/**
	 * Prune the object pools.  This method requests that each individual
	 * pool prune itself, removing unusable or unnecessary PoolAbles.
	 *
	 * @exception PoolException if something bad happens
	 */
	public void prune() throws PoolException {
		Vector v=new Vector();
		synchronized (pools) {
			for(Enumeration e=pools.elements(); e.hasMoreElements(); ) {
				PoolContainer pc=(PoolContainer)e.nextElement();

				// If it's empty, remove it.
				if(pc.avaliableObjects()==0) {
					destroyPool(pc.getName());
				} else {
					v.addElement(pc);
				}
			}
		}
		for(Enumeration e=v.elements(); e.hasMoreElements(); ) {
			PoolContainer pc=(PoolContainer)e.nextElement();
			pc.prune();
		}
	}

	protected static synchronized PoolContainer getPool(String name)
		throws PoolException {

		PoolContainer ret=null;

		synchronized (pools) {
			ret=(PoolContainer)pools.get(name);
			if(ret==null) {
				throw new PoolException("There's no pool called " + name);
			}
		}
		return(ret);
	}

	protected void initialize() {
		// Do we have a pool?
		synchronized(POOL_MUTEX) {
			if(pools==null) {
				pools=new Hashtable();
			}
		}
		// Is our cleaner working?
		synchronized(CLEANER_MUTEX) {
			if(cleaner==null || (!cleaner.isAlive())) {
				cleaner=new ObjectPoolCleaner(this);
			}
		}
	}


	// This is a private class that keeps the pool clean.
	private class ObjectPoolCleaner extends Thread {

		// The object pool reference we'll be cleaning.
		protected ObjectPool op=null;

		// How many times we've cleaned so far.
		protected int numCleans=0;

		// Create (and start) the ObjectPoolCleaner.
		public ObjectPoolCleaner(ObjectPool op) {
			super();
			this.op=op;
			setDaemon(true);
			setName("ObjectPoolCleaner");
			start();
		}

		// Look like a normal thread, but report number of times the thing's
		// cleaned.
		public String toString() {
			return(super.toString() + " - " + numCleans + " served.");
		}

		protected void doPrune() throws Exception {
			System.out.println("Cleaning at " + new Date() + ":\n" + op);
			op.prune();
			numCleans++;
			System.out.println("Now looks like this:\n" + op);
		}

		public void run() {
			// Only do six cleans (sleeping ten minutes, that's an hour!)
			while(numCleans<6) {
				try {
					// Prune every once in a while.
					sleep(5*60*1000);
					doPrune();
				} catch(Exception e) {
					System.err.println("***\nCleaner got an exception:  "
						+ e + "\n***");
				}
			}
		}
	} // ObjectPoolCleaner

} // ObjectPool
