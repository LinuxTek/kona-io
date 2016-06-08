/*
 * Copyright (C) 2011 LINUXTEK, Inc.  All Rights Reserved.
 */

package com.linuxtek.kona.io.protocol.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KFileURLConnection extends URLConnection {
	private static Logger logger = LoggerFactory.getLogger(KFileURLConnection.class);

	URL url = null;
	File file = null;
	boolean create = false;

	public KFileURLConnection(URL url) throws IOException {
		this(url, false);
	}

	public KFileURLConnection(URL url, boolean create) throws IOException {
		super(url);
		this.url = url;
		this.create = create;
		connect();
	}

	public void connect() throws IOException {
		if (connected)
			return;

		try {
			// logger.debug("url: " + url);
			URI uri = url.toURI();
			// logger.debug("uri: " + uri);

			this.file = new File(uri);
			if (!file.isFile()) {
				if (create) {
					FileOutputStream fs = new FileOutputStream(file);
					fs.close();
				}
			}
			connected = true;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new IOException(e.getMessage());
		}

	}

	public File getFile() {
		return (file);
	}

	public InputStream getInputStream() throws IOException {
		if (!connected)
			connect();

		return new FileInputStream(file);
	}

	public OutputStream getOutputStream() throws IOException {
		return (getOutputStream(false));
	}

	public OutputStream getOutputStream(boolean append) throws IOException {
		OutputStream os = new FileOutputStream(file, append);
		return (os);
	}

	public Object getContent() throws IOException {
		String s = new String();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		while (reader.ready())
			s += reader.readLine() + "\n";

        reader.close();
		return (s);
	}

	// --------------------------------------------------------------

	public static void main(String[] args) {
	}
}
