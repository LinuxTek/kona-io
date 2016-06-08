/*
 * Copyright (C) 2011 LINUXTEK, Inc.  All Rights Reserved.
 */
package com.linuxtek.kona.io;


/**
 * Thrown when processing Script.
 */

@SuppressWarnings("serial")
public class KScriptException extends Exception {

    public KScriptException(String message) {
        super(message);
    }

    /*
    public static String createDetailedMessage(KProcess proc, String message) {
        StringBuffer sb = new StringBuffer();

        if (proc != null) {
            sb.append("\tPath: " + proc.getCmdUrl() + "\n");    
            if (proc.getArgs() != null) {
                List args = Arrays.asList(proc.getArgs());
                sb.append("\tArgs: " + args + "\n");    
            }

            if (proc.getEnv() != null) {
                sb.append("\tEnv: " + proc.getEnv() + "\n");    
            }

            sb.append("\tExit Value: " + proc.getExitValue() + "\n");    
            sb.append("\tError Message:\n");
        }

        sb.append(message);
        return (sb.toString())
    }
    */

    public KScriptException(String ex, Throwable cause) {
        super(ex, cause);
    }

    public KScriptException(Throwable cause) {
        super(cause);
    }
}
