package com.koushikdutta.async.rtsp;

import java.util.Hashtable;
import java.util.Locale;

public enum Protocol {
    /**
     * An obsolete plaintext framing that does not use persistent sockets by
     * default.
     */
    RTSP_1_0("RTSP/1.0");

    private final String protocol;
    private static final Hashtable<String, Protocol> protocols = new Hashtable<String, Protocol>();

    static {
        protocols.put(RTSP_1_0.toString(), RTSP_1_0);
    }


    Protocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * Returns the protocol identified by {@code protocol}.
     */
    public static Protocol get(String protocol) {
        if (protocol == null)
            return null;
        return protocols.get(protocol.toLowerCase(Locale.US));
    }

    @Override
    public String toString() {
        return protocol;
    }
}
