// swift-tools-version: 5.10
import PackageDescription

let package = Package(
    name: "BookMindCore",
    platforms: [
        .iOS(.v17),
        .macOS(.v14)
    ],
    products: [
        .library(name: "SharedModels", targets: ["SharedModels"]),
        .library(name: "ReaderCore", targets: ["ReaderCore"]),
        .library(name: "Persistence", targets: ["Persistence"]),
        .library(name: "BookMemory", targets: ["BookMemory"]),
        .library(name: "Retrieval", targets: ["Retrieval"]),
        .library(name: "LLM", targets: ["LLM"]),
        .library(name: "Safety", targets: ["Safety"])
    ],
    dependencies: [
        .package(url: "https://github.com/groue/GRDB.swift.git", from: "6.29.0")
    ],
    targets: [
        .target(
            name: "SharedModels",
            path: "Sources/SharedModels"
        ),
        .target(
            name: "ReaderCore",
            dependencies: ["SharedModels"],
            path: "Sources/ReaderCore"
        ),
        .target(
            name: "Persistence",
            dependencies: [
                "SharedModels",
                .product(name: "GRDB", package: "GRDB.swift")
            ],
            path: "Sources/Persistence"
        ),
        .target(
            name: "BookMemory",
            dependencies: ["SharedModels", "Persistence"],
            path: "Sources/BookMemory"
        ),
        .target(
            name: "Retrieval",
            dependencies: ["SharedModels", "BookMemory", "Persistence"],
            path: "Sources/Retrieval"
        ),
        .target(
            name: "Safety",
            dependencies: ["SharedModels", "BookMemory"],
            path: "Sources/Safety"
        ),
        .target(
            name: "LLM",
            dependencies: ["SharedModels", "Retrieval", "Safety"],
            path: "Sources/LLM"
        ),
        .testTarget(
            name: "BookMindTests",
            dependencies: [
                "SharedModels",
                "ReaderCore",
                "Persistence",
                "BookMemory",
                "Retrieval",
                "LLM",
                "Safety"
            ],
            path: "Tests/Unit"
        ),
        .testTarget(
            name: "BookMindIntegrationTests",
            dependencies: [
                "SharedModels",
                "ReaderCore",
                "Persistence",
                "BookMemory",
                "Retrieval",
                "LLM",
                "Safety"
            ],
            path: "Tests/Integration"
        )
    ]
)
