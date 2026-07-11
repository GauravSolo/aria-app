import { registerWidgetTaskHandler, requestWidgetUpdate } from 'react-native-android-widget';

import { AriaWidget } from './AriaWidget';
import { loadWidgetSnapshot } from './snapshot';
import { widgetTaskHandler } from './widgetTaskHandler';

// Register the headless render handler at module load (also runs in the widget's
// background JS context, which loads the same entry).
registerWidgetTaskHandler(widgetTaskHandler);

export function registerAriaWidgetTask(): void {
  // Registration already happened at import time; kept for a stable call site.
}

/** Push fresh data into any placed Aria widgets. Safe no-op if none exist. */
export async function updateAriaWidget(): Promise<void> {
  try {
    const snapshot = await loadWidgetSnapshot();
    await requestWidgetUpdate({
      widgetName: 'Aria',
      renderWidget: () => <AriaWidget snapshot={snapshot} />,
      widgetNotFound: () => {},
    });
  } catch {
    // ignore (widget framework unavailable / no widgets placed)
  }
}
