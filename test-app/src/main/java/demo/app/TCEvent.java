package demo.app;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import vest.doctor.Async;
import vest.doctor.Eager;
import vest.doctor.event.ApplicationStarted;
import vest.doctor.event.EventBus;
import vest.doctor.event.EventConsumer;

import java.util.concurrent.CompletableFuture;

@Eager
@Singleton
public class TCEvent implements EventConsumer<Object> {

    private static final Logger log = LoggerFactory.getLogger(TCEvent.class);
    public CompletableFuture<Void> eventListened = new CompletableFuture<>();
    public String messageReceived;

    private final EventBus producer;

    @Inject
    public TCEvent(EventBus producer) {
        this.producer = producer;
    }

    @Inject
    @Async("background")
    public void message() {
        producer.publish("test");
    }

    @Override
    public void accept(Object event) {
        if (event instanceof ApplicationStarted startup) {
            eventListened.complete(null);
            Assert.assertNotNull(startup);
            Assert.assertNotNull(startup.providerRegistry());
        } else if (event instanceof String) {
            this.messageReceived = (String) event;
        } else {
            log.info("{}", event);
        }
    }
}
