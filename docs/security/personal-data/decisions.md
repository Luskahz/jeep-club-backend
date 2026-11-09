# BACK-477 — classificação e contratos para decisão

Data: 04/11/2026. Estado: preparado para revisão; aceite funcional e de
tratamento pendente. A [matriz](matrix.md) continua sendo o inventário por campo
da auditoria. Este registro explicita decisões necessárias para implementar a
Story BACK-409 vinculada à Epic BACK-425.

## Proteção proposta por finalidade

| Grupo | Tratamento proposto | Critério que precisa de aceite |
| --- | --- | --- |
| Saúde atual e histórica, contatos de emergência | AEAD; sem índice clínico | Finalidade, papéis de acesso emergencial e prazo de retenção |
| CPF/RG/e-mail e identificadores de convidados | AEAD e HMAC separado por propósito nas buscas de igualdade | Resolver substring/sort de Identity antes de mudar persistência |
| Nome, nascimento, telefone, vínculo e textos privados | AEAD conforme matriz; índice apenas quando houver busca aprovada | Exceções temporárias explícitas para campos pesquisados/ordenados |
| Placa e RENAVAM operacionais | AEAD e HMAC, preservando unicidade e canonicalização | Histórico cifrado sem unicidade do identificador atual |
| Conteúdo destinado à publicação | Política explícita de publicação e minimização | Separar conteúdo privado de pedido/revisão do conteúdo autorizado para publicação |
| Valores, estados financeiros e vínculos relacionais | Proposta de manter em claro com autorização e controles de infraestrutura | Aceitar risco de correlação e exposição de situação financeira ou ampliar proteção |
| Comprovantes, fotos e outros bytes privados | Proteção dos bytes e autorização por recurso | Classes de mídia, EXIF, acesso, retenção e recuperação |
| Logs operacionais | Minimizar dados; correlação por requestId e ID interno | Retenção/acesso por ambiente; tratar também proxy, SMTP e APM |
| Auditoria obrigatória | Conservar fatos necessários e proteger alvos privados | Retenção por finalidade; cifrar campos selecionados sem perder auditabilidade |

Nenhuma linha equivale a aprovação jurídica ou aceite de risco. Dados de
dependentes e textos/imagens exigem avaliação da finalidade; não classificar
toda imagem como biometria nem todo texto como dado sensível por presunção.

## Contrato de busca vigente que bloqueia Identity

Revalidado em `AdminUserJpaQueryRepository`, `AdminUserFilter` e `UserSortMapper`:

- filtros `name` e `email`: substring com normalização para minúsculas;
- filtro `phoneNumber`: substring;
- filtros `cpf`, `rg` e `birthDate`: igualdade;
- `q`: substring em nome/e-mail/CPF/RG/telefone e resolução de número de membro;
- sort permitido: `id`, `name`, `birthDate`, `email`, `cpf`, `rg`,
  `phoneNumber`, `profilePhotoStorageKey`, `status`, `createdAt`, `disabledAt`,
  `updatedAt`; `id` é acrescentado para desempate;
- paginação, contagem total e seleção de campos são executadas no banco.

HMAC de igualdade não atende substring; ordenar ciphertext não atende o sort
vigente. Descriptografar a base inteira na aplicação não é o plano de paginação.

Decisão D01, responsável: produto + arquitetura + responsável pelo tratamento.
Escolher e registrar uma destas alternativas com data e responsável:

1. Evoluir explicitamente o contrato para busca exata em campos protegidos e
   ordenação por metadados permitidos, com ajuste de frontend/OpenAPI/testes.
2. Preservar o contrato atual com exceções temporárias por campo, risco aceito,
   prazo e plano de retirada. Nesse caso a proteção parcial precisa ficar
   explícita no gate da Story.

Nenhuma alternativa foi escolhida nesta entrega. As queries atuais permanecem
como referência obrigatória para BACK-481.

## Demais decisões e evidências de fechamento

| Decisão | Responsável esperado | Evidência exigida |
| --- | --- | --- |
| D02 — finalidades, prazos e exceções por grupo/campo | Responsável pelo tratamento + módulos | Aceite datado, prazos e regra de expurgo/anonimização, incluindo históricos/backups |
| D03 — provider e key management por ambiente | Platform + operação | BACK-478: serviço, IAM, região, versões, segregação, recuperação e ensaio de indisponibilidade |
| D04 — mídia e auditoria clínica | Produto + módulos + segurança | Política de acesso por classe e tratamento de alvos/bytes privados |
| D05 — lacunas desde a auditoria de setembro | Módulos + arquitetura | Revisão de campos acrescentados desde a matriz, inclusive cadastro, onboarding e novos históricos |

A existência de runbooks VPS/Docker/MySQL no repositório não comprova KMS ou
proteção efetiva do ambiente. Não acessar segredos ou executar backfill para
produzir esse aceite. BACK-477 permanece pendente até registrar as decisões
aplicáveis e atualizar a matriz com as alterações posteriores à auditoria.
