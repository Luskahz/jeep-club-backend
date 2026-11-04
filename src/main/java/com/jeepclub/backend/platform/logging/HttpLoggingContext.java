package com.jeepclub.backend.platform.logging;

public final class HttpLoggingContext {

    public static final String REQUEST_ID = "requestId";
    public static final String USER_ID = "userId";
    public static final String DEVICE = "device";

    public static final String REQUEST_ID_ATTRIBUTE = attribute("requestId");
    public static final String USER_ID_ATTRIBUTE = attribute("userId");

    private HttpLoggingContext() {
    }

    private static String attribute(String name) {
        return HttpLoggingContext.class.getName() + "." + name;
    }
}
