import Foundation
import LLM
import SharedModels

@MainActor
final class AssistantViewModel: ObservableObject {
    struct QAItem: Identifiable, Equatable {
        let id = UUID()
        let question: String
        var answer: String
        var isLoading: Bool
    }

    @Published var items: [QAItem] = []
    @Published var draft: String = ""
    @Published var isBusy: Bool = false
    @Published var lastError: String?

    private let answer: AnswerProviding
    private let book: Book
    private let currentChapterIndex: () -> Int

    init(
        answer: AnswerProviding,
        book: Book,
        currentChapterIndex: @escaping () -> Int
    ) {
        self.answer = answer
        self.book = book
        self.currentChapterIndex = currentChapterIndex
    }

    func ask() async {
        let q = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else { return }
        draft = ""
        await ask(question: q)
    }

    func ask(question: String) async {
        var placeholder = QAItem(question: question, answer: "", isLoading: true)
        items.append(placeholder)
        isBusy = true
        defer { isBusy = false }

        do {
            let result = try await answer.answer(
                question: question,
                bookID: book.id,
                currentChapterIndex: currentChapterIndex()
            )
            if let idx = items.firstIndex(where: { $0.id == placeholder.id }) {
                items[idx].answer = result
                items[idx].isLoading = false
            }
        } catch {
            lastError = error.localizedDescription
            if let idx = items.firstIndex(where: { $0.id == placeholder.id }) {
                items[idx].answer = "Error: \(error.localizedDescription)"
                items[idx].isLoading = false
            }
        }
    }

    func clear() { items.removeAll() }
}
