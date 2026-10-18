package com.jeepclub.backend.memberships.registry;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
@SpringBootTest(properties={"memberships.registry.issuance.enabled=true","memberships.registry.issuance.approved=true","memberships.registry.issuance.initial-number=100","memberships.registry.issuance.width=3","memberships.registry.issuance.prefix="})
@ActiveProfiles("test")
class MemberRegistryIntegrationTest extends MemberRegistryScenarios { }
