# Arquivos e commits por história

Manifesto dos commits de implementação. Os relatórios de validação em docs/testing são compartilhados pelas quatro histórias.

## BACK-585

Commits: 5c5fed85, 8be07b84.

- [docs/testing/admin-hardening.md](../../docs/testing/admin-hardening.md)
- [src/main/java/com/jeepclub/backend/iam/identity/api/http/controller/admin/AdminUserController.java](../../src/main/java/com/jeepclub/backend/iam/identity/api/http/controller/admin/AdminUserController.java)
- [src/main/java/com/jeepclub/backend/iam/identity/infra/persistence/query/AdminUserJpaQueryRepository.java](../../src/main/java/com/jeepclub/backend/iam/identity/infra/persistence/query/AdminUserJpaQueryRepository.java)
- [src/main/java/com/jeepclub/backend/iam/identity/infra/persistence/sort/UserSortMapper.java](../../src/main/java/com/jeepclub/backend/iam/identity/infra/persistence/sort/UserSortMapper.java)
- [src/test/java/com/jeepclub/backend/identity/api/http/controller/admin/AdminUserControllerTest.java](../../src/test/java/com/jeepclub/backend/identity/api/http/controller/admin/AdminUserControllerTest.java)
- [src/test/java/com/jeepclub/backend/identity/infra/persistence/AdminUserSearchMySqlIT.java](../../src/test/java/com/jeepclub/backend/identity/infra/persistence/AdminUserSearchMySqlIT.java)
- [src/test/java/com/jeepclub/backend/identity/infra/persistence/AdminUserSearchPersistenceTest.java](../../src/test/java/com/jeepclub/backend/identity/infra/persistence/AdminUserSearchPersistenceTest.java)
- [src/test/java/com/jeepclub/backend/identity/infra/persistence/sort/UserSortMapperTest.java](../../src/test/java/com/jeepclub/backend/identity/infra/persistence/sort/UserSortMapperTest.java)

## BACK-574

Commits: ad2a4847, 6fdc31ef, b72617e5, e6c5b184.

- [docs/testing/admin-hardening.md](../../docs/testing/admin-hardening.md)
- [src/main/java/com/jeepclub/backend/tools/api/http/controller/admin/AdminToolController.java](../../src/main/java/com/jeepclub/backend/tools/api/http/controller/admin/AdminToolController.java)
- [src/main/java/com/jeepclub/backend/tools/api/http/dto/AdminToolFilterDTO.java](../../src/main/java/com/jeepclub/backend/tools/api/http/dto/AdminToolFilterDTO.java)
- [src/main/java/com/jeepclub/backend/tools/api/http/dto/AdminToolSummaryResponseDTO.java](../../src/main/java/com/jeepclub/backend/tools/api/http/dto/AdminToolSummaryResponseDTO.java)
- [src/main/java/com/jeepclub/backend/tools/api/http/exception/ToolExceptionHandler.java](../../src/main/java/com/jeepclub/backend/tools/api/http/exception/ToolExceptionHandler.java)
- [src/main/java/com/jeepclub/backend/tools/core/application/exception/InvalidToolSortException.java](../../src/main/java/com/jeepclub/backend/tools/core/application/exception/InvalidToolSortException.java)
- [src/main/java/com/jeepclub/backend/tools/core/application/query/AdminToolField.java](../../src/main/java/com/jeepclub/backend/tools/core/application/query/AdminToolField.java)
- [src/main/java/com/jeepclub/backend/tools/core/application/query/AdminToolFilter.java](../../src/main/java/com/jeepclub/backend/tools/core/application/query/AdminToolFilter.java)
- [src/main/java/com/jeepclub/backend/tools/core/application/result/AdminToolResult.java](../../src/main/java/com/jeepclub/backend/tools/core/application/result/AdminToolResult.java)
- [src/main/java/com/jeepclub/backend/tools/core/application/service/tool/AdminToolService.java](../../src/main/java/com/jeepclub/backend/tools/core/application/service/tool/AdminToolService.java)
- [src/main/java/com/jeepclub/backend/tools/core/repository/ToolRepository.java](../../src/main/java/com/jeepclub/backend/tools/core/repository/ToolRepository.java)
- [src/main/java/com/jeepclub/backend/tools/docs/README.md](../../src/main/java/com/jeepclub/backend/tools/docs/README.md)
- [src/main/java/com/jeepclub/backend/tools/infra/persistence/adapter/ToolRepositoryAdapter.java](../../src/main/java/com/jeepclub/backend/tools/infra/persistence/adapter/ToolRepositoryAdapter.java)
- [src/main/java/com/jeepclub/backend/tools/infra/persistence/jpa/ToolSpecifications.java](../../src/main/java/com/jeepclub/backend/tools/infra/persistence/jpa/ToolSpecifications.java)
- [src/test/java/com/jeepclub/backend/tools/api/contract/ToolsHttpIntegrationTest.java](../../src/test/java/com/jeepclub/backend/tools/api/contract/ToolsHttpIntegrationTest.java)
- [src/test/java/com/jeepclub/backend/tools/api/contract/ToolsOpenApiIntegrationTest.java](../../src/test/java/com/jeepclub/backend/tools/api/contract/ToolsOpenApiIntegrationTest.java)
- [src/test/java/com/jeepclub/backend/tools/api/contract/ToolsSearchMySqlIT.java](../../src/test/java/com/jeepclub/backend/tools/api/contract/ToolsSearchMySqlIT.java)
- [src/test/java/com/jeepclub/backend/tools/infra/persistence/adapter/ToolRepositoryAdapterTest.java](../../src/test/java/com/jeepclub/backend/tools/infra/persistence/adapter/ToolRepositoryAdapterTest.java)

## BACK-463

Commits: 116f4fbf, 3a3b8199, d625d24e.

- [.github/workflows/backend-ci.yml](../../.github/workflows/backend-ci.yml)
- [pom.xml](../../pom.xml)
- [src/main/java/com/jeepclub/backend/publications/api/http/controller/member/PublicationSocialController.java](../../src/main/java/com/jeepclub/backend/publications/api/http/controller/member/PublicationSocialController.java)
- [src/main/java/com/jeepclub/backend/publications/api/http/exception/EventExceptionHandler.java](../../src/main/java/com/jeepclub/backend/publications/api/http/exception/EventExceptionHandler.java)
- [src/main/java/com/jeepclub/backend/publications/api/http/exception/PublicationConstraintConflicts.java](../../src/main/java/com/jeepclub/backend/publications/api/http/exception/PublicationConstraintConflicts.java)
- [src/main/java/com/jeepclub/backend/publications/api/http/exception/PublicationSocialExceptionHandler.java](../../src/main/java/com/jeepclub/backend/publications/api/http/exception/PublicationSocialExceptionHandler.java)
- [src/main/java/com/jeepclub/backend/publications/core/domain/model/PublicationComment.java](../../src/main/java/com/jeepclub/backend/publications/core/domain/model/PublicationComment.java)
- [src/main/java/com/jeepclub/backend/publications/docs/README.md](../../src/main/java/com/jeepclub/backend/publications/docs/README.md)
- [src/main/java/com/jeepclub/backend/vehicles/infra/persistence/entity/VehicleEntity.java](../../src/main/java/com/jeepclub/backend/vehicles/infra/persistence/entity/VehicleEntity.java)
- [src/main/java/com/jeepclub/backend/vehicles/infra/persistence/entity/VehicleHistoryEntity.java](../../src/main/java/com/jeepclub/backend/vehicles/infra/persistence/entity/VehicleHistoryEntity.java)
- [src/test/java/com/jeepclub/backend/publications/api/http/PublicationHardeningHttpTest.java](../../src/test/java/com/jeepclub/backend/publications/api/http/PublicationHardeningHttpTest.java)
- [src/test/java/com/jeepclub/backend/publications/api/http/PublicationSocialFeedOpenApiIntegrationTest.java](../../src/test/java/com/jeepclub/backend/publications/api/http/PublicationSocialFeedOpenApiIntegrationTest.java)
- [src/test/java/com/jeepclub/backend/publications/core/application/service/PublicationSocialWorkflowTest.java](../../src/test/java/com/jeepclub/backend/publications/core/application/service/PublicationSocialWorkflowTest.java)
- [src/test/java/com/jeepclub/backend/publications/EventOperationsPostgresIT.java](../../src/test/java/com/jeepclub/backend/publications/EventOperationsPostgresIT.java)
- [src/test/java/com/jeepclub/backend/publications/infra/persistence/adapter/PublicationPersistencePostgresIT.java](../../src/test/java/com/jeepclub/backend/publications/infra/persistence/adapter/PublicationPersistencePostgresIT.java)
- [src/test/java/com/jeepclub/backend/publications/infra/persistence/adapter/ServiceChangePersistencePostgresIT.java](../../src/test/java/com/jeepclub/backend/publications/infra/persistence/adapter/ServiceChangePersistencePostgresIT.java)

## BACK-573

Commits: 9907e182, aaa5eb65, b6009290, 1053be9d.

Revisão do finding OR do PR #126: a pré-busca nominal limitada foi substituída por
EXISTS composto pelo contrato público abaixo. As evidências atualizadas estão em
[admin-hardening.md](admin-hardening.md).

- [src/main/java/com/jeepclub/backend/iam/identity/api/module/UserNameCriteria.java](../../src/main/java/com/jeepclub/backend/iam/identity/api/module/UserNameCriteria.java)
- [src/main/java/com/jeepclub/backend/iam/identity/infra/persistence/query/UserNameJpaCriteria.java](../../src/main/java/com/jeepclub/backend/iam/identity/infra/persistence/query/UserNameJpaCriteria.java)

- [src/main/java/com/jeepclub/backend/iam/identity/api/module/exception/UserNameSearchTooBroadException.java](../../src/main/java/com/jeepclub/backend/iam/identity/api/module/exception/UserNameSearchTooBroadException.java)
- [src/main/java/com/jeepclub/backend/iam/identity/api/module/UserQuery.java](../../src/main/java/com/jeepclub/backend/iam/identity/api/module/UserQuery.java)
- [src/main/java/com/jeepclub/backend/iam/identity/core/application/service/user/UserQueryService.java](../../src/main/java/com/jeepclub/backend/iam/identity/core/application/service/user/UserQueryService.java)
- [src/main/java/com/jeepclub/backend/iam/identity/core/repository/UserRepository.java](../../src/main/java/com/jeepclub/backend/iam/identity/core/repository/UserRepository.java)
- [src/main/java/com/jeepclub/backend/iam/identity/docs/README.md](../../src/main/java/com/jeepclub/backend/iam/identity/docs/README.md)
- [src/main/java/com/jeepclub/backend/iam/identity/infra/persistence/adapter/UserRepositoryAdapter.java](../../src/main/java/com/jeepclub/backend/iam/identity/infra/persistence/adapter/UserRepositoryAdapter.java)
- [src/main/java/com/jeepclub/backend/vehicles/api/http/controller/admin/AdminVehicleController.java](../../src/main/java/com/jeepclub/backend/vehicles/api/http/controller/admin/AdminVehicleController.java)
- [src/main/java/com/jeepclub/backend/vehicles/api/http/dto/list/AdminListResponseDTO.java](../../src/main/java/com/jeepclub/backend/vehicles/api/http/dto/list/AdminListResponseDTO.java)
- [src/main/java/com/jeepclub/backend/vehicles/api/http/dto/list/AdminVehicleFilterDTO.java](../../src/main/java/com/jeepclub/backend/vehicles/api/http/dto/list/AdminVehicleFilterDTO.java)
- [src/main/java/com/jeepclub/backend/vehicles/api/http/exceptions/VehicleExceptionHandler.java](../../src/main/java/com/jeepclub/backend/vehicles/api/http/exceptions/VehicleExceptionHandler.java)
- [src/main/java/com/jeepclub/backend/vehicles/core/application/exceptions/InvalidVehicleSearchException.java](../../src/main/java/com/jeepclub/backend/vehicles/core/application/exceptions/InvalidVehicleSearchException.java)
- [src/main/java/com/jeepclub/backend/vehicles/core/application/query/AdminVehicleField.java](../../src/main/java/com/jeepclub/backend/vehicles/core/application/query/AdminVehicleField.java)
- [src/main/java/com/jeepclub/backend/vehicles/core/application/query/AdminVehicleFilter.java](../../src/main/java/com/jeepclub/backend/vehicles/core/application/query/AdminVehicleFilter.java)
- [src/main/java/com/jeepclub/backend/vehicles/core/application/result/AdminVehicleSearchResult.java](../../src/main/java/com/jeepclub/backend/vehicles/core/application/result/AdminVehicleSearchResult.java)
- [src/main/java/com/jeepclub/backend/vehicles/core/application/service/vehicle/AdminVehicleService.java](../../src/main/java/com/jeepclub/backend/vehicles/core/application/service/vehicle/AdminVehicleService.java)
- [src/main/java/com/jeepclub/backend/vehicles/core/port/UserPort.java](../../src/main/java/com/jeepclub/backend/vehicles/core/port/UserPort.java)
- [src/main/java/com/jeepclub/backend/vehicles/core/repository/VehicleRepository.java](../../src/main/java/com/jeepclub/backend/vehicles/core/repository/VehicleRepository.java)
- [src/main/java/com/jeepclub/backend/vehicles/docs/README.md](../../src/main/java/com/jeepclub/backend/vehicles/docs/README.md)
- [src/main/java/com/jeepclub/backend/vehicles/infra/integration/identity/VehicleIdentityAdapter.java](../../src/main/java/com/jeepclub/backend/vehicles/infra/integration/identity/VehicleIdentityAdapter.java)
- [src/main/java/com/jeepclub/backend/vehicles/infra/persistence/adapter/VehicleRepositoryAdapter.java](../../src/main/java/com/jeepclub/backend/vehicles/infra/persistence/adapter/VehicleRepositoryAdapter.java)
- [src/main/java/com/jeepclub/backend/vehicles/infra/persistence/query/AdminVehicleJpaQuery.java](../../src/main/java/com/jeepclub/backend/vehicles/infra/persistence/query/AdminVehicleJpaQuery.java)
- [src/test/java/com/jeepclub/backend/iam/identity/core/application/service/user/UserQueryServiceTest.java](../../src/test/java/com/jeepclub/backend/iam/identity/core/application/service/user/UserQueryServiceTest.java)
- [src/test/java/com/jeepclub/backend/vehicles/api/contract/VehiclesOpenApiIntegrationTest.java](../../src/test/java/com/jeepclub/backend/vehicles/api/contract/VehiclesOpenApiIntegrationTest.java)
- [src/test/java/com/jeepclub/backend/vehicles/api/controller/AdminVehicleSearchHttpIntegrationTest.java](../../src/test/java/com/jeepclub/backend/vehicles/api/controller/AdminVehicleSearchHttpIntegrationTest.java)
- [src/test/java/com/jeepclub/backend/vehicles/api/controller/AdminVehicleSearchMySqlIT.java](../../src/test/java/com/jeepclub/backend/vehicles/api/controller/AdminVehicleSearchMySqlIT.java)
- [src/test/java/com/jeepclub/backend/vehicles/api/controller/VehicleAdminSecurityIntegrationTest.java](../../src/test/java/com/jeepclub/backend/vehicles/api/controller/VehicleAdminSecurityIntegrationTest.java)
- [src/test/java/com/jeepclub/backend/vehicles/core/application/service/vehicle/AdminVehicleServiceTest.java](../../src/test/java/com/jeepclub/backend/vehicles/core/application/service/vehicle/AdminVehicleServiceTest.java)
- [src/test/java/com/jeepclub/backend/vehicles/infra/persistence/adapter/VehicleRepositoryAdapterTest.java](../../src/test/java/com/jeepclub/backend/vehicles/infra/persistence/adapter/VehicleRepositoryAdapterTest.java)
