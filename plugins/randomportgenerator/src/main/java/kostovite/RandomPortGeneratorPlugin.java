package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class RandomPortGeneratorPlugin extends Plugin {

    public RandomPortGeneratorPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("RandomPortGenerator plugin started");
    }

    @Override
    public void stop() {
        System.out.println("RandomPortGenerator plugin stopped");
    }
}