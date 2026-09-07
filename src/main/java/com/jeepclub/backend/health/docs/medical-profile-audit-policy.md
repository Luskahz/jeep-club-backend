# Política de auditoria e minimização de perfis médicos

## Eventos registrados

O Health registra em `medical_profile_audit_events` somente metadados de
governança: identificador do ator, tipo e identificador do owner, operação,
resultado e instante UTC. Leituras administrativas bem-sucedidas, criações,
atualizações e exclusões realizadas pelos fluxos de membro ou administração
são auditadas.

Uma negação de vínculo com dependente é persistida mesmo que a transação da
requisição seja revertida. Respostas `403` produzidas pelas permissões dos
endpoints administrativos também geram evento. Quando a autorização falha
antes de ser possível resolver o owner (por exemplo, rota por ID do perfil), o
ator e a operação são preservados e os campos do owner permanecem nulos.

Eventos de sucesso são escritos somente depois do commit da transação de
negócio, evitando registrar como concluída uma alteração revertida.

## Dados proibidos

Conteúdo clínico, dados de convênio, contato de emergência e o corpo da
requisição não podem integrar eventos, logs, mensagens de erro, tags de
métricas ou nomes de métricas. IDs técnicos podem ser usados para correlação.
Novos campos no contrato `MedicalProfileAuditEvent` exigem revisão de
privacidade.

Os DTOs detalhados de entrada e saída e o comando de upsert possuem
representação textual redigida. Os loggers internos do Spring MVC que imprimem
corpos ou exceções resolvidas e os loggers de bind de parâmetros do Hibernate
permanecem limitados/desativados pela configuração da aplicação.

## Minimização da API administrativa

- A listagem retorna somente IDs do perfil e owner e `updatedAt`.
- PUT retorna somente IDs do perfil e owner e `updatedAt`; não ecoa o payload.
- GET de um perfil individual continua detalhado porque essa é a operação
  explicitamente autorizada por `HEALTH_MEDICAL_PROFILE_READ`.
- DELETE não possui corpo de resposta.

A permissão de Dependents continua controlando o cadastro e o vínculo. As
permissões `HEALTH_MEDICAL_PROFILE_*` controlam exclusivamente o acesso ao
conteúdo no Health; uma não substitui a outra.

## Acesso e retenção

Os registros de auditoria não são expostos por endpoint HTTP. O acesso direto
à tabela é restrito à equipe de segurança/privacidade e a operadores de banco
formalmente autorizados, usando credencial nominal e trilha de acesso da
plataforma. Permissões administrativas de Health não concedem acesso à tabela.

Não existe foreign key física entre a auditoria do Health e Identity ou
Dependents. `actor_user_id`, `owner_type` e `owner_id` são referências lógicas;
uma FK entre bounded contexts depende de decisão arquitetural explícita.

Os eventos são retidos por cinco anos a partir de `occurred_at`, salvo retenção
legal formal. Após esse prazo devem ser removidos por rotina operacional
controlada, em lotes, registrando apenas quantidade e intervalo temporal
apagados. A rotina de expurgo não deve ler nem registrar dados de
`medical_profiles` ou `medical_profiles_history`.

Backups que contenham os eventos seguem o mesmo prazo; a expiração efetiva
ocorre pela rotação normal dos backups. Exportações para investigação têm
acesso temporário, finalidade documentada e destruição ao encerrar o caso.
