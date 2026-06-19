package com.hit.protocol;

/**
 * Outcome status carried by every {@link RouteResponse}.
 */
public enum Status {

    /** Path found successfully. */
    OK,

    /** Source and destination are valid but disconnected. */
    NO_PATH,

    /** Source or destination doesn't exist on campus. */
    BAD_REQUEST,

    /** Unexpected server-side error — see {@code errorMessage}. */
    SERVER_ERROR
}
