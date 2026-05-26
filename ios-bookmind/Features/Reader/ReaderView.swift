import SwiftUI
import WebKit

import ReaderCore
import SharedModels

struct ReaderView: View {
    let book: Book
    @EnvironmentObject private var container: AppContainer
    @State private var rawText: String = ""
    @State private var showAssistant = false
    @State private var showChapterList = false
    @State private var preselectedQuestion: String?

    var body: some View {
        ZStack {
            background
            content
        }
        .navigationTitle(book.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar { toolbarContent }
        .task { await load() }
        .sheet(isPresented: $showAssistant) {
            AssistantView(book: book, prefilledQuestion: preselectedQuestion)
                .presentationDetents([.medium, .large])
        }
        .sheet(isPresented: $showChapterList) {
            ChapterListView(book: book)
                .presentationDetents([.medium, .large])
        }
    }

    private var background: some View {
        Group {
            switch container.readerSettings.theme {
            case .light: Color(.systemBackground)
            case .dark:  Color.black
            case .sepia: Color(red: 0.96, green: 0.91, blue: 0.79)
            }
        }
        .ignoresSafeArea()
    }

    @ViewBuilder
    private var content: some View {
        if book.format == .epub, let url = container.session.currentChapter?.contentRef {
            EPUBChapterWebView(url: url, theme: container.readerSettings.theme)
        } else {
            ScrollView {
                Text(rawText.isEmpty ? "Loading…" : rawText)
                    .font(.system(size: container.readerSettings.fontSize))
                    .lineSpacing(container.readerSettings.lineSpacing * 4)
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .textSelection(.enabled)
        }
    }

    @ToolbarContentBuilder
    private var toolbarContent: some ToolbarContent {
        ToolbarItemGroup(placement: .bottomBar) {
            Button {
                Task {
                    await container.session.goToPreviousChapter()
                    await load()
                }
            } label: {
                Image(systemName: "chevron.left")
            }
            Spacer()
            Button { showChapterList = true } label: {
                Image(systemName: "list.bullet")
            }
            Spacer()
            Button {
                preselectedQuestion = nil
                showAssistant = true
            } label: {
                Image(systemName: "sparkles")
            }
            Spacer()
            Button {
                Task {
                    await container.session.goToNextChapter()
                    await load()
                }
            } label: {
                Image(systemName: "chevron.right")
            }
        }
    }

    private func load() async {
        do {
            try await container.session.open(book: book)
            guard let chapter = container.session.currentChapter else { return }
            let parser: BookParsing = {
                switch book.format {
                case .epub: return EPUBParser()
                case .txt:  return TXTParser()
                }
            }()
            rawText = (try? await parser.rawText(for: chapter, in: book)) ?? ""
        } catch {
            rawText = "Failed to load chapter: \(error.localizedDescription)"
        }
    }
}

// MARK: - WebView for EPUB

struct EPUBChapterWebView: UIViewRepresentable {
    let url: URL
    let theme: ReaderTheme

    func makeUIView(context: Context) -> WKWebView {
        let cfg = WKWebViewConfiguration()
        let view = WKWebView(frame: .zero, configuration: cfg)
        view.isOpaque = false
        return view
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {
        uiView.loadFileURL(url, allowingReadAccessTo: url.deletingLastPathComponent())
        let css: String = {
            switch theme {
            case .light: return "body{background:white;color:black;}"
            case .dark:  return "body{background:black;color:#ddd;}"
            case .sepia: return "body{background:#F5E9CA;color:#3A2F1D;}"
            }
        }()
        let script = """
        (function(){
            var s = document.createElement('style');
            s.innerHTML = `\(css) body{font-family:-apple-system;}`;
            document.head.appendChild(s);
        })();
        """
        uiView.evaluateJavaScript(script)
    }
}
