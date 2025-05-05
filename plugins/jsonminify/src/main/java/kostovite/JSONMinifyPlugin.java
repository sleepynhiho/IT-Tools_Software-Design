package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class JSONMinifyPlugin extends Plugin {

    public JSONMinifyPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("JSONMinify plugin started");
    }

    @Override
    public void stop() {
        System.out.println("JSONMinify plugin stopped");
    }
}