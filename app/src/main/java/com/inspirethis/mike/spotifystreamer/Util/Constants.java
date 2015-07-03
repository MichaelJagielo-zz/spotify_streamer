package com.inspirethis.mike.spotifystreamer.Util;

/**
 * Created by mike on 6/30/15.
 */
public class Constants {
    public interface ACTION {
        public static String MAIN_ACTION = "com.inspirethis.mike.spotifystreamer.action.main";
        public static String PLAY_ACTION = "com.inspirethis.mike.spotifystreamer.action.play";
        public static String PAUSE_ACTION = "com.inspirethis.mike.spotifystreamer.action.pause";
        public static String RESUME_ACTION = "com.inspirethis.mike.spotifystreamer.action.resume";
        public static String PREV_ACTION = "com.inspirethis.mike.spotifystreamer.action.previous";
        public static String NEXT_ACTION = "com.inspirethis.mike.spotifystreamer.action.next";
        public static String STARTFOREGROUND_ACTION = "com.inspirethis.mike.spotifystreamer.action.startforeground";
        public static String STOPFOREGROUND_ACTION = "com.inspirethis.mike.spotifystreamer.action.stopforeground";
    }

    public interface NOTIFICATION_ID {
        public static int FOREGROUND_SERVICE = 101;
    }
}
