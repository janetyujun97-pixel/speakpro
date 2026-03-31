import SwiftUI

/// 音频波形可视化组件
struct WaveformView: View {

    let data: [Float]

    var barColor: Color = .spAccent
    var barWidth: CGFloat = 3
    var barSpacing: CGFloat = 2
    var minBarHeight: CGFloat = 2

    var body: some View {
        GeometryReader { geometry in
            let availableWidth = geometry.size.width
            let totalBarWidth = barWidth + barSpacing
            let maxBars = Int(availableWidth / totalBarWidth)
            let displayData = recentSamples(count: maxBars)

            HStack(alignment: .center, spacing: barSpacing) {
                ForEach(0..<displayData.count, id: \.self) { index in
                    RoundedRectangle(cornerRadius: barWidth / 2)
                        .fill(barColor)
                        .frame(
                            width: barWidth,
                            height: max(
                                minBarHeight,
                                CGFloat(displayData[index]) * geometry.size.height
                            )
                        )
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
        }
    }

    /// 截取最近 N 个样本用于显示
    private func recentSamples(count: Int) -> [Float] {
        guard !data.isEmpty else {
            // 空数据时返回低矮的静态条
            return Array(repeating: Float(0.05), count: min(count, 30))
        }
        if data.count <= count {
            return Array(data)
        }
        return Array(data.suffix(count))
    }
}

#Preview {
    VStack(spacing: 20) {
        // 静态（无录音）
        WaveformView(data: [])
            .frame(height: 40)
            .padding(.horizontal)

        // 模拟录音数据
        WaveformView(data: (0..<50).map { _ in Float.random(in: 0.1...0.9) })
            .frame(height: 40)
            .padding(.horizontal)
    }
    .background(Color.spBackground)
}
