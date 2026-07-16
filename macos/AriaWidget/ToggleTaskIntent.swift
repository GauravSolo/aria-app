import AppIntents
import WidgetKit

/// Tapping a task checkbox in the widget: optimistically remove it from the widget
/// snapshot and queue the completion for the app to sync to Supabase on next launch.
struct ToggleTaskIntent: AppIntent {
    static var title: LocalizedStringResource = "Complete task"

    @Parameter(title: "Task ID") var taskId: String
    @Parameter(title: "Recurrence") var recurrence: String

    init() {}
    init(taskId: String, recurrence: String) {
        self.taskId = taskId
        self.recurrence = recurrence
    }

    func perform() async throws -> some IntentResult {
        var snap = WidgetBridge.read()
        snap.tasks.removeAll { $0.id == taskId }
        snap.pendingTasks = max(0, snap.pendingTasks - 1)
        snap.nextTaskTitle = snap.tasks.first?.title
        WidgetBridge.writeOptimistic(snap)

        // Instant sync: write straight to Supabase. If offline / no session, fall back
        // to the pending queue so the app applies it on next launch.
        let uid = WidgetBridge.readAuth()?.userId ?? ""
        let ok = await SupaREST.completeTask(id: taskId, recurrence: recurrence, userId: uid)
        if !ok { WidgetBridge.enqueuePending(PendingToggle(id: taskId, recurrence: recurrence)) }
        return .result()
    }
}
