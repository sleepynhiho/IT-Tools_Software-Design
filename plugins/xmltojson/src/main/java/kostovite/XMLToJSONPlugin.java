package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class XMLToJSONPlugin extends Plugin {

    public XMLToJSONPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("XMLToJSON plugin started");
    }

    @Override
    public void stop() {
        System.out.println("XMLToJSON plugin stopped");
    }
}