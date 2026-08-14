# Dependents Test Matrix

## Context

BACK-262 is the baseline for this suite: medical data is owned by the Health module and linked by `ownerType=DEPENDENT` and `ownerId=dependentId`.
New tests must not depend on the removed `dependents.core.domain.model.MedicalProfile` class.

## Matrix

| Area | Case | Layer | Evidence |
| --- | --- | --- | --- |
| Domain | Valid dependent creation normalizes CPF, phone and consent date | Unit | `DependentTest` |
| Domain | Name, relationship type, socio id and consent are required | Unit | `DependentTest` |
| Domain | CPF empty, less than 11, exactly 11, more than 11 and masked | Unit | `DependentTest` |
| Domain | Phone null, blank, masked and numeric normalization | Unit | `DependentTest` |
| Domain | Update keeps original consent date when already accepted | Unit | `DependentTest` |
| Normal service | Creates, queries, lists, updates and deletes within titular ownership | Unit | `DependentServiceTest` |
| Normal service | Rejects missing socio, duplicate CPF and access by another titular | Unit | `DependentServiceTest` |
| Admin service | Lists and queries dependents for the specified socio | Unit | `AdminDependentServiceTest` |
| Persistence | Save/find/list/delete and ownership query use `membership_dependents` only | JPA slice | `DependentRepositoryAdapterTest` |
| HTTP validation | Required CPF and consent validation returns 400 | MVC | `DependentControllerTest` |
| HTTP exceptions | Not found, conflict and forbidden map to expected status/code | MVC | `DependentControllerTest` |
| Security | Unauthenticated request is 401; titular token can access own route | Spring Boot + MockMvc | `DependentSecurityIntegrationTest` |
| Authorization | Admin authority can list dependents by socio; missing authority is forbidden | Spring Boot + MockMvc | `DependentSecurityIntegrationTest` |
| Health contract | Dependent endpoint calls Health by `DEPENDENT`/dependent id only | MVC | `DependentControllerTest` |
| System flow | Create, consult, list, update and remove dependent end-to-end through services/persistence | Spring Boot | `DependentSystemFlowTest` |

## Commands

```bash
./mvnw test
./mvnw verify
./mvnw org.pitest:pitest-maven:mutationCoverage -DtargetClasses=com.jeepclub.backend.dependents.* -DtargetTests=com.jeepclub.backend.dependents.*
```

## Reports

| Report | Path |
| --- | --- |
| JaCoCo HTML | `target/site/jacoco/index.html` |
| JaCoCo XML | `target/site/jacoco/jacoco.xml` |
| PIT HTML | `target/pit-reports/index.html` |
| PIT XML | `target/pit-reports/mutations.xml` |
