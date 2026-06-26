(function () {
    const GROUP_NAME_EXTENSION = "x-operation-group";
    const GROUP_ORDER_EXTENSION = "x-operation-group-order";

    const DEFAULT_GROUP_NAME = "Outras rotas";
    const DEFAULT_GROUP_ORDER = 9999;

    const SPEC_RETRY_INTERVAL_MS = 100;
    const MAX_SPEC_RETRIES = 100;

    let applyScheduled = false;
    let specRetryScheduled = false;
    let specRetryCount = 0;

    function getSwaggerSpec() {
        try {
            if (!window.ui || !window.ui.getSystem) {
                return null;
            }

            const system = window.ui.getSystem();

            if (
                !system ||
                !system.specSelectors ||
                !system.specSelectors.specJson
            ) {
                return null;
            }

            const immutableSpec =
                system.specSelectors.specJson();

            if (!immutableSpec || !immutableSpec.toJS) {
                return null;
            }

            return immutableSpec.toJS();
        } catch (error) {
            console.warn(
                "[swagger-operation-groups] Não foi possível ler a especificação OpenAPI.",
                error
            );

            return null;
        }
    }

    function getOperationMethod(operationElement) {
        const methodElement =
            operationElement.querySelector(
                ".opblock-summary-method"
            );

        if (!methodElement) {
            return null;
        }

        return methodElement.textContent
            .trim()
            .toLowerCase();
    }

    function getOperationPath(operationElement) {
        const pathElement =
            operationElement.querySelector(
                ".opblock-summary-path"
            );

        if (!pathElement) {
            return null;
        }

        const dataPath =
            pathElement.getAttribute("data-path");

        if (dataPath) {
            return dataPath.trim();
        }

        return pathElement.textContent.trim();
    }

    function findOperationInSpec(
        operationElement,
        swaggerSpec
    ) {
        const method =
            getOperationMethod(operationElement);

        const path =
            getOperationPath(operationElement);

        if (
            !method ||
            !path ||
            !swaggerSpec ||
            !swaggerSpec.paths
        ) {
            return null;
        }

        const pathItem = swaggerSpec.paths[path];

        if (!pathItem) {
            return null;
        }

        return pathItem[method] || null;
    }

    function getOperationGroup(
        operationElement,
        swaggerSpec
    ) {
        const operation = findOperationInSpec(
            operationElement,
            swaggerSpec
        );

        if (!operation) {
            return {
                name: DEFAULT_GROUP_NAME,
                order: DEFAULT_GROUP_ORDER
            };
        }

        const rawOrder =
            operation[GROUP_ORDER_EXTENSION];

        const parsedOrder = Number(rawOrder);

        return {
            name:
                operation[GROUP_NAME_EXTENSION] ||
                DEFAULT_GROUP_NAME,

            order:
                rawOrder === undefined ||
                rawOrder === null ||
                Number.isNaN(parsedOrder)
                    ? DEFAULT_GROUP_ORDER
                    : parsedOrder
        };
    }

    function createGroupKey(group) {
        return group.order + "::" + group.name;
    }

    function createGroupElement(group) {
        const details =
            document.createElement("details");

        details.className =
            "swagger-operation-group";

        details.dataset.operationGroupKey =
            createGroupKey(group);

        details.dataset.operationGroupOrder =
            String(group.order);

        /*
         * Somente o subgrupo começa fechado.
         *
         * Depois disso, o script nunca mais altera
         * details.open.
         */
        details.open = false;

        const summary =
            document.createElement("summary");

        summary.className =
            "swagger-operation-group-title";

        summary.textContent = group.name;

        details.appendChild(summary);

        return details;
    }

    function findExistingGroupElement(
        container,
        group
    ) {
        const groupKey = createGroupKey(group);

        return Array.from(container.children)
            .find(function (child) {
                return (
                    child.classList.contains(
                        "swagger-operation-group"
                    ) &&
                    child.dataset.operationGroupKey ===
                        groupKey
                );
            }) || null;
    }

    function sortGroupElements(container) {
        const groupElements =
            Array.from(container.children)
                .filter(function (child) {
                    return child.classList.contains(
                        "swagger-operation-group"
                    );
                });

        groupElements.sort(function (left, right) {
            const leftOrder = Number(
                left.dataset.operationGroupOrder
            );

            const rightOrder = Number(
                right.dataset.operationGroupOrder
            );

            if (leftOrder !== rightOrder) {
                return leftOrder - rightOrder;
            }

            const leftTitle =
                left.querySelector(
                    ".swagger-operation-group-title"
                );

            const rightTitle =
                right.querySelector(
                    ".swagger-operation-group-title"
                );

            return (
                leftTitle?.textContent || ""
            ).localeCompare(
                rightTitle?.textContent || "",
                "pt-BR"
            );
        });

        groupElements.forEach(function (groupElement) {
            container.appendChild(groupElement);
        });
    }

    function groupTagSection(
        tagSection,
        swaggerSpec
    ) {
        /*
         * Considera apenas operações que ainda não estão
         * dentro de um subgrupo.
         */
        const operations = Array.from(
            tagSection.querySelectorAll(".opblock")
        ).filter(function (operationElement) {
            return !operationElement.closest(
                ".swagger-operation-group"
            );
        });

        if (operations.length === 0) {
            return false;
        }

        const container =
            operations[0].parentElement;

        if (!container) {
            return false;
        }

        const groups = new Map();

        operations.forEach(function (operationElement) {
            const group = getOperationGroup(
                operationElement,
                swaggerSpec
            );

            const groupKey =
                createGroupKey(group);

            if (!groups.has(groupKey)) {
                groups.set(groupKey, {
                    name: group.name,
                    order: group.order,
                    operations: []
                });
            }

            groups
                .get(groupKey)
                .operations
                .push(operationElement);
        });

        const existingGroupElements =
            Array.from(container.children)
                .filter(function (child) {
                    return child.classList.contains(
                        "swagger-operation-group"
                    );
                });

        const sortedGroups =
            Array.from(groups.values())
                .sort(function (left, right) {
                    if (left.order !== right.order) {
                        return left.order - right.order;
                    }

                    return left.name.localeCompare(
                        right.name,
                        "pt-BR"
                    );
                });

        /*
         * Caso nenhuma rota tenha grupo customizado,
         * mantém o comportamento original do Swagger.
         */
        if (
            sortedGroups.length === 1 &&
            sortedGroups[0].name ===
                DEFAULT_GROUP_NAME &&
            existingGroupElements.length === 0
        ) {
            return false;
        }

        sortedGroups.forEach(function (group) {
            let groupElement =
                findExistingGroupElement(
                    container,
                    group
                );

            if (!groupElement) {
                groupElement =
                    createGroupElement(group);

                container.appendChild(groupElement);
            }

            group.operations.forEach(
                function (operationElement) {
                    groupElement.appendChild(
                        operationElement
                    );
                }
            );
        });

        sortGroupElements(container);

        return true;
    }

    function scheduleSpecRetry() {
        if (
            specRetryScheduled ||
            specRetryCount >= MAX_SPEC_RETRIES
        ) {
            return;
        }

        specRetryScheduled = true;
        specRetryCount++;

        window.setTimeout(function () {
            specRetryScheduled = false;
            scheduleApply();
        }, SPEC_RETRY_INTERVAL_MS);
    }

    function applyOperationGroups() {
        const swaggerSpec = getSwaggerSpec();

        if (!swaggerSpec) {
            scheduleSpecRetry();
            return;
        }

        specRetryCount = 0;

        const tagSections =
            document.querySelectorAll(
                ".opblock-tag-section"
            );

        tagSections.forEach(function (tagSection) {
            groupTagSection(
                tagSection,
                swaggerSpec
            );
        });
    }

    function scheduleApply() {
        if (applyScheduled) {
            return;
        }

        applyScheduled = true;

        window.requestAnimationFrame(function () {
            applyScheduled = false;
            applyOperationGroups();
        });
    }

    function nodeContainsOperation(node) {
        if (!(node instanceof Element)) {
            return false;
        }

        if (node.matches(".opblock")) {
            return true;
        }

        return node.querySelector(".opblock") !== null;
    }

    function mutationContainsOperation(mutation) {
        return Array.from(
            mutation.addedNodes
        ).some(nodeContainsOperation);
    }

    function observeSwaggerChanges() {
        const root =
            document.getElementById("swagger-ui");

        if (!root) {
            return;
        }

        const observer = new MutationObserver(
            function (mutations) {
                const operationWasAdded =
                    mutations.some(
                        mutationContainsOperation
                    );

                if (operationWasAdded) {
                    scheduleApply();
                }
            }
        );

        observer.observe(root, {
            childList: true,
            subtree: true
        });
    }

    function start() {
        observeSwaggerChanges();
        scheduleApply();

        console.info(
            "[swagger-operation-groups] Observador iniciado."
        );
    }

    if (document.readyState === "loading") {
        document.addEventListener(
            "DOMContentLoaded",
            start
        );
    } else {
        start();
    }
})();