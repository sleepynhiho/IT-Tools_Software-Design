package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class QRCodeGeneratorPlugin extends Plugin {

    public QRCodeGeneratorPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("QRCodeGenerator plugin started");
    }

    @Override
    public void stop() {
        System.out.println("QRCodeGenerator plugin stopped");
    }
}