//
// $Id: PoolAble.java,v 1.21 2001/08/30 20:26:46 dustin Exp $

package net.spy.pool;

import java.util.*;
import java.io.*;
import net.spy.SpyConfig;

/**
 * PoolAble is the object container that is used to store objects in the
 * pool.
 *
 * <p>
 *
 * The system property net.spy.pool.debug can be set to a file where the
 * debugging output will go.
 */

public abstract class PoolAble extends Object {
	private int object_id=-1;
	private boolean checked_out=false;
	private Object the_object=null;
	private long max_age=0;
	private long start_time=0;
	private String pool_name=null;
	private PoolDebug pooldebug=null;
	private int checkouts=0;
	private int checkins=0;
	private int poolHash=0;

	/**
	 * Minimum value returned from pruneStatus() if we may clean the object.
	 */
	public static final int MAY_CLEAN=1;
	/**
	 * Minimum value returned from pruneStatus() if we must clean the object.
	 */
	public static final int MUST_CLEAN=2;

	/**
	 * Availability flag.
	 */
	protected boolean available=true;

	/**
	 * Get a PoolAble representation for an object.
	 */
	public PoolAble(Object the_object, int poolHash) {
		super(); // thanks for asking.
		this.the_object=the_object;
		this.poolHash=poolHash;
		start_time=System.currentTimeMillis();
		debug("New object");
	}

	/**
	 * Get a PoolAble representation for an object, including a given
	 * maximum lifetime the object can have.
	 *
	 * @param max_age the amount of time, in milliseconds, that the object
	 * will be valid.  Objects will not be checked out if they are older
	 * than their maximum lifetime.
	 */
	public PoolAble(Object the_object, long max_age, int poolHash) {
		super(); // thanks for asking.
		this.the_object=the_object;
		this.max_age=max_age;
		this.poolHash=poolHash;
		start_time=System.currentTimeMillis();
		debug("New object.");
	}

	// Get the debug name
	private String debugName() {
		StringBuffer sb=new StringBuffer();
		sb.append("PoolAble ");
		sb.append(object_id);
		sb.append(" for ");
		sb.append(Integer.toHexString(poolHash));
		return(sb.toString());
	}

	/**
	 * Set the maximum age of this PoolAble.
	 */
	public void setMaxAge(long to) {
		this.max_age=to;
	}

	/**
	 * Find out of the PoolAble represents a usable object.  Objects
	 * extending PoolAble should implement isAlive() methods for their
	 * particular needs.
	 * <p>
	 * Objects implementing isAlive() <i>should</i> turn off object
	 * availability if they determine the object no longer isAlive().
	 *
	 * @return true if the object will be usable
	 */
	public synchronized boolean isAlive() {
		return(true);
	}

	/**
	 * Get the object we're pooling.
	 *
	 * @return the object.
	 *
	 * @exception PoolException if something bad happens (i.e. the object is
	 * not checked out)
	 */
	public synchronized Object getObject() throws PoolException {
		if(!checked_out) {
			throw new PoolException("This PoolAble has not been checked out.");
		}
		return(the_object);
	}

	/**
	 * Internal version of getObject().  Returns regardless of whether the
	 * object has been checked out.
	 */
	protected Object intGetObject() {
		return(the_object);
	}

	/**
	 * Set the internal object ID.  This probably shouldn't be called
	 * outside of the pool container.
	 *
	 * @param id ObjectId of this object.
	 */
	public void setObjectID(int id) {
		this.object_id=id;
	}

	/**
	 * Set the pool name this thing sits in.  This is for debugging, but
	 * it's useful information, nonetheless.
	 */
	public void setPoolName(String to) {
		this.pool_name=to;
	}

	/**
	 * Get the object ID of this object.
	 *
	 * @return the object ID
	 */
	public int getObjectID() {
		return(object_id);
	}

	/**
	 * Activate this PoolAble object.
	 */
	public void activate() {
		debug("Activated.");
	}

	/**
	 * Check the object back in.  The PoolAble will not be usable again
	 * until it's checked back out from the pooler.
	 * <p>
	 * checkIn also does some checks such as making sure the item is not
	 * too old, and that it is still alive.
	 */
	public synchronized void checkIn() {
		checked_out=false;
		checkins++;

		// At this point, set the availability based on whether this object
		// is expired.
		available=!isExpired();

		// Also, make sure the thing's alive.
		if(!isAlive()) {
			available=false;
		}

		debug("Checked in.");
	}

	// Returns true if this should be invalidated based on the time
	private boolean isExpired() {
		boolean rv=true;

		if(max_age==0) {
			rv=false;
		} else {
			long current_time=System.currentTimeMillis();
			if(current_time-start_time < max_age) {
				rv=false;
			}
		}
		return(rv);
	}

	/**
	 * Check the object out.  Called from the PoolContainer
	 */
	public synchronized void checkOut() {
		checkouts++;
		checked_out=true;
		available=false;
		debug("Checked out.");
	}

	/**
	 * Find out if the object is checked out.
	 */
	public synchronized boolean isCheckedOut() {
		return(checked_out);
	}

	/**
	 * Find out if the object is available for a requestor
	 */
	public synchronized boolean isAvailable() {
		// If it currently believes it's available, but it's expired, make
		// it unavailable.
		if(available) {
			if(isExpired()) {
				available=false;
			}
		}
		return(available);
	}

	/**
	 * Find out if an object is prunable
	 *
	 * @return 0 if not available, 1 if we may clean, greater than one if
	 * we must clean.
	 */
	public synchronized int pruneStatus() {
		int ret=0;

		// If it's not checked out, we can prune it.
		if(!checked_out) {
			ret++;
			// If it's not checked out, and it's not available, we *need*
			// to prune it.
			if(!isAvailable()) {
				ret++;
			}

			// If it's not alive, we don't want it.
			if(!isAlive()) {
				ret++;
			}
		}

		return(ret);
	}

	/**
	 * Discard the object.  Anything that extends from this class needs to
	 * call super.discard() when it's done.
	 */
	public void discard() {
		debug("Discard called.");
		available=false;
		the_object=null;
	}

	/**
	 * Debugging info.
	 */
	protected final void debug(String msg) {
		if(pooldebug==null) {
			pooldebug=new PoolDebug();
		}

		String classname=getClass().getName();
		String objectClassname="n/a";
		if(the_object!=null) {
			objectClassname=the_object.getClass().getName();
		}

		String tmsg= "Poolable=" + classname + ", oid=" + object_id
			+ " in " + pool_name + ", object=" + objectClassname + ":  " + msg;

		pooldebug.debug(tmsg);
	}

	/**
	 * Return a string representation of this object.
	 *
	 * @return a string representation of this object.
	 */
	public synchronized String toString() {
		StringBuffer out=new StringBuffer();
		out.append(debugName());
		if(isCheckedOut()) {
			out.append(" is checked out");
		} else {
			out.append(" is not checked out");
		}
		out.append(" (o=" + checkouts + ", i=" + checkins + ")");
		if(max_age>0) {
			out.append(" expires " + new Date(start_time + max_age));
		}
		if(!isAvailable()) {
			out.append(" (not available)");
		}
		return(out.toString());
	}
}
