package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class PhoneParserPlugin extends Plugin {

    public PhoneParserPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("PhoneParser plugin started");
    }

    @Override
    public void stop() {
        System.out.println("PhoneParser plugin stopped");
    }
}