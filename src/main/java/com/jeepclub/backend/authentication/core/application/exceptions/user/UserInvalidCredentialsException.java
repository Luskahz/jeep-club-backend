package com.jeepclub.backend.authentication.core.application.exceptions.user;

public class UserInvalidCredentialsException extends RuntimeException {

  private static final String DEFAULT_MESSAGE = "Invalid CPF or password.";

  public UserInvalidCredentialsException() {
    super(DEFAULT_MESSAGE);
  }
}