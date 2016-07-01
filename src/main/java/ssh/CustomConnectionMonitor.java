package ssh;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.testingbot.tunnel.App;

import ch.ethz.ssh2.ConnectionMonitor;
/**
 *
 * @author TestingBot
 */
public class CustomConnectionMonitor implements ConnectionMonitor {
    private final SSHTunnel tunnel;
    private final App app;
    private Timer timer;
    private boolean retrying = false;

    public CustomConnectionMonitor(SSHTunnel tunnel, App app) {
        this.tunnel = tunnel;
        this.app = app;
    }

    @Override
    public void connectionLost(Throwable reason) {
        if (tunnel.isShuttingDown()) {
            return;
        }

        app.getHttpProxy().stop();

        Logger.getLogger(CustomConnectionMonitor.class.getName()).log(Level.SEVERE, "SSH Connection lost! {0}", reason.getMessage());

        if (this.retrying == false) {
            this.retrying = true;
            timer = new Timer();
            timer.schedule(new PollTask(), 10000, 10000);
        }
    }

    class PollTask extends TimerTask {
        @Override
        public void run() {
            try {
                Logger.getLogger(CustomConnectionMonitor.class.getName()).log(Level.INFO, "Trying to re-establish SSH Connection");
                tunnel.stop();
                tunnel.connect();
                if (tunnel.isAuthenticated()) {
                    retrying = false;
                    timer.cancel();
                    app.getHttpProxy().start();
                    tunnel.createPortForwarding();
                    Logger.getLogger(CustomConnectionMonitor.class.getName()).log(Level.INFO, "Successfully re-established SSH Connection");
                }
            } catch (Exception ex) {
                Logger.getLogger(CustomConnectionMonitor.class.getName()).log(Level.WARNING, ex.getMessage());
            }
        }
    }
}
