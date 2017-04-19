package com.leaves.app.shareme;

import com.leaves.app.shareme.bean.Media;

import java.util.List;

/**
 * Created by zhangshuyue on 17-2-18.
 */
public class Constant {
    public static final String PLAY_TYPE = "play_type";
    public static final String URI = "uri";
    public static final String MEDIA = "media";
    public static final int TAG_QUERY_PLAYING_AUDIO = 0;
    public static final int TAG_MEDIA = 1;
    public volatile static List<Media> mPlayingMedias;
    public static final int DEFAULT_PLAY_TIME_DELAY = 5000;//5sec

    public interface WifiDirect {
        String INSTANCE_NAME = "ShareMe";
        String SERVICE_NAME = "sync music player";
        String KEY_PASSWORD = "password";
        String KEY_TIMESTAMP = "timestamp";
    }

    public interface WebSocket {
        String REGEX = "/ShareMe";
        int PORT = 8080;
    }

    public interface Intent{
        String SERVER_IP = "server_ip";
    }
}
