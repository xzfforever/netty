package com.xzf.spring.boot.netty.constants;

import java.util.regex.Pattern;

/**
 * Created by Administrator on 2017/6/24.
 */
public interface ServerConstants {

    String HTTP_DATE_GMT_TIMEZONE = "GMT";

    String FAVICON_ICO = "/favicon.ico";

    String MULTIPART_FORM_DATA = "multipart/form-data";

    String APPLICATION_JSON= "application/json";

    String CONNECTION_KEEP_ALIVE = "keep-alive";

    String CONNECTION_CLOSE = "close";

    Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");

    String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";

    String CRLF = System.getProperty("line.separator");
}
