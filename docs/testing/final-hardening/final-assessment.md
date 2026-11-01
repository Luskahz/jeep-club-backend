# Parecer final — BACK-435, BACK-436, BACK-499 e BACK-505

## A. Estado do repositório

- Baseline develop: f52e152363151c5556dd5f04e9dc7435e02ffecd, após fetch --all --prune.
- Branch única: test/back-435-436-499-505-final-hardening.
- SHA final de implementação/compilação medida: 56cce5b41fe5682a714fd96e39a8124bf9dd7475.
- PR único para develop: [#125](https://github.com/Luskahz/jeep-club-backend/pull/125). A revisão de entrega é o HEAD do PR contendo estes relatórios; seu SHA é registrado no corpo do PR/Jira após publicação, evitando referência circular ao hash do próprio documento.
- [CI da implementação](https://github.com/Luskahz/jeep-club-backend/actions/runs/41255014767): Maven verify, MySQL e probe de migration futura aprovados; deploy ignorado. Os [checks do HEAD de entrega](https://github.com/Luskahz/jeep-club-backend/pull/125/checks) precisam corresponder ao SHA publicado.
- Nenhuma mudança em Java de produção, migrations, fixtures históricas ou thresholds. Sem merge, deploy, VPS, flags de produção ou alteração de master. output/ e tmp/ preexistentes preservados.

## B. Identity — BACK-435

Queries batch/keyset/status e parâmetros; consultas/ausências reais no adapter, filtros temporais, propagação de falhas desconhecidas; invariantes de User, arquivamento, relógio e normalização. Registro, /me, provisionamento/rollback, Membership, export privado e contratos existentes foram reutilizados.

49 entradas Surefire novas; variantes/assertions fortalecidas não são contadas como novos testes. Subtasks: BACK-491, BACK-492, BACK-493; BACK-494 permanece absorvida/concluída. Evidência técnica completa; encerramento formal condicionado à revisão/integração.
- Identity: LINE 93.3333% (952/68) → 94.3137% (962/58); BRANCH 85.8120% (502/83) → 91.9658% (538/47). Cobertos/não cobertos, sem média de classes.
- PIT focal: 135/137 KILLED (98.5401%); 2 SURVIVED, 0 NO_COVERAGE; zero timeout/nonviable/erros.
- [Relatório e testes concretos](identity.md).

## C. Authorization — BACK-436

Atribuição/revogação/substituição, ROOT, roles inativas/ausentes, Permission, catálogo/no-op, HTTP/JWT real, perda imediata de autoridade no mesmo token e apresentação histórica. Dois casos MySQL observam contenção real do lock de pai e um único vínculo final. Histórico de exclusão exige autoridade própria, no-store e auditoria durável, com 400/404/403.

70 entradas Surefire novas; variantes/assertions fortalecidas não são contadas como novos testes. Subtasks: BACK-495, BACK-496, BACK-497, BACK-498. Evidência técnica completa; encerramento formal condicionado à revisão/integração.
- Authorization: LINE 71.6049% (638/253) → 92.3681% (823/68); BRANCH 54.1985% (142/120) → 77.4809% (203/59). Cobertos/não cobertos, sem média de classes.
- PIT focal: 157/160 KILLED (98.1250%); 3 SURVIVED, 0 NO_COVERAGE; zero timeout/nonviable/erros.
- [Relatório e testes concretos](authorization.md).

## D. Publications — BACK-499

Event: negativos de inscrição/alocação/guest/ride, capacidade exata/overflow e estados finais; filtros, batches501, composição esparsa, privacidade de famílias/Health, snapshots e financeiro. A assertion do veículo de outro condutor validamente inscrito elimina a mutação da guarda de ownership. Notice, ServicePublication e social reutilizam contratos, persistência, locks, idempotência e rollback existentes.

52 entradas Surefire novas; variantes/assertions fortalecidas não são contadas como novos testes. Subtasks: BACK-500, BACK-501, BACK-502, BACK-504; BACK-503 permanece absorvida/concluída. Evidência técnica completa; encerramento formal condicionado à revisão/integração.
- Publications: LINE 93.7408% (1917/128) → 93.8386% (1919/126); BRANCH 77.1446% (1259/373) → 82.7206% (1350/282). Cobertos/não cobertos, sem média de classes.
- PIT focal: 280/290 KILLED (96.5517%); 9 SURVIVED, 1 NO_COVERAGE; zero timeout/nonviable/erros.
- [Relatório e testes concretos](publications.md).

## E. Platform/Shared — BACK-505

Imagem: MIME/assinatura/limites e compensação privada com serviço real. Storage/path traversal/symlink/falhas já cobertos por filesystem real. Logging: fila cheia, preservação/ordem e recuperação determinística da persistência. RFC9457 sanitizado, ExportPages e cópias/contratos compartilhados. Fechamento dos contextos elimina pools após o fim dos containers; sem campanha de getters/configuração.

40 entradas Surefire novas; variantes/assertions fortalecidas não são contadas como novos testes. Subtasks: BACK-506, BACK-507, BACK-509; BACK-508 permanece absorvida/concluída. Evidência técnica completa; encerramento formal condicionado à revisão/integração.
- Platform: LINE 92.5714% (972/78) → 94.4762% (992/58); BRANCH 75.6957% (408/131) → 81.6327% (440/99). Cobertos/não cobertos, sem média de classes.
- Shared: LINE 100.0000% (412/0) → 100.0000% (412/0); BRANCH 82.9268% (34/7) → 97.5610% (40/1). Cobertos/não cobertos, sem média de classes.
- PIT focal: 57/58 KILLED (98.2759%); 1 SURVIVED, 0 NO_COVERAGE; zero timeout/nonviable/erros.
- [Relatório e testes concretos](platform-shared.md).

## F. Indicadores consolidados

| Indicador / mecanismo primário da fixture | Baseline | Final | Delta |
| --- | --- | --- | --- |
| Unitários puros | 672 | 740 | +68 |
| Serviços/unitários com mocks | 491 | 604 | +113 |
| Spring (sem categoria HTTP/persistência) | 195 | 211 | +16 |
| Persistência | 99 | 102 | +3 |
| HTTP/contrato/segurança (fixtures) | 847 | 858 | +11 |
| Surefire total por execução | 2304 | 2515 | +211 |
| MySQL/Failsafe por execução | 456 | 479 | +23 |

Categorias primárias são mutuamente exclusivas por mecanismo de fixture; tags sobrepostas e grupos @Nested estão em test-mechanisms.csv. Duas assertions de serviço/query pertencem à fixture HTTP mista de Authorization. Nenhum tipo é inferido pelo nome IntegrationTest ou pelo JaCoCo.

clean test e verify locais: 2515 descobertos, 2514 aprovados, zero falhas/erros, um skip Windows condicional de symlink. MySQL local: 479/0/0/0. CI Linux: 2515/0/0/0 e 479/0/0/0; o probe repete 479, sem somar cenários. Dos 479 MySQL,339 repetem métodos herdados descobertos no Surefire;140 não têm correspondência de herança direta (não representam 140 novos comportamentos).

São 213 novas entradas únicas de caso JUnit:211 Surefire e2 exclusivas MySQL. O delta MySQL 23 inclui 21 repetições dos casos novos. Há 122 novos métodos JUnit e 14 corpos preexistentes fortalecidos, em 12 arquivos novos e 16 alterados (seis somente ciclo de vida dos contextos). Nenhum caso anterior removido. A matriz tem 64 grupos de comportamento,62 aplicáveis cobertos e 2 não aplicáveis;28 grupos estavam parciais/ausentes na baseline.

[Cobertura histórica/atual/final, pontos percentuais e contadores físicos de INSTRUCTION/LINE/BRANCH/METHOD/CLASS](coverage-summary.md). [Pacotes](coverage-by-package.csv), [discovery](discovery-inventory.csv), [execuções/hashes/declarações sem execução](execution-evidence.json), [mutação completa](mutation-summary.md).

## G. Findings

- CRITICAL: nenhum bug de produção confirmado no escopo auditado.
- HIGH: lacunas de detecção em Authorization, invariantes Identity, composição de relatórios, autoridade/auditoria HTTP do histórico e ownership de carona (F01/F02/F03/F07/F09). Resolvidas por regressões concretas; a guarda de ownership alterada pelo mutante é eliminada pelo teste.
- MEDIUM: pools de testes após containers e orçamento do job que executa duas suítes completas (F04/F08), corrigidos. Avisos de conexão fechada:40→0; ocorrências de Communications link failure:144→0. F05 cobre overflow e persistência/requeue sem sleeps.
- LOW: edge de filename iniciando por ponto aceito como limitação não bloqueante; proveniência/classificação do extrator corrigidas (F06/F10).

Causas, impacto, reprodução, correções, testes e pendências residuais estão em [findings.md](findings.md). Tentativas intermediárias falhas/canceladas permanecem identificadas e não substituem a última rodada válida.

## H. Parecer

**GO COM RESSALVAS para revisão/integração.** A implementação e as evidências técnicas das quatro histórias estão completas. Permanecem limitações não bloqueantes documentadas do PIT seletivo: 15 sobreviventes e 1 NO_COVERAGE analisados individualmente, incluindo dois mutantes de pré-lock cujo limite é a observação de escalonamento; a validação/locks antes de persistir e a ordem atual de produção são preservadas. Não são aceitos como redundantes mutantes que permitam escalada, violação financeira, ownership ou bypass do lifecycle.

Os oito gates locais saíram zero; CI de implementação aprovado e CI do SHA de publicação conferido no PR antes da entrega. Nenhum GO deriva apenas de JaCoCo. O bloqueio de encerramento formal é revisão/integração final na develop, não código pendente. As 14 subtasks residuais e 4 histórias devem permanecer Em andamento; BACK-494/BACK-503/BACK-508 permanecem Concluído por absorção. Não executar merge automático ou deploy.
