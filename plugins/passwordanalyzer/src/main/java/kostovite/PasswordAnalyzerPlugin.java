package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class PasswordAnalyzerPlugin extends Plugin {

    public PasswordAnalyzerPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("PasswordAnalyzer plugin started");
    }

    @Override
    public void stop() {
        System.out.println("PasswordAnalyzer plugin stopped");
    }
}