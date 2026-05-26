import SwiftUI
import Safety
import SharedModels

struct SettingsView: View {
    @EnvironmentObject private var container: AppContainer

    var body: some View {
        Form {
            Section("Reader") {
                Picker("Theme", selection: $container.readerSettings.theme) {
                    ForEach(ReaderTheme.allCases, id: \.self) { Text($0.rawValue.capitalized).tag($0) }
                }
                Slider(
                    value: $container.readerSettings.fontSize,
                    in: 12...28,
                    step: 1
                ) {
                    Text("Font size")
                } minimumValueLabel: { Text("A").font(.caption) }
                  maximumValueLabel: { Text("A").font(.title2) }
                Slider(
                    value: $container.readerSettings.lineSpacing,
                    in: 1.0...2.2,
                    step: 0.1
                ) {
                    Text("Line spacing")
                }
            }
            Section("Assistant") {
                Picker("Spoiler mode", selection: $container.answerMode) {
                    Text("Safe — no spoilers").tag(AnswerMode.safe)
                    Text("Hint — soft hints").tag(AnswerMode.hint)
                    Text("Full — spoilers OK").tag(AnswerMode.full)
                }
                .onChange(of: container.answerMode) { _, newValue in
                    AnswerModeHolder.shared.set(newValue)
                }
            }
            Section("About") {
                LabeledContent("On-device LLM", value: "Gemma 4 E2B")
                LabeledContent("Memory store", value: "SQLite + FTS5")
            }
        }
        .navigationTitle("Settings")
        .navigationBarTitleDisplayMode(.inline)
    }
}
