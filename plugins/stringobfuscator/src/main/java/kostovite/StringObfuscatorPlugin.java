package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class StringObfuscatorPlugin extends Plugin {

    public StringObfuscatorPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("StringObfuscator plugin started");
    }

    @Override
    public void stop() {
        System.out.println("StringObfuscator plugin stopped");
    }
}