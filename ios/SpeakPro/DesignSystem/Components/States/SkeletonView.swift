import SwiftUI

/// Skeleton 加载态 —— 卡片占位 + 底部 Fraunces italic `patience` loader。
struct SkeletonView: View {
    var headerTitle: String = "LOADING · 加载中"
    var cardCount: Int = 3

    @State private var pulse = 0

    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // masthead
            HStack {
                Text(headerTitle)
                    .font(.spEyebrow)
                    .foregroundColor(.spMuted)
                Spacer()
            }
            .padding(.horizontal, 24)
            .padding(.top, 16)
            .padding(.bottom, 14)
            .overlay(alignment: .bottom) {
                Rectangle().fill(Color.spLine).frame(height: 1)
            }

            // hero placeholder
            VStack(alignment: .leading, spacing: 6) {
                RoundedRectangle(cornerRadius: 4).fill(Color.spLine)
                    .frame(width: 220, height: 30)
                RoundedRectangle(cornerRadius: 4).fill(Color.spLine)
                    .frame(width: 170, height: 30)
            }
            .padding(.horizontal, 24)
            .padding(.top, 16)
            .opacity(0.55)

            // cards
            VStack(spacing: 10) {
                ForEach(0..<cardCount, id: \.self) { i in
                    card
                        .opacity(1.0 - Double(i) * 0.15)
                }
            }
            .padding(.horizontal, 24)
            .padding(.top, 28)

            Spacer()

            // typographic loader
            VStack(spacing: 4) {
                Text("patience")
                    .font(.spSerif(32, italic: true))
                    .foregroundColor(.spMuted)
                Text("FETCHING · \(dots)")
                    .font(.system(size: 10, weight: .semibold))
                    .tracking(3)
                    .foregroundColor(.spMuted)
            }
            .frame(maxWidth: .infinity)
            .padding(.bottom, 36)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.spBackground)
        .onReceive(timer) { _ in pulse += 1 }
    }

    private var card: some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 10)
                .fill(Color.spLine)
                .frame(width: 44, height: 44)
            VStack(alignment: .leading, spacing: 6) {
                RoundedRectangle(cornerRadius: 3).fill(Color.spLine)
                    .frame(width: 80, height: 10)
                RoundedRectangle(cornerRadius: 3).fill(Color.spLine)
                    .frame(height: 14)
                RoundedRectangle(cornerRadius: 3).fill(Color.spLine)
                    .frame(width: 140, height: 8)
            }
            RoundedRectangle(cornerRadius: 3).fill(Color.spLine)
                .frame(width: 30, height: 20)
        }
        .padding(16)
        .background(Color.spIvory)
        .overlay(
            RoundedRectangle(cornerRadius: 10).stroke(Color.spLine, lineWidth: 1),
        )
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    private var dots: String {
        ["●○○", "○●○", "○○●"][pulse % 3]
    }
}

#Preview { SkeletonView() }
