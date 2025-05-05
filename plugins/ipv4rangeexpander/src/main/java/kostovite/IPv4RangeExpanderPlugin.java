package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class IPv4RangeExpanderPlugin extends Plugin {

    public IPv4RangeExpanderPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("IPv4RangeExpander plugin started");
    }

    @Override
    public void stop() {
        System.out.println("IPv4RangeExpander plugin stopped");
    }
}