package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class ETACalculatorPlugin extends Plugin {

    public ETACalculatorPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("ETACalculator plugin started");
    }

    @Override
    public void stop() {
        System.out.println("ETACalculator plugin stopped");
    }
}