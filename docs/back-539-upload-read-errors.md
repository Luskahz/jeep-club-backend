# BACK-539: controlled image request read errors

`ImageMultipartAdapter.read()` translates **only** `IOException` thrown by `MultipartFile.getBytes()`
to the HTTP-adapter exception `ImageUploadReadException`. No service, filesystem write, database,
transaction or storage operation is inside that catch. The scoped `ImageMediaExceptionHandler`
returns RFC 9457 HTTP **500**, code **`IMAGE_UPLOAD_READ_FAILED`**, with a fixed localized detail
(Portuguese/English). OpenAPI declares the response on all nine affected upload operations.

An arbitrary byte-read IOException cannot reliably establish a malformed client request: the
Servlet upload may already have been parsed to a server temporary file that is now unreadable.
It also does not establish that storage is unavailable or that retry is safe, so neither 400
nor 503 is inferred from that exception. The original message/cause is deliberately discarded;
neither it nor filename, path, storage key, bytes or stack trace is sent to the response or logged.
The existing request logger can record route, status and request ID without the exception.

| Detected condition | Existing/new HTTP contract |
| --- | --- |
| Invalid multipart structure, missing parts, DTO, empty/over-5-MiB image, extension/MIME/signature mismatch | Existing 400; `INVALID_IMAGE` for image/structure errors, existing validation/framework codes otherwise |
| Servlet multipart size ceiling | Existing 413 (image application ceiling remains 400) |
| Unsupported structured part/request media type | Existing 415 |
| IOException while materializing image request bytes | New classified 500 / `IMAGE_UPLOAD_READ_FAILED` |
| Storage read/write unavailable (`StorageOperationException`) | Existing 503 / `IMAGE_STORAGE_UNAVAILABLE` |
| Database, transaction and unrelated runtime failures | Existing handlers; never relabeled as request read errors |

Audited consumers: Vehicle legacy creation and gallery creation/replacement, member/admin;
Dependent creation/update and private photo replacement, member/admin; standalone `POST /media/images`.
The standalone upload now uses the same adapter. Identity, Tools and Publications reference
global images through this transitional uploader and do not read multipart bytes directly.
Billing's receipt reader already has its own `InvalidPaymentReceiptException` mapping and is
unchanged. No public GET authorization or private-photo policy changed.

Regressions simulate byte-read IOException on all affected paths (11 route/format cases), including
a gallery whose first file reads successfully and second fails. They assert controlled error,
sanitized body, no persistence call/new row/update, no storage call and no success. Dependents use
real local storage and committed database transactions to prove existing private files, photo
references, registration and timestamps remain intact. Unit/MockMvc tests check sanitized logs,
the narrow exception boundary, valid files, empty/large files, MIME/signature, 415 and independent
storage-write 503. Real Servlet tests retain 413 and optional/legacy/gallery upload behavior.
Existing suites exercise transactional compensation, outer rollback, cleanup failure and races.

No migration, schema/JPA change, provider, storage cleanup or BACK-547 functionality is required.
Final SHA, complete local test matrix and exact-head CI evidence are recorded in the shared PR.
