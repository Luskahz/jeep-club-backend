package com.jeepclub.backend.vehicles.core.application.exceptions;

public class UserNotActiveException extends RuntimeException {
  public UserNotActiveException(String message) {
    super(message);
  }
}
