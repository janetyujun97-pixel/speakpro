import Foundation

extension Date {

    /// 相对时间描述，例如 "2小时前"、"刚刚"
    var relativeTimeString: String {
        let now = Date()
        let interval = now.timeIntervalSince(self)

        if interval < 60 {
            return "刚刚"
        } else if interval < 3600 {
            let minutes = Int(interval / 60)
            return "\(minutes)分钟前"
        } else if interval < 86400 {
            let hours = Int(interval / 3600)
            return "\(hours)小时前"
        } else if interval < 604800 {
            let days = Int(interval / 86400)
            return "\(days)天前"
        } else {
            return formattedDate
        }
    }

    /// 格式化日期，例如 "2026-04-05"
    var formattedDate: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "zh_CN")
        return formatter.string(from: self)
    }

    /// 格式化日期时间，例如 "2026-04-05 14:30"
    var formattedDateTime: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm"
        formatter.locale = Locale(identifier: "zh_CN")
        return formatter.string(from: self)
    }

    /// 友好的截止日期描述，例如 "还剩2天"
    var deadlineString: String {
        let now = Date()
        let interval = self.timeIntervalSince(now)

        if interval < 0 {
            return "已截止"
        } else if interval < 3600 {
            let minutes = Int(interval / 60)
            return "还剩\(minutes)分钟"
        } else if interval < 86400 {
            let hours = Int(interval / 3600)
            return "还剩\(hours)小时"
        } else {
            let days = Int(interval / 86400)
            return "还剩\(days)天"
        }
    }
}
