package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class HashToolsPlugin extends Plugin {

    public HashToolsPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("HashTools plugin started");
    }

    @Override
    public void stop() {
        System.out.println("HashTools plugin stopped");
    }
}