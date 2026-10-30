package com.jeepclub.backend.iam.authentication.infra.integration.dependents;

import com.jeepclub.backend.dependents.core.port.DependentPhotoSessionAccess;
import com.jeepclub.backend.iam.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.session.*;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.account.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.security.access.AccessDeniedException;

@Component @RequiredArgsConstructor
class DependentPhotoSessionAccessAdapter implements DependentPhotoSessionAccess {
    private final AccessTokenAuthenticationService authentication;
    public void validate(Long actor,Long session) {
        try {authentication.validate(actor,session);}
        catch(SessionInvalidException | SessionNotFoundException | SessionUserMismatchException
                | AuthenticationAccountAccessDeniedException | AuthenticationAccountNotFoundException ex) {
            throw new AccessDeniedException("Private photo access denied.");
        }
    }
}
