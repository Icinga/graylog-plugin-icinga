package com.icinga.icinga;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Icinga2Filter {
    public static StringBuilder quoteString(String s) {
        StringBuilder result = new StringBuilder();
        Formatter formatter = new Formatter(result, Locale.US);
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(s);
        int current;

        result.append('"');

        while (buffer.hasRemaining()) {
            current = buffer.get();
            current += 256;
            current %= 256;

            result.append('\\');
            formatter.format("%03o", current);
        }

        return result.append('"');
    }
}
