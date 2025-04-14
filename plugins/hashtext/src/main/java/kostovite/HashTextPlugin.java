package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class HashTextPlugin extends Plugin {

    public HashTextPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("HashText plugin started");
    }

    @Override
    public void stop() {
        System.out.println("HashText plugin stopped");
    }
}