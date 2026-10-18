# Incremento V4 — Membership Registry (BACK-528 / BACK-536)

A V4 é aditiva e mantém V1–V3 e a baseline intactas. Cria quatro tabelas do
registry e acrescenta a permission de gestão ao ENUM existente, sem alterar o
valor/posição dos códigos anteriores. Só a linha de coordenação do contador é
inicializada, com próximo número null. Nenhuma Identity, aplicação ou associação
é criada/renumerada. A geração permanece desabilitada por padrão.

Modelo, constraints, comportamento, conciliação e gates operacionais estão no
[documento proprietário de Memberships](../../src/main/java/com/jeepclub/backend/memberships/docs/member-registry.md).
A evidência local e o estado dos testes ficam em [BACK-526](../back-526-validation.md).

O ensaio local usa fixtures sintéticas e MySQL 8.0.46. Não equivale à restauração
de um backup da VPS. O agente de produção deverá ensaiar o incremento sobre cópia
autorizada, conferir registros/constraints, revalidar os backups e manter emissão
desabilitada. Nenhuma operação em produção faz parte deste PR.
