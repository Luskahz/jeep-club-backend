# Jeep Club Backend — instruções para agentes de desenvolvimento

Este arquivo orienta agentes que trabalham no repositório `Jeep-Club/backend-application`. É uma política de execução, não substitui o escopo aprovado no Jira, as instruções explícitas da tarefa nem a documentação arquitetural. Mantenha o comportamento verificável, a segurança e a compatibilidade acima da economia de tokens.

## 1. Responsabilidade e fonte de verdade

- **Agente principal:** preserva o modelo e o esforço de raciocínio escolhidos na sessão; coordena a execução, toma decisões técnicas, integra alterações, revisa trabalhos delegados e entrega evidências.
- **Jira BACK:** contém requisitos e critérios de aceite aprovados. Leia as issues indicadas e dependências essenciais, quando houver acesso. Não invente requisitos nem modifique issues por iniciativa própria.
- **Código/contratos no SHA alvo:** demonstram o comportamento existente. Distinga `master`, `develop`, branch de trabalho, PR aberto, merge e produção.
- **Documentação:** define boundaries e regras arquiteturais, sujeita a confronto com o código vigente. OpenAPI é a fonte de contrato HTTP.
- Se uma questão essencial não puder ser resolvida por investigação, registre a decisão pendente e solicite esclarecimento antes de assumir uma nova regra.

## 2. Investigação antes de implementar

Para features, fixes e refatorações com impacto arquitetural, consulte na branch/SHA de trabalho, nesta ordem:

1. `docs/architecture/README.md`
2. `docs/architecture/module-organization.md`
3. `docs/architecture/feature-development-rules.md`
4. `src/main/java/com/jeepclub/backend/<modulo>/docs/README.md` do bounded context proprietário; em IAM, use o submódulo real (`authentication`, `authorization` ou `identity`).
5. Docs especializados dos contextos fornecedores/consumidores afetados; código, testes, `api.module`, controllers/DTOs e OpenAPI necessários.

Identifique owner, dependências, contratos, dados/estados, autorização, persistência, testes e consumidores. **Consultar outro módulo não autoriza modificá-lo.** Reaproveite capacidades existentes antes de introduzir endpoint, service, port, adapter ou DTO. Não presuma que uma proposta histórica já esteja implementada.

Reutilize leituras rastreáveis do mesmo SHA; procure apenas conteúdo alterado ou lacunas. Não transforme uma edição simples de documentação/configuração em varredura de todo o repositório. Não recopie documentos extensos para o contexto quando caminhos e trechos verificáveis bastam.

## 3. Subagentes: delegação econômica e limite rígido

Se o cliente Codex fornecer subagentes e a configuração do projeto estiver ativa:

- Utilize **no máximo dois subagentes**, além do agente principal; não inicie agentes recursivos.
- O padrão de subagentes está em `.codex/config.toml`: modelo `gpt-6-luna`, raciocínio `low`. **Não altere o modelo/esforço do agente principal.**
- **Delegue preferencialmente, e como regra para blocos operacionais extensos e independentes,** tarefas que consumiriam leitura/análise mecânica do principal e tenham custo de coordenação menor que o benefício esperado.
- Evite delegação em tarefas curtas, sequenciais, altamente acopladas ou que exijam copiar grande parte do contexto. Subagentes também consomem tokens; **economia não é garantida**.
- A decisão final de negócio, arquitetura, segurança, concorrência, migration crítica, integração e aceite é do agente principal. Não terceirize esses julgamentos exclusivamente a um agente de baixo esforço.

**Subagente de exploração — somente leitura por padrão:** localizar símbolos, contratos, referências, testes, consumidores e dependências; inventariar arquivos e comparar trechos. Retornar paths, símbolos, SHA, evidências, lacunas e um resumo compacto — não despejar arquivos inteiros.

**Subagente de validação — comandos delimitados:** executar verificações previamente definidas, coletar relatórios Surefire/Failsafe/JaCoCo e classificar resultados; retornar comando, ambiente, status, quantidade de testes/falhas/skips, arquivos de evidência e pequenos extratos relevantes. Diagnóstico profundo e correção ficam com o principal.

Para cada delegação, forneça objetivo específico, arquivos/escopo, proibição ou permissão expressa de editar, resultado esperado e momento de retorno. Não delegue trabalhos sobre os mesmos arquivos/estado mutável simultaneamente. Não execute suítes concorrentes disputando banco, worktree ou artefatos. Revise evidências dos subagentes antes de utilizá-las.

Se não houver suporte a subagentes ou o modelo configurado não estiver disponível, prossiga com o agente principal e registre a limitação; não substitua silenciosamente por outro modelo nem alegue delegação.

## 4. Eficiência de ferramentas, contexto e testes

- Antes de cada leitura extensa, identifique qual pergunta ela responde. Prefira buscas direcionadas (`rg`, símbolo, módulo, arquivo) e leituras em lotes coerentes a varreduras repetidas.
- Evite logs gigantes, cópias integrais de documentação, polling verboso e reexecuções sem alteração relevante. Guarde evidências completas em artefatos e reporte resumos com referências.
- Use checkpoints compactos por marco: issues, branch/SHA, arquivos/contratos, decisões, comandos/testes, falhas, skips e pendências para retomada.
- Execute testes focais durante a implementação. No encerramento, valide os gates pertinentes ao escopo e informe o que não foi executado; não substitua testes reais de DB/concorrência por H2 ou mocks quando o risco exigir mais.
- Consulte `.github/workflows/backend-ci.yml` vigente; avalie Maven `verify`, `mysql-migrations`, `publications-postgresql` quando aplicáveis. Relate CI no SHA efetivo, não num commit antigo.
- Comando longo não consome necessariamente tokens só por estar em execução. Otimize número de chamadas e volume de saída, sem cortar validações essenciais.

## 5. Arquitetura e preservação de contratos

- Respeite Hexagonal/DDD: `api.http` cuida de HTTP/DTO, `api.module` expõe contratos entre módulos, `core` contém domínio/aplicação/ports, `infra` contém persistência e adapters.
- Serviços são organizados por recurso, conforme o padrão documentado. Não crie uma classe para cada operação CRUD.
- Não importe entity, repository ou implementação interna de outro bounded context. Use contratos públicos ou porta consumer-owned e adapter quando apropriado; não transfira negócio para `shared`/`platform`.
- Preserve autorização e ownership; diferencie `@PreAuthorize` (enforcement), `@RequiredPermission` (documentação) e `@RequiresMembership` (validação de vínculo).
- Preserve contratos HTTP/OpenAPI, paginação, erros RFC 9457, semântica de estados e consumidores existentes; mudanças observáveis exigem escopo explícito, teste e documentação na fonte proprietária.
- Use `Clock` para decisões temporais. Flyway controla DDL; Hibernate usa `validate`. Não edite migrations aplicadas, não use `ddl-auto=update` e não altere dados reais sem autorização.
- Investigue todos os fluxos de uma regra alterada: administrativo, membro, reemissão, cancelamento, rejeição, expiração, concorrência e casos negativos relevantes.
- Frontend `Jeep-Club/front-end-web-application` na `master` serve como referência de consumidores; não o modifique salvo autorização e escopo explícitos.

## 6. Plano, marcos, evidências e entrega

1. Leia as issues selecionadas, liste critérios de aceite, restrições e dependências.
2. Confirme baseline, risco, caminhos existentes e plano de mudanças mínimo e coerente.
3. Implemente por marcos; execute testes focais antes de acumular novas alterações.
4. Para o encerramento, verifique integração, regressão, contratos, docs e gates pertinentes no estado final.
5. Entregue relatório compacto: issue/critério → implementação → evidência; commits/SHA, testes executados, falhas, skips, limitações e pendências.

Relatório do agente ou CI verde não substitui revisão independente. Não alegue testes, leitura, publicação ou conclusão sem evidência.

## 7. Segurança operacional

Não faça merge, deploy, push na `master`, alterações destrutivas ou mudanças em produção sem autorização específica. **Push na `master` pode disparar deploy automático.** Criar um PR não autoriza integrá-lo. Não exponha senhas, tokens, cookies, dados médicos ou pessoais em logs/relatórios. Não altere requisitos aprovados unilateralmente.

**Princípio:** investigação dirigida, delegação operacional criteriosa, implementação responsável e validação baseada em evidências.
