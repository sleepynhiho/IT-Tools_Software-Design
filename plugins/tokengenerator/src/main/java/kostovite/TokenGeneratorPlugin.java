package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class TokenGeneratorPlugin extends Plugin {

    public TokenGeneratorPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("TokenGenerator plugin started");
    }

    @Override
    public void stop() {
        System.out.println("TokenGenerator plugin stopped");
    }
}