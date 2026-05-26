import SwiftUI
import UniformTypeIdentifiers

import SharedModels

struct LibraryView: View {
    @EnvironmentObject private var container: AppContainer
    @State private var books: [Book] = []
    @State private var showingImporter = false
    @State private var importError: String?
    @State private var ingesting: Set<BookID> = []

    var body: some View {
        NavigationStack {
            Group {
                if books.isEmpty {
                    emptyState
                } else {
                    bookList
                }
            }
            .navigationTitle("Library")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        showingImporter = true
                    } label: {
                        Label("Add", systemImage: "plus")
                    }
                }
                ToolbarItem(placement: .topBarLeading) {
                    NavigationLink(destination: SettingsView()) {
                        Image(systemName: "gearshape")
                    }
                }
            }
            .fileImporter(
                isPresented: $showingImporter,
                allowedContentTypes: [.epub, .plainText],
                allowsMultipleSelection: false
            ) { result in
                Task { await handleImport(result) }
            }
            .task { await reload() }
            .alert("Import error", isPresented: .constant(importError != nil)) {
                Button("OK") { importError = nil }
            } message: {
                Text(importError ?? "")
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "books.vertical")
                .resizable().scaledToFit()
                .frame(width: 80, height: 80)
                .foregroundStyle(.tertiary)
            Text("Your library is empty")
                .font(.headline)
            Text("Add an EPUB or TXT file to start reading.")
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button("Add book") { showingImporter = true }
                .buttonStyle(.borderedProminent)
        }
        .padding()
    }

    private var bookList: some View {
        List {
            ForEach(books) { book in
                NavigationLink(destination: ReaderView(book: book)) {
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Text(book.title).font(.headline)
                            if ingesting.contains(book.id) {
                                ProgressView().controlSize(.mini)
                            }
                        }
                        if let author = book.author, !author.isEmpty {
                            Text(author).font(.subheadline).foregroundStyle(.secondary)
                        }
                        Text(book.format.rawValue.uppercased())
                            .font(.caption2)
                            .padding(.horizontal, 6).padding(.vertical, 2)
                            .background(.quaternary, in: Capsule())
                    }
                }
            }
        }
    }

    private func reload() async {
        do {
            books = try await container.bookStore.listBooks()
        } catch {
            importError = "Could not load library: \(error.localizedDescription)"
        }
    }

    private func handleImport(_ result: Result<[URL], Error>) async {
        switch result {
        case .success(let urls):
            guard let url = urls.first else { return }
            let secured = url.startAccessingSecurityScopedResource()
            defer { if secured { url.stopAccessingSecurityScopedResource() } }
            do {
                let book = try await container.bookImport.importBook(at: url)
                await reload()
                ingesting.insert(book.id)
                Task.detached {
                    do {
                        let chapters = try await container.bookStore.chapters(for: book.id)
                        try await container.ingestion.ingest(book: book, chapters: chapters)
                    } catch {
                        print("Ingestion failed: \(error)")
                    }
                    await MainActor.run { ingesting.remove(book.id) }
                }
            } catch {
                importError = error.localizedDescription
            }
        case .failure(let error):
            importError = error.localizedDescription
        }
    }
}

extension UTType {
    static let epub: UTType = UTType(filenameExtension: "epub", conformingTo: .data) ?? .data
}
