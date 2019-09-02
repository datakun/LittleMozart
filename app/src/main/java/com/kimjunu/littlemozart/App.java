package com.kimjunu.littlemozart;

public class App {
    public static String MediaPath = "";

    // 녹음 비트 레이트
    public static final int RECORD_BIT_RATE = 128 * 1000;

    // 녹음 최대 시간
    public static final long RECORD_LIMIT_MILISECOND = 60 * 60 * 1000;

    // 서버 주소
    public static final String SERVER_ADDRESS = "http://127.0.0.1:5000";

    // Rest API URL
    public static final String URL_LITTLE_MOZART = "call-little-mozart";

    // 에러 코드
    public static final int ERROR_NONE = 0;
    public static final int ERROR_INVALID = 1;
}
