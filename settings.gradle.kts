rootProject.name = "solicitation-platform"

// Include all subprojects
include(
    "solicitation-common",
    "solicitation-models",
    "solicitation-storage",
    "solicitation-connectors",
    "solicitation-scoring",
    "solicitation-filters",
    "solicitation-serving",
    "solicitation-channels",
    "solicitation-workflow-etl",
    "solicitation-workflow-filter",
    "solicitation-workflow-score",
    "solicitation-workflow-store",
    "solicitation-workflow-reactive"
)
