package application;

public class GameSettings {
    public static int roundDurationSeconds = 90;
    public static boolean powerUpsEnabled = true;
    public static boolean sfxEnabled = true;
    public static boolean musicEnabled = true;

    // Callback used to immediately update the background music volume/mute state in Main.java
    public static Runnable onSettingsUpdated;
}
