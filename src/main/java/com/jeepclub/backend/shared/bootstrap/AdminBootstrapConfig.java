package com.jeepclub.backend.shared.bootstrap;

import java.time.LocalDate;

public interface AdminBootstrapConfig {

    boolean enabled();

    String name();

    LocalDate birthDate();

    String email();

    String cpf();

    String rg();

    String password();

    String phoneNumber();

    String roleName();
}