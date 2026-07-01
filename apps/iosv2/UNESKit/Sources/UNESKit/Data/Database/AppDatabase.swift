import Foundation
import GRDB

func appDatabase() throws -> any DatabaseWriter {
    try FileManager.default.createDirectory(
        at: .applicationSupportDirectory,
        withIntermediateDirectories: true
    )
    let path = URL.applicationSupportDirectory.appending(path: "unes.sqlite").path(percentEncoded: false)
    let database = try DatabaseQueue(path: path)

    var migrator = DatabaseMigrator()
    #if DEBUG
    migrator.eraseDatabaseOnSchemaChange = true
    #endif
    try migrator.migrate(database)
    return database
}
