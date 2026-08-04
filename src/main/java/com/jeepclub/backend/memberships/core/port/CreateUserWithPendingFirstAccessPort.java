package com.jeepclub.backend.memberships.core.port;

/**
 * Port (outbound) do módulo membership para criação de usuários.
 *
 * A implementação deste port vive no módulo authentication
 * (authentication.infra.adapter), garantindo que o core do membership
 * não dependa diretamente da infraestrutura de autenticação.
 */
public interface CreateUserWithPendingFirstAccessPort {

    /**
     * Cria um novo usuário com status PENDING_FIRST_ACCESS a partir
     * dos dados básicos de uma solicitação de adesão aprovada.
     *
     * @param name        nome completo do candidato
     * @param email       e-mail do candidato
     * @param cpf         CPF normalizado (somente dígitos)
     * @param phoneNumber telefone do candidato
     * @return usuário criado e senha temporária em texto puro para entrega única
     */
    PendingFirstAccessUser createPendingUserWithTemporaryPassword(
            String name,
            String email,
            String cpf,
            String phoneNumber
    );

    /**
     * Cria um usuário pendente e gera um link que autoriza a definição da senha.
     */
    PendingFirstAccessLink createPendingUserWithAccessLink(
            String name,
            String email,
            String cpf,
            String phoneNumber
    );
}
