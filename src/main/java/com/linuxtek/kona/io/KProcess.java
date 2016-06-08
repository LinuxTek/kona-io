/*
 * Copyright (C) 2011 LINUXTEK, Inc.  All Rights Reserved.
 */
package com.linuxtek.kona.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownServiceException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.commons.io.output.NullOutputStream;

import com.linuxtek.kona.io.protocol.file.KFileURLConnection;
import com.linuxtek.kona.util.KStringUtil;

public class KProcess implements Runnable {
	private static Logger logger = Logger.getLogger(KProcess.class);

	private static Integer pidCounter = 1;

	public enum ProcState {
		init, running, exception, completed, killed
	}

	private Thread procThread = null;
	private URL cmdUrl = null;
	private String[] args = null;
	private Map<String, String> env = null;
	private URL stdOutUrl = null;
	private URL stdErrUrl = null;
	private Integer exitValue = null;
	private ProcState procState = null;

	private OutputStream stdOut = null;
	private OutputStream stdErr = null;

	private volatile Process process = null;
	private String pid = null;

    /*
	private InputStream procIn = null;
	private InputStream procErr = null;
	private OutputStream procOut = null;
    */

	public KProcess(URL cmdUrl, String[] args, Map<String, String> env,
			URL stdOutUrl, URL stdErrUrl) {
		this.cmdUrl = cmdUrl;
		this.pid = (++pidCounter).toString();
		this.procState = ProcState.init;
		this.args = args;
		this.env = env;
		this.stdOutUrl = stdOutUrl;
		this.stdErrUrl = stdErrUrl;
	}

	public KProcess(URL cmdUrl, String[] args) {
		this(cmdUrl, args, null, null, null);
	}

	public KProcess(String path, String... args) throws IOException {
		File f = new File(path);
		if (!f.exists())
			throw new IOException("File not found: " + path);

		try {
			this.cmdUrl = new URL("file://" + path);
			// this.pid = "" + ++pidCounter;
			this.pid = (++pidCounter).toString();
			this.procState = ProcState.init;
			// this.args = argList;
			this.args = args;
		} catch (MalformedURLException e) {
		}
	}

	public static KProcess exec(String path, String... args) throws IOException {
		KProcess proc = new KProcess(path, args);
		proc.start();
		return proc;
	}

	public static KProcess exec(String path, String[] args,
			Map<String, String> env) throws IOException {
		KProcess proc = new KProcess(path, args);
		proc.setEnv(env);
		proc.start();
		return proc;
	}

	// -------------------------------------------------------------

    /*
	public InputStream getProcInStream() {
		return (procIn);
	}

	public OutputStream getProcOutStream() {
		return (procOut);
	}

	public InputStream getProcErrStream() {
		return (procErr);
	}
    */

	public Boolean isActive() {
		if (procState == ProcState.init || procState == ProcState.running)
			return (true);
		else
			return (false);
	}

	public ProcState getProcState() {
		return (procState);
	}

	public Process getProcess() {
		return (process);
	}

	public String getPid() {
		return (pid);
	}

	public Integer getExitValue() {
		while (isActive()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				logger.error(e);
			}
		}
		return (exitValue);
	}

	public URL getCmdUrl() {
		return (cmdUrl);
	}

	public String[] getArgs() {
		return (args);
	}

	public Map<String, String> getEnv() {
		return (env);
	}

	public URL getStdOutUrl() {
		return (stdOutUrl);
	}


	public OutputStream getStdOut() throws IOException {
		return stdOut;
	}

	public URL getStdErrUrl() {
		return (stdErrUrl);
	}


	public OutputStream getStdErr() throws IOException {
		return stdErr;
	}

	public String getCmdPath() throws IOException {
		File f = getCmdFile();
		return (f.getCanonicalPath());
	}

	public File getCmdFile() throws IOException {
		KFileURLConnection conn = null;

		logger.debug("cmdUrl: " + cmdUrl);

		if (cmdUrl.getProtocol().equalsIgnoreCase("file"))
			conn = new KFileURLConnection(cmdUrl);

		if (conn == null)
			throw new IOException("Cannot obtain URLConnection for "
					+ "cmdUrl URL: " + cmdUrl);

		File f = conn.getFile();
		return (f);
	}

	// -------------------------------------------------------------

	public void setEnv(Map<String, String> env) {
		this.env = env;
	}

	public void setArgs(String[] args) {
		this.args = args;
	}

	public void setOutputStream(OutputStream stdOut) {
		this.stdOut = stdOut;
	}

	public void setStdOut(OutputStream stdOut) {
		this.stdOut = stdOut;
	}

	public void setStdErr(OutputStream stdErr) {
		this.stdErr = stdErr;
	}

	public void setStdOutUrl(URL stdOutUrl) {
		this.stdOutUrl = stdOutUrl;
	}

	public void setStdErrUrl(URL stdErrUrl) {
		this.stdErrUrl = stdErrUrl;
	}


	// -------------------------------------------------------------
	protected OutputStream getOutputStream(URL url, Boolean append) throws IOException {
		OutputStream s = null;

		if (url == null) {
			return null;
		}

		try {
			KFileURLConnection conn = null;
			if (url.getProtocol().equalsIgnoreCase("file"))
				conn = new KFileURLConnection(url, true);

			if (conn == null)
				throw new IOException("Invalid URL protocol.  Only file:// "
						+ "is currently suppported.");

			// logger.debug("got URLConnection  for url: " + url);
			s = conn.getOutputStream(append);
		} catch (UnknownServiceException e) {
			logger.debug(e);
			throw new IOException("Cannot obtain OutputStream for url: " + url
					+ "\nCaught UnknownServiceException: " + e.getMessage(), e);
		}
		// logger.debug("got OutputStream for url: " + url);
		return (s);
	}

	// -------------------------------------------------------------

	protected void kill() {
		if (isActive()) {
			if (process != null)
				process.destroy();

			switch (procState) {
			case running:
			case exception:
			case completed:
			case killed: // shouldn't be needed
			default:
				break;
			}

			procState = ProcState.killed;
			procThread = null;

			logger.debug(getPid() + ": has been killed.\n" + "\tProcState: "
					+ procState);
		}
	}

	public String start() {
		procState = ProcState.running;
		exitValue = null;
		procThread = new Thread(this);
		procThread.start();
		return (pid);
	}
    
	protected class StreamGobbler implements Runnable {
		private Thread t = null;
		private InputStream is;
		private String type;

		StreamGobbler(InputStream is, String type) {
			this.is = is;
			this.type = type;
			t = new Thread(this);
			t.start();
		}

		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line=null;
				while ( (line = br.readLine()) != null) {
					System.out.println(type + ">" + line);    
				}
			} catch (IOException e) {
				logger.error(e);
			}
		}
	}

	public void run() {
		InputStream pout = null;
		InputStream perr = null;
        
		KPipe p1 = null;
		KPipe p2 = null;

		try {
            if (stdOut == null) {
            	stdOut = System.out;
            }
            
            if (stdErr == null) {
            	stdErr = System.err;
            }

			String cmd = getCmdPath();
			String[] args = getArgs();
			Map<String, String> env = getEnv();

			logger.debug("exec thread: running cmd: " + cmd);

			List<String> cmdList = new ArrayList<String>();

			cmdList.add(cmd);
			if (args != null) {
                logger.debug("adding args to cmdlist");
				cmdList.addAll(1, Arrays.asList(args));
			}

			logger.debug("cmdList: " + KStringUtil.join(cmdList, " "));

			// ProcessBuilder pb = new ProcessBuilder(cmd, args);
			ProcessBuilder pb = new ProcessBuilder(cmdList);
			String tmpdir = System.getProperty("java.io.tmpdir");

			if (tmpdir == null)
				tmpdir = "/tmp";

			logger.debug("tmpdir: " + tmpdir);

			pb.directory(new File(tmpdir));

			if (env != null) {
				Map<String, String> pbEnv = pb.environment();
				Iterator<String> it = env.keySet().iterator();
				while (it.hasNext()) {
					String key = (String) it.next();
					String value = env.get(key);
					pbEnv.put(key, value);
				}
			}

			process = pb.start();

			pout = process.getInputStream();
			perr = process.getErrorStream();

			//procIn = pout;
			//procErr = perr;
			//procOut = process.getOutputStream();
            
			
            /*
			StreamGobbler errGobbler = new StreamGobbler(perr, "ERROR");            
			StreamGobbler outGobbler = new StreamGobbler(pout, "OUTPUT");
            */

			p1 = new KPipe(pout, stdOut);
			p2 = new KPipe(perr, stdErr);

			exitValue = process.waitFor();
			// sleep to catch any remaining I/O
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			procState = ProcState.completed;

			logger.debug("process [" + getPid() + "] " + "\n\texit value: "
					+ exitValue);
		} catch (Exception e) {
			procState = ProcState.exception;
			throw new RuntimeException(e);
		} finally {
			try {
				if (p1 != null)
					logger.debug("p1 count = " + p1.getCount());

				if (p2 != null)
					logger.debug("p2 count = " + p2.getCount());
			} catch (Exception e) {
				logger.error("Error closing streams", e);
			}
		}
	}

	protected void finalize() throws Throwable {
		try {
			if (process != null && process.getInputStream() != null) {
				process.getInputStream().close();
			}
            
			if (process != null && process.getErrorStream() != null) {
				process.getErrorStream().close();
			}
            
			if (process != null && process.getOutputStream() != null) {
				process.getOutputStream().close();
			}
		} catch (Exception e) {
			logger.error("Error closing streams", e);
		} finally {
			super.finalize();
		}
	}

	// -------------------------------------------------------------

	public String toString() {
		String s = "KProcess: \n";
		s += "\tcmdUrl: " + cmdUrl + "\n";
		s += "\targs: " + args + "\n";
		s += "\tstdOutUrl: " + stdOutUrl + "\n";
		s += "\tstdErrUrl: " + stdErrUrl + "\n";

		return (s);
	}
}
