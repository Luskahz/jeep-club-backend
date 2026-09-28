# Validação — BACK-410

## Escopo executado

Base: origin/develop b051da139395708aba61a2895ebdce3d51cee967. Story, BACK-411..422 e BACK-476 relidos no Jira durante a revisão final; BACK-409 consultada antes da implementação. Nenhuma issue foi encerrada.

- ExportHttpIntegrationTest: 31 rotas cadastrais, formatos CSV/PDF reais com H2, download, 401/403, formato inválido, ID inexistente, OpenAPI; cenários persistidos de Identity, Authentication, Authorization, Memberships, Vehicles, Tools (205 registros), Health agregado, Billing e os seis relatórios Event/PDF emergencial.
- EventReportServiceTest: múltiplos veículos sem duplicação de MEMBER, dependentes não alocados, convidados, transporte/lotação, caronas, separação inscrição/cobrança/pagamento, cobertura sem clínica, acesso mínimo, pós-evento, PDF e limites.
- EventOperationsIntegrationTest: regressões dos fluxos Event, guestName validado/persistido, emergência antes/depois do início, ownership revalidado e auditoria persistida.
- ExportRendererTest: BOM/UTF-8, escaping, oito formas de formula injection, vazio, null/booleano, campos longos/paginação PDF, filename, linhas/bytes, auditoria obrigatória falhando antes de liberar arquivo.

## Inspeção arquitetural e de dados

A inspeção de referências Java nos arquivos novos/alterados não encontrou novo acesso a internals de outro bounded context. O import preexistente RequiresMembership em EventController é uma anotação de autorização, preservada. A associação legada AuthenticationAccountEntity → UserEntity não foi ampliada. Não existe suíte ArchUnit no projeto nem foram criadas exceções arquiteturais.

Todas as queries de export declaram as colunas. Hashes/tokens não são selecionados em Authentication. Storage não é renderizado: somente existência funcional de foto, quando aplicável. Public contracts de identificação permanecem read-only; Health oferece cobertura batch sem clínica e leitura emergencial individual. Nenhum endpoint global foi criado.

Amostra sintética de PDF renderizada pelo próprio DefaultExportRenderer e inspecionada visualmente: títulos, campos, resumo, agrupamento, margens e rodapé legíveis. Testes extraem o texto de PDFs reais para conferir conteúdo e ausência de clínica nas visões operacionais.

## Volume

Consultas tabulares em páginas de 200; enriquecimento em lote; EntityManager não materializa a tabela toda. Event reutiliza um grafo operacional existente após limites explícitos. A resposta fica em buffer limitado até completar geração/auditoria, evitando download parcial. Não há benchmark nem acesso a volume de produção; limites e essa restrição estão registrados na matriz.

## Resultado das suítes completas

`./mvnw test`: passou, 1.081 testes, 0 falhas/erros/ignorados (28/09/2026).

`./mvnw verify`: passou, 1.081 testes, 0 falhas/erros/ignorados (28/09/2026). A execução inclui a correção final do OpenAPI: a ficha emergencial declara somente application/pdf.
