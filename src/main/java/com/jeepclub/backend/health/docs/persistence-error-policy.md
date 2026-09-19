# Fronteira de erros de persistência do Health

`MedicalProfileRepositoryAdapter` é a fronteira entre o core e Spring
Data/JPA. Exceções de infraestrutura não atravessam essa fronteira como tipo
público do core e suas mensagens nunca são usadas na resposta HTTP.

## Tradução

| Condição | Exceção de aplicação | HTTP | Código |
| --- | --- | --- | --- |
| Owner já possui perfil | `MedicalProfileConflictException` | 409 | `MEDICAL_PROFILE_CONFLICT` |
| Lock, timeout de lock ou atualização concorrente | `MedicalProfileConflictException` | 409 | `MEDICAL_PROFILE_CONFLICT` |
| Falha transitória de acesso a dados | `MedicalProfilePersistenceUnavailableException` | 503 | `MEDICAL_PROFILE_PERSISTENCE_UNAVAILABLE` |
| Outra falha de persistência | `MedicalProfilePersistenceException` | 500 | `MEDICAL_PROFILE_PERSISTENCE_FAILURE` |

O adapter força `flush` nas mutações para que violações diferidas pelo
JPA sejam capturadas ainda dentro da fronteira. A operação completa continua
transacional.

Não há retry automático de escrita: uma repetição invisível poderia alterar a
ordem de atualizações concorrentes. Para 409, o cliente deve buscar o estado
atual antes de decidir se repete a alteração. Para 503, pode repetir com
backoff, usando uma nova requisição.

## Observabilidade e sigilo

Unicidade, lock e falhas são registrados como evento estruturado com operação,
resultado, motivo, tipo do owner e IDs técnicos. O throwable não é anexado ao
evento, portanto SQL, nome de constraint, stack trace e conteúdo médico não
são escritos pelo adapter.

As causas originais ficam encadeadas somente para diagnóstico interno em
depurador/APM seguro. O `MedicalProfileExceptionHandler` sempre constrói o
contrato compartilhado `ApiErrorResponse` a partir de código e mensagem
controlados. Causas nunca são serializadas para o cliente.
