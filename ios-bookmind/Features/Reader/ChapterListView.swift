import SwiftUI
import SharedModels

struct ChapterListView: View {
    let book: Book
    @EnvironmentObject private var container: AppContainer
    @Environment(\.dismiss) private var dismiss
    @State private var chapters: [Chapter] = []

    var body: some View {
        NavigationStack {
            List(chapters) { chapter in
                Button {
                    Task {
                        await container.session.setChapter(chapter)
                        dismiss()
                    }
                } label: {
                    HStack {
                        Text("\(chapter.index + 1).")
                            .foregroundStyle(.secondary)
                            .frame(width: 32, alignment: .leading)
                        Text(chapter.title ?? "Chapter \(chapter.index + 1)")
                        Spacer()
                        if chapter.id == container.session.currentChapter?.id {
                            Image(systemName: "checkmark")
                                .foregroundStyle(.tint)
                        }
                    }
                }
                .buttonStyle(.plain)
            }
            .navigationTitle("Chapters")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
            .task {
                chapters = (try? await container.bookStore.chapters(for: book.id)) ?? []
            }
        }
    }
}
