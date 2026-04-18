import SwiftUI

/// Splash — 启动过场。Editorial 墨底，"Speak" + italic accent "Pro."
/// 用法：首次 App 启动时短暂展示，完成后触发 `onFinished`。
struct SplashView: View {

    var onFinished: () -> Void

    @State private var progress: Double = 0.0

    var body: some View {
        ZStack {
            Color.spPrimary.ignoresSafeArea()

            VStack(spacing: 0) {
                // 顶部小字 editorial marks
                HStack {
                    Text("A DAILY EDITION")
                    Spacer()
                    Text("N° 001")
                }
                .font(.spEyebrow)
                .foregroundColor(Color.spIvory.opacity(0.35))
                .padding(.horizontal, 28)
                .padding(.top, 28)

                Spacer()

                // centerpiece
                VStack(spacing: 0) {
                    Text("EST. 2026 · SPEAKPRO")
                        .font(.spEyebrow)
                        .foregroundColor(Color.spIvory.opacity(0.55))
                        .padding(.bottom, 24)

                    Text("Speak")
                        .font(.spSerif(72))
                        .foregroundColor(.spIvory)

                    Text("Pro.")
                        .font(.spSerif(72, italic: true))
                        .foregroundColor(.spAccentWarm)
                        .padding(.top, -12)

                    Rectangle()
                        .fill(Color.spIvory.opacity(0.35))
                        .frame(width: 40, height: 1)
                        .padding(.top, 24)

                    Text("The language of speaking,\nrehearsed in good taste.")
                        .font(.spSerif(15, italic: true))
                        .foregroundColor(Color.spIvory.opacity(0.65))
                        .multilineTextAlignment(.center)
                        .lineSpacing(3)
                        .padding(.top, 20)
                }

                Spacer()

                // loading bar
                VStack(spacing: 14) {
                    ZStack(alignment: .leading) {
                        Rectangle()
                            .fill(Color.spIvory.opacity(0.2))
                            .frame(width: 120, height: 1)
                        Rectangle()
                            .fill(Color.spAccentWarm)
                            .frame(width: max(20, 120 * progress), height: 1)
                    }

                    Text("LOADING · \(Int(progress * 100))%")
                        .font(.spEyebrow)
                        .foregroundColor(Color.spIvory.opacity(0.4))
                }
                .padding(.bottom, 48)
            }
        }
        .task {
            // 1.2s 渐进到完成
            for i in 1...12 {
                try? await Task.sleep(nanoseconds: 100_000_000) // 0.1s
                progress = Double(i) / 12.0
            }
            onFinished()
        }
    }
}

#Preview {
    SplashView(onFinished: {})
}
