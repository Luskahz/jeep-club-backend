# ADR preliminar — Fundação do catálogo Storage (BACK-549)

**Status: PROPOSTO / preliminar; pendente de validação final na BACK-548.**

Data: 05/11/2026. Implementação isolada autorizada excepcionalmente por BACK-548 #10357 e BACK-549 #10358. Este ADR não fecha o gate do MVP.

## Contexto

O provider técnico atual não representa identidade lógica, proveniência, variantes, múltiplos consumidores/histórico ou trilha transacional. D01/D02 aprovam um módulo independente antes das integrações; D05 separa uploader, titular/sujeito e referências; D06 exige semântica contextual; D08 mantém fonte MASTER privada; D12 preserva legado até migração posterior.

## Decisão técnica desta entrega

Criar `com.jeepclub.backend.storage` com domínio puro imutável, porta interna de catálogo e adapter JPA transacional. IDs Long próprios seguem padrões existentes. Quatro tabelas novas normalizam asset, representação, intervalo de referência e evento. Nenhuma associação JPA cruza módulos. Versão otimista protege registros mutáveis; variantes/eventos têm persistência append-only. Gravação e auditoria são atômicas. MASTER e origem byte-original são conceitos diferentes, com um único slot canônico.

Migrations exclusivamente aditivas; V13 MySQL nesta base. Localização física sensível a caixa e trailing spaces. Constraints de enums têm nomes explícitos; converters VARCHAR evitam depender de enums nativos de cada dialeto. Não criar camadas sem operação real nem modificar platform/shared ou consumidores.

## Consequências e limitações

O catálogo pode ser validado sem filesystem. Não disponibiliza upload, leitura de bytes, compartilhamento ou associação por HTTP. As tabelas nascem vazias; nenhuma mídia atual ganha classificação ou dono inferido. Integridade SQL não prova autorização contextual. Não há política executável de moderação/retenção, deduplicação ou expiração de upload. Não existe full Flyway PostgreSQL no projeto; teste desse dialeto segue Hibernate DDL como os testes preexistentes.

## Validação ainda necessária

Aprovar o ADR definitivo, famílias/Billing do MVP, matriz ACL, reuso, presets/crop/qualidade, benchmark síncrono, políticas de moderação e temporalidade jurídica. D13/cache e limites D08 estão aprovados como direção, sem implementação HTTP/pipeline neste PR. Nenhuma aprovação de merge/deploy/descarte decorre deste ADR.

Detalhes, invariantes, schema e sequência de evolução: [documentação da fundação](../back-549-storage-foundation.md).
