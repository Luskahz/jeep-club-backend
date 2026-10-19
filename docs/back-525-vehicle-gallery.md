# BACK-525 — Galeria de Vehicles

Base temporária: BACK-524, PR #109, `3ea39a5347ea9212ddf185a5e4e427fa427068f6`.
Cadeia: develop → BACK-460 (#105) → BACK-510 (#106) → BACK-512 (#107) → BACK-523 (#108) → BACK-524 (#109) → BACK-525.

## Investigação anterior à implementação

Lidas BACK-525, BACK-394/418/518 e as referências BACK-392/510, com inspeção de Vehicle, services member/admin, DTOs/reader de edição parcial, repositories/adapters, mappers e exports. Publications Gallery serve como padrão, sem importar seu domínio privado. EventVehicleQuery e VehicleLifecycleLock expõem identificação/capacidade, sem mídia; serão preservados. A arquitetura atual usa Flyway e Hibernate validate, prevalecendo sobre o texto histórico do Jira que antecedia as migrations consolidadas.

V7 é a última migration da ancestralidade; V8 é a próxima. V1–V7 e baseline permanecem intactas. As colunas photo operacional/histórica são nullable VARCHAR(500) e podem conter URLs antigas. A galeria será canônica, com photo como projeção da principal para compatibilidade de leitura e rollout. Backfill deve preservar cada valor não nulo, inclusive URL antiga, sem validar como upload novo, e não reinserir capa depois de uma limpeza consciente.

Critérios: zero a cinco imagens; ordem 0..N-1 única e contígua; referências únicas; exatamente uma principal se não vazia; atualização atômica e concorrência; criação multipart member/admin; manutenção integral com reordenação/principal/limpeza; omissão preserva; histórico independente completo; paginação sem duplicação/N+1; exports sem referências; permissions/ownership/lifecycle; compensação só de arquivos novos; testes unitários, HTTP/JPA, MySQL/Flyway, regressão e OpenAPI.

Decisões: tipos próprios VehicleImage/VehicleGallery; revisão esperada para substituição integral sob lock atual do agregado; leitura corrente da coleção em MySQL para não reutilizar snapshot anterior ao lock. A edição legada de photo será deprecada e não apagará uma galeria múltipla silenciosamente. Novos arquivos usam exclusivamente o command da BACK-510. Referências retidas podem conservar valores legados; novas associações devem validar referência pública e existência. Fotos privadas de Dependents continuam fora do resolvedor público.

As rotas administrativas usam authorities atuais de Vehicles; histórico exige a autoridade de exportação histórica. Nenhuma authority ROOT/CUSTOM é ampliada. Mudanças de mídia respeitam ACTIVE e ownership, preservando os guards de exclusão da BACK-512 e a compatibilidade da V4/Registry. O limite de request de 12 MiB será ampliado para comportar cinco arquivos de 5 MiB, mantendo o teto individual de imagem e os recibos Billing existentes.

## Domínio, consistência e persistência

VehicleGallery é imutável e ordenada; VehicleImage distingue reconstrução opaca de validação de nova referência. Zero imagens é válido. A coleção protege limite de cinco, chave/posição únicas, posições contíguas e exatamente uma principal quando não vazia. Nenhum domínio privado de Publications é importado. getPhoto deriva exclusivamente da principal; alterações cadastrais preservam a galeria quando o campo legado é omitido ou não muda.

`gallery_revision` é independente da disponibilidade e só avança quando a galeria muda. PUT exige expectedRevision. O estado atual de Vehicle é relido sob lock e as imagens recebem leitura bloqueante própria: uma consulta lazy comum poderia continuar usando um snapshot MySQL REPEATABLE READ iniciado antes do lock. Uma revisão antiga com estado diferente retorna VEHICLE_GALLERY_CONFLICT/409; o frontend deve reler antes de tentar novamente. Repetição JSON exatamente igual ao estado atual é idempotente, preservando revisão e timestamp. Arquivos novos com revisão desatualizada são recusados antes de qualquer upload. Reenviar bytes não é idempotência distribuída de upload; se a resposta se perder, consultar o estado antes de repetir.

Criação e manutenção permanecem agrupadas nos services member/admin existentes. O colaborador interno VehicleGalleryAssembler usa somente ImageMediaCommand e a leitura pública de mídia, sem filesystem/provider privado. Novas referências públicas exigem formato e existência; uma referência já pertencente à própria galeria pode manter o valor legado sem revalidação como upload novo. Nenhuma URL legada é baixada pelo backend. Referências privadas de Dependents são recusadas como novas associações. O titular é validado/bloqueado pelo contrato público de Identity antes das alterações; nenhuma alteração no Registry foi necessária.

Upload de cada arquivo registra compensação da BACK-510. Falha posterior desfaz a galeria, a revisão e a auditoria no banco, compensando apenas arquivos recém-criados. Falha de limpeza preserva o erro original e gera aviso sanitizado; commit UNKNOWN retém arquivo para reconciliação. Arquivos anteriores, compartilhados ou retidos em histórico nunca são apagados por troca, desassociação ou DELETE de Vehicle. Não há nova rotina periódica de consistência.

A V8 acrescenta revisão operacional/histórica e coleções `vehicle_images`/`vehicle_image_history`, com referência VARCHAR(500), collation binária para identidade exata, posição, principal, FKs para seu próprio parent, unicidade por parent/posição e parent/referência e CHECK de posição 0..4. Cinco posições únicas limitam fisicamente a quantidade; principal única e continuidade completa são invariantes do agregado sob lock. O histórico se vincula ao snapshot de exclusão, nunca ao Vehicle operacional. O snapshot copia a galeria atual sob lock, não uma coleção ORM antiga.

Backfill: cada photo não nula vira imagem 0/principal, em ambos os cadastros, sem validar URLs antigas como novas chaves; null vira vazio. Valores legados vazios também são conservados sem inventar descarte. Não sobrescreve galeria populada e não duplica imagens em replay controlado. Escritas novas sincronizam a coluna legada com a principal; limpar a galeria também torna photo nula, impedindo ressurreição em replay. A coluna antiga não é removida. O teto de entrada para referência retida acomoda UTF-16 de até 500 caracteres MySQL legados; novas associações seguem estritamente o padrão global de imagens.

Listagens paginam os parents e carregam suas coleções em uma segunda consulta por lote, sem join de coleção na paginação nem N+1. ListResponseDTO/AdminListResponseDTO continuam usando photo como capa derivada. DetailResponseDTO e DetailForEditResponseDTO acrescentam images e galleryRevision. Exports operacional/histórico usam existência da coleção, sem multiplicar linhas nem expor chaves; o contrato público usado por Events continua sem mídia.

## HTTP e permissions

| Operação | Contrato | Autorização/resposta |
| --- | --- | --- |
| POST /vehicles/include/member | JSON sem foto ou multipart vehicle + photos e gallery opcional | Titular autenticado ativo; 201 vazio preservado |
| POST /vehicles/include/admin/{memberId} | Mesmo contrato; titular existente e ativo | VEHICLES_VEHICLE_CREATE; 201 |
| GET /vehicles/detail/member/{vehicleId} e detail-for-edit/member | Galeria completa, revisão e alias photo | Ownership; 200; consulta inclusive INACTIVE/ARCHIVED não legados |
| GET /vehicles/detail/admin/{vehicleId} e detail-for-edit/admin | Galeria completa, revisão e alias | VEHICLES_VEHICLE_READ |
| PUT /vehicles/gallery/member/{vehicleId} | JSON ou multipart; substituição integral | Ownership e ACTIVE; 200 com DetailResponseDTO/no-store |
| PUT /vehicles/gallery/admin/{vehicleId} | Mesmo contrato | VEHICLES_VEHICLE_UPDATE; titular/Vehicle elegíveis; 200 |
| GET /vehicles/gallery/history/admin/{historyId} | Snapshot independente, imagens/revisão/ator/data | VEHICLES_VEHICLE_EXPORT; 200/no-store |

Na criação multipart, `vehicle` contém o DTO estruturado já existente, `photos` contém até cinco arquivos JPEG/PNG/WebP de até 5 MiB cada, e `gallery` é JSON opcional com images/ordem/principal. Sem gallery, a ordem dos arquivos é usada e a primeira imagem é principal. Sem arquivos a galeria é vazia. A part `photo` única anterior permanece temporariamente compatível e não pode ser combinada com photos/gallery. Não enviar photo/storageKey/images dentro do DTO vehicle. Na manutenção, gallery é obrigatória no multipart e expectedRevision é obrigatório em ambos os tipos de conteúdo. Arquivos repetem a part photos e são referidos por uploadIndex zero-based; cada arquivo deve ser usado exatamente uma vez.

Erro 400: DTO/part/arquivo/metadados inválidos, posições/principal/limite ou revisão ausente. 401: autenticação inelegível. 403: authority administrativa. 404: ausência/ownership ocultado conforme padrão de Vehicles, ou nova mídia pública inexistente. 409: revisão desatualizada/lifecycle/titular inelegível/identificador duplicado. 413: teto servlet/proxy. 503: storage indisponível. Erros seguem RFC 9457, sem provider/path físico. Uma revisão antiga não autoriza sobrescrever edições concorrentes.

PUT de dados cadastrais preserva a galeria se photo for omitida. O alias legado photo é deprecado: mudança explícita só é aceita em galeria de até uma imagem e revisão zero. Depois de qualquer alteração da associação, ou em galeria múltipla, mudanças usam PUT versionado e o alias retorna 409; isso impede escritor antigo de sobrescrever uma confirmação concorrente. Valor idêntico e omissão continuam preservando. Um valor legado idêntico não é tratado como nova associação. Campos images/gallery/galleryRevision no PUT cadastral são recusados para evitar ignorar intenção de alteração. Não existem duas coleções independentes nem duas fontes de verdade.

## Frontend: zero, uma e cinco imagens

```javascript
// Zero: JSON de cadastro anterior sem photo permanece válido.
await fetch('/vehicles/include/member', {
  method: 'POST', headers: authJsonHeaders, body: JSON.stringify(vehicleData)
});

// Uma ou cinco: arquivos na própria requisição; não fazer upload avulso obrigatório.
const form = new FormData();
if (files.length > 5) throw new Error('Escolha até cinco imagens');
form.append('vehicle', new Blob([JSON.stringify(vehicleData)], { type: 'application/json' }));
files.forEach(file => form.append('photos', file));
// Opcional: para uma foto, [{uploadIndex:0,position:0,primary:true}].
// Para cinco, índices/posições 0..4 e apenas uma primary.
form.append('gallery', new Blob([JSON.stringify({ images: files.map((_, i) => ({
  uploadIndex: i, position: i, primary: i === Math.min(2, files.length - 1)
})) })], { type: 'application/json' }));
await fetch('/vehicles/include/member', { method: 'POST', headers: authHeaders, body: form });
```

O exemplo escolhe a terceira como principal quando há pelo menos três imagens, a única quando há uma, e nenhuma na galeria vazia. A sexta é rejeitada pelo backend, sem truncamento silencioso. O navegador define o boundary, sem Content-Type manual. gallery omitida usa a primeira principal automaticamente. Para consulta, `images` é canônica e `photo` é apenas alias de capa; URLs legadas são valores opacos preservados e não passam pelo resolvedor de chaves atuais. A UI deve tratar somente protocolos HTTP(S) legados aprovados, sem interpretar o valor como HTML/código.

```json
{"expectedRevision": 3, "images": []}
```

O JSON acima limpa a galeria. Para reordenar/trocar principal sem upload, enviar a lista completa das referências atuais, com posições 0..N-1 e exatamente uma primary. Para adicionar/substituir com arquivo, multipart gallery usa entradas retidas com storageKey e entradas novas com uploadIndex, cada uma com posição/principal; nunca enviar ambos na mesma entrada. Após 409, reler detail-for-edit e reconciliar a intenção do usuário. Histórico usa historyId e não depende do ID operacional continuar existente. Listagens/CSV/PDF preservam paginação e o indicador funcional de existência de foto.

## Rollout, infraestrutura e rollback

Nenhuma variável, provider, volume ou flag nova. `spring.servlet.multipart.max-file-size=10MB` mantém o teto servlet necessário aos recibos Billing de 10 MiB; imagens continuam limitadas pelo command a 5 MiB. O teto total passa de 12 para 30 MiB em application.properties e nos dois Nginx versionados, para comportar cinco arquivos mais JSON/overhead. Isto prepara arquivos do repositório; não altera a VPS. Homologar capacidade de memória, disco temporário, concorrência e overrides servlet/proxy existentes. Confirmar os limites em todos os proxies antes da release.

Pré-deploy: integrar PRs ordenadamente após revisão humana; preservar #103/#104; verificar V4→V5→V6→V7→V8, checksums, schema/retention e backup/restauração. V8 exige MySQL 8 com CHECK aplicado e InnoDB/índices/collation compatíveis; estimar metadata locks e espaço de índice/backfill com dados representativos. Validar startup Flyway/Hibernate validate, fotos antigas/URLs/null, histórico, paginação, exports e limites HTTP reais. Não executar repair/clean, importação real ou ativar emissão/import apply do Registry.

Interromper escritores antigos antes de habilitar escritas de galeria: binários anteriores não sincronizam a coleção/revisão e não podem coexistir como writers. A coluna photo preserva compatibilidade de leitura, não autorização para rollout com escritores misturados. Atualizar todos os writers e frontend de forma coordenada; avaliar janela de manutenção conforme o ambiente real. Não dropar photo nesta release.

Rollback binário não recompõe galerias completas/histórico a partir da capa e não entende a nova coleção. Mesmo mantendo tabelas, código antigo pode gravar projeção divergente ou falhar ao excluir parent com filhos protegidos por FK. Preferir correção para frente ou restauração coordenada de banco/código/storage, preservando históricos e arquivos compartilhados; nunca apagar imagens reais como atalho. Falha de compensação/commit UNKNOWN exige reconciliação controlada que considere referências operacionais e históricas. A proteção privada de Dependents e a ausência de exposição estática do volume continuam requisitos da V7.

## Validação

A regressão completa revelou timestamps submicrossegundo arredondados para o futuro pelo banco, afetando transições imediatamente após criação (inclusive fora de Vehicles). O Clock transversal de Platform passa a produzir microssegundos truncados, compatíveis com DATETIME(6), sem relaxar validações de domínio, adicionar esperas ou alterar TTLs. Um teste JDBC determinístico reproduz o arredondamento com controle negativo e valida o round-trip do Clock corrigido. O fixture herdado de token expirado agora usa um instante explicitamente passado, sem depender de latência entre requests. Os gates anteriores com falha ficam registrados; os gates finais são repetidos sobre o novo commit. A mudança transversal de precisão deve ser revisada junto com o contrato de persistência, e não extrapolada para garantia de relógio monotônico ou proteção contra ajustes reais do relógio do sistema.

Implementação e gates finais ainda em execução. Evidências finais, tentativas corrigidas, hashes e resultados reais serão registrados em docs/back-525-test-evidence.json antes da publicação. Cobertura inclui domínio, 28 cenários integrados repetidos no MySQL, quatro corridas determinísticas, schema/backfill/replay/FKs, paginação sem N+1, CSV/PDF, permissions/ownership/lifecycle, compensação e HTTP real com cinco arquivos de 5 MiB. Não se afirma teste produtivo nem homologação de infraestrutura privada.
