# Incremento V4 — Membership Registry (BACK-528 / BACK-536)

A V4 é aditiva e mantém V1–V3 e a baseline intactas. Cria quatro tabelas do
registry e acrescenta as seis permissions independentes do registry ao ENUM existente, sem alterar o
valor/posição dos códigos anteriores. Só a linha de coordenação do contador é
inicializada, com próximo número null. Nenhuma Identity, aplicação ou associação
é criada/renumerada. A geração e a aplicação real de importação permanecem desabilitadas por padrão.

Modelo, constraints, comportamento, conciliação e gates operacionais estão no
[documento proprietário de Memberships](../../src/main/java/com/jeepclub/backend/memberships/docs/member-registry.md).
A evidência local e o estado dos testes ficam em [BACK-526](../back-526-validation.md).

O ensaio local usa fixtures sintéticas e MySQL 8.0.46. Não equivale à restauração
de um backup da VPS. O agente de produção deverá ensaiar o incremento sobre cópia
autorizada, conferir registros/constraints, revalidar os backups e manter emissão
desabilitada. Nenhuma operação em produção faz parte deste PR.


Na revisão pré-merge do PR #104, master/develop foram conferidas por fetch e
continham somente V1–V3. A ampliação do ENUM mantém todos os valores originais
na ordem e acrescenta READ, AUDIT_READ, NUMBER_RESERVE, NUMBER_ISSUE e IMPORT após
o MANAGE já proposto no PR. O fixture de comparação JPA acompanha esse contrato.
Não há CASCADE: o vínculo protege Identity contra exclusão física incompatível.
O futuro lifecycle BACK-512 precisará preservar os registros e traduzir conflitos.
A decisão de ajustar a V4 segue a autorização do pedido e o estado remoto
verificado: PR aberto, master/develop somente V1–V3 e deploy do PR ignorado.
Nenhum banco compartilhado/VPS foi consultado. Se houve aplicação manual fora
desse fluxo, interromper o merge e exigir correção incremental posterior, sem
repair ou alteração silenciosa de checksums. DevOps deve conferir a history real.
