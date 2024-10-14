package hu.bbox.proxy.handlers;

import org.reactivestreams.Publisher;
import reactor.netty.*;

import java.util.function.BiFunction;
import java.util.function.Consumer;

public interface ConnectionHandler extends
        BiFunction<NettyInbound, NettyOutbound, Publisher<Void>>, Consumer<Connection>, ConnectionObserver {
}
