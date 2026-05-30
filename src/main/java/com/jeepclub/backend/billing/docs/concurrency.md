# Concorrência no Billing

## Objetivo

Este documento registra as decisões de concorrência do módulo `billing`.

O objetivo é garantir consistência financeira quando múltiplas operações tentam alterar os mesmos recursos ao mesmo tempo.

As regras aqui documentadas devem orientar:

* services de aplicação;
* contratos de repository;
* adapters de persistência;
* testes transacionais;
* implementação de locks reais na infraestrutura.

## Princípio geral

O Billing não usa lock de tela.

O usuário não precisa apertar um botão para "começar edição" e outro para "finalizar edição".

A proteção contra concorrência deve acontecer no backend, dentro da transação.

## Lock de frontend não deve existir

Não deve existir fluxo como:

```text id="3d6tpi"
1. Usuário abre tela de edição.
2. Frontend chama endpoint para bloquear recurso.
3. Backend mantém recurso travado.
4. Usuário edita por tempo indeterminado.
5. Frontend chama endpoint para desbloquear.
```

Esse modelo não deve ser usado porque:

* depende do comportamento do navegador;
* pode deixar recurso preso se o usuário fechar a aba;
* cria complexidade desnecessária;
* não garante consistência real no banco;
* não resolve completamente concorrência entre requisições.

## Lock correto

O lock correto é transacional.

Ele acontece durante a execução de uma operação crítica.

Exemplo:

```text id="qjn1ty"
1. Requisição chega ao backend.
2. Service inicia transação.
3. Repository busca entidade com lock.
4. Service valida regras com estado atual.
5. Service altera entidade.
6. Service salva entidade.
7. Transação termina.
8. Lock é liberado.
```

## Onde o lock deve existir

As principais entidades que precisam de lock transacional são:

```text id="gcv56z"
MemberCharge
MemberPayment
```

## MemberCharge

`MemberCharge` deve ser lockada em operações que alteram ou dependem criticamente do estado financeiro da cobrança.

Exemplos:

* enviar pagamento;
* atualizar pagamento;
* confirmar pagamento;
* atualizar valor final da cobrança;
* cancelar cobrança.

## MemberPayment

`MemberPayment` deve ser lockado em operações que alteram o envio de pagamento.

Exemplos:

* atualizar envio;
* confirmar pagamento;
* rejeitar pagamento;
* cancelar pagamento, se esse fluxo existir no futuro.

## Contratos de repository

O core declara métodos específicos para busca com lock.

Exemplos:

```text id="jmvcg9"
MemberChargeRepository.findByIdForUpdate
MemberPaymentRepository.findByIdForUpdate
```

Esses métodos expressam intenção de escrita e concorrência.

## Implementação na infraestrutura

Na infraestrutura JPA, métodos `findByIdForUpdate` devem usar lock pessimista real.

Exemplo esperado:

```java id="kdj1g2"
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select c from MemberChargeEntity c where c.id = :id")
Optional<MemberChargeEntity> findByIdForUpdate(@Param("id") Long id);
```

E para payment:

```java id="6kcg0k"
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select p from MemberPaymentEntity p where p.id = :id")
Optional<MemberPaymentEntity> findByIdForUpdate(@Param("id") Long id);
```

## Ordem de lock

Quando uma operação precisa lockar `MemberPayment` e `MemberCharge`, a ordem deve ser consistente.

Ordem recomendada:

```text id="1c2m4q"
1. MemberPayment
2. MemberCharge
```

Essa ordem é usada em fluxos que partem do payment, como:

* atualizar payment;
* confirmar payment.

Para fluxos que partem diretamente da charge, lockar apenas a `MemberCharge` é suficiente.

Exemplos:

* atualizar finalAmount;
* cancelar cobrança individual;
* enviar novo payment por charge.

## Por que manter ordem consistente

A ordem consistente reduz risco de deadlock.

Exemplo perigoso:

```text id="ljcdsu"
Transação A:
1. locka MemberPayment
2. tenta lockar MemberCharge

Transação B:
1. locka MemberCharge
2. tenta lockar MemberPayment
```

Se as duas transações acontecerem ao mesmo tempo, pode haver deadlock.

Por isso, fluxos que precisam das duas entidades devem seguir uma ordem previsível.

## Fluxos críticos

## 1. Submit de payment

Fluxo:

```text id="zb85ik"
1. Usuário envia novo pagamento para uma MemberCharge.
2. Sistema locka MemberCharge.
3. Sistema valida se a charge pertence ao usuário.
4. Sistema verifica se não existe payment editável.
5. Sistema valida se a charge aceita nova submissão.
6. Sistema salva MemberPayment.
```

Entidade lockada:

```text id="zsh6t7"
MemberCharge
```

Motivo:

* impedir alteração concorrente de `finalAmount`;
* impedir cancelamento concorrente da cobrança;
* garantir validação atualizada de status e janela de pagamento.

## 2. Update de payment

Fluxo:

```text id="anhpq0"
1. Usuário atualiza payment existente.
2. Sistema locka MemberPayment.
3. Sistema locka MemberCharge vinculada.
4. Sistema valida propriedade da cobrança.
5. Sistema valida status do payment.
6. Sistema valida estado da charge.
7. Sistema atualiza o payment.
```

Entidades lockadas:

```text id="4buvj7"
MemberPayment
MemberCharge
```

Motivo:

* impedir confirmação concorrente do payment errado;
* impedir rejeição concorrente inconsistente;
* impedir alteração concorrente de `finalAmount`;
* impedir pagamento ser atualizado depois de confirmado.

## 3. Confirm de payment

Fluxo:

```text id="kqkxls"
1. Administrador confirma payment.
2. Sistema locka MemberPayment.
3. Sistema locka MemberCharge vinculada.
4. Sistema valida se payment está PENDING_VALIDATION.
5. Sistema valida se charge está PENDING.
6. Sistema valida se amount ainda bate com finalAmount.
7. Sistema confirma payment.
8. Sistema marca charge como PAID.
```

Entidades lockadas:

```text id="l122vj"
MemberPayment
MemberCharge
```

Motivo:

* impedir update concorrente do payment;
* impedir reject concorrente;
* impedir alteração concorrente do valor final;
* impedir cancelamento concorrente da cobrança;
* garantir que a cobrança seja marcada como paga uma única vez.

## 4. Reject de payment

Fluxo:

```text id="zimelr"
1. Administrador rejeita payment.
2. Sistema locka MemberPayment.
3. Sistema valida se está PENDING_VALIDATION.
4. Sistema marca como REJECTED.
```

Entidade lockada:

```text id="schd7q"
MemberPayment
```

Motivo:

* impedir confirmação concorrente;
* impedir update concorrente no mesmo payment.

## 5. Update finalAmount

Fluxo:

```text id="bj2a3n"
1. Administrador altera valor final da cobrança.
2. Sistema locka MemberCharge.
3. Sistema verifica se não há payment PENDING_VALIDATION.
4. Sistema valida se cobrança está PENDING.
5. Sistema valida se cobrança não está efetivamente expirada.
6. Sistema atualiza finalAmount.
```

Entidade lockada:

```text id="m79zdl"
MemberCharge
```

Motivo:

* impedir submit concorrente de payment com valor antigo;
* impedir confirm concorrente;
* impedir cancelamento concorrente da cobrança.

## 6. Cancel de MemberCharge

Fluxo:

```text id="fe1cwn"
1. Administrador cancela cobrança individual.
2. Sistema locka MemberCharge.
3. Sistema valida se não está PAID.
4. Sistema valida se não está CANCELED.
5. Sistema marca como CANCELED.
```

Entidade lockada:

```text id="f422p1"
MemberCharge
```

Motivo:

* impedir confirmação concorrente de payment;
* impedir novo submit concorrente;
* impedir alteração concorrente do valor.

## Cenários de concorrência esperados

## Cenário 1 — Usuário atualiza payment enquanto admin confirma

### Admin confirma primeiro

```text id="b72xlz"
1. Admin chama confirm.
2. Backend locka MemberPayment.
3. Backend locka MemberCharge.
4. Payment vira CONFIRMED.
5. Charge vira PAID.
6. Transação termina.
7. Usuário chama PUT depois.
8. Backend locka MemberPayment.
9. Backend detecta status CONFIRMED.
10. Backend retorna conflito de estado.
```

Resultado:

```text id="k7c53y"
O usuário não consegue sobrescrever payment confirmado.
```

### Usuário atualiza primeiro

```text id="1lqach"
1. Usuário chama PUT.
2. Backend locka MemberPayment.
3. Backend locka MemberCharge.
4. Payment é atualizado.
5. Payment fica PENDING_VALIDATION.
6. Transação termina.
7. Admin chama confirm depois.
8. Admin confirma a versão atualizada.
```

Resultado:

```text id="nj5cyd"
O admin confirma os dados mais recentes.
```

## Cenário 2 — Admin rejeita enquanto usuário atualiza

### Admin rejeita primeiro

```text id="9c95rd"
1. Admin chama reject.
2. Backend locka MemberPayment.
3. Payment vira REJECTED.
4. Transação termina.
5. Usuário chama PUT.
6. Backend locka MemberPayment.
7. Backend detecta REJECTED.
8. Backend valida se charge ainda aceita nova submissão.
9. Se aceitar, payment volta para PENDING_VALIDATION.
```

Resultado:

```text id="8xsa01"
O usuário consegue corrigir rejeição se a janela da cobrança ainda permitir.
```

### Usuário atualiza primeiro

```text id="mkbayf"
1. Usuário chama PUT.
2. Backend locka MemberPayment.
3. Payment é atualizado.
4. Payment fica PENDING_VALIDATION.
5. Transação termina.
6. Admin chama reject depois.
7. Admin rejeita a versão atualizada.
```

Resultado:

```text id="u1qmtu"
O admin rejeita os dados mais recentes.
```

## Cenário 3 — Admin altera finalAmount enquanto usuário envia payment

```text id="lkfaxc"
1. Admin chama updateFinalAmount.
2. Usuário chama submit payment.
3. Ambos disputam lock na mesma MemberCharge.
4. Quem lockar primeiro conclui.
5. O segundo fluxo revalida as regras com o estado atualizado.
```

Resultado:

```text id="3hctga"
Não deve existir payment pendente com valor incompatível com o finalAmount atual.
```

## Cenário 4 — Admin cancela cobrança enquanto usuário envia payment

```text id="f5vkyx"
1. Admin chama cancel MemberCharge.
2. Usuário chama submit payment.
3. Ambos disputam lock na mesma MemberCharge.
4. Se cancelamento vencer, submit falha porque charge não aceita payment.
5. Se submit vencer, cancelamento deve reavaliar o estado depois.
```

Resultado:

```text id="rhsjq9"
Não deve haver pagamento novo para cobrança cancelada.
```

## Cenário 5 — Admin confirma payment depois do prazo

```text id="iz53gr"
1. Usuário enviou payment enquanto a cobrança aceitava pagamento.
2. Payment ficou PENDING_VALIDATION.
3. A janela da cobrança expirou.
4. Admin confirma o payment.
5. Backend confirma sem validar a janela atual da cobrança.
6. Charge vira PAID.
```

Resultado:

```text id="a95x9t"
Pagamento enviado corretamente pode ser confirmado depois do prazo.
```

## Cenário 6 — Payment rejeitado depois do prazo

```text id="xz54vf"
1. Usuário enviou payment enquanto a cobrança aceitava pagamento.
2. Admin rejeitou o payment.
3. A janela da cobrança expirou.
4. Usuário tenta corrigir via PUT.
5. Backend valida nova submissão porque status era REJECTED.
6. Backend bloqueia porque charge não aceita mais payment.
```

Resultado:

```text id="69pl88"
Payment rejeitado não pode ser reenviado após expiração da cobrança.
```

## Cenário 7 — Payment pendente atualizado depois do prazo

```text id="z5srxj"
1. Usuário enviou payment enquanto a cobrança aceitava pagamento.
2. Payment ficou PENDING_VALIDATION.
3. A janela da cobrança expirou.
4. Usuário atualiza o payment via PUT.
5. Backend não revalida janela porque a submissão já estava dentro do fluxo.
6. Backend mantém payment como PENDING_VALIDATION.
```

Resultado:

```text id="jwnl2x"
Payment pendente pode ser corrigido mesmo após o prazo, pois já entrou no fluxo.
```

## Concorrência com refund

## Criação de refund por cancelamento de ciclo

Quando um ciclo é cancelado, o sistema pode criar elegibilidades de reembolso para pagamentos elegíveis.

Antes de criar um refund, deve verificar:

```text id="xwt49h"
1. se já existe refund ativo para o MemberPayment;
2. se já existe refund marcado como REFUNDED para o MemberPayment.
```

Isso evita duplicidade lógica.

## Possível melhoria futura

Em cenários de alta concorrência, a infraestrutura pode reforçar a regra com índice único parcial ou estratégia equivalente para impedir múltiplos refunds ativos do mesmo payment.

A regra já deve existir no core, mas pode ser reforçada no banco.

## Locks declarados no core

O core deve declarar métodos com intenção explícita de lock:

```text id="7sdrrj"
MemberChargeRepository.findByIdForUpdate
MemberPaymentRepository.findByIdForUpdate
```

## Locks futuros possíveis

Pode ser avaliado futuramente adicionar lock em:

```text id="ihtqlz"
ChargeCycleRepository.findByIdForUpdate
MemberRefundRepository.findByIdForUpdate
```

Principalmente para:

* cancelamento de ciclo;
* aprovação de refund;
* marcação de refund como reembolsado.

Esses locks não bloqueiam o desenho atual de API + CORE, mas podem fortalecer a infraestrutura.

## Regras de implementação para adapters

## Adapter JPA

O adapter JPA deve garantir que `findByIdForUpdate` realmente aplique lock no banco.

Exemplo:

```java id="x6dwnu"
@Lock(LockModeType.PESSIMISTIC_WRITE)
```

## Transação obrigatória

Métodos que usam `findByIdForUpdate` devem executar dentro de transação.

Exemplo:

```java id="t8f9wm"
@Transactional
public MemberPaymentResult confirm(...) {
    ...
}
```

Sem transação, o lock pode não se comportar corretamente.

## Read-only não deve usar lock de escrita

Métodos apenas de consulta devem usar `@Transactional(readOnly = true)` e não devem buscar entidades com lock de escrita.

## Timeout de lock

A infraestrutura pode configurar timeout para evitar espera indefinida.

Exemplo conceitual:

```text id="2bzszy"
jakarta.persistence.lock.timeout
```

Se o timeout expirar, o sistema deve retornar erro adequado de concorrência ou indisponibilidade temporária.

## Erros de concorrência

Quando uma operação falhar porque outra transação alterou o estado antes, a resposta deve ser tratada como conflito de estado.

Exemplos:

```text id="7ljtqv"
PUT em payment já confirmado -> 409 CONFLICT
Confirm em payment já rejeitado -> 409 CONFLICT
Submit em charge cancelada -> 409 CONFLICT
Update finalAmount com payment pendente -> 409 CONFLICT
```

## Regras finais

```text id="5j7ve0"
1. Não usar lock de tela.
2. Não criar endpoint de lock/deslock.
3. Usar lock transacional no backend.
4. Lockar MemberPayment em update/confirm/reject.
5. Lockar MemberCharge em submit/update/confirm payment.
6. Lockar MemberCharge em updateFinalAmount/cancel.
7. Implementar findByIdForUpdate com PESSIMISTIC_WRITE na infra.
8. Garantir que métodos com lock estejam dentro de @Transactional.
9. Revalidar regra após adquirir lock.
10. Retornar conflito quando o estado mudou antes da operação.
```
