(function () {
    const GROUP_NAME_EXTENSION = "x-operation-group";
    const GROUP_ORDER_EXTENSION = "x-operation-group-order";
    const DEFAULT_GROUP_NAME = "Outras rotas";
    const DEFAULT_GROUP_ORDER = 9999;
    const MAX_ATTEMPTS = 80;
    const INTERVAL_MS = 250;

    let attempts = 0;
    let originalTagsClosed = false;

    function getSwaggerSpec() {
        try {
            if (!window.ui || !window.ui.getSystem) {
                return null;
            }

            const system = window.ui.getSystem();

            if (!system || !system.specSelectors || !system.specSelectors.specJson) {
                return null;
            }

            const immutableSpec = system.specSelectors.specJson();

            if (!immutableSpec || !immutableSpec.toJS) {
                return null;
            }

            return immutableSpec.toJS();
        } catch (error) {
            console.warn("[swagger-operation-groups] Could not read swagger spec.", error);
            return null;
        }
    }

    function getOperationMethod(operationElement) {
        const methodElement = operationElement.querySelector(".opblock-summary-method");

        if (!methodElement) {
            return null;
        }

        return methodElement.textContent.trim().toLowerCase();
    }

    function getOperationPath(operationElement) {
        const pathElement = operationElement.querySelector(".opblock-summary-path");

        if (!pathElement) {
            return null;
        }

        const dataPath = pathElement.getAttribute("data-path");

        if (dataPath) {
            return dataPath.trim();
        }

        return pathElement.textContent.trim();
    }

    function findOperationInSpec(operationElement, swaggerSpec) {
        const method = getOperationMethod(operationElement);
        const path = getOperationPath(operationElement);

        if (!method || !path || !swaggerSpec || !swaggerSpec.paths) {
            return null;
        }

        const pathItem = swaggerSpec.paths[path];

        if (!pathItem) {
            return null;
        }

        return pathItem[method] || null;
    }

    function getOperationGroup(operationElement, swaggerSpec) {
        const operation = findOperationInSpec(operationElement, swaggerSpec);

        if (!operation) {
            return {
                name: DEFAULT_GROUP_NAME,
                order: DEFAULT_GROUP_ORDER
            };
        }

        const order = operation[GROUP_ORDER_EXTENSION];

        return {
            name: operation[GROUP_NAME_EXTENSION] || DEFAULT_GROUP_NAME,
            order: order === undefined || order === null
                ? DEFAULT_GROUP_ORDER
                : Number(order)
        };
    }

    function createGroupElement(groupName) {
        const details = document.createElement("details");
        details.className = "swagger-operation-group";

        // Subgrupos customizados nascem fechados.
        details.open = false;

        const summary = document.createElement("summary");
        summary.className = "swagger-operation-group-title";
        summary.textContent = groupName;

        details.appendChild(summary);

        return details;
    }

    function groupTagSection(tagSection, swaggerSpec) {
        const operations = Array.from(tagSection.querySelectorAll(".opblock"))
            .filter(function (operationElement) {
                return !operationElement.closest(".swagger-operation-group");
            });

        if (operations.length === 0) {
            return false;
        }

        const container = operations[0].parentElement;

        if (!container || container.dataset.operationGroupsApplied === "true") {
            return false;
        }

        const groups = new Map();

        operations.forEach(function (operationElement) {
            const group = getOperationGroup(operationElement, swaggerSpec);
            const groupKey = group.order + "::" + group.name;

            if (!groups.has(groupKey)) {
                groups.set(groupKey, {
                    name: group.name,
                    order: group.order,
                    operations: []
                });
            }

            groups.get(groupKey).operations.push(operationElement);
        });

        const sortedGroups = Array.from(groups.values())
            .sort(function (left, right) {
                if (left.order !== right.order) {
                    return left.order - right.order;
                }

                return left.name.localeCompare(right.name);
            });

        if (sortedGroups.length <= 1 && sortedGroups[0] && sortedGroups[0].name === DEFAULT_GROUP_NAME) {
            return false;
        }

        container.dataset.operationGroupsApplied = "true";

        sortedGroups.forEach(function (group) {
            const groupElement = createGroupElement(group.name);

            group.operations.forEach(function (operationElement) {
                groupElement.appendChild(operationElement);
            });

            container.appendChild(groupElement);
        });

        return true;
    }

    function applyOperationGroups() {
        const swaggerSpec = getSwaggerSpec();

        if (!swaggerSpec) {
            return false;
        }

        const tagSections = document.querySelectorAll(".opblock-tag-section");

        if (!tagSections || tagSections.length === 0) {
            return false;
        }

        let applied = false;

        tagSections.forEach(function (tagSection) {
            if (groupTagSection(tagSection, swaggerSpec)) {
                applied = true;
            }
        });

        return applied;
    }

    function closeOriginalSwaggerTags() {
        if (originalTagsClosed) {
            return;
        }

        const tagSections = Array.from(document.querySelectorAll(".opblock-tag-section"));

        if (tagSections.length === 0) {
            return;
        }

        tagSections.forEach(function (tagSection) {
            const tagButton = tagSection.querySelector(".opblock-tag");

            if (!tagButton) {
                return;
            }

            const hasVisibleOperations = tagSection.querySelector(".opblock") !== null;

            if (hasVisibleOperations) {
                tagButton.click();
            }
        });

        originalTagsClosed = true;

        console.info("[swagger-operation-groups] Original Swagger tags closed.");
    }

    function closeCustomGroups() {
        document.querySelectorAll(".swagger-operation-group").forEach(function (groupElement) {
            groupElement.open = false;
        });
    }

    function applyAndClose() {
        const applied = applyOperationGroups();

        if (applied) {
            closeCustomGroups();
        }

        closeOriginalSwaggerTags();

        return applied;
    }

    function retryUntilSwaggerIsReady() {
        attempts++;

        const applied = applyAndClose();

        if (applied) {
            console.info("[swagger-operation-groups] Groups applied.");
            return;
        }

        if (attempts >= MAX_ATTEMPTS) {
            closeOriginalSwaggerTags();
            console.warn("[swagger-operation-groups] Swagger UI was not ready or no groups were applied.");
            return;
        }

        window.setTimeout(retryUntilSwaggerIsReady, INTERVAL_MS);
    }

    function observeSwaggerChanges() {
        const root = document.getElementById("swagger-ui");

        if (!root) {
            return;
        }

        const observer = new MutationObserver(function () {
            applyOperationGroups();
            closeCustomGroups();
        });

        observer.observe(root, {
            childList: true,
            subtree: true
        });
    }

    function start() {
        console.info("[swagger-operation-groups] Script loaded.");

        retryUntilSwaggerIsReady();

        window.setTimeout(observeSwaggerChanges, 1000);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", start);
    } else {
        start();
    }
})();