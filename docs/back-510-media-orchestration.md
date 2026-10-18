# BACK-510 — Vehicle upload orchestration

Base: `feat/back-460-event-billing-fixes@5df55533b7d6f11f225f9c09e079a1b091bb8627`, [PR #105](https://github.com/Luskahz/jeep-club-backend/pull/105). This PR targets that branch, not develop; its diff contains only BACK-510. Keep the predecessor branch. Remaining chain: BACK-512 → BACK-523 → BACK-524 → BACK-525. No merge, deploy or VPS operation is part of this delivery.

## Acceptance and boundaries

Vehicle member/admin creation accepts one optional image in the same multipart request as its validated JSON. JSON-only creation remains available without a photo. `IncludeRequestDTO` no longer has `photo` or `storageKey`; those fields, arbitrary owner injection and other unknown fields are rejected. Existing photo references in responses, editing, historical snapshots and legacy URLs are preserved.

Application services receive the provider/HTTP-neutral `ImageUpload` and use Platform's `ImageMediaCommand.storeInTransaction`. They never access filesystem/provider implementations or delete a supplied storage key. Platform reuses `ImageMediaService` JPEG/PNG/WebP signature/MIME/extension validation and `FileStorage`; no new storage implementation, media database table or alternate image pipeline is introduced. The existing Vehicles query API for Events is unchanged.

## Consistency

The command requires an active database transaction and synchronization before storing any bytes. The storage provider generates a fresh exclusive key with CREATE_NEW. After storage succeeds, a transaction callback is registered:

| Outcome | Action |
| --- | --- |
| Commit | Keep image and persisted reference |
| Known rollback, including outer rollback after JPA save | Delete only the image created by this call |
| Registration failure before returning the key | Compensate fresh image and preserve the original failure |
| Compensation also fails | Preserve the original business/JPA error; log only a fixed message and exception category, without keys, names, paths, provider messages or stack traces |
| Commit outcome UNKNOWN | Retain the image and log a sanitized reconciliation signal; deleting could break an actually committed reference |

There is no distributed ACID transaction with storage. Known rollback cleanup is deterministic and tested; provider outage during cleanup can leave an orphan requiring operational reconciliation. An unknown commit outcome also requires reconciliation. A periodic cleanup job is not the primary consistency mechanism. Do not publish/share a fresh key through an independently committed operation before the owning transaction finishes.

The LOCAL provider also removes its own newly created partial file if writing/closing fails before it can return a key. CREATE_NEW failures never delete pre-existing files. A controlled interrupted-write regression preserves a separate old object. Old/shared photos are never removed when editing, disassociating or deleting a business resource; their existing retention policy remains intact.

## HTTP and frontend

| Method/path | Data | Image | Access |
| --- | --- | --- | --- |
| POST /vehicles/include/member | part `vehicle`, application/json | part `photo`, optional file | Authenticated owner from principal |
| POST /vehicles/include/admin/{memberId} | same | same | VEHICLES_VEHICLE_CREATE; existing active Identity owner |

Both return the existing empty 201 response. `memberId` remains the technical Identity ID required by the existing route, never a membership number. JSON application/json requests use the same DTO without a photo. Multipart accepts exactly one structured part and at most one photo; duplicate/unknown file parts and textual image references are rejected. An explicitly empty file is invalid; omission means no photo.

Images are JPEG, PNG or WebP, non-empty and at most 5 MiB, with matching filename extension, MIME and signature. Invalid structured data/images return controlled 400, missing/incorrect authentication/authority retain 401/403, owner absence 404, duplicate identifiers/inactive admin owner 409, storage I/O failure sanitized 503 IMAGE_STORAGE_UNAVAILABLE. OpenAPI describes both content types, typed JSON, binary optional photo and problem responses. Existing update paths still accept existing global references until the Gallery delivery changes that contract.

```javascript
const form = new FormData();
form.append('vehicle', new Blob([JSON.stringify(vehicle)], {type: 'application/json'}));
if (photo) form.append('photo', photo);
await fetch('/vehicles/include/member', {
  method: 'POST', headers: {Authorization: `Bearer ${accessToken}`}, body: form
});
// Let the browser set multipart Content-Type with its boundary.
```

## Consumer audit and transitional endpoint

| Consumer | Observed contract | Decision in BACK-510 |
| --- | --- | --- |
| Vehicles creation | Was upload then photo key in JSON | Replaced by direct optional multipart upload, member/admin |
| Vehicles editing | Field-presence-aware photo reference | Preserve until gallery; omitted photo must preserve legacy data |
| Identity | Registration/profile photo reference and ProfileImageAdapter | Preserve legitimate global upload consumer; later migration is separate |
| Tools | Create/photo update via ImageMediaService references | Preserve legitimate global upload consumer |
| Publications | Notice/Event/Service galleries, requests/change requests and comments | Audit confirms real consumers; preserve reference contracts |
| Billing | Dedicated payment receipt validation, lifecycle and authorized download | Preserve its domain-specific port; do not turn receipt into public image |
| Dependents | No photo fields before BACK-524 | Reuse this internal command when photo is added; do not implement the feature here |

`POST /media/images` therefore remains temporarily available for those legitimate clients. It is not required by Vehicle creation. Global read stays authenticated with existing semantics; private dependent photo access must be addressed in BACK-524. The inventory is current code evidence, replacing the historical claim that Publications was absent.

## Persistence, configuration and pre-deploy

No migration or entity mapping change. V1–V4 are untouched and V5 remains available. No new storage volume/provider or mandatory custom environment variable. `spring.servlet.multipart.max-file-size=10MB` and `max-request-size=12MB` allow the existing 10 MiB Billing receipt contract and multipart overhead; the image command still enforces 5 MiB. Standard Spring environment overrides are possible when explicitly forwarded to the container. Review overrides against both Billing and Images; lowering the servlet ceiling below either advertised contract breaks it.

The real servlet HTTP test exercises 5 MiB and 5 MiB+1 rather than assuming MockMvc enforces container limits. Future infrastructure review must confirm reverse-proxy body/time limits, disk/volume capacity, storage permissions, backup/restoration of DB plus uploads, and startup with Flyway/Hibernate validate. No private server setting was inspected or changed. Preserve the #103/#104 handoff, keep Registry issuance/import flags disabled until separate operational approval, and review all stacked PRs together before a future release. A downgrade to the old Vehicle create contract requires frontend coordination; files created by committed operations remain retained.

## Validation

Final verify, MySQL, forward-migration and published CI evidence are recorded after execution. Focused tests cover success with/without photo, DTO/multipart/file rejection, actual servlet limits, storage failure, real JPA constraint failure, outer rollback, cleanup failure/original exception, concurrent duplicate creation, previous reference retention, member ownership, admin permissions/owner eligibility and generated OpenAPI.
