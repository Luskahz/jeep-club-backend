# Findings e backlog de implementação

Relacionado à [auditoria](README.md) e à [proposta](architecture.md).
Os IDs `PD-*` são referências locais, **não issues já criadas no Jira**.
As estimativas são faixas iniciais de dias de engenharia, sem espera por
provider/aprovação, para refinamento pelo time. Nenhum item autoriza mudança
de contrato ou criptografia antes da revisão da proposta.

## Findings verificáveis

| ID | Evidência da baseline | Risco / decisão |
| --- | --- | --- |
| F01 | `MedicalProfileEntity`, `MedicalProfileHistoryEntity` e mappers persistem campos clínicos sem transformação criptográfica | Alta prioridade: leak de DB expõe saúde atual e histórica; implementar ambos no mesmo incremento |
| F02 | CPF/contatos em Identity, Memberships, Dependents; identificadores em Vehicles e guest requests | Fraude/correlação, múltiplas cópias; matriz e constraints orientam a proposta de cifra + índices por propósito |
| F03 | `logging.pattern.console`, `RequestContextEnrichmentFilter`, `JwtServiceImpl` incluem nome em logs/claim | Sanitização neutraliza caracteres, mas não mascara PII; remover nome do logging padrão e avaliar minimização de JWT com compatibilidade |
| F04 | `GlobalExceptionHandler` loga Throwable inesperado; worker System Logs e `PaymentReceiptLifecycle` também logam causa/key | Exceções de DB podem carregar valor duplicado/SQL; paths desconhecidos usam URI bruta; implementar redaction preservando diagnóstico seguro |
| F05 | `LocalFileStorage` grava bytes com `Files.write`, sem camada criptográfica; images só verificam MIME/extensão/signature | Bytes de foto, documento e comprovante são outra cópia; provider e volume externos não foram inspecionados; EXIF/texto embutido permanecem possíveis |
| F06 | `ImageMediaController.download` exige autenticação, mas não recebe owner/recurso; `PaymentReceiptService.find` verifica dono/authority | Conhecer chave permite resolução de imagem por qualquer autenticado; UUID não é autorização. Definir política por classe de mídia, evitando tratar imagens privadas como públicas |
| F07 | `PaymentReceiptController` envia `Cache-Control: private, max-age=300`; fila de System Logs até 5.000 eventos; nenhum cache distribuído identificado | Cliente pode reter comprovante após resposta; fila contém metadados pessoais. Definir no-store para materiais privados e revisão de proxy/APM/browser |
| F08 | Históricos de exclusão retidos em Health/Dependents/Vehicles/Tools/Publications; cleanup encontrado somente em System Logs (90 dias padrão) | Ausência de política demonstrada de expurgo por finalidade; deletion de domínio não atende automaticamente eliminação do titular |
| F09 | `AdminUserJpaQueryRepository` usa LIKE e projections; `UserSortMapper` permite sort em todos os campos do cadastro | Cifra/blind index exige decisão de produto e redesenho das queries; não reduzir silenciosamente contrato |
| F10 | Properties usam perfil `dev` por padrão; bootstrap runner é `@Profile("dev")`; dados identificadores de exemplo em config local | Deploy sem perfil explícito pode carregar fluxo de desenvolvimento; não há evidência de execução indevida em produção; exigir configuração segura e teste de bootstrap |
| F11 | Não localizada política versionada de backup/restore, TDE, KMS ou infraestrutura de logs | Não afirmar que infra é desprotegida; obter evidências e runbooks antes do go-live |
| F12 | SMTP envia nome/e-mail/motivo/links; temporaryPassword/access tokens saem em respostas; hashes no DB | Proteção de banco não cobre mailboxes/clientes, links e dumps de responses; retenção/TLS/telemetria precisam ser validados fora do DB |

## Stories/Tasks prontas para refinamento

| ID / tipo | Prioridade / owner / esforço | Escopo e aceite | Dependências |
| --- | --- | --- | --- |
| PD-01 Spike — aprovar classificação e contrato de busca | P0; arquitetura + produto + responsável pelo tratamento; 2–3 dias | Revisar matriz e risco residual, finalidade/retenção, modelo de ameaça; decidir substring/sort de Identity e busca por identificadores; registrar aceite ou exceção datada por campo | BACK-425/409 |
| PD-02 Spike/Task — selecionar KMS e configurar key management | P0; Platform + DevOps; 3–5 dias | Comparar provider/custo/quotas/DR; workload identity, segregação por ambiente, keyring externo, DEK/HMAC/JWT separados, política de rotação/revogação e alertas; nenhum segredo em DB/Git; restore ensaiado | PD-01 |
| PD-03 Story — capacidade técnica de AEAD e blind index | P0; Platform/Shared; 4–6 dias | Envelope/AAD/cryptoRecordId, nonce/tag, keyring e erros seguros; round-trip/null, tamper/swap, versão/chave desconhecida, rotação e indisponibilidade; métricas sem valores pessoais | PD-02 |
| PD-04 Story — proteger Health operacional e histórico | P0; Health; 3–5 dias | Cifrar todos os campos clínicos/contato listados; snapshot sob contexto próprio, mappers/read models, owner lookup inalterado; dump sintético sem texto clínico; emergency read/auditoria e indisponibilidade cobertos | PD-03; PD-12 se houver legado |
| PD-05 Story — proteger cadastro Identity e admissão Memberships | P0; Identity/Memberships; 5–8 dias | Ciphertext/HMAC de CPF/RG/e-mail, nome/DOB/telefone e textos privados; activeCpf nullable, constraints equivalentes, projections/sparse fields, consultas e canonicalizações; concorrência real MySQL; nenhum plaintext residual de cópias | PD-01 decide consultas; PD-03; PD-12 |
| PD-06 Story — proteger Dependents e Vehicles com históricos | P1 antes de go-live com esses módulos; módulos; 4–6 dias | Identificadores, nomes/DOB/contato/vínculo/apelido conforme matriz; snapshots; HMAC CPF/plate/RENAVAM, unicidade e tradução de erro; ownership e queries preservados | PD-03; PD-12 |
| PD-07 Story — proteger Event guests e contatos privados de Publications | P1 antes de go-live com esses módulos; Publications; 4–6 dias | CPF HMAC para unique(event,cpf), IN/GROUP BY/contagem e deduplicação; cifrar telefone/textos privados de request/change/history conforme decisão; conteúdo destinado à publicação mantém política explícita; auditar comentários/imagens/ocupantes | PD-01; PD-03; PD-12 |
| PD-08 Story — privacidade de logs e auditoria clínica | P0; Platform + módulos; 2–4 dias | Remover nome do MDC padrão; limitar/mascarar fallback URI, stack traces, storageKeys, dados de constraint; impedir payload/token/query em DEBUG/TRACE/APM; preservar auditoria clínica obrigatória e definir proteção/retention de path com target | PD-01; alinhar com PD-03 se path cifrado |
| PD-09 Story — storage protegido e classes de acesso a mídia | P0 para comprovantes; Platform/Billing; 4–7 dias | Bytes privados cifrados com envelope/provider externo, ACL de volume/objeto; owner/permission por classe de imagem, referência não é capability; avaliar EXIF e descarte de originais; upload/download/compensação/restore testados | PD-02/03; revisão contrato de imagens |
| PD-10 Task — controlar perímetro cliente, SMTP e deploy | P0; DevOps + frontend + Platform; 2–4 dias | Avaliar/implementar no-store para clínica/comprovante/credenciais, acesso e TTL de mail; TLS SMTP e DB, perfil prod explícito, bootstrap desativado; sanitizar proxy/tracing; evidências por ambiente | PD-01; F07/F10/F12 |
| PD-11 Story — retenção e eliminação por finalidade | P0 definição, P1 implementação; responsável pelo tratamento + módulos; 3–6 dias de engenharia após decisão | Prazos aprovados para clínicos/históricos, admissão/bloqueios, finanças, mídia e logs; expurgo/anonimização compatível com obrigações financeiras e backups; restauração não reintroduz dado eliminado; auditoria sem conteúdo | PD-01; política DR PD-02 |
| PD-12 Task — cutover/backfill e gate de produção | P0; arquitetura + DevOps + módulos; 3–6 dias por lote de módulos | Confirmar vazio/legado; schema revisado sem framework improvisado; job idempotente autorizado, índices duplos na rotação, snapshots e bytes, lock/checkpoint; remover plaintext ao final; rollback/restore e métricas verificados | PD-02/03 e incrementos de domínio |

PD-04 deve ser o primeiro incremento de persistência após a capacidade técnica.
PD-05 não pode ser liberado removendo filtros ou ordenação vigentes sem a
decisão de PD-01. PD-08/10 podem avançar após revisão de suas políticas, em
paralelo ao desenho criptográfico. Se ferramentas/publicações puderem conter
texto pessoal privado, o refinamento inclui essas colunas; não presumir que
todo texto de catálogo precise ser cifrado.

## Gate antes de considerar implementação concluída

Cada Story deve apresentar: mappings atuais e históricos protegidos, constraints
e queries compatíveis, evidência com massa sintética, erros seguros, ausência
de dados em logs, key management/restore e `./mvnw test` + `./mvnw verify`.
Não medir conclusão apenas pelo round-trip do cipher ou percentual de testes.

Infraestrutura e políticas externas pendentes devem ter owner, prazo e evidência
para aprovação de go-live. Esta task de investigação está concluída com a
proposta e o backlog; as Stories de implementação e as decisões de tratamento
permanecem separadas.
