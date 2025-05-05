package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class MACAddressGeneratorPlugin extends Plugin {

    public MACAddressGeneratorPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("MACAddressGenerator plugin started");
    }

    @Override
    public void stop() {
        System.out.println("MACAddressGenerator plugin stopped");
    }
}