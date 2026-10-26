package com.jeepclub.backend.memberships.registry;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
@SpringBootTest(properties={"memberships.registry.imports.apply-enabled=true","memberships.registry.issuance.enabled=true","memberships.registry.issuance.approved=true"})
@ActiveProfiles("test")
class MemberRegistryIntegrationTest extends MemberRegistryScenarios { }
