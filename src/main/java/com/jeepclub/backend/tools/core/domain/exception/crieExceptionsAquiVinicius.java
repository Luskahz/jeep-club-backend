package com.jeepclub.backend.tools.core.domain.exception;


// aqui ficam as exceptions que estouram no model, para defesa da classe.
public class crieExceptionsAquiVinicius extends RuntimeException {
    public crieExceptionsAquiVinicius(String message) {
        super(message);
    }
}
