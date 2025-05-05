package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class IBANValidatorPlugin extends Plugin {

    public IBANValidatorPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("IBANValidator plugin started");
    }

    @Override
    public void stop() {
        System.out.println("IBANValidator plugin stopped");
    }
}