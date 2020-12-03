package doctor.cluster;

import doctor.cluster.model.MessageContainer;
import io.netty.util.concurrent.CompleteFuture;

public interface ComChannel {

    CompleteFuture<MessageContainer> sendReceive(MessageContainer message);
}
