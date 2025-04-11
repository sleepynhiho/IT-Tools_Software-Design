package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class MediaToolsPlugin extends Plugin {

    public MediaToolsPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("MediaTools plugin started");
    }

    @Override
    public void stop() {
        System.out.println("MediaTools plugin stopped");
    }
}