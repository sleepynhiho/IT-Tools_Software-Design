package kostovite;

import org.pf4j.ExtensionPoint;

public interface PluginInterface extends ExtensionPoint {
    String getName();
    void execute();
}