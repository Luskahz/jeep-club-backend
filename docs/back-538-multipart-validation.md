# BACK-538: Vehicle multipart validation

Baseline: `ab327ab7ebadf23bea3d51f39a1e1c4b26cf8850` (`origin/develop`, fetched 2026-10-29).

The original `unexpected=value` finding is already blocked by `VehicleGalleryHttp.validate()`.
Real Servlet HTTP tests confirm HTTP 400, `application/problem+json`, `INVALID_IMAGE`, no new
Vehicle and no storage call on member/admin creation and gallery maintenance. Registration
editing (`/vehicles/edit/...`) uses JSON; multipart image editing uses `/vehicles/gallery/...`.

The audit reproduced a remaining ambiguity on **all four multipart routes**: a structured JSON
field without filename plus another part with the same name and a filename passed the separate
parameter/file-map checks. Baseline regression run: 8 tests, 4 failures (creation returned 201;
gallery maintenance returned 200). Original Servlet parts are now counted for `vehicle`/`gallery`
before existing validation. One legitimate JSON part represented in multiple Servlet maps is
counted once. No additional global parameter whitelist was added.

HTTP regression covers unknown text/file fields, textual `photo`/`photos`, repeated structured
parts (including mixed field/file representation), repeated legacy photo, six photos, legacy
photo mixed with gallery/photos, and inappropriate creation fields on gallery maintenance.
Invalid requests preserve row count and existing gallery revision/photo and never call storage.
Valid cases cover typed JSON fields without filename, optional photo, legacy photo, galleries
of 0 through 5 uploads, explicit gallery metadata, member/admin ownership and JSON creation.
Existing integration suites cover DTO validation, JSON editing, authorization and rollback.

After the fix, the real HTTP suite plus Vehicle gallery and media orchestration integrations
passed: 52 tests, zero failures/errors/skips. Full final-commit validation and CI are recorded
in the shared BACK-538/BACK-539 PR. No migration, JPA/schema change, storage cleanup or BACK-547 work.
