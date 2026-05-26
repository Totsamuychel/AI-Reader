import Foundation
import GRDB

enum Migrator {
    static func run(on pool: DatabasePool) throws {
        var migrator = DatabaseMigrator()

        migrator.registerMigration("v1_books") { db in
            try db.create(table: "books") { t in
                t.column("id", .text).primaryKey()
                t.column("title", .text).notNull()
                t.column("author", .text)
                t.column("format", .text).notNull()
                t.column("file_url", .text).notNull()
                t.column("added_at", .datetime).notNull()
            }

            try db.create(table: "chapters") { t in
                t.column("id", .text).primaryKey()
                t.column("book_id", .text)
                    .notNull()
                    .references("books", onDelete: .cascade)
                t.column("idx", .integer).notNull()
                t.column("title", .text)
                t.column("content_ref", .text)
                t.uniqueKey(["book_id", "idx"])
            }

            try db.create(table: "reading_progress") { t in
                t.column("book_id", .text)
                    .primaryKey()
                    .references("books", onDelete: .cascade)
                t.column("chapter_id", .text)
                t.column("progress_fraction", .double).notNull()
                t.column("updated_at", .datetime).notNull()
            }
        }

        migrator.registerMigration("v2_book_memory") { db in
            try db.create(table: "chunks") { t in
                t.column("id", .text).primaryKey()
                t.column("book_id", .text).notNull().references("books", onDelete: .cascade)
                t.column("chapter_id", .text).notNull()
                t.column("idx", .integer).notNull()
                t.column("text", .text).notNull()
                t.column("token_count", .integer).notNull()
                t.column("spoiler_level", .integer).notNull().defaults(to: 0)
                t.column("chapter_index", .integer).notNull().defaults(to: 0)
            }
            try db.create(indexOn: "chunks", columns: ["book_id", "chapter_index"])

            try db.execute(sql: """
                CREATE VIRTUAL TABLE chunks_fts USING fts5(
                    text,
                    content='chunks',
                    content_rowid='rowid',
                    tokenize='unicode61'
                );
            """)
            // Triggers to keep FTS in sync.
            try db.execute(sql: """
                CREATE TRIGGER chunks_ai AFTER INSERT ON chunks BEGIN
                    INSERT INTO chunks_fts(rowid, text) VALUES (new.rowid, new.text);
                END;
            """)
            try db.execute(sql: """
                CREATE TRIGGER chunks_ad AFTER DELETE ON chunks BEGIN
                    INSERT INTO chunks_fts(chunks_fts, rowid, text) VALUES ('delete', old.rowid, old.text);
                END;
            """)
            try db.execute(sql: """
                CREATE TRIGGER chunks_au AFTER UPDATE ON chunks BEGIN
                    INSERT INTO chunks_fts(chunks_fts, rowid, text) VALUES ('delete', old.rowid, old.text);
                    INSERT INTO chunks_fts(rowid, text) VALUES (new.rowid, new.text);
                END;
            """)

            try db.create(table: "characters") { t in
                t.column("id", .text).primaryKey()
                t.column("book_id", .text).notNull().references("books", onDelete: .cascade)
                t.column("canonical_name", .text).notNull()
                t.column("aliases_json", .text).notNull().defaults(to: "[]")
                t.column("first_chapter_index", .integer).notNull()
                t.column("last_safe_chapter_index", .integer).notNull()
                t.column("safe_summary", .text)
            }
            try db.create(indexOn: "characters", columns: ["book_id", "canonical_name"])

            try db.create(table: "events") { t in
                t.column("id", .text).primaryKey()
                t.column("book_id", .text).notNull().references("books", onDelete: .cascade)
                t.column("chapter_id", .text).notNull()
                t.column("chapter_index", .integer).notNull()
                t.column("event_type", .text).notNull()
                t.column("short_description", .text).notNull()
                t.column("long_description_safe", .text)
                t.column("spoiler_level", .integer).notNull().defaults(to: 0)
            }
            try db.create(indexOn: "events", columns: ["book_id", "chapter_index"])

            try db.create(table: "relations") { t in
                t.column("id", .text).primaryKey()
                t.column("book_id", .text).notNull().references("books", onDelete: .cascade)
                t.column("source_character_id", .text).notNull()
                t.column("target_character_id", .text).notNull()
                t.column("relation_type", .text).notNull()
                t.column("confidence", .double).notNull()
                t.column("safe_until_chapter_index", .integer).notNull()
            }

            try db.create(table: "recaps") { t in
                t.column("id", .text).primaryKey()
                t.column("book_id", .text).notNull().references("books", onDelete: .cascade)
                t.column("chapter_id", .text).notNull()
                t.column("chapter_index", .integer).notNull()
                t.column("recap_text", .text).notNull()
                t.column("style", .text).notNull().defaults(to: "safe")
            }
            try db.create(indexOn: "recaps", columns: ["book_id", "chapter_index"])

            try db.create(table: "facts") { t in
                t.column("id", .text).primaryKey()
                t.column("book_id", .text).notNull().references("books", onDelete: .cascade)
                t.column("chapter_id", .text).notNull()
                t.column("chapter_index", .integer).notNull()
                t.column("fact_type", .text).notNull()
                t.column("subject_id", .text)
                t.column("object_id", .text)
                t.column("text", .text).notNull()
                t.column("spoiler_level", .integer).notNull().defaults(to: 0)
            }
            try db.create(indexOn: "facts", columns: ["book_id", "chapter_index"])

            try db.execute(sql: """
                CREATE VIRTUAL TABLE facts_fts USING fts5(
                    text,
                    content='facts',
                    content_rowid='rowid',
                    tokenize='unicode61'
                );
            """)
            try db.execute(sql: """
                CREATE TRIGGER facts_ai AFTER INSERT ON facts BEGIN
                    INSERT INTO facts_fts(rowid, text) VALUES (new.rowid, new.text);
                END;
            """)
            try db.execute(sql: """
                CREATE TRIGGER facts_ad AFTER DELETE ON facts BEGIN
                    INSERT INTO facts_fts(facts_fts, rowid, text) VALUES ('delete', old.rowid, old.text);
                END;
            """)
            try db.execute(sql: """
                CREATE TRIGGER facts_au AFTER UPDATE ON facts BEGIN
                    INSERT INTO facts_fts(facts_fts, rowid, text) VALUES ('delete', old.rowid, old.text);
                    INSERT INTO facts_fts(rowid, text) VALUES (new.rowid, new.text);
                END;
            """)
        }

        try migrator.migrate(pool)
    }
}
