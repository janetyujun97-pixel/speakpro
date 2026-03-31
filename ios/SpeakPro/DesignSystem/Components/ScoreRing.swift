import SwiftUI

/// 环形分数展示组件
/// 用于显示 0-100 分的评分，中心显示数字，外圈为动画进度环
struct ScoreRing: View {

    let score: Double      // 0 ~ 100
    var color: Color = .spAccent
    var lineWidth: CGFloat = 10
    var size: CGFloat = 100

    @State private var animatedProgress: Double = 0

    var body: some View {
        ZStack {
            // 背景环
            Circle()
                .stroke(
                    color.opacity(0.15),
                    lineWidth: lineWidth
                )

            // 进度环
            Circle()
                .trim(from: 0, to: animatedProgress)
                .stroke(
                    color,
                    style: StrokeStyle(lineWidth: lineWidth, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))

            // 分数文本
            VStack(spacing: 2) {
                Text("\(Int(score))")
                    .font(.spTitleMedium)
                    .fontWeight(.bold)
                    .foregroundColor(.spTextPrimary)

                Text("分")
                    .font(.spCaption)
                    .foregroundColor(.spTextSecondary)
            }
        }
        .frame(width: size, height: size)
        .onAppear {
            withAnimation(.easeOut(duration: 1.0)) {
                animatedProgress = score / 100.0
            }
        }
        .onChange(of: score) { _, newValue in
            withAnimation(.easeOut(duration: 0.5)) {
                animatedProgress = newValue / 100.0
            }
        }
    }
}

#Preview {
    HStack(spacing: 24) {
        ScoreRing(score: 85, color: .spSuccess)
        ScoreRing(score: 62, color: .spWarning, lineWidth: 8, size: 80)
        ScoreRing(score: 30, color: .spError, lineWidth: 6, size: 60)
    }
    .padding()
}
