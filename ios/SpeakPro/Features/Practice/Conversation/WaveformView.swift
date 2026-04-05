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
            let displayData = normalizedSamples(count: maxBars, height: geometry.size.height)

            HStack(alignment: .center, spacing: barSpacing) {
                ForEach(0..<displayData.count, id: \.self) { index in
                    RoundedRectangle(cornerRadius: barWidth / 2)
                        .fill(barColor)
                        .frame(
                            width: barWidth,
                            height: max(minBarHeight, displayData[index])
                        )
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
        }
    }

    /// 对波形数据归一化，确保最大值占满高度的 80%
    private func normalizedSamples(count: Int, height: CGFloat) -> [CGFloat] {
        guard !data.isEmpty else {
            // 空数据时返回低矮的静态条（虚线效果）
            return (0..<min(count, 40)).map { i in
                i % 2 == 0 ? minBarHeight : minBarHeight * 0.5
            }
        }

        let samples: [Float]
        if data.count <= count {
            samples = Array(data)
        } else {
            samples = Array(data.suffix(count))
        }

        // 找到最大值用于归一化
        let maxVal = samples.max() ?? 0.001
        let scale: CGFloat = maxVal > 0 ? (height * 0.8) / CGFloat(maxVal) : 1.0

        return samples.map { sample in
            max(minBarHeight, CGFloat(sample) * scale)
        }
    }
}

#Preview {
    VStack(spacing: 20) {
        // 空数据
        WaveformView(data: [])
            .frame(height: 40)
            .padding(.horizontal)

        // 模拟录音（小值）
        WaveformView(data: (0..<50).map { _ in Float.random(in: 0.001...0.05) })
            .frame(height: 40)
            .padding(.horizontal)

        // 正常录音
        WaveformView(data: (0..<50).map { _ in Float.random(in: 0.1...0.9) })
            .frame(height: 40)
            .padding(.horizontal)
    }
    .background(Color.spBackground)
}
