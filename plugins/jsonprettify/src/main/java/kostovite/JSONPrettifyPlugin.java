package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class JSONPrettifyPlugin extends Plugin {

    public JSONPrettifyPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("JSONPrettify plugin started");
    }

    @Override
    public void stop() {
        System.out.println("JSONPrettify plugin stopped");
    }
}