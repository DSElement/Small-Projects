package city;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public abstract class NavigationApp {
    protected final EventBus eventBus;
    protected final String appName;
    protected final boolean debugMode;

    public NavigationApp(EventBus eventBus, String appName) {
        this(eventBus, appName, false);
    }

    public NavigationApp(EventBus eventBus, String appName, boolean debugMode) {
        this.eventBus = eventBus;
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

    public void reportCongestion(String streetName, CongestionLevelEnum congestionLevel) {
        InformationDashboardEvent event = new InformationDashboardEvent(streetName, CityEventEnum.TRAFFIC_CONGESTION);
        eventBus.post(event);
    }
}
