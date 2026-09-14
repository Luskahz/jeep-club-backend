# Arquitetura e governança documental

Este diretório é a primeira leitura obrigatória antes de qualquer feature, fix
ou criação de módulo no Jeep Club Backend. Ele registra regras transversais já
consolidadas na `develop`; o comportamento integrado no código prevalece sobre
documentação antiga ou proposta ainda não implementada.

## Ordem oficial de leitura e trabalho

```text
Feature / Fix
     ↓
1. Ler docs globais
     ↓
2. Identificar o módulo proprietário
     ↓
3. Ler os docs do módulo
     ↓
4. Auditar código e OpenAPI afetados
     ↓
5. Implementar e validar
     ↓
6. Atualizar somente as fontes documentais responsáveis
```

Não implemente uma regra descrita apenas em ticket ou documento futuro como se
ela já fizesse parte da arquitetura. Quando código e documentação vigente
divergirem, confirme o comportamento da `develop` e corrija a fonte documental
responsável na mesma mudança, se isso estiver dentro do escopo.

## Três níveis oficiais

| Nível | Fonte principal | Responsabilidade |
| --- | --- | --- |
| Global | `docs/architecture/` | Arquitetura transversal, boundaries, Platform, Shared e regras comuns de desenvolvimento. |
| Módulo | `<module>/docs/README.md` | Domínio, conceitos, invariantes, estados, fluxos, concorrência, integrações e decisões específicas do bounded context. |
| HTTP | OpenAPI gerado por controllers e DTOs | Método, path, parâmetros, payloads, schemas, validações observáveis, respostas, autorização e erros da superfície consumida pelo frontend. |

### Documentação de módulo

Todo bounded context deve ter `docs/README.md` como entrada, ao lado de `api`,
`core` e `infra`. Arquivos como `business-rules.md`, `flows.md`,
`state-models.md`, `concurrency.md` e `integration-contracts.md` são opcionais:
crie-os somente quando separarem conhecimento útil e durável. Não crie arquivo
vazio para completar uma árvore.

Docs de módulo não repetem regras globais. Eles referenciam este diretório e
registram somente o que pertence semanticamente ao bounded context.

### OpenAPI

OpenAPI/Swagger é o contrato da superfície HTTP consumida pelo frontend. A
documentação textual pode explicar intenção, fluxo ou decisão de domínio, mas
não mantém catálogo concorrente de rotas, requests e responses. `api-routes.md`
não é obrigatório; quando apenas repetir o Swagger, deve ser removido, reduzido
ou convertido em documentação conceitual na auditoria do módulo responsável.

## Uma informação, uma fonte de verdade

- arquitetura transversal → `docs/architecture/`;
- regra de negócio de Billing → `billing/docs/`;
- estado e transições de Vehicle → `vehicles/docs/`;
- contrato de `GET /billing/...` → OpenAPI do controller e dos DTOs envolvidos;
- detalhes profundos de logging ou storage → docs próprios da capacidade em
  `platform`, referenciados pelo guia global.

Uma mudança atualiza a fonte proprietária, não cópias independentes. Se uma
regra de módulo depender de regra global, use link e registre apenas a parte
específica. Se uma capacidade Platform tiver guia especializado, o global
resume como consumi-la e aponta para esse guia.

## Navegação obrigatória

1. [Organização dos módulos](module-organization.md) — ownership, camadas,
   comunicação entre módulos e limites de Platform/Shared.
2. [Regras globais para features e fixes](feature-development-rules.md) —
   identidade autenticada, autorização, erros, tempo, logging, storage,
   persistência, testes e OpenAPI.
3. [Logging e observabilidade HTTP](../../src/main/java/com/jeepclub/backend/platform/logging/docs/README.md)
   — contrato especializado de `platform.logging`.
4. [Armazenamento global](../../src/main/java/com/jeepclub/backend/platform/storage/docs/README.md)
   — contrato especializado de `shared.storage` e `platform.storage`.

`contract-sensitive-findings.md` é um registro de achados pendentes de trabalhos
anteriores, não uma fonte arquitetural. Revalide cada item contra a `develop`
antes de usá-lo.

## Responsabilidade da mudança

Ao concluir um trabalho:

- atualize este nível somente se uma regra transversal mudou;
- atualize os docs do módulo somente se seu domínio ou arquitetura específica
  mudou;
- atualize as anotações OpenAPI e seus testes quando o contrato HTTP mudou;
- atualize um guia especializado de Platform quando o detalhe daquela
  capacidade mudou;
- preserve links entre as fontes, sem copiar o mesmo conteúdo.

Evolução observável de método, path, parâmetros, payload, validação, status,
erro ou autorização não é uma simples correção de Markdown: exige escopo
explícito de evolução de contrato e validação compatível.
