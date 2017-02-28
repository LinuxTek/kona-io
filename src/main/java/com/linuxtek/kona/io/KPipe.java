/*
 * Copyright (C) 2011 LINUXTEK, Inc.  All Rights Reserved.
 */
package com.linuxtek.kona.io;


import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KPipe implements Runnable {
	private static Logger logger = LoggerFactory.getLogger(KPipe.class);

	private Thread t = null;
	private InputStream in = null;
	private OutputStream out = null;
	private volatile int count = 0;
	//private char[] chars = null;

	public KPipe(InputStream in, OutputStream out) {
		this.in = in;
		this.out = out;
		//chars = new char[1024];
        
		t = new Thread(this);
		t.start();
	}

	public int getCount() {
		return count;
	}

	public void stop() {
		t = null;
	}

	public void run() {
		Thread current = Thread.currentThread();

		try {
			//if (in == null || out == null)
			//	return;
	
			int c = in.read();	
            
			while (c != -1 && t == current) {
				//chars[count] = (char) c;
				count++;
				out.write(c);
				c = in.read();
			}
		}
		catch (Exception e) { 
            logger.error(e.getMessage(), e);
        } finally {
			//String s = new String(chars);
			//System.err.println("------------- CHARS -------\n" +s);
		}
	}
}

