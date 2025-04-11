package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class WifiQRCodeGeneratorPlugin extends Plugin {

    public WifiQRCodeGeneratorPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("WifiQRCodeGenerator plugin started");
    }

    @Override
    public void stop() {
        System.out.println("WifiQRCodeGenerator plugin stopped");
    }
}