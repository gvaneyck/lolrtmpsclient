package com.gvaneyck.rtmp.encoding;

import java.io.IOException;

/**
 * A basic exception used within AMF3Encoder and AMF3Decoder to notify of
 * parsing problems.
 *
 * @author Gabriel Van Eyck
 */
public class EncodingException extends IOException {

    public EncodingException(String message) {
        super(message);
    }
}
