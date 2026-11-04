package com.jeepclub.backend.iam.identity.api.module.exception;

public final class UserNameSearchTooBroadException extends RuntimeException {
    public UserNameSearchTooBroadException() { super("Owner name matches more than 500 identities; refine the search."); }
}
