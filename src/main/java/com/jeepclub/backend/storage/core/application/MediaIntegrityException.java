package com.jeepclub.backend.storage.core.application;

/** Physical content or custody metadata differs from the verified catalogue. Never an ACL decision. */
public class MediaIntegrityException extends IllegalStateException {
    public MediaIntegrityException() {super("Media integrity verification failed");}
}
