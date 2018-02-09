package utils;

import play.shaded.ahc.org.asynchttpclient.util.Base64;

import java.security.SecureRandom;

public class PageUtils {
    public static String createNonce() {
        final byte[] nonceBytes = new byte[32];
        new SecureRandom().nextBytes(nonceBytes);
        return Base64.encode(nonceBytes);
    }
}
