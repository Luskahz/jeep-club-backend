package com.jeepclub.backend.tools.core.application.exception;


// aqui vc tem que criar as exceptions personalizadas que estouram no seu service, no domain vc vai criar as exceptions personalizadas
// que estouram no seu domain essas classes são as que vão gerar conteudo pro seu handler.
public class CrieExceptionsAquiVinicius extends RuntimeException {
    public CrieExceptionsAquiVinicius(String message) {
        super(message);
    }
}
