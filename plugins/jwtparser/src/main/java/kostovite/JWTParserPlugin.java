package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class JWTParserPlugin extends Plugin {

    public JWTParserPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("JWTParser plugin started");
    }

    @Override
    public void stop() {
        System.out.println("JWTParser plugin stopped");
    }
}