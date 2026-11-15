import React from 'react';
import {
  Pressable,
  ScrollView,
  StyleSheet,
  Switch,
  View,
  useWindowDimensions,
} from 'react-native';
import {Text} from 'react-native-paper';
import {getSettings, saveSettings} from '../store/settingStore';
import {useGamepadNavigation} from '../utils/useGamepadNavigation';
import pkg from '../../package.json';

const RED = '#ff1828';
const PANEL = '#151515';
const MUTED = '#a7a7a7';

function SettingsScreen({navigation}: any) {
  const {width} = useWindowDimensions();
  const [settings, setSettings] = React.useState<any>(() => getSettings());
  const [focus, setFocus] = React.useState(0);
  const wide = width >= 900;

  React.useEffect(() => {
    const {DeviceEventEmitter} = require('react-native');
    const sub = DeviceEventEmitter.addListener('SETTINGS_CHANGED', (next: any) => {
      if (next) setSettings({...next});
    });
    return () => sub.remove();
  }, []);

  const openSetting = (id: string) => navigation.navigate('SettingDetail', {id});
  const toggle = (key: string) => {
    const next = {...settings, [key]: !settings[key]};
    saveSettings(next);
    setSettings(next);
  };

  const actions = [
    () => navigation.navigate('Home'),
    () => navigation.navigate('Cloud'),
    () => navigation.navigate('Search'),
    () => navigation.navigate('Cloud'),
    () => {},
    () => navigation.navigate('About'),
  ];
  useGamepadNavigation({
    onUp: () => setFocus(v => Math.max(0, v - 1)),
    onDown: () => setFocus(v => Math.min(5, v + 1)),
    onSelect: () => actions[focus]?.(),
    onBack: () => navigation.goBack(),
  });

  const nav = [
    ['⌂', 'Home'], ['🎮', 'Games'], ['⌕', 'Search'],
    ['▣', 'Game Pass'], ['⚙', 'Settings'], ['ⓘ', 'About'],
  ];
  const row = (label: string, value: string, onPress: () => void) => (
    <Pressable onPress={onPress} style={({focused}: any) => [styles.settingRow, focused && styles.focused]}>
      <Text style={styles.rowLabel}>{label}</Text>
      <Text style={styles.rowValue}>{value}  ‹</Text>
    </Pressable>
  );
  const toggleRow = (label: string, key: string) => (
    <View style={styles.settingRow}>
      <Text style={styles.rowLabel}>{label}</Text>
      <Switch
        value={!!settings[key]}
        onValueChange={() => toggle(key)}
        trackColor={{false: '#555', true: RED}}
        thumbColor="#fff"
      />
    </View>
  );
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
          <Text style={styles.logo}>X</Text>
          <Text style={styles.brandName}><Text style={{color: RED}}>ONNX</Text>CloudGaming</Text>
          <Text style={styles.tagline}>Play Xbox. Anywhere.</Text>
        </View>
        {nav.map(([icon, label], i) => (
          <Pressable key={label} onPress={actions[i]} style={[styles.navItem, i === 4 && styles.navActive]}>
            <Text style={styles.navIcon}>{icon}</Text>
            <Text style={styles.navText}>{label}</Text>
          </Pressable>
        ))}
        <View style={styles.sidebarFooter}>
          <Text style={styles.accountIcon}>●</Text>
          <View><Text style={styles.accountName}>Xbox Account</Text><Text style={styles.signOut}>Account settings</Text></View>
        </View>
      </View>

      <View style={styles.main}>
        <View style={styles.topBar}>
          <Text style={styles.pageTitle}>⚙  Settings</Text>
          <Text style={styles.status}>● Connected     │     🎮 Controller</Text>
        </View>
        <ScrollView contentContainerStyle={[styles.grid, {gap: 16}]}>
          <View style={[styles.cardColumn, {width: wide ? '49%' : '100%'}]}>
            {card('▣', 'Streaming Settings', 'Adjust your streaming preferences', <>
              {row('Resolution', settings.resolution === 1080 ? '1080p' : settings.resolution === 1081 ? '1080p HQ' : '720p', () => openSetting('resolution'))}
              {row('Bitrate', settings.xcloud_bitrate_mode === 'custom' ? String(settings.xcloud_bitrate) : 'Auto', () => openSetting('xcloud_bitrate_mode'))}
              {row('Video Codec', settings.codec || 'Auto', () => openSetting('codec'))}
            </>)}
            {card('◖', 'Audio Settings', 'Adjust audio options', <>
              {row('Audio Quality', settings.audio_bitrate_mode === 'custom' ? String(settings.audio_bitrate) : 'Auto', () => openSetting('audio_bitrate_mode'))}
              {toggleRow('Stereo Sound', 'enable_stereo_audio')}
            </>)}
            {card('◉', 'Appearance', 'Personalize your app', <>
              {row('Theme', settings.theme || 'Dark', () => openSetting('theme'))}
              {row('Accent Color', settings.theme_primary_color || 'Red', () => openSetting('theme_primary_color'))}
            </>)}
          </View>
          <View style={[styles.cardColumn, {width: wide ? '49%' : '100%'}]}>
            {card('🎮', 'Controller Settings', 'Customize your controller experience', <>
              {toggleRow('Controller Vibration', 'vibration')}
              {row('Button Mapping', 'Configure', () => navigation.navigate('GameMap'))}
              {row('Controller Test', 'Open', () => navigation.navigate('GamepadTest'))}
            </>)}
            {card('◎', 'Network Settings', 'Manage your network connection', <>
              {toggleRow('IPv6', 'ipv6')}
              {row('Network Diagnostics', 'View details', () => navigation.navigate('DeviceInfos'))}
            </>)}
            {card('ⓘ', 'About', 'App information and updates', <>
              <View style={styles.settingRow}><Text style={styles.rowLabel}>App Version</Text><Text style={styles.rowValue}>{pkg.version}</Text></View>
              <View style={styles.settingRow}><Text style={styles.rowLabel}>Build</Text><Text style={styles.rowValue}>Phase 3</Text></View>
              {row('About ONNXCloudGaming', 'Open', () => navigation.navigate('About'))}
            </>)}
          </View>
        </ScrollView>
        <View style={styles.bottomBar}>
          <Text style={styles.footerBrand}>ⓧ  ONNXCloudGaming  ·  Powered by XStreaming</Text>
          <Text style={styles.footerAction}>↻  Check for Updates</Text>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {flex: 1, flexDirection: 'row', backgroundColor: '#080808'},
  sidebar: {width: '24%', minWidth: 190, maxWidth: 310, backgroundColor: '#090909', borderRightWidth: 1, borderRightColor: '#481017', paddingHorizontal: 14, paddingTop: 16, paddingBottom: 12},
  brand: {alignItems: 'center', paddingVertical: 12, marginBottom: 18},
  logo: {fontSize: 68, lineHeight: 78, color: RED, fontWeight: '900', textShadowColor: RED, textShadowRadius: 16},
  brandName: {fontSize: 21, color: '#fff', fontWeight: '800'},
  tagline: {fontSize: 12, color: '#ddd', marginTop: 5},
  navItem: {height: 48, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 12, borderRadius: 10, marginBottom: 6, borderWidth: 1, borderColor: 'transparent'},
  navActive: {backgroundColor: '#26070b', borderColor: RED},
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
  settingRow: {minHeight: 42, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 4},
  rowLabel: {fontSize: 14, color: '#fff'},
  rowValue: {fontSize: 14, color: '#fff', fontWeight: '700'},
  focused: {borderColor: RED, borderWidth: 1, borderRadius: 8},
  bottomBar: {position: 'absolute', left: 18, right: 18, bottom: 0, height: 48, borderTopWidth: 1, borderTopColor: '#481017', flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', backgroundColor: '#080808'},
  footerBrand: {color: '#ddd', fontSize: 12},
  footerAction: {color: '#fff', fontSize: 12},
});

export default SettingsScreen;
