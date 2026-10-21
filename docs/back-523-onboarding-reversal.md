# BACK-523 — Link administrativo e reversão segura do onboarding

Base temporária: BACK-512, PR #107, `b5c3c3b8e81944e8b2aded51ad5c7f8bb3eaea84`.
Cadeia: develop → BACK-460 (#105) → BACK-510 (#106) → BACK-512 (#107) → BACK-523 → BACK-524 → BACK-525.
Esta entrega é para revisão humana; não autoriza merge ou deploy.

## Investigação e critérios

A história oficial BACK-523 e as relacionadas BACK-511, BACK-512/514, BACK-481/487 e BACK-526 foram confrontadas com develop e os contratos acumulados. O código anterior retornava somente userId, montava a URL no sender SMTP e aceitava rejeição somente de PENDING. V4 mantém Member e FKs restritivas; V5 fornece exclusão segregada e segura de Identity, retenção financeira, histórico de vínculos e revogação de Authentication.

Aceites: devolver exatamente um convite também ao administrador; hash como única representação persistida do segredo; expiração/uso único/reenvio preservados; no-store e mesmas permissões; rejeitar PENDING ou APPROVED realmente pendente por ambos os métodos; recusar conta concluída, Member, finanças ou retenções; serializar ativação/reenvio/rejeição; rollback integral e histórico; notificações sem segredo e sem rejeição falsa antes de commit.

## HTTP e frontend

`POST /admin/membership-applications/{id}/approve/access-link` mantém `MEMBERSHIP_MEMBERSHIP_REQUEST_APPROVE`, retorna 200 com:

```json
{"userId":456,"activationLink":"https://frontend.example/membership/activate?token=REDACTED"}
```

O link da resposta é exatamente o argumento entregue ao sender SMTP, construído uma vez com `app.user-facing.membership-activation-url`. Query parameters existentes são preservados e o token é codificado como valor, sem duplicar parâmetro token. A resposta e a aprovação por senha temporária usam `Cache-Control: no-store`; OpenAPI documenta o header e a natureza sensível. Não há recuperação posterior do link. Listas, detalhe e exportações permanecem sem link/token. Os records de resposta redigem `toString`.

Na UI, mostrar/copiar apenas para o administrador autorizado quando solicitado; tratar como credencial bearer. Não persistir em localStorage, analytics, logs de rede, traces, relatórios de erro ou histórico de navegação. Proxies e ferramentas de observabilidade precisam respeitar a sensibilidade do corpo; no-store não substitui essa configuração. Não afirmar entrega na caixa postal com base no aceite do sender.

Reenvio mantém `POST /{id}/activation-link/resend`, 204 e permissão atual APPROVE; rotaciona hash e invalida os anteriores sem criar outro User. Os mesmos endpoints `/{id}/reject` e `/{id}/reject-and-block` passam a admitir APPROVED reversível, preservando respectivamente REQUEST_REJECT e APPLICANT_BLOCK. 204 indica commit da decisão; PENDING conserva a semântica anterior. COMPLETED/REJECTED ou provisionamento inseguro dão 409 RFC 9457. Falha de lock/versionamento também dá 409 recuperável. Não foi adicionado status ou rota redundante de rollback.

## Transação e concorrência

1. Um primeiro read resolve o createdUserId. Para APPROVED, obter o lock público de Identity antes do lock da solicitação. Recarregar a solicitação com PESSIMISTIC_WRITE e estado atual; descartar antes a instância JPA antiga, inclusive seu @Version. MySQL repeatable read não pode decidir a reversão por uma cópia antiga.
2. Confirmar via porta consumidora implementada por Authentication que a conta existe, está PENDING_FIRST_ACCESS, sem passwordChangedAt/lastLoginAt. Ausência de credencial é conflito.
3. Gravar evidência mínima da decisão anterior; invalidar todos os tokens Membership; gravar e flush da transição REJECTED na transação atual. Esse flush é necessário para que o guard público de exclusão não retenha a própria aprovação que está sendo revertida.
4. Chamar exclusivamente `UserAdministration.delete`. Identity/Authentication/Authorization conservam os próprios locks, guards, revogações, histórico e limpeza. Excluir credencial e cadastro operacional somente quando seguro. UserId original permanece na solicitação e nos históricos independentes. Nenhum repository privado de outro contexto é acessado.
5. Se solicitado, criar bloqueio na mesma transação. Qualquer erro desfaz solicitação, tokens, exclusão, históricos, auditoria e bloqueio. Notificar rejeição somente após commit.

O primeiro acesso por link e a troca obrigatória de senha já obtêm o mesmo lock de Identity. Ambos agora leem a solicitação para escrita com estado atual. Reenvio usa a mesma ordem. Aprovação de PENDING bloqueia a solicitação antes de criar o novo User; duas aprovações não provisionam duas contas/convites. Se uma leitura PENDING encontrar APPROVED após esperar, devolver conflito para retry sem inverter ordem de locks. O relógio é consultado após esperas e o consumo continua validando expiração após Authentication; TTL existente não foi alterado.

Impedimentos conservadores da BACK-512 continuam válidos: Member/matrícula/claims/auditoria, vínculos financeiros, ROOT, dados operacionais retidos, perfil complementar e qualquer fato histórico de Session. Credencial permanente ou primeiro acesso concluído também impedem. Uma conta pendente não recebe access/refresh tokens pelos fluxos legítimos; se houver Session histórica inesperada, a reversão inteira é recusada, preservando evidência. Não se apagam sessões ou fatos financeiros para permitir DELETE. Tokens/challenges pendentes, convites administrativos e recuperações são inutilizados pelo mecanismo seguro existente; sem Identity/credencial, desafios antigos não completam acesso. O teste de COMPLETED comprova que o conflito não revoga o access/refresh legítimo de um usuário já ativado.

O contrato público Identity normaliza impedimentos privados de exclusão em `UserDeletionConflictException`. HTTP DELETE de Identity continua 409, com código `USER_DELETION_CONFLICT` nesses casos. Membership traduz esse contrato para `MEMBERSHIP_FIRST_ACCESS_CONFLICT`. O fluxo administrativo independente BACK-511 não é alterado.

## Persistência e auditoria

Única migration desta entrega: `V6__membership_onboarding_review_history.sql`. V1–V5 e baseline permanecem intactas. Cria `membership_application_review_history`, índice `(application_id,id)`, operação/método, estados anterior/final, revisor/data anteriores, UserId original, ator e instante. Não copia CPF, contato, senha, token, link ou motivo livre. Sem FK para User ou solicitação: a evidência deve sobreviver à remoção operacional e não impede a reversão que documenta. Os campos da solicitação e histórico mínimo segregado de User continuam guardando a decisão e o identificador original.

Não há backfill fictício de auditoria antiga. Ao reverter uma aprovação anterior à V6, os dados efetivamente presentes na solicitação são capturados antes de sobrescrever sua revisão. A migration pode ser reexecutada de modo controlado sem duplicar dados, sem Flyway repair/clean. Flyway normal executa uma vez. Escritas de revisão e SystemLog são atômicas; tentativas impedidas são observáveis pela auditoria HTTP existente sem segredo.

## Notificações e limites operacionais

Aprovação preserva SMTP síncrono existente: falha de envio faz rollback e não devolve link; o administrador pode tentar novamente. Exceções do provider podem carregar o corpo completo do e-mail: a emissão as substitui por erro estático sem causa/suppressed antes de chegarem ao logger HTTP genérico. Nenhum segredo ou destinatário é copiado para o erro público/log. O banco e SMTP não constituem uma transação distribuída. Se SMTP aceitar e o commit falhar depois, o e-mail pode conter um convite sem efeito; repetir a aprovação/reenvio é a recuperação. Não há armazenamento reversível do token nem novo outbox com segredo. A garantia é uma emissão por transação bem-sucedida, não entrega exatamente uma vez pela rede.

Rejeição usa evento AFTER_COMMIT; rollback não notifica. Falha de entrega após commit registra somente mensagem sanitizada e mantém a decisão já confirmada. Não existe retry durável automático dessa notificação; suporte operacional pode providenciar novo contato. É uma limitação explícita a revisar, sem comprometer a integridade da reversão. Nenhuma exceção SMTP com possível conteúdo sensível é registrada pelo novo listener.

## Verificação e revisão

As evidências efetivamente executadas ficam em [back-523-test-evidence.json](back-523-test-evidence.json). Incluem aprovação/link idêntico, no-store, OpenAPI/permissions, segredo hash-only/redaction, expiração/uso único/reenvio, rejeições dos dois métodos, desafio anterior inutilizado, preservação de Member/finanças/sessões, rollback após exclusão real quando bloqueio falha, histórico de revisão e Identity, vínculos de Authorization, convite administrativo, SMTP e concorrência com as duas ordens de vitória. MySQL repete os cenários com Flyway e Hibernate validate; migration verifica legado e replay.

Priorizar na revisão: política conservadora de retenção; ausência de retry durável de aviso de rejeição; necessidade de redaction nos proxies; interpretação de conflitos em clientes que antes recebiam USER_LIFECYCLE_CONFLICT no DELETE de Identity; comportamento após aceitar SMTP e falhar commit. Testes não comprovam entrega de e-mail em serviço SMTP real.

## Checklist para release futura

- Integrar somente após revisão humana e na ordem dos PRs; esta branch tem base temporária BACK-512.
- Conferir V4→V5→V6 no banco local/homologação com dados representativos, checksums e validação JPA; nunca alterar baseline/repair/clean.
- V6 é DDL aditiva, sem transformação destrutiva dos dados existentes; prever lock de metadados e permissões CREATE/INDEX, backup e manutenção conforme volume/atividade.
- Nenhuma variável, volume, provider de storage ou flag operacional nova. Manter URL frontend e SMTP existentes válidos e corpos sensíveis redigidos em observabilidade.
- Preservar orientações de #103/#104 e deixar emissão/importação Registry desabilitadas até aprovação operacional. Esta entrega não homologa cadastro oficial, faixa inicial, importação histórica ou emissão automática.
- Adaptar frontend à nova resposta e aos 409 da reversão; não expor link nas consultas gerais. Verificar startup validate, autenticação antiga, convite/reenvio/expiração e rejeição com rollback em homologação.
- Depois de reversões confirmadas, downgrade de código não recompõe User/credenciais apagados. Recuperação exige procedimento auditado/forward fix ou restauração coordenada; não remover a tabela de histórico como rollback.
- Nenhuma ação nesta missão em VPS, master, develop, banco compartilhado/produtivo, deploy ou merges.
