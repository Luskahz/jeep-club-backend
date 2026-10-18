package com.jeepclub.backend.iam.identity.api.http.controller.admin;

import com.jeepclub.backend.iam.identity.api.http.dto.admin.user.AdminUpdateUserRequestDTO;
import com.jeepclub.backend.iam.identity.api.http.dto.user.UserProfileDataDTO;
import com.jeepclub.backend.iam.identity.api.http.dto.user.UserProfileResponseDTO;
import jakarta.validation.Validator;
import org.springframework.http.CacheControl;
import tools.jackson.databind.JsonNode;
import com.jeepclub.backend.iam.identity.api.http.dto.admin.user.AdminUserFilterDTO;
import com.jeepclub.backend.iam.identity.api.http.dto.admin.user.AdminUserResponseDTO;
import com.jeepclub.backend.iam.identity.api.http.dto.admin.user.AdminCreateUserRequestDTO;
import com.jeepclub.backend.iam.identity.api.http.dto.admin.user.AdminCreateUserResponseDTO;
import com.jeepclub.backend.iam.identity.api.module.UserRegistration;
import com.jeepclub.backend.iam.identity.api.module.UserActivationMethod;
import com.jeepclub.backend.iam.identity.api.module.UserRegistrationData;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.iam.identity.core.application.query.user.AdminUserField;
import com.jeepclub.backend.iam.identity.core.application.result.admin.user.AdminUserResult;
import com.jeepclub.backend.iam.identity.core.application.service.user.AdminUserService;
import com.jeepclub.backend.platform.openapi.group.SwaggerOperationGroup;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.web.pagination.PageResponse;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.EnumSet;
import java.util.Set;
import java.time.Clock;
import java.time.Instant;
import java.net.URI;

@RestController
@RequestMapping(
        value = "/identity/admin/users",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
@Tag(
        name = "Identity - Users",
        description = "Operações administrativas sobre os dados cadastrais dos usuários."
)
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final com.jeepclub.backend.iam.identity.api.module.spi.UserMembershipNumberProjection membershipNumbers;
    private final Validator validator;
    private final UserRegistration userRegistration;
    private final Clock clock;

    @PatchMapping(value = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('IDENTITY_USER_UPDATE')")
    @RequiredPermission("IDENTITY_USER_UPDATE")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(summary = "Editar cadastro completo do usuário", description = "Edição parcial dos sete campos cadastrais. "
            + "Omitir preserva; null limpa opcionais; {} preserva updatedAt. CPF/e-mail alterados revogam sessões/challenges "
            + "e invalidam links pendentes afetados. Reenvio é explícito. Outro administrador não altera CPF/e-mail de ROOT. "
            + "Não altera lifecycle, credenciais, roles, IDs ou vínculos históricos.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = AdminUpdateUserRequestDTO.class))))
    @ApiResponse(responseCode = "200", description = "Cadastro atualizado ou preservado para no-op.", content = @Content(schema = @Schema(implementation = AdminUserResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Payload, CPF, e-mail, data ou referência de imagem inválidos.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Sem IDENTITY_USER_UPDATE.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Usuário ou imagem inexistente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "CPF/e-mail/RG duplicado, identificador ROOT protegido, conta de Authentication ausente ou conflito concorrente (reler e repetir).", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<AdminUserResponseDTO> update(@PathVariable @Positive Long userId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody JsonNode body) {
        var update = AdminUpdateUserRequestDTO.read(body, validator);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(AdminUserResponseDTO.from(adminUserService.update(userId, principal.getUserId(), update),membershipNumbers.currentNumber(userId)));
    }

    @GetMapping("/{userId}/profile")
    @PreAuthorize("hasAuthority('IDENTITY_USER_READ')")
    @RequiredPermission("IDENTITY_USER_READ")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(summary = "Consultar perfil complementar do usuário", description = "Perfil ausente retorna blocos null e pendência, sem criar registro.")
    @ApiResponse(responseCode = "200", description = "Perfil complementar.", content = @Content(schema = @Schema(implementation = UserProfileResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "ID inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Sem IDENTITY_USER_READ.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Usuário inexistente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<UserProfileResponseDTO> getProfile(@PathVariable @Positive Long userId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(UserProfileResponseDTO.from(adminUserService.getProfile(userId),membershipNumbers.currentNumber(userId)));
    }

    @PutMapping(value = "/{userId}/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('IDENTITY_USER_UPDATE')")
    @RequiredPermission("IDENTITY_USER_UPDATE")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(summary = "Substituir perfil complementar do usuário", description = "Upsert integral: omissão/null/branco remove; {} limpa. "
            + "Reutiliza regras do perfil próprio, aceita dados parciais, sem alterar lifecycle ou cadastro básico.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = UserProfileDataDTO.class))))
    @ApiResponse(responseCode = "200", description = "Perfil substituído.", content = @Content(schema = @Schema(implementation = UserProfileResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Payload, CEP, UF ou tamanho inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Sem IDENTITY_USER_UPDATE.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Usuário inexistente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Conflito concorrente; reler e repetir a substituição.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<UserProfileResponseDTO> replaceProfile(@PathVariable @Positive Long userId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody JsonNode body) {
        var data = UserProfileDataDTO.read(body);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(UserProfileResponseDTO.from(adminUserService.replaceProfile(userId, principal.getUserId(),
                        data.workProfile() == null ? null : data.workProfile().toDomain(), data.address() == null ? null : data.address().toDomain()),membershipNumbers.currentNumber(userId)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('IDENTITY_USER_CREATE')")
    @RequiredPermission("IDENTITY_USER_CREATE")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(summary = "Criar usuário administrativamente",
            description = "Cria User e credencial pendente em uma transação. TEMPORARY_PASSWORD revela a senha somente nesta resposta; EMAIL_INVITATION prepara envio após commit e permite reenvio em falha. Não cria MembershipApplication nem atribui roles.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Usuário criado; Cache-Control: no-store.",
                            content = @Content(schema = @Schema(implementation = AdminCreateUserResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Dados ou modalidade inválidos.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Sem IDENTITY_USER_CREATE.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "CPF, e-mail ou RG já cadastrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public ResponseEntity<AdminCreateUserResponseDTO> create(
            @RequestBody @Valid AdminCreateUserRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Instant now = Instant.now(clock);
        var result = userRegistration.createAdministratively(new UserRegistrationData(
                request.name(), request.birthDate(), request.email(), request.cpf(), request.rg(),
                request.phoneNumber(), null, now), request.activationMethod(), principal.getUserId());
        String invitationStatus = result.activationMethod() == UserActivationMethod.EMAIL_INVITATION
                ? userRegistration.administrativeInvitationStatus(result.userId()) : null;
        return ResponseEntity.created(URI.create("/identity/admin/users/" + result.userId()))
                .header("Cache-Control", "no-store")
                .body(AdminCreateUserResponseDTO.from(result, invitationStatus));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('IDENTITY_USER_READ')")
    @RequiredPermission("IDENTITY_USER_READ")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Listar usuários",
            description = """
                Retorna os usuários cadastrados no módulo de identidade.

                A consulta suporta:
                - paginação zero-based por `page` e `size` (padrão 20, máximo 50);
                - ordenação por `sort`;
                - filtros administrativos combináveis;
                - matrícula vigente exata por `membershipNumber`, combinável com os demais filtros;
                - busca textual por `q`, incluindo matrícula vigente exata;
                - seleção dos campos retornados por `fields`.

                Quando `fields` não é informado, todos os campos disponíveis
                em AdminUserResponse são considerados no retorno, incluindo MEMBERSHIP_NUMBER.
                fields=NAME,MEMBERSHIP_NUMBER não expõe id; sua chave auxiliar é interna.
                fields=ID,NAME omite matrícula intencionalmente.
                sort=membershipNumber não é suportado nesta superfície (400);
                use /admin/members para ordenação global de associados por matrícula.

                Os filtros, paginação, ordenação e seleção de campos são
                aplicados na consulta de persistência.
                """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Página de usuários retornada com sucesso."
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Filtros, paginação, ordenação ou campos solicitados são inválidos.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(
                                            implementation = ApiErrorResponse.class
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Usuário não autenticado.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(
                                            implementation = ApiErrorResponse.class
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Usuário sem permissão para consultar usuários.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(
                                            implementation = ApiErrorResponse.class
                                    )
                            )
                    )
            }
    )
    public ResponseEntity<PageResponse<AdminUserResponseDTO>> findAll(
            @Valid
            @ParameterObject
            @ModelAttribute
            AdminUserFilterDTO filters,

            @Parameter(
                    description = """
                        Campos que devem ser retornados para cada usuário.
                        Quando omitido, todos os campos disponíveis são considerados.
                        """,
                    example = "ID,NAME,EMAIL,STATUS"
            )
            @RequestParam(required = false)
            Set<AdminUserField> fields,

            @ParameterObject
            @SortDefault(
                    sort = "id",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable
    ) {
        Set<AdminUserField> selectedFields =
                fields == null || fields.isEmpty()
                        ? EnumSet.allOf(AdminUserField.class)
                        : EnumSet.copyOf(fields);

        var page = adminUserService.findAll(filters.toFilter(), selectedFields, pageable);
        var numbers = selectedFields.contains(AdminUserField.MEMBERSHIP_NUMBER)
                ? membershipNumbers.currentNumbers(page.getContent().stream().map(AdminUserResult::id)
                    .filter(java.util.Objects::nonNull).toList()) : java.util.Map.<Long,String>of();
        PageResponse<AdminUserResponseDTO> response = PageResponse.from(
                page.map(result -> AdminUserResponseDTO.from(result,result.id()==null ? null : numbers.get(result.id()),selectedFields.contains(AdminUserField.ID))));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('IDENTITY_USER_READ')")
    @RequiredPermission("IDENTITY_USER_READ")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Consultar usuário",
            description = "Retorna os dados administrativos de um usuário específico.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Usuário retornado com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = AdminUserResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Usuário não encontrado.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Identificador do usuário inválido.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Usuário não autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Usuário sem a permissão IDENTITY_USER_READ.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))
                    )
            }
    )
    public ResponseEntity<AdminUserResponseDTO> findById(
            @PathVariable @Positive Long userId
    ) {
        AdminUserResult result = adminUserService.findById(userId);

        return ResponseEntity.ok(AdminUserResponseDTO.from(result,membershipNumbers.currentNumber(result.id())));
    }

    @PatchMapping("/{userId}/disable")
    @PreAuthorize("hasAuthority('IDENTITY_USER_DISABLE')")
    @RequiredPermission("IDENTITY_USER_DISABLE")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Desativar usuário",
            description = """
                    Desativa administrativamente o User e o acesso da AuthenticationAccount na
                    mesma operação. Sessões, refresh tokens e challenges aplicáveis são revogados
                    ou invalidados.

                    Usuários portadores da role estrutural ROOT são protegidos contra desativação
                    e resultam em conflito 409.

                    A operação preserva senha, CredentialStatus, lock automático e
                    failedLoginAttempts.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Usuário desativado com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = AdminUserResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Usuário não encontrado.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Usuário já está desativado, possui role ROOT protegida contra desativação ou a conta de Authentication necessária ao lifecycle não existe.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Identificador do usuário inválido.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Usuário não autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Usuário sem a permissão IDENTITY_USER_DISABLE.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))
                    )
            }
    )
    public ResponseEntity<AdminUserResponseDTO> disable(
            @PathVariable @Positive Long userId
    ) {
        AdminUserResult result = adminUserService.disable(userId);

        return ResponseEntity.ok(AdminUserResponseDTO.from(result,membershipNumbers.currentNumber(result.id())));
    }

    @PatchMapping("/{userId}/enable")
    @PreAuthorize("hasAuthority('IDENTITY_USER_ENABLE')")
    @RequiredPermission("IDENTITY_USER_ENABLE")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Reativar usuário",
            description = """
                    Reativa administrativamente o User e habilita o acesso da
                    AuthenticationAccount na mesma operação.

                    A operação não remove lock automático, não zera tentativas de login, não
                    altera senha ou CredentialStatus e não recria tokens nem sessões.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Usuário reativado com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = AdminUserResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Usuário não encontrado.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Usuário já está ativo ou não pode ser reativado no estado atual.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Identificador do usuário inválido.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Usuário não autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Usuário sem a permissão IDENTITY_USER_ENABLE.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))
                    )
            }
    )
    public ResponseEntity<AdminUserResponseDTO> enable(
            @PathVariable @Positive Long userId
    ) {
        AdminUserResult result = adminUserService.enable(userId);

        return ResponseEntity.ok(AdminUserResponseDTO.from(result,membershipNumbers.currentNumber(result.id())));
    }
}
