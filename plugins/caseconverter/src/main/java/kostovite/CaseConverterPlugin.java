package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class CaseConverterPlugin extends Plugin {

    public CaseConverterPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("CaseConverter plugin started");
    }

    @Override
    public void stop() {
        System.out.println("CaseConverter plugin stopped");
    }
}