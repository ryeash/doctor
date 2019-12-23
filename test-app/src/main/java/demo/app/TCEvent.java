package demo.app;

import org.testng.Assert;
import vest.doctor.ApplicationStartedEvent;
import vest.doctor.Async;
import vest.doctor.EventListener;
import vest.doctor.EventProducer;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TCEvent {

    public boolean eventListened = false;
    public String messageReceived;

    private final EventProducer producer;

    @Inject
    public TCEvent(EventProducer producer) {
        this.producer = producer;
    }

    @Inject
    @Async
    public void message() {
        producer.publish("test");
    }

    @EventListener
    public void stringMessages(String message) {
        this.messageReceived = message;
    }

    @EventListener
    public void onStartup(ApplicationStartedEvent startup) {
        eventListened = true;
        Assert.assertNotNull(startup);
        Assert.assertNotNull(startup.beanProvider());
    }
}
