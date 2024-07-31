package com.ccctc.adaptor.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Deserialize a boolean, string or integer to a Boolean, with null handling.
 *
 * Case is ignored.
 *
 * True: Y, YES, 1, TRUE
 * False: N, NO, 0, FALSE
 * Null: null or empty string
 */
public class BooleanDeserializer extends JsonDeserializer<Boolean> {

    @Override
    public Boolean deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonToken currentToken = p.getCurrentToken();

        if (currentToken.equals(JsonToken.VALUE_STRING) || currentToken.equals(JsonToken.VALUE_NUMBER_INT)) {
            String s = p.getValueAsString().toUpperCase();

            if ("Y".equals(s) || "YES".equals(s) || "1".equals(s) || "TRUE".equals(s))
                return Boolean.TRUE;
            if ("N".equals(s) || "NO".equals(s) || "0".equals(s) || "FALSE".equals(s))
                return Boolean.FALSE;
            if ("".equals(s))
                return (Boolean) null;

            throw ctxt.weirdStringException(s, Boolean.class, "Invalid string or number, cannot convert to Boolean");

        } else if (currentToken.equals(JsonToken.VALUE_TRUE)) {
            return Boolean.TRUE;
        } else if (currentToken.equals(JsonToken.VALUE_FALSE)) {
            return Boolean.FALSE;
        } else if (currentToken.equals(JsonToken.VALUE_NULL)) {
            return (Boolean) null;
        }

        throw ctxt.wrongTokenException(p, Boolean.class, currentToken, "Invalid token - cannot parse to Boolean");
    }
}
