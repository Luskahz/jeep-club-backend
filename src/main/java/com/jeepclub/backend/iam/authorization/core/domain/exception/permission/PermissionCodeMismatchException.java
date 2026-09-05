package com.jeepclub.backend.iam.authorization.core.domain.exception.permission;

import com.jeepclub.backend.shared.authorization.PermissionCode;

public class PermissionCodeMismatchException extends RuntimeException {

    public PermissionCodeMismatchException(PermissionCode currentCode, PermissionCode definitionCode) {
        super("Cannot synchronize permission with different code. Current code: "
                + currentCode + ". Definition code: " + definitionCode + ".");
    }
}