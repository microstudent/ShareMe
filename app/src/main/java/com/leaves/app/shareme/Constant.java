package com.leaves.app.shareme;

import com.leaves.app.shareme.bean.Media;

import net.majorkernelpanic.streaming.rtp.unpacker.RtpReceiveSocket;

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
    public static final String SP_VOLUME = "volume";
    public static final String TAG_LEFT_VOLUME = "leftVolume";
    public static final String TAG_RIGHT_VOLUME = "rightVolume";
    public volatile static List<Media> mPlayingMedias;
    public static final String TAG_ROLE_TYPE = "role_type";

    public static final int ROLE_CLIENT = 2;
    public static final int ROLE_SERVER = 1;
    public static final int ROLE_UNDEFINED = 0;

    public static final long DEFAULT_PLAY_TIME_DELAY = RtpReceiveSocket.FIRST_RUN_DELAY + 1000;

    public static List<Media> sPlayList;

    public interface WifiDirect {
        String INSTANCE_NAME = "ShareMe";
        String SERVICE_NAME = "sync music player";
        String KEY_PASSWORD = "password";
        String KEY_TIMESTAMP = "timestamp";
    }

    public interface WebSocket {
        String REGEX = "/";
        int PORT = 9000;
    }

    public interface Intent{
        String SERVER_IP = "server_ip";
    }

}
