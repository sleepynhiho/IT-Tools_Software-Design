package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class PercentageCalculatorPlugin extends Plugin {

    public PercentageCalculatorPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("PercentageCalculator plugin started");
    }

    @Override
    public void stop() {
        System.out.println("PercentageCalculator plugin stopped");
    }
}