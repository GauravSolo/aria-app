import SwiftUI
import WidgetKit

@main
struct AriaWidgetBundle: WidgetBundle {
    var body: some Widget {
        AriaWidget()
        HabitCalWidget()
        TaskCalWidget()
    }
}
