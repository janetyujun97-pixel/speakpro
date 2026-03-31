import SwiftUI

/// 雷达图（蛛网图）组件
/// 用于展示多维度评分，例如发音、语法、流利度等
struct RadarChart: View {

    /// 各维度的分数 (0.0 ~ 1.0)
    let scores: [String: Double]

    var fillColor: Color = .spAccent.opacity(0.2)
    var strokeColor: Color = .spAccent
    var gridColor: Color = .gray.opacity(0.3)
    var size: CGFloat = 200

    private var sortedKeys: [String] {
        Array(scores.keys).sorted()
    }

    private var values: [Double] {
        sortedKeys.map { scores[$0] ?? 0 }
    }

    var body: some View {
        ZStack {
            // 网格背景
            radarGrid

            // 数据多边形
            radarPolygon

            // 维度标签
            radarLabels
        }
        .frame(width: size + 80, height: size + 80)
    }

    // MARK: - Grid

    private var radarGrid: some View {
        Canvas { context, canvasSize in
            let center = CGPoint(x: canvasSize.width / 2, y: canvasSize.height / 2)
            let radius = size / 2
            let count = sortedKeys.count
            guard count >= 3 else { return }

            // 绘制同心多边形
            for level in 1...4 {
                let r = radius * CGFloat(level) / 4.0
                var path = Path()
                for i in 0..<count {
                    let angle = angleForIndex(i, total: count)
                    let point = pointOnCircle(center: center, radius: r, angle: angle)
                    if i == 0 { path.move(to: point) }
                    else { path.addLine(to: point) }
                }
                path.closeSubpath()
                context.stroke(path, with: .color(gridColor), lineWidth: 0.5)
            }

            // 绘制从中心到顶点的连线
            for i in 0..<count {
                let angle = angleForIndex(i, total: count)
                let point = pointOnCircle(center: center, radius: radius, angle: angle)
                var line = Path()
                line.move(to: center)
                line.addLine(to: point)
                context.stroke(line, with: .color(gridColor), lineWidth: 0.5)
            }
        }
    }

    // MARK: - Data Polygon

    private var radarPolygon: some View {
        Canvas { context, canvasSize in
            let center = CGPoint(x: canvasSize.width / 2, y: canvasSize.height / 2)
            let radius = size / 2
            let count = sortedKeys.count
            guard count >= 3 else { return }

            var path = Path()
            for i in 0..<count {
                let angle = angleForIndex(i, total: count)
                let value = CGFloat(values[i])
                let point = pointOnCircle(center: center, radius: radius * value, angle: angle)
                if i == 0 { path.move(to: point) }
                else { path.addLine(to: point) }
            }
            path.closeSubpath()

            context.fill(path, with: .color(fillColor))
            context.stroke(path, with: .color(strokeColor), lineWidth: 2)
        }
    }

    // MARK: - Labels

    private var radarLabels: some View {
        GeometryReader { geometry in
            let center = CGPoint(x: geometry.size.width / 2, y: geometry.size.height / 2)
            let radius = (size / 2) + 30
            let count = sortedKeys.count

            ForEach(0..<count, id: \.self) { i in
                let angle = angleForIndex(i, total: count)
                let point = pointOnCircle(center: center, radius: radius, angle: angle)

                Text(sortedKeys[i])
                    .font(.spCaption)
                    .foregroundColor(.spTextSecondary)
                    .position(point)
            }
        }
    }

    // MARK: - Helpers

    private func angleForIndex(_ index: Int, total: Int) -> CGFloat {
        let base = -CGFloat.pi / 2  // 从顶部开始
        return base + (2 * .pi / CGFloat(total)) * CGFloat(index)
    }

    private func pointOnCircle(center: CGPoint, radius: CGFloat, angle: CGFloat) -> CGPoint {
        CGPoint(
            x: center.x + radius * cos(angle),
            y: center.y + radius * sin(angle)
        )
    }
}

#Preview {
    RadarChart(scores: [
        "发音": 0.8,
        "语法": 0.6,
        "流利度": 0.75,
        "词汇": 0.5,
        "连贯性": 0.7
    ])
}
