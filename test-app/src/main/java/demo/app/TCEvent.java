package demo.app;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import vest.doctor.Async;
import vest.doctor.event.ApplicationStarted;
import vest.doctor.event.EventConsumer;
import vest.doctor.event.EventProducer;

@Singleton
public class TCEvent implements EventConsumer<Object> {

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

    @Override
    public void accept(Object event) {
        if (event instanceof ApplicationStarted) {
            eventListened = true;
            ApplicationStarted startup = (ApplicationStarted) event;
            Assert.assertNotNull(startup);
            Assert.assertNotNull(startup.providerRegistry());
        } else if (event instanceof String) {
            this.messageReceived = (String) event;
        } else {
            log.info("{}", event);
        }
    }
}
