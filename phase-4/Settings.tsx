import React from 'react';
import {
  DeviceEventEmitter,
  Pressable,
  ScrollView,
  StyleSheet,
  View,
  useWindowDimensions,
} from 'react-native';
import {Text} from 'react-native-paper';
import {
  getSettings,
  saveSettings,
  SETTINGS_CHANGED_EVENT,
} from '../store/settingStore';
import type {Settings} from '../store/settingStore';
import {useGamepadNavigation} from '../utils/useGamepadNavigation';
import pkg from '../../package.json';

const RED = '#ff1828';
const PANEL = '#151515';
const MUTED = '#a7a7a7';

const FOCUS = {
  home: 0,
  games: 1,
  search: 2,
  gamePass: 3,
  settings: 4,
  aboutNav: 5,
  resolution: 6,
  bitrate: 7,
  codec: 8,
  audioBitrate: 9,
  stereo: 10,
  theme: 11,
  accent: 12,
  vibration: 13,
  mapping: 14,
  controllerTest: 15,
  ipv6: 16,
  diagnostics: 17,
  about: 18,
} as const;

function resolutionLabel(value: number) {
  if (value === 1081) return '1080p HQ / 1440p';
  if (value === 1080) return '1080p';
  return '720p';
}

function codecLabel(value: string) {
  switch (value) {
    case 'video/H264-4d':
      return 'H.264 High';
    case 'video/H264-42e':
      return 'H.264 Medium';
    case 'video/H264-420':
      return 'H.264 Low';
    default:
      return 'Auto';
  }
}

function bitrateLabel(mode: string, bitrate: number | string) {
  return mode === 'custom' ? `${bitrate} Mbps` : 'Auto';
}

function SettingsScreen({navigation}: any) {
  const {width} = useWindowDimensions();
  const [settings, setSettings] = React.useState<Settings>(() => getSettings());
  const [focus, setFocus] = React.useState<number>(FOCUS.resolution);
  const scrollRef = React.useRef<ScrollView>(null);
  const wide = width >= 900;

  React.useEffect(() => {
    const sub = DeviceEventEmitter.addListener(
      SETTINGS_CHANGED_EVENT,
      (next: Settings) => {
        if (next) setSettings({...next});
      },
    );
    return () => sub.remove();
  }, []);

  const openSetting = (id: string) =>
    navigation.navigate('SettingDetail', {id});

  const toggle = (key: keyof Settings) => {
    const next = {...settings, [key]: !settings[key]} as Settings;
    saveSettings(next);
    setSettings(next);
  };

  const navActions = [
    () => navigation.navigate('Home'),
    () => navigation.navigate('Cloud'),
    () => navigation.navigate('Search'),
    () => navigation.navigate('Cloud'),
    () => {},
    () => navigation.navigate('About'),
  ];

  const focusActions: Array<() => void> = [
    ...navActions,
    () => openSetting('resolution'),
    () => openSetting('xcloud_bitrate_mode'),
    () => openSetting('codec'),
    () => openSetting('audio_bitrate_mode'),
    () => toggle('enable_stereo_audio'),
    () => openSetting('theme'),
    () => openSetting('theme_primary_color'),
    () => toggle('vibration'),
    () => navigation.navigate('GameMap'),
    () => navigation.navigate('GamepadTest'),
    () => toggle('ipv6'),
    () => navigation.navigate('DeviceInfos'),
    () => navigation.navigate('About'),
  ];

  useGamepadNavigation({
    priority: 10,
    onUp: () => setFocus(v => Math.max(0, v - 1)),
    onDown: () => setFocus(v => Math.min(focusActions.length - 1, v + 1)),
    onSelect: () => focusActions[focus]?.(),
    onBack: () => navigation.goBack(),
  });

  React.useEffect(() => {
    if (focus >= FOCUS.ipv6) {
      scrollRef.current?.scrollToEnd({animated: true});
    } else if (focus >= FOCUS.mapping) {
      scrollRef.current?.scrollTo({y: 180, animated: true});
    } else {
      scrollRef.current?.scrollTo({y: 0, animated: true});
    }
  }, [focus]);

  const nav = [
    ['⌂', 'Home'],
    ['🎮', 'Games'],
    ['⌕', 'Search'],
    ['▣', 'Game Pass'],
    ['⚙', 'Settings'],
    ['ⓘ', 'About'],
  ];

  const row = (
    label: string,
    value: string,
    onPress: () => void,
    index: number,
  ) => (
    <Pressable
      focusable
      accessibilityRole="button"
      onFocus={() => setFocus(index)}
      onPress={onPress}
      style={({focused}: any) => [
        styles.settingRow,
        (focused || focus === index) && styles.focused,
      ]}>
      <Text style={styles.rowLabel}>{label}</Text>
      <Text style={styles.rowValue}>{value}  ‹</Text>
    </Pressable>
  );

  const toggleRow = (label: string, key: keyof Settings, index: number) => {
    const checked = !!settings[key];
    return (
      <Pressable
        focusable
        accessibilityRole="switch"
        accessibilityState={{checked}}
        onFocus={() => setFocus(index)}
        onPress={() => toggle(key)}
        style={({focused}: any) => [
          styles.settingRow,
          (focused || focus === index) && styles.focused,
        ]}>
        <Text style={styles.rowLabel}>{label}</Text>
        <View style={[styles.switchTrack, checked && styles.switchOn]}>
          <View style={[styles.switchThumb, checked && styles.switchThumbOn]} />
        </View>
      </Pressable>
    );
  };

  const card = (icon: string, title: string, subtitle: string, children: any) => (
    <View style={styles.card}>
      <View style={styles.cardHeader}>
        <Text style={styles.cardIcon}>{icon}</Text>
        <View style={{flex: 1}}>
          <Text style={styles.cardTitle}>{title}</Text>
          <Text style={styles.cardSubtitle}>{subtitle}</Text>
        </View>
      </View>
      <View style={styles.cardDivider} />
      {children}
    </View>
  );

  return (
    <View style={styles.screen}>
      <View style={styles.sidebar}>
        <View style={styles.brand}>
          <View style={styles.logoCircle}>
            <Text style={styles.logo}>X</Text>
          </View>
          <Text style={styles.brandName}>
            <Text style={{color: RED}}>ONNX</Text>CloudGaming
          </Text>
          <Text style={styles.tagline}>Play Xbox. Anywhere.</Text>
        </View>
        {nav.map(([icon, label], i) => (
          <Pressable
            key={label}
            focusable
            accessibilityRole="button"
            onFocus={() => setFocus(i)}
            onPress={navActions[i]}
            style={({focused}: any) => [
              styles.navItem,
              i === FOCUS.settings && styles.navActive,
              (focused || focus === i) && styles.navFocused,
            ]}>
            <Text style={styles.navIcon}>{icon}</Text>
            <Text style={styles.navText}>{label}</Text>
          </Pressable>
        ))}
        <View style={styles.sidebarFooter}>
          <Text style={styles.accountIcon}>●</Text>
          <View>
            <Text style={styles.accountName}>Xbox Account</Text>
            <Text style={styles.signOut}>Account settings</Text>
          </View>
        </View>
      </View>

      <View style={styles.main}>
        <View style={styles.topBar}>
          <Text style={styles.pageTitle}>⚙  Settings</Text>
          <Text style={styles.status}>ONNXCloudGaming  │  Native Streaming</Text>
        </View>
        <ScrollView
          ref={scrollRef}
          contentContainerStyle={[styles.grid, {gap: 16}]}>
          <View style={[styles.cardColumn, {width: wide ? '49%' : '100%'}]}>
            {card('▣', 'Streaming Settings', 'Changes are saved to the native streaming engine', <>
              {row('Resolution', resolutionLabel(settings.resolution), () => openSetting('resolution'), FOCUS.resolution)}
              {row('Cloud Bitrate', bitrateLabel(settings.xcloud_bitrate_mode, settings.xcloud_bitrate), () => openSetting('xcloud_bitrate_mode'), FOCUS.bitrate)}
              {row('Video Codec', codecLabel(settings.codec), () => openSetting('codec'), FOCUS.codec)}
            </>)}
            {card('◖', 'Audio Settings', 'Adjust audio options', <>
              {row('Audio Quality', bitrateLabel(settings.audio_bitrate_mode, settings.audio_bitrate), () => openSetting('audio_bitrate_mode'), FOCUS.audioBitrate)}
              {toggleRow('Stereo Sound', 'enable_stereo_audio', FOCUS.stereo)}
            </>)}
            {card('◉', 'Appearance', 'Personalize your app', <>
              {row('Theme', settings.theme || 'Dark', () => openSetting('theme'), FOCUS.theme)}
              {row('Accent Color', settings.theme_primary_color || 'Red', () => openSetting('theme_primary_color'), FOCUS.accent)}
            </>)}
          </View>
          <View style={[styles.cardColumn, {width: wide ? '49%' : '100%'}]}>
            {card('🎮', 'Controller Settings', 'Customize your controller experience', <>
              {toggleRow('Controller Vibration', 'vibration', FOCUS.vibration)}
              {row('Button Mapping', 'Configure', () => navigation.navigate('GameMap'), FOCUS.mapping)}
              {row('Controller Test', 'Open', () => navigation.navigate('GamepadTest'), FOCUS.controllerTest)}
            </>)}
            {card('◎', 'Network Settings', 'Manage your network connection', <>
              {toggleRow('IPv6', 'ipv6', FOCUS.ipv6)}
              {row('Network Diagnostics', 'View details', () => navigation.navigate('DeviceInfos'), FOCUS.diagnostics)}
            </>)}
            {card('ⓘ', 'About', 'App information and updates', <>
              <View style={styles.settingRow}>
                <Text style={styles.rowLabel}>App Version</Text>
                <Text style={styles.rowValue}>{pkg.version}</Text>
              </View>
              <View style={styles.settingRow}>
                <Text style={styles.rowLabel}>Build</Text>
                <Text style={styles.rowValue}>Phase 4</Text>
              </View>
              {row('About ONNXCloudGaming', 'Open', () => navigation.navigate('About'), FOCUS.about)}
            </>)}
          </View>
        </ScrollView>
        <View style={styles.bottomBar}>
          <Text style={styles.footerBrand}>ⓧ  ONNXCloudGaming  ·  Powered by XStreaming</Text>
          <Text style={styles.footerAction}>Settings saved automatically</Text>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {flex: 1, flexDirection: 'row', backgroundColor: '#080808'},
  sidebar: {width: '24%', minWidth: 190, maxWidth: 310, backgroundColor: '#090909', borderRightWidth: 1, borderRightColor: '#481017', paddingHorizontal: 14, paddingTop: 16, paddingBottom: 12},
  brand: {alignItems: 'center', paddingVertical: 12, marginBottom: 18},
  logoCircle: {width: 82, height: 82, borderRadius: 41, backgroundColor: RED, alignItems: 'center', justifyContent: 'center', marginBottom: 8, shadowColor: RED, shadowRadius: 14, shadowOpacity: 0.8, elevation: 8},
  logo: {fontSize: 65, lineHeight: 74, color: '#090909', fontWeight: '900'},
  brandName: {fontSize: 21, color: '#fff', fontWeight: '800'},
  tagline: {fontSize: 12, color: '#ddd', marginTop: 5},
  navItem: {height: 48, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 12, borderRadius: 10, marginBottom: 6, borderWidth: 1, borderColor: 'transparent'},
  navActive: {backgroundColor: '#26070b', borderColor: RED},
  navFocused: {borderColor: '#ff6570', backgroundColor: '#351016'},
  navIcon: {width: 34, color: '#fff', fontSize: 23},
  navText: {color: '#fff', fontSize: 16, fontWeight: '600'},
  sidebarFooter: {marginTop: 'auto', borderWidth: 1, borderColor: '#4a151b', borderRadius: 12, padding: 12, flexDirection: 'row', alignItems: 'center', gap: 10},
  accountIcon: {fontSize: 28, color: RED},
  accountName: {color: '#fff', fontWeight: '700'},
  signOut: {color: MUTED, fontSize: 12, marginTop: 3},
  main: {flex: 1, paddingTop: 16, paddingHorizontal: 18},
  topBar: {height: 72, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between'},
  pageTitle: {fontSize: 31, color: '#fff', fontWeight: '800'},
  status: {color: '#eee', fontSize: 13},
  grid: {flexDirection: 'row', flexWrap: 'wrap', paddingBottom: 70, justifyContent: 'space-between'},
  cardColumn: {gap: 14},
  card: {backgroundColor: PANEL, borderRadius: 13, borderWidth: 1, borderColor: '#292929', padding: 15},
  cardHeader: {flexDirection: 'row', alignItems: 'center', gap: 12, minHeight: 54},
  cardIcon: {fontSize: 30, color: '#fff', width: 42, textAlign: 'center'},
  cardTitle: {fontSize: 18, fontWeight: '800', color: '#fff'},
  cardSubtitle: {fontSize: 12, color: '#bcbcbc', marginTop: 3},
  cardDivider: {height: 1, backgroundColor: '#383838', marginVertical: 10},
  settingRow: {minHeight: 42, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 4, borderRadius: 8, borderWidth: 1, borderColor: 'transparent'},
  rowLabel: {fontSize: 14, color: '#fff'},
  rowValue: {fontSize: 14, color: '#fff', fontWeight: '700'},
  focused: {borderColor: RED, backgroundColor: '#26070b'},
  switchTrack: {width: 42, height: 24, borderRadius: 12, backgroundColor: '#555', padding: 3, justifyContent: 'center'},
  switchOn: {backgroundColor: RED},
  switchThumb: {width: 18, height: 18, borderRadius: 9, backgroundColor: '#fff'},
  switchThumbOn: {alignSelf: 'flex-end'},
  bottomBar: {position: 'absolute', left: 18, right: 18, bottom: 0, height: 48, borderTopWidth: 1, borderTopColor: '#481017', flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', backgroundColor: '#080808'},
  footerBrand: {color: '#ddd', fontSize: 12},
  footerAction: {color: '#fff', fontSize: 12},
});

export default SettingsScreen;
