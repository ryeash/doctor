package demo.app;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import vest.doctor.Async;
import vest.doctor.event.ApplicationStarted;
import vest.doctor.event.EventListener;
import vest.doctor.event.EventProducer;
import vest.doctor.event.ServiceStarted;
import vest.doctor.event.ServiceStopped;

@Singleton
public class TCEvent {

    private static final Logger log = LoggerFactory.getLogger(TCEvent.class);
    public boolean eventListened = false;
    public String messageReceived;

    private final EventProducer producer;

    @Inject
    public TCEvent(EventProducer producer) {
        this.producer = producer;
    }

    @Inject
    @Async("background")
    public void message() {
        producer.publish("test");
    }

    @EventListener
    public void stringMessages(String message) {
        this.messageReceived = message;
    }

    @EventListener
    @Async
    public void onStartup(ApplicationStarted startup) {
        eventListened = true;
        Assert.assertNotNull(startup);
        Assert.assertNotNull(startup.providerRegistry());
    }

    @EventListener
    public void serviceStarts(ServiceStarted serviceStarted) {
        log.info("{}", serviceStarted);
    }

    @EventListener
    public void serviceStops(ServiceStopped serviceStopped) {
        log.info("{}", serviceStopped);
    }
}
