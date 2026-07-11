import { FlexWidget, TextWidget, type ColorProp } from 'react-native-android-widget';

import type { WidgetSnapshot } from './snapshot';

// Widgets render in a separate context (no ThemeProvider), so colors are inlined.
// A dark indigo card reads well over any wallpaper.
const C = {
  bg: '#15171C',
  card: '#1A1D24',
  text: '#F2F3F5',
  muted: '#A6ACB8',
  indigo: '#A5B4FC',
  blue: '#60A5FA',
  amber: '#FBBF24',
  green: '#34D399',
} as const;

function Stat({ value, label, color }: { value: string; label: string; color: ColorProp }) {
  return (
    <FlexWidget style={{ flexDirection: 'column', alignItems: 'center', flex: 1 }}>
      <TextWidget text={value} style={{ fontSize: 22, fontWeight: '700', color }} />
      <TextWidget text={label} style={{ fontSize: 10, color: C.muted, marginTop: 2 }} />
    </FlexWidget>
  );
}

export function AriaWidget({ snapshot, width = 200 }: { snapshot: WidgetSnapshot; width?: number }) {
  const compact = width < 170;
  const nextTitle = snapshot.nextTaskTitle
    ? snapshot.nextTaskTitle.length > 22
      ? snapshot.nextTaskTitle.slice(0, 21) + '…'
      : snapshot.nextTaskTitle
    : 'No tasks left today 🎉';

  return (
    <FlexWidget
      clickAction="OPEN_APP"
      style={{
        height: 'match_parent',
        width: 'match_parent',
        backgroundColor: C.bg,
        borderRadius: 24,
        padding: 14,
        flexDirection: 'column',
        justifyContent: 'space-between',
      }}>
      {/* Header */}
      <FlexWidget
        style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', width: 'match_parent' }}>
        <TextWidget text="ARIA" style={{ fontSize: 12, fontWeight: '700', color: C.indigo }} />
        <TextWidget
          text={`🔥 ${snapshot.topStreak}`}
          style={{ fontSize: 12, color: C.amber }}
        />
      </FlexWidget>

      {/* Stats */}
      <FlexWidget style={{ flexDirection: 'row', width: 'match_parent', marginTop: 8 }}>
        <Stat value={`${snapshot.waterPct}%`} label="Water" color={C.blue} />
        <Stat value={`${snapshot.pendingTasks}`} label="Tasks left" color={C.indigo} />
        <Stat value={`${snapshot.habitsDone}/${snapshot.habitsTotal}`} label="Habits" color={C.green} />
      </FlexWidget>

      {/* Next task */}
      {!compact ? (
        <FlexWidget
          style={{
            flexDirection: 'row',
            alignItems: 'center',
            backgroundColor: C.card,
            borderRadius: 12,
            padding: 10,
            marginTop: 10,
            width: 'match_parent',
          }}>
          <TextWidget text="▸  " style={{ fontSize: 13, color: C.indigo }} />
          <TextWidget
            text={nextTitle}
            style={{ fontSize: 13, fontWeight: '600', color: C.text }}
          />
          {snapshot.nextTaskTime ? (
            <TextWidget text={`  ${snapshot.nextTaskTime}`} style={{ fontSize: 12, color: C.muted }} />
          ) : (
            <TextWidget text="" style={{ fontSize: 12 }} />
          )}
        </FlexWidget>
      ) : (
        <TextWidget text="" style={{ fontSize: 1 }} />
      )}
    </FlexWidget>
  );
}
