import SwiftUI

struct NotificationsView: View {

    @StateObject private var vm = NotificationsViewModel()
    @State private var showingPrefs = false

    var body: some View {
        ZStack {
            Color.spBackground.ignoresSafeArea()
            Group {
                if vm.isLoading && vm.items.isEmpty {
                    SwiftUI.ProgressView()
                } else if vm.items.isEmpty {
                    emptyState
                } else {
                    list
                }
            }
        }
        .navigationTitle("通知")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button("全部已读") {
                        Task { await vm.markAllRead() }
                    }
                    Button("免打扰设置") {
                        showingPrefs = true
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .foregroundColor(.spPrimary)
                }
            }
        }
        .task { await vm.load() }
        .refreshable { await vm.load() }
        .sheet(isPresented: $showingPrefs) {
            NavigationStack { NotificationPrefsView() }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: "bell.slash")
                .font(.system(size: 36))
                .foregroundColor(.spMuted)
            Text("暂无通知")
                .font(.spBodyMedium)
                .foregroundColor(.spMuted)
        }
    }

    private var list: some View {
        List {
            ForEach(vm.items) { n in
                NotificationRow(item: n)
                    .listRowBackground(Color.spBackground)
                    .listRowSeparator(.hidden)
                    .onTapGesture {
                        Task { await vm.markRead(id: n.id) }
                    }
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
    }
}

private struct NotificationRow: View {
    let item: NotificationItem

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            ZStack {
                Circle()
                    .fill(kindColor.opacity(0.15))
                    .frame(width: 36, height: 36)
                Image(systemName: kindIcon)
                    .foregroundColor(kindColor)
            }
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(item.title)
                        .font(.spBodyMedium)
                        .foregroundColor(.spPrimary)
                        .lineLimit(2)
                    Spacer()
                    if !item.isRead {
                        Circle().fill(Color.spAccent).frame(width: 6, height: 6)
                    }
                }
                Text(item.body)
                    .font(.spCaption)
                    .foregroundColor(.spMuted)
                    .lineLimit(3)
                Text(timeLabel)
                    .font(.spCaption)
                    .foregroundColor(.spMuted)
            }
        }
        .padding(.vertical, 8)
    }

    private var kindColor: Color {
        switch item.kind {
        case .homework: return .spAccent
        case .feedback: return .spMoss
        case .streak:   return .spAccentWarm
        case .reminder: return .spMuted
        case .system, .unknown: return .spPrimary
        }
    }

    private var kindIcon: String {
        switch item.kind {
        case .homework: return "doc.text"
        case .feedback: return "checkmark.bubble"
        case .streak:   return "flame"
        case .reminder: return "clock"
        case .system, .unknown: return "bell"
        }
    }

    private var timeLabel: String {
        let f = RelativeDateTimeFormatter()
        f.locale = Locale(identifier: "zh_CN")
        return f.localizedString(for: item.createdAt, relativeTo: Date())
    }
}

// MARK: - Prefs sheet

struct NotificationPrefsView: View {

    @StateObject private var vm = NotificationPrefsViewModel()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        Form {
            Section("免打扰时段") {
                HStack {
                    Text("开始")
                    Spacer()
                    TimePickerField(text: $vm.quietStart)
                }
                HStack {
                    Text("结束")
                    Spacer()
                    TimePickerField(text: $vm.quietEnd)
                }
            }
            Section("推送通道") {
                Toggle("接收推送通知", isOn: $vm.pushEnabled)
                Text("v1 仅在 App 内显示通知列表；凭证齐备后将接入系统推送。")
                    .font(.spCaption)
                    .foregroundColor(.spMuted)
            }
            if let err = vm.errorMessage {
                Section { Text(err).foregroundColor(.spError) }
            }
        }
        .scrollContentBackground(.hidden)
        .background(Color.spBackground)
        .navigationTitle("免打扰设置")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("关闭") { dismiss() }
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("保存") {
                    Task {
                        await vm.save()
                        if vm.errorMessage == nil { dismiss() }
                    }
                }
                .disabled(vm.isSaving)
            }
        }
        .task { await vm.load() }
    }
}

private struct TimePickerField: View {
    @Binding var text: String

    @State private var date = Date()

    var body: some View {
        DatePicker(
            "",
            selection: $date,
            displayedComponents: .hourAndMinute,
        )
        .labelsHidden()
        .onAppear { date = Self.parse(text) }
        .onChange(of: date) { _, newValue in
            let f = DateFormatter()
            f.dateFormat = "HH:mm:ss"
            text = f.string(from: newValue)
        }
    }

    private static func parse(_ s: String) -> Date {
        let f = DateFormatter()
        f.dateFormat = "HH:mm:ss"
        return f.date(from: s) ?? Date()
    }
}
