package vest.doctor.reactor.http.impl;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import vest.doctor.ProviderRegistry;
import vest.doctor.TypeInfo;
import vest.doctor.reactor.http.ExceptionHandler;
import vest.doctor.reactor.http.Filter;
import vest.doctor.reactor.http.Handler;
import vest.doctor.reactor.http.HttpResponse;
import vest.doctor.reactor.http.RequestContext;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Used internally for generated handlers based on endpoint methods.
 */
public abstract class AbstractWiredHandler implements Handler {

    private static final Logger log = LoggerFactory.getLogger(AbstractWiredHandler.class);
    protected static final String FILTER_ITERATOR = "doctor.reactor.http.filterIterator";
    protected final ProviderRegistry providerRegistry;
    protected final List<Filter> filters;
    protected final BodyInterchange bodyInterchange;
    protected final Scheduler workerScheduler;
    protected final ExceptionHandler exceptionHandler;

    protected AbstractWiredHandler(ProviderRegistry providerRegistry, String schedulerName) {
        this.providerRegistry = providerRegistry;
        this.filters = providerRegistry.getInstances(Filter.class).toList();
        this.bodyInterchange = providerRegistry.getInstance(BodyInterchange.class);
        this.workerScheduler = providerRegistry.getInstance(Scheduler.class, schedulerName);
        this.exceptionHandler = providerRegistry.getInstance(CompositeExceptionHandler.class);
    }

    @Override
    public final Publisher<Void> apply(HttpServerRequest request, HttpServerResponse response) {
        RequestContext requestContext = new RequestContextImpl(
                new HttpRequestImpl(request),
                new HttpResponseImpl(response),
                new LinkedHashMap<>());
        requestContext.attribute(FILTER_ITERATOR, filters.iterator());

        return Flux.just(requestContext)
                .subscribeOn(workerScheduler)
                .switchMap(this::doNextFilter)
                .onErrorResume(error -> handleError(requestContext, error))
                .switchMap(HttpResponse::send);
    }

    protected abstract TypeInfo responseType();

    protected abstract Object handle(RequestContext requestContext) throws Exception;

    private Publisher<HttpResponse> doNextFilter(RequestContext requestContext) {
        try {
            Iterator<Filter> iterator = requestContext.attribute(FILTER_ITERATOR);
            if (iterator.hasNext()) {
                Filter filter = iterator.next();
                return filter.filter(requestContext, this::doNextFilter);
            } else {
                Object result = handle(requestContext);
                return bodyInterchange.write(requestContext, responseType(), result);
            }
        } catch (Throwable t) {
            return Mono.error(t);
        }
    }

    private Publisher<HttpResponse> handleError(RequestContext requestContext, Throwable error) {
        try {
            return exceptionHandler.handle(requestContext, error);
        } catch (Throwable t) {
            log.error("error thrown by error handler", t);
            requestContext.response().status(500);
            return requestContext;
        }
    }
}
