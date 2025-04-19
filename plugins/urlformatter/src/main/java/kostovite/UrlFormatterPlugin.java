package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class UrlFormatterPlugin extends Plugin {

    public UrlFormatterPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("UrlFormatter plugin started");
    }

    @Override
    public void stop() {
        System.out.println("UrlFormatter plugin stopped");
    }
}