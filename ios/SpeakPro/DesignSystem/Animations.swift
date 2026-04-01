import SwiftUI

// MARK: - 通用动画常量

extension Animation {
    /// 标准淡入（0.3s）
    static let spFadeIn = Animation.easeOut(duration: 0.3)

    /// 标准滑入（带弹性）
    static let spSlideIn = Animation.spring(response: 0.4, dampingFraction: 0.8)

    /// 弹入效果
    static let spBounceIn = Animation.spring(response: 0.5, dampingFraction: 0.6)

    /// 列表 stagger 动画（根据 index 延迟）
    static func spStagger(index: Int, baseDelay: Double = 0.05) -> Animation {
        .easeOut(duration: 0.3).delay(Double(index) * baseDelay)
    }
}

// MARK: - View Modifiers

/// 淡入 + 上滑动画
struct SlideUpModifier: ViewModifier {
    @State private var isVisible = false
    let delay: Double

    func body(content: Content) -> some View {
        content
            .opacity(isVisible ? 1 : 0)
            .offset(y: isVisible ? 0 : 12)
            .onAppear {
                withAnimation(.spSlideIn.delay(delay)) {
                    isVisible = true
                }
            }
    }
}

/// 简单淡入
struct FadeInModifier: ViewModifier {
    @State private var isVisible = false
    let delay: Double

    func body(content: Content) -> some View {
        content
            .opacity(isVisible ? 1 : 0)
            .onAppear {
                withAnimation(.spFadeIn.delay(delay)) {
                    isVisible = true
                }
            }
    }
}

extension View {
    /// 添加淡入+上滑动画
    func slideUpAnimation(delay: Double = 0) -> some View {
        modifier(SlideUpModifier(delay: delay))
    }

    /// 添加淡入动画
    func fadeInAnimation(delay: Double = 0) -> some View {
        modifier(FadeInModifier(delay: delay))
    }
}
