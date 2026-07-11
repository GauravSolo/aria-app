// Custom entry point. Registering the Android widget task handler here (the real JS
// entry) ensures it runs in the widget's headless background context too — not just
// when the app UI mounts. Without this, Expo Router screens never load in the headless
// task, so the widget renders blank.
import 'expo-router/entry';

import { Platform } from 'react-native';

if (Platform.OS === 'android') {
  const { registerWidgetTaskHandler } = require('react-native-android-widget');
  const { widgetTaskHandler } = require('./src/widgets/widgetTaskHandler');
  registerWidgetTaskHandler(widgetTaskHandler);
}
