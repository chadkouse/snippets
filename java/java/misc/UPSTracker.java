// Copyright (c) 2000  Dustin Sallings <dustin@spy.net>
//
// $Id: UPSTracker.java,v 1.3 2000/06/15 22:14:07 dustin Exp $

import java.sql.*;
import java.util.*;

import net.spy.net.SNPP;
import net.spy.info.*;

public class UPSTracker extends PackageTracker {

	public UPSTracker() {
		super(1);
	}

	protected Info getInfoObj(String id) {
		return(new UPS(id));
	}

	public static void main(String args[]) throws ClassNotFoundException {
		// Load our driver up-front
		Class.forName("org.postgresql.Driver");
		UPSTracker t = new UPSTracker();
		t.run();
	}
}
