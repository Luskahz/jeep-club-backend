# BACK-599 — reconciliação de ausência física

No código anterior, `LocalPhysicalBlobStore.readVerified` distinguia arquivo ausente com `StorageObjectNotFoundException`, mas `BlobInspectionAdapter` capturava apenas `IllegalStateException`. O teste `StorageUploadFlowTest#reconciliationReportsMissingPhysicalBlob` reproduziu a falha: em vez de classificar um blob READY ausente, a consulta abortou com `StorageObjectNotFoundException` (`target/back-599-missing-before.log`).

O adapter agora trata explicitamente essa exceção como ausência/inconsistência: READY passa a `MISSING_OR_CORRUPT`; reserva não READY continua `PENDING_WRITE`. A correção não recria, sobrescreve ou exclui arquivos e não altera o tratamento de erros fora dessa inspeção. O teste localizado e a regressão de paginação em lote passaram, junto aos testes HTTP de cursor opaco: `mvnw.cmd -Dtest=StorageHttpFlowTest,StorageUploadFlowTest#reconciliationReportsMissingPhysicalBlob+reconciliationBatchesSqlRegardlessOfPageSize test`, 6 executados, 5 aprovados e benchmark opt-in ignorado.

Esta evidência não prova recuperação após crash de máquina, sincronização entre processos/instâncias ou ausência de canais temporais. A validação MySQL concorrente, rollback, integridade e regressões 6.1 no estado final continuam necessárias; a correção de leitura atual sob REPEATABLE READ deve ser preservada.
