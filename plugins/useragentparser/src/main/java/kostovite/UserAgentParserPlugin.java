package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class UserAgentParserPlugin extends Plugin {

    public UserAgentParserPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("UserAgentParser plugin started");
    }

    @Override
    public void stop() {
        System.out.println("UserAgentParser plugin stopped");
    }
}