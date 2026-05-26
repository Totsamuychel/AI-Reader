import Foundation
import SharedModels

@MainActor
public final class ReaderSessionController: ReaderSessionControlling, ObservableObject {
    @Published public private(set) var currentBook: Book?
    @Published public private(set) var currentChapter: Chapter?
    @Published public private(set) var chapters: [Chapter] = []
    @Published public private(set) var position: ReadingPosition?

    private let bookStore: BookStoring
    private let progressStore: ReadingProgressStoring
    private let parserFactory: (BookFormat) -> BookParsing

    public init(
        bookStore: BookStoring,
        progressStore: ReadingProgressStoring,
        parserFactory: @escaping (BookFormat) -> BookParsing
    ) {
        self.bookStore = bookStore
        self.progressStore = progressStore
        self.parserFactory = parserFactory
    }

    public func open(book: Book) async throws {
        self.currentBook = book
        var loaded = try await bookStore.chapters(for: book.id)
        if loaded.isEmpty {
            loaded = try await parserFactory(book.format).parseChapters(for: book)
            try await bookStore.saveChapters(loaded, for: book.id)
        }
        self.chapters = loaded.sorted { $0.index < $1.index }

        if let saved = try await progressStore.loadPosition(for: book.id) {
            self.position = saved
            self.currentChapter = chapters.first { $0.id == saved.chapterID } ?? chapters.first
        } else {
            self.currentChapter = chapters.first
            self.position = ReadingPosition(
                bookID: book.id,
                chapterID: currentChapter?.id,
                progressFraction: 0
            )
        }
    }

    public func goToNextChapter() async {
        guard let current = currentChapter,
              let idx = chapters.firstIndex(where: { $0.id == current.id }),
              idx + 1 < chapters.count
        else { return }
        await setChapter(chapters[idx + 1])
    }

    public func goToPreviousChapter() async {
        guard let current = currentChapter,
              let idx = chapters.firstIndex(where: { $0.id == current.id }),
              idx > 0
        else { return }
        await setChapter(chapters[idx - 1])
    }

    public func setChapter(_ chapter: Chapter) async {
        currentChapter = chapter
        guard let bookID = currentBook?.id else { return }
        let pos = ReadingPosition(
            bookID: bookID,
            chapterID: chapter.id,
            progressFraction: progressFraction(for: chapter)
        )
        await updateProgress(pos)
    }

    public func updateProgress(_ position: ReadingPosition) async {
        self.position = position
        try? await progressStore.savePosition(position)
    }

    public func currentChapterIndex() -> Int {
        currentChapter?.index ?? 0
    }

    private func progressFraction(for chapter: Chapter) -> Double {
        guard !chapters.isEmpty else { return 0 }
        return Double(chapter.index) / Double(max(chapters.count - 1, 1))
    }
}
