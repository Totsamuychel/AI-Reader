import SwiftUI
import LLM
import SharedModels

struct AssistantView: View {
    let book: Book
    var prefilledQuestion: String?

    @EnvironmentObject private var container: AppContainer

    var body: some View {
        AssistantViewContent(
            book: book,
            prefilledQuestion: prefilledQuestion,
            answerProvider: container.answer,
            currentChapterIndex: { container.session.currentChapter?.index ?? 0 }
        )
    }
}

private struct AssistantViewContent: View {
    let book: Book
    let prefilledQuestion: String?
    @StateObject private var vm: AssistantViewModel
    @Environment(\.dismiss) private var dismiss
    @FocusState private var inputFocused: Bool

    init(
        book: Book,
        prefilledQuestion: String?,
        answerProvider: AnswerProviding,
        currentChapterIndex: @escaping () -> Int
    ) {
        self.book = book
        self.prefilledQuestion = prefilledQuestion
        _vm = StateObject(wrappedValue: AssistantViewModel(
            answer: answerProvider,
            book: book,
            currentChapterIndex: currentChapterIndex
        ))
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                presets
                Divider()
                conversation
                Divider()
                inputBar
            }
            .navigationTitle("Assistant")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button { vm.clear() } label: { Image(systemName: "trash") }
                        .disabled(vm.items.isEmpty)
                }
            }
            .task {
                if let q = prefilledQuestion {
                    vm.draft = q
                }
                inputFocused = true
            }
        }
    }

    private var presets: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                PresetChip(label: "Что было в прошлой главе?") {
                    Task { await vm.ask(question: "Что было в прошлой главе?") }
                }
                PresetChip(label: "Кто это?") {
                    vm.draft = "Кто такой "
                    inputFocused = true
                }
                PresetChip(label: "Кратко напомни сюжет") {
                    Task { await vm.ask(question: "Кратко напомни сюжет до текущей главы.") }
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 8)
        }
    }

    private var conversation: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 12) {
                ForEach(vm.items) { item in
                    QABubble(item: item)
                }
            }
            .padding()
        }
    }

    private var inputBar: some View {
        HStack(spacing: 8) {
            TextField("Ask about the book…", text: $vm.draft, axis: .vertical)
                .lineLimit(1...4)
                .textFieldStyle(.roundedBorder)
                .focused($inputFocused)
            Button {
                Task { await vm.ask() }
            } label: {
                Image(systemName: vm.isBusy ? "hourglass" : "arrow.up.circle.fill")
                    .font(.title2)
            }
            .disabled(vm.draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || vm.isBusy)
        }
        .padding()
    }
}

private struct QABubble: View {
    let item: AssistantViewModel.QAItem
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(item.question).font(.subheadline.weight(.semibold))
            if item.isLoading {
                HStack(spacing: 8) {
                    ProgressView().controlSize(.small)
                    Text("Thinking…").foregroundStyle(.secondary)
                }
            } else {
                Text(item.answer).textSelection(.enabled)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(.quaternary.opacity(0.4), in: RoundedRectangle(cornerRadius: 12))
    }
}

private struct PresetChip: View {
    let label: String
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(label)
                .padding(.horizontal, 12).padding(.vertical, 6)
                .background(.tint.opacity(0.15), in: Capsule())
                .foregroundStyle(.tint)
                .lineLimit(1)
        }
        .buttonStyle(.plain)
    }
}
