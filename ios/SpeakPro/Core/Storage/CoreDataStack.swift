import Foundation
import CoreData

/// CoreData 离线存储管理
final class CoreDataStack {

    static let shared = CoreDataStack()

    // MARK: - Persistent Container

    lazy var persistentContainer: NSPersistentContainer = {
        let container = NSPersistentContainer(name: "SpeakPro")
        container.loadPersistentStores { _, error in
            if let error = error as NSError? {
                // TODO: 生产环境应使用日志上报而非 fatalError
                fatalError("[CoreData] 加载持久化存储失败: \(error), \(error.userInfo)")
            }
        }
        container.viewContext.automaticallyMergesChangesFromParent = true
        return container
    }()

    var viewContext: NSManagedObjectContext {
        persistentContainer.viewContext
    }

    private init() {}

    // MARK: - Save

    func save() {
        let context = viewContext
        guard context.hasChanges else { return }
        do {
            try context.save()
        } catch {
            print("[CoreData] 保存失败: \(error.localizedDescription)")
        }
    }

    // MARK: - Fetch Helper

    func fetch<T: NSManagedObject>(_ request: NSFetchRequest<T>) -> [T] {
        do {
            return try viewContext.fetch(request)
        } catch {
            print("[CoreData] 查询失败: \(error.localizedDescription)")
            return []
        }
    }

    // MARK: - Background Context

    func performBackgroundTask(_ block: @escaping (NSManagedObjectContext) -> Void) {
        persistentContainer.performBackgroundTask(block)
    }
}
