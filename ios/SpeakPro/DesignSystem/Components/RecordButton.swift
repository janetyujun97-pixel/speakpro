import SwiftUI

/// 录音按钮组件 — 带脉冲动画
struct RecordButton: View {

    @Binding var isRecording: Bool

    var size: CGFloat = 72
    var onTap: (() -> Void)?

    @State private var pulseScale: CGFloat = 1.0

    private var buttonColor: Color {
        isRecording ? .spError : .spAccent
    }

    var body: some View {
        ZStack {
            // 脉冲光圈（录音时显示）
            if isRecording {
                Circle()
                    .fill(buttonColor.opacity(0.2))
                    .frame(width: size * 1.5, height: size * 1.5)
                    .scaleEffect(pulseScale)
                    .onAppear {
                        withAnimation(
                            .easeInOut(duration: 1.0)
                            .repeatForever(autoreverses: true)
                        ) {
                            pulseScale = 1.3
                        }
                    }
                    .onDisappear {
                        pulseScale = 1.0
                    }
            }

            // 主按钮
            Circle()
                .fill(buttonColor)
                .frame(width: size, height: size)
                .shadow(color: buttonColor.opacity(0.4), radius: 8, y: 4)

            // 图标
            Image(systemName: isRecording ? "stop.fill" : "mic.fill")
                .font(.system(size: size * 0.35))
                .foregroundColor(.white)
        }
        .onTapGesture {
            onTap?()
        }
        .animation(.spring(response: 0.3), value: isRecording)
    }
}

#Preview {
    VStack(spacing: 40) {
        RecordButton(isRecording: .constant(false))
        RecordButton(isRecording: .constant(true))
    }
}
