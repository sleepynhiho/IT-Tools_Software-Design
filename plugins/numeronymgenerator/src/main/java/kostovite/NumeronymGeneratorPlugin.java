package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class NumeronymGeneratorPlugin extends Plugin {

    public NumeronymGeneratorPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("NumeronymGenerator plugin started");
    }

    @Override
    public void stop() {
        System.out.println("NumeronymGenerator plugin stopped");
    }
}