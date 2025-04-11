package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class ChronometerPlugin extends Plugin {

    public ChronometerPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("Chronometer plugin started");
    }

    @Override
    public void stop() {
        System.out.println("Chronometer plugin stopped");
    }
}