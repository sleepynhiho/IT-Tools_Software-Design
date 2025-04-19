package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class MathEvaluatorPlugin extends Plugin {

    public MathEvaluatorPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("MathEvaluator plugin started");
    }

    @Override
    public void stop() {
        System.out.println("MathEvaluator plugin stopped");
    }
}