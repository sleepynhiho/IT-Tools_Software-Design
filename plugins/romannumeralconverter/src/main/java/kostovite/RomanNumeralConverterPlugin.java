package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class RomanNumeralConverterPlugin extends Plugin {

    public RomanNumeralConverterPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("RomanNumeralConverter plugin started");
    }

    @Override
    public void stop() {
        System.out.println("RomanNumeralConverter plugin stopped");
    }
}