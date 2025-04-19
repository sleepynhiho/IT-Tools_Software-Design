package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class IntegerBaseConverterPlugin extends Plugin {

    public IntegerBaseConverterPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("IntegerBaseConverter plugin started");
    }

    @Override
    public void stop() {
        System.out.println("IntegerBaseConverter plugin stopped");
    }
}