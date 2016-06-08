/*
 * Copyright (C) 2011 LINUXTEK, Inc.  All Rights Reserved.
 */
package com.linuxtek.kona.io;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Script execution.
 */
public class KScript {
    private static Logger logger = Logger.getLogger(KScript.class);

    private String command = null;
    private String[] args = null;
    private String stdout = null;
    private String stderr = null;
    private Map<String,String> env = new HashMap<String,String>();

    public KScript(String command) {
        this(command, (String[])null);
    }

    public KScript(String command, String... args) {
        this.command = command;
        this.args = args;

        logger.debug("command: " + command);
        if (args != null) {
            for (String arg: args) {
        	    logger.debug("arg: " + arg);
            }
        }
    }

    public void setEnv(String key, String value) {
        env.put(key, value);
    }

    private String readBuffer(BufferedReader in) 
            throws IOException {
        String result = in.readLine();

        String line;
        while (in.ready() && (line = in.readLine()) != null)
            result += "\n" + line;

        return (result);
    }

    public String run() throws IOException, KScriptException {
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	PrintStream pout = new PrintStream(out);
        
    	ByteArrayOutputStream err = new ByteArrayOutputStream();
    	PrintStream perr = new PrintStream(err);
    	
    	
        KProcess proc = new KProcess(command, args);
		proc.setEnv(env);
        proc.setStdOut(pout);
        proc.setStdErr(perr);
		proc.start();

        Integer exitValue = proc.getExitValue();
        
        stdout = out.toString();
        stderr = err.toString();
        
        /*
        BufferedReader procIn = new BufferedReader(
            new InputStreamReader(proc.getProcInStream()));
        
        String stdout = readBuffer(procIn);
        
        BufferedReader procErr = new BufferedReader(
        		new InputStreamReader(proc.getProcErrStream()));
        String stderr = readBuffer(procErr);
        */
        
        String result = "---STDOUT---\n" + stdout + "\n---STDERR---\n" + stderr;

        if (exitValue == null || exitValue > 0) {
            logger.error(toErrorMessage(proc, result));
            throw new KScriptException(result);
        }

        logger.debug("exec [" + command + "]\n" + result);
        return stdout;
    }

    private String toErrorMessage(KProcess proc, String message) {
        StringBuilder sb = new StringBuilder();

        if (proc != null) {
            sb.append("\tPath: " + proc.getCmdUrl() + "\n");
            if (proc.getArgs() != null) {
                List<String> args = Arrays.asList(proc.getArgs());
                sb.append("\tArgs: " + args + "\n");
            }

            if (proc.getEnv() != null) {
                sb.append("\tEnv: " + proc.getEnv() + "\n");
            }

            sb.append("\tExit Value: " + proc.getExitValue() + "\n");
            sb.append("\tError Message:\n");
        }

        sb.append(message);
        return (sb.toString());
    }
}
