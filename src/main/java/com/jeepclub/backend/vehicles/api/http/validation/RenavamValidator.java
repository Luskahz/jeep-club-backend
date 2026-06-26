package com.jeepclub.backend.vehicles.api.http.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RenavamValidator implements ConstraintValidator<ValidRenavam, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        String renavam = value.replaceAll("\\D", "");

        if (renavam.length() != 11) {
            return false;
        }

        if (renavam.matches("^(\\d)\\1{10}$")) {
            return false;
        }

        int digitoInformado = Character.getNumericValue(renavam.charAt(10));

        String primeirosDezDigitos = renavam.substring(0, 10);

        int[] pesos = {3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int soma = 0;

        for (int i = 0; i < 10; i++) {
            int digito = Character.getNumericValue(primeirosDezDigitos.charAt(i));
            soma += digito * pesos[i];
        }

        int resto = (soma * 10) % 11;

        int digitoCalculado = (resto == 10) ? 0 : resto;

        return digitoCalculado == digitoInformado;
    }
}
