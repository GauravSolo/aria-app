import { requestWidgetUpdate } from 'react-native-android-widget';

import { AriaWidget } from './AriaWidget';
import { loadWidgetSnapshot } from './snapshot';

export function registerAriaWidgetTask(): void {
  // The task handler is registered in index.js (the JS entry point) so it also runs
  // in the widget's headless background context. Nothing to do here.
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
