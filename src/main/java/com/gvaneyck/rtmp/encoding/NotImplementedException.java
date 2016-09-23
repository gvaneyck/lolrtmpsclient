package com.gvaneyck.rtmp.encoding;

import java.io.IOException;

/**
 * A basic exception used within AMF3Encoder and AMF3Decoder for notifying of
 * unimplemented functionality
 *
 * @author Gabriel Van Eyck
 */
public class NotImplementedException extends IOException {

    public NotImplementedException(String message) {
        super(message);
    }
}
