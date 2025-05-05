package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class IPv4AddressConverterPlugin extends Plugin {

    public IPv4AddressConverterPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("IPv4AddressConverter plugin started");
    }

    @Override
    public void stop() {
        System.out.println("IPv4AddressConverter plugin stopped");
    }
}