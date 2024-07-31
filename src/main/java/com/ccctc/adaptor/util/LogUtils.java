package com.ccctc.adaptor.util;

public class LogUtils {

    /**
     * Escape a string prior to logging it. Notably this will remove things like CR/LF and other non-printing characters.
     *
     * CR, LF and TAB are replaced with \r, \n and \t respectively. All other non-alphanumeric characters except space
     * are replaced with a question mark.
     *
     * This solution was put in place to prevent log forging per a security review.
     *
     * @param text Message
     * @return Escaped string
     */
    public static String escapeString(String text) {
        if (text == null)
            return null;

        return text
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .replaceAll("[^\\x20-\\x7E]", "?");
    }
}