package util;

import java.util.HashMap;
import java.util.Map;

public class ResponseUtil {

    public static Map<String, String> getHeaderMap(String contentType) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", contentType);
        headers.put("X-Custom-Header", contentType);
        return headers;
    }
}
