package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class WorldClockPlugin extends Plugin {

    public WorldClockPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("WorldClock plugin started");
    }

    @Override
    public void stop() {
        System.out.println("WorldClock plugin stopped");
    }
}