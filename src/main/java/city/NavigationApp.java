package city;

import org.greenrobot.eventbus.Subscribe;

public abstract class NavigationApp {
    protected final String appName;
    protected final boolean debugMode;

    public NavigationApp(String appName) {
        this(appName, false);
    }

    public NavigationApp(String appName, boolean debugMode) {
        this.appName = appName;
        this.debugMode = debugMode;
    }

    @Subscribe
    public void onTrafficCongestion(TrafficCongestionEvent event) {
        update(event);
    }

    public abstract void update(TrafficCongestionEvent event);

    public String getAppName() {
        return appName;
    }
}
