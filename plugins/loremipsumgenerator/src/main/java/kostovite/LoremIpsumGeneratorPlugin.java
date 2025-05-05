package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class LoremIpsumGeneratorPlugin extends Plugin {

    public LoremIpsumGeneratorPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("LoremIpsumGenerator plugin started");
    }

    @Override
    public void stop() {
        System.out.println("LoremIpsumGenerator plugin stopped");
    }
}