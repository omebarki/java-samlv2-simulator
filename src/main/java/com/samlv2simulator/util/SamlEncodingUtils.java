package com.samlv2simulator.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public final class SamlEncodingUtils {

    private SamlEncodingUtils() {
    }

    public static String base64Encode(String input) {
        return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    public static String base64Encode(byte[] input) {
        return Base64.getEncoder().encodeToString(input);
    }

    public static byte[] base64DecodeToBytes(String input) {
        return Base64.getDecoder().decode(input);
    }

    public static String base64Decode(String input) {
        return new String(Base64.getDecoder().decode(input), StandardCharsets.UTF_8);
    }

    public static String deflateAndBase64Encode(String input) throws Exception {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.DEFLATED, true);
        DeflaterOutputStream deflaterStream = new DeflaterOutputStream(bytesOut, deflater);
        deflaterStream.write(input.getBytes(StandardCharsets.UTF_8));
        deflaterStream.finish();
        deflaterStream.close();
        return Base64.getEncoder().encodeToString(bytesOut.toByteArray());
    }

    public static String base64DecodeAndInflate(String input) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(input);
        ByteArrayInputStream bytesIn = new ByteArrayInputStream(decoded);
        InflaterInputStream inflater = new InflaterInputStream(bytesIn, new Inflater(true));
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inflater.read(buffer)) != -1) {
            bytesOut.write(buffer, 0, len);
        }
        inflater.close();
        return bytesOut.toString(StandardCharsets.UTF_8);
    }

    public static String urlDecode(String input) {
        return URLDecoder.decode(input, StandardCharsets.UTF_8);
    }
}
