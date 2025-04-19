package kostovite;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class WebCamCapturePlugin extends Plugin {

    public WebCamCapturePlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("WebCamCapture plugin started");
    }

    @Override
    public void stop() {
        System.out.println("WebCamCapture plugin stopped");
    }
}