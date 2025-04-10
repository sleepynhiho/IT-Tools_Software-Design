package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class CryptoToolsPlugin extends Plugin {

    public CryptoToolsPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("CryptoTools plugin started");
    }

    @Override
    public void stop() {
        System.out.println("CryptoTools plugin stopped");
    }
}