package com.petsparc5.alerts.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Template base for scheduled pull-based pollers. Subclasses provide the
 * mapping from the source-specific DTO to {@link RawEvent}s; this class owns
 * the fetch-map-publish flow and default error handling.
 *
 * @param <T> the type returned by the {@link FeedClient}
 */
public abstract class SourcePoller<T> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final FeedClient<T> feedClient;
    private final RawEventPublisher rawEventPublisher;

    /**
     * Creates a poller wired to the given feed client and publisher.
     *
     * @param feedClient        the client used to fetch the feed
     * @param rawEventPublisher the publisher used to emit raw events
     */
    protected SourcePoller(FeedClient<T> feedClient, RawEventPublisher rawEventPublisher) {
        this.feedClient = feedClient;
        this.rawEventPublisher = rawEventPublisher;
    }

    /**
     * Maps a deserialized feed response to the raw events to be published.
     *
     * @param result the deserialized response from the feed client
     * @return the raw events derived from the feed response
     */
    protected abstract List<RawEvent> toRawEvents(T result);

    /**
     * Runs the poll cycle inside a try-catch, delegating to
     * {@link #onPollError} if an exception is thrown. Concrete subclasses
     * should call this from their {@code @Scheduled} method.
     */
    protected final void safeExecute() {
        try {
            execute();
        } catch (Exception e) {
            onPollError(e);
        }
    }

    /**
     * Called when {@link #safeExecute()} catches an exception. The default
     * implementation logs the error; subclasses may override to add alerting,
     * metrics, or custom recovery logic.
     *
     * @param e the exception thrown during the poll cycle
     */
    protected void onPollError(Exception e) {
        log.error("Poll failed for {}: {}", getClass().getSimpleName(), e.getMessage(), e);
    }

    private void execute() {
        toRawEvents(feedClient.fetch()).forEach(rawEventPublisher::publish);
    }
}
