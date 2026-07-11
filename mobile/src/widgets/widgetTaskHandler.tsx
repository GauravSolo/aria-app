import type { WidgetTaskHandlerProps } from 'react-native-android-widget';

import { AriaWidget } from './AriaWidget';
import { emptySnapshot, loadWidgetSnapshot } from './snapshot';

/** Runs in a headless JS context when Android asks the widget to (re)render. */
export async function widgetTaskHandler(props: WidgetTaskHandlerProps): Promise<void> {
  const width = props.widgetInfo?.width ?? 200;
  const height = props.widgetInfo?.height ?? 150;
  switch (props.widgetAction) {
    case 'WIDGET_ADDED':
    case 'WIDGET_UPDATE':
    case 'WIDGET_RESIZED': {
      const snapshot = await loadWidgetSnapshot().catch(() => emptySnapshot());
      props.renderWidget(<AriaWidget snapshot={snapshot} width={width} height={height} />);
      break;
    }
    default:
      break;
  }
}
