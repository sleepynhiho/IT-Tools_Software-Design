package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class EmailNormalizerPlugin extends Plugin {

    public EmailNormalizerPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("EmailNormalizer plugin started");
    }

    @Override
    public void stop() {
        System.out.println("EmailNormalizer plugin stopped");
    }
}