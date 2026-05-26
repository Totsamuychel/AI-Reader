import Foundation
import GRDB

public final class DatabaseManager: @unchecked Sendable {
    public let pool: DatabasePool

    public init(url: URL) throws {
        var config = Configuration()
        config.busyMode = .timeout(5)
        config.maximumReaderCount = 4
        try FileManager.default.createDirectory(
            at: url.deletingLastPathComponent(),
            withIntermediateDirectories: true
        )
        self.pool = try DatabasePool(path: url.path, configuration: config)
        try Migrator.run(on: pool)
    }

    /// Convenience for in-memory testing.
    public static func inMemory() throws -> DatabaseManager {
        let mgr = try DatabaseManager(url: URL(fileURLWithPath: "/tmp/bookmind-\(UUID().uuidString).sqlite"))
        return mgr
    }
}
