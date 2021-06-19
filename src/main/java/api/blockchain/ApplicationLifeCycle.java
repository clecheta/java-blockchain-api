package api.blockchain;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
//import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class ApplicationLifeCycle {

    //TODO use the below methods to chain syncronization

    //private static final Logger LOGGER = Logger.getLogger("ListenerBean");

    void onStart(@Observes StartupEvent ev) {
        //LOGGER.info("Blockchain network node starting...");
        System.out.println("\nBlockchain network node starting...\n");
    }

    void onStop(@Observes ShutdownEvent ev) {
        //LOGGER.info("Blockchain network node stopping...\n");
        System.out.println("\nBlockchain network node stopping...\n");
    }

}
