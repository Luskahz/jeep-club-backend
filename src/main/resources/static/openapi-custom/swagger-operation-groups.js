(function () {
    const GROUP_NAME_EXTENSION = "x-operation-group";
    const GROUP_ORDER_EXTENSION = "x-operation-group-order";
    const DEFAULT_GROUP_NAME = "Outras rotas";
    const DEFAULT_GROUP_ORDER = 9999;

    function getSwaggerSpec() {
        try {
            return window.ui
                ?.getSystem?.()
                ?.specSelectors
                ?.specJson?.()
                ?.toJS?.();
        } catch (error) {
            return null;
        }
    }

    function normalizeText(value) {
        return String(value || "")
            .toLowerCase()
            .replace(/[^a-z0-9]/g, "");
    }

    function getOperationDataFromElement(operationElement, swaggerSpec) {
        if (!swaggerSpec || !swaggerSpec.paths) {
            return null;
        }

        const operationText = normalizeText(operationElement.textContent);
        const operationId = normalizeText(operationElement.getAttribute("id"));

        for (const path of Object.keys(swaggerSpec.paths)) {
            const pathItem = swaggerSpec.paths[path];

            for (const method of Object.keys(pathItem)) {
                const operation = pathItem[method];

                if (!operation || typeof operation !== "object") {
                    continue;
                }

                const normalizedMethod = normalizeText(method);
                const normalizedPath = normalizeText(path);
                const normalizedSummary = normalizeText(operation.summary);
                const normalizedOperationId = normalizeText(operation.operationId);

                const matchesByOperationId =
                    normalizedOperationId && operationId.includes(normalizedOperationId);

                const matchesByPathAndMethod =
                    operationText.includes(normalizedMethod)
                    && operationText.includes(normalizedPath);

                const matchesBySummaryAndMethod =
                    normalizedSummary
                    && operationText.includes(normalizedMethod)
                    && operationText.includes(normalizedSummary);

                if (matchesByOperationId || matchesByPathAndMethod || matchesBySummaryAndMethod) {
                    return operation;
                }
            }
        }

        return null;
    }

    function getOperationGroup(operationElement, swaggerSpec) {
        const operation = getOperationDataFromElement(operationElement, swaggerSpec);

        if (!operation) {
            return {
                name: DEFAULT_GROUP_NAME,
                order: DEFAULT_GROUP_ORDER
            };
        }

        return {
            name: operation[GROUP_NAME_EXTENSION] || DEFAULT_GROUP_NAME,
            order: Number(operation[GROUP_ORDER_EXTENSION] ?? DEFAULT_GROUP_ORDER)
        };
    }

    function createGroupElement(groupName) {
        const details = document.createElement("details");
        details.className = "swagger-operation-group";
        details.open = true;

        const summary = document.createElement("summary");
        summary.className = "swagger-operation-group-title";
        summary.textContent = groupName;

        details.appendChild(summary);

        return details;
    }

    function groupOperationsInsideTag(tagSection, swaggerSpec) {
        const operations = Array.from(tagSection.querySelectorAll(".opblock"))
            .filter(operation => !operation.closest(".swagger-operation-group"));

        if (operations.length === 0) {
            return;
        }

        const firstOperationParent = operations[0].parentElement;

        if (!firstOperationParent || firstOperationParent.dataset.operationGroupsApplied === "true") {
            return;
        }

        const groups = new Map();

        for (const operationElement of operations) {
            const group = getOperationGroup(operationElement, swaggerSpec);
            const groupKey = `${group.order}::${group.name}`;

            if (!groups.has(groupKey)) {
                groups.set(groupKey, {
                    name: group.name,
                    order: group.order,
                    operations: []
                });
            }

            groups.get(groupKey).operations.push(operationElement);
        }

        const sortedGroups = Array.from(groups.values())
            .sort((left, right) => {
                if (left.order !== right.order) {
                    return left.order - right.order;
                }

                return left.name.localeCompare(right.name);
            });

        if (sortedGroups.length <= 1 && sortedGroups[0]?.name === DEFAULT_GROUP_NAME) {
            return;
        }

        firstOperationParent.dataset.operationGroupsApplied = "true";

        for (const group of sortedGroups) {
            const groupElement = createGroupElement(group.name);

            for (const operationElement of group.operations) {
                groupElement.appendChild(operationElement);
            }

            firstOperationParent.appendChild(groupElement);
        }
    }

    function applyOperationGroups() {
        const swaggerSpec = getSwaggerSpec();

        if (!swaggerSpec) {
            return;
        }

        const tagSections = document.querySelectorAll(".opblock-tag-section");

        for (const tagSection of tagSections) {
            groupOperationsInsideTag(tagSection, swaggerSpec);
        }
    }

    function bootstrap() {
        const observer = new MutationObserver(function () {
            applyOperationGroups();
        });

        observer.observe(document.body, {
            childList: true,
            subtree: true
        });

        setTimeout(applyOperationGroups, 300);
        setTimeout(applyOperationGroups, 800);
        setTimeout(applyOperationGroups, 1500);
        setTimeout(applyOperationGroups, 3000);
    }

    window.addEventListener("load", bootstrap);
})();