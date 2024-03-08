package vest.doctor.ssf;

import vest.doctor.sleipnir.ChannelContext;

public interface WSHandler {

    void onConnect(ChannelContext channelContext);

    void onFrame(); // TODO
}
