import React, { useEffect, useRef, useState } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  SafeAreaView,
  Pressable,
  Animated,
  Switch,
  StatusBar,
} from 'react-native';
import { Feather } from '@expo/vector-icons';
import Svg, { Path, Defs, LinearGradient as SvgLinearGradient, Stop } from 'react-native-svg';
import { LinearGradient } from 'expo-linear-gradient';

import { Colors, Shadows, Radius } from '../theme/colors';
import { FontFamily, FontSize } from '../theme/typography';
import GradientCard from '../components/ui/GradientCard';
import AddressChip from '../components/ui/AddressChip';
import TokenRow from '../components/ui/TokenRow';
import TransactionRow from '../components/ui/TransactionRow';

// ── Mock Data ─────────────────────────────────────────────────────────────────
const MOCK_PRICES   = [3820, 3975, 3890, 4102, 4055, 4310, 4821];
const MOCK_CHANGE   = { amount: '+$124.50', pct: '+2.6%', positive: true };
const ETH_ADDRESS   = '0x4a9f3bD0a3b9A1E3cF8bDE2e5f3CD8A0E6c5D7Fa';
const NOW = new Date();

const ASSETS = [
  { symbol: 'ETH',  name: 'Ethereum',    balance: '1.24 ETH',   usdValue: '$3,102.00', change24h:  1.4  },
  { symbol: 'SOL',  name: 'Solana',      balance: '28.5 SOL',   usdValue: '$1,482.00', change24h:  3.2  },
  { symbol: 'USDC', name: 'USD Coin',    balance: '124.50 USDC', usdValue: '$124.50',  change24h:  0.0, chain: '(ETH)' },
  { symbol: 'USDC', name: 'USD Coin',    balance: '112.84 USDC', usdValue: '$112.84',  change24h:  0.0, chain: '(SOL)' },
];

const TRANSACTIONS = [
  { direction: 'received' as const, amount: '0.15',  asset: 'ETH',  address: '0xAbC123...789D', timestamp: new Date(NOW.getTime() - 1000 * 60 * 8),    status: 'confirmed' as const },
  { direction: 'sent'     as const, amount: '50.00', asset: 'USDC', address: '0x9Ef4a8...231E', timestamp: new Date(NOW.getTime() - 1000 * 60 * 60 * 3), status: 'confirmed' as const },
  { direction: 'sent'     as const, amount: '0.08',  asset: 'ETH',  address: '0x72Bc5d...A49F', timestamp: new Date(NOW.getTime() - 1000 * 60 * 60 * 26), status: 'confirmed' as const },
  { direction: 'received' as const, amount: '200.00',asset: 'USDC', address: '0xD1e9f0...834C', timestamp: new Date(NOW.getTime() - 1000 * 60 * 60 * 72), status: 'confirmed' as const },
];

// ── Mini sparkline chart ──────────────────────────────────────────────────────
function SparklineChart({ prices, width }: { prices: number[]; width: number }) {
  const HEIGHT = 80;
  const PAD    = 12;
  const w      = width - PAD * 2;
  const min    = Math.min(...prices);
  const max    = Math.max(...prices);
  const range  = max - min || 1;

  const pts = prices.map((p, i) => ({
    x: PAD + (i / (prices.length - 1)) * w,
    y: PAD + ((1 - (p - min) / range) * (HEIGHT - PAD * 2)),
  }));

  const linePath = pts.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ');
  const areaPath = `${linePath} L${pts[pts.length - 1].x.toFixed(1)},${HEIGHT} L${pts[0].x.toFixed(1)},${HEIGHT} Z`;

  return (
    <View style={{ height: HEIGHT + 18 }}>
      <Svg width={width} height={HEIGHT}>
        <Defs>
          <SvgLinearGradient id="areaGrad" x1="0" y1="0" x2="0" y2="1">
            <Stop offset="0"   stopColor={Colors.brandOrange} stopOpacity="0.28" />
            <Stop offset="1"   stopColor={Colors.brandOrange} stopOpacity="0"    />
          </SvgLinearGradient>
        </Defs>
        <Path d={areaPath} fill="url(#areaGrad)" />
        <Path d={linePath} stroke={Colors.brandOrange} strokeWidth="2" fill="none" strokeLinejoin="round" />
      </Svg>
      <View style={styles.chartLabels}>
        <Text allowFontScaling={false} style={styles.chartLabel}>6h ago</Text>
        <Text allowFontScaling={false} style={styles.chartLabel}>Now</Text>
      </View>
    </View>
  );
}

// ── Quick Action Button ───────────────────────────────────────────────────────
function ActionBtn({ icon, label, onPress }: { icon: string; label: string; onPress: () => void }) {
  const scaleAnim = useRef(new Animated.Value(1)).current;
  const pressIn  = () => Animated.timing(scaleAnim, { toValue: 0.88, duration: 80,  useNativeDriver: true }).start();
  const pressOut = () => Animated.spring(scaleAnim,  { toValue: 1,    useNativeDriver: true, speed: 30 }).start();

  return (
    <Pressable onPress={onPress} onPressIn={pressIn} onPressOut={pressOut} style={styles.actionBtn}>
      <Animated.View style={[styles.actionCircle, { transform: [{ scale: scaleAnim }] }]}>
        <Feather name={icon as any} size={20} color={Colors.brandOrange} />
      </Animated.View>
      <Text allowFontScaling={false} style={styles.actionLabel}>{label}</Text>
    </Pressable>
  );
}

// ── Main Screen ───────────────────────────────────────────────────────────────
export default function WalletScreen({ navigation }: any) {
  const [hideSmall,  setHideSmall]  = useState(false);
  const [cardWidth,  setCardWidth]  = useState(340);
  const fadeIn   = useRef(new Animated.Value(0)).current;
  const slideIn  = useRef(new Animated.Value(20)).current;

  // Shimmer animation on balance card
  const shimmerX = useRef(new Animated.Value(-200)).current;

  useEffect(() => {
    // Screen entrance
    Animated.parallel([
      Animated.timing(fadeIn,  { toValue: 1, duration: 300, useNativeDriver: true }),
      Animated.timing(slideIn, { toValue: 0, duration: 300, useNativeDriver: true }),
    ]).start();

    // Shimmer loop every 4s
    const shimmerLoop = Animated.loop(
      Animated.sequence([
        Animated.delay(3200),
        Animated.timing(shimmerX, {
          toValue: 500,
          duration: 900,
          useNativeDriver: true,
        }),
        Animated.timing(shimmerX, { toValue: -200, duration: 0, useNativeDriver: true }),
      ])
    );
    shimmerLoop.start();
    return () => shimmerLoop.stop();
  }, []);

  const filteredAssets = hideSmall
    ? ASSETS.filter(a => parseFloat(a.usdValue.replace(/[$,]/g, '')) >= 50)
    : ASSETS;

  const initials = ETH_ADDRESS.slice(2, 4).toUpperCase();

  return (
    <SafeAreaView style={styles.safe}>
      <StatusBar barStyle="light-content" />
      <Animated.View
        style={[styles.screen, { opacity: fadeIn, transform: [{ translateY: slideIn }] }]}
      >
        {/* ── Header ──────────────────────────────────────────────────────── */}
        <View style={styles.header}>
          <LinearGradient
            colors={[Colors.brandOrange, '#FF9A50']}
            style={styles.avatarGrad}
          >
            <Text allowFontScaling={false} style={styles.avatarText}>{initials}</Text>
          </LinearGradient>

          <Text allowFontScaling={false} style={styles.headerTitle}>My Wallet</Text>

          <View style={styles.headerRight}>
            <Pressable style={styles.headerIcon}>
              <Feather name="bell" size={20} color={Colors.offWhite} />
            </Pressable>
            <Pressable style={styles.headerIcon}>
              <Feather name="settings" size={20} color={Colors.offWhite} />
            </Pressable>
          </View>
        </View>
        <View style={styles.divider} />

        <ScrollView
          showsVerticalScrollIndicator={false}
          style={styles.scroll}
          contentContainerStyle={styles.scrollContent}
        >
          {/* ── Balance Hero Card ─────────────────────────────────────────── */}
          <View
            onLayout={e => setCardWidth(e.nativeEvent.layout.width)}
            style={styles.cardWrap}
          >
            <GradientCard glowColor={Colors.orangeMid} noPadding style={styles.heroCard}>
              {/* Shimmer sweep */}
              <Animated.View
                pointerEvents="none"
                style={[styles.shimmer, { transform: [{ translateX: shimmerX }] }]}
              />

              <View style={styles.heroPad}>
                <Text allowFontScaling={false} style={styles.balanceLabel}>
                  TOTAL BALANCE
                </Text>
                <Text allowFontScaling={false} style={styles.balanceAmount}>
                  $4,821.34
                </Text>
                <Text
                  allowFontScaling={false}
                  style={[
                    styles.balanceChange,
                    { color: MOCK_CHANGE.positive ? Colors.success : Colors.error },
                  ]}
                >
                  {MOCK_CHANGE.amount} ({MOCK_CHANGE.pct}) today
                </Text>
              </View>

              {/* Chart */}
              <SparklineChart prices={MOCK_PRICES} width={cardWidth || 340} />

              {/* Footer row */}
              <View style={styles.heroFooter}>
                <AddressChip address={ETH_ADDRESS} chain="ETH" />
                <Pressable style={styles.refreshBtn}>
                  <Feather name="refresh-cw" size={16} color={Colors.textMuted} />
                </Pressable>
              </View>
            </GradientCard>
          </View>

          {/* ── Quick Actions ─────────────────────────────────────────────── */}
          <View style={styles.actionsRow}>
            <ActionBtn icon="arrow-up-right" label="Send"    onPress={() => navigation?.navigate('Pay')} />
            <ActionBtn icon="arrow-down-left" label="Receive" onPress={() => navigation?.navigate('Receive')} />
            <ActionBtn icon="maximize"        label="Scan"    onPress={() => {}} />
            <ActionBtn icon="clock"           label="History" onPress={() => {}} />
          </View>

          {/* ── Asset List ────────────────────────────────────────────────── */}
          <View style={styles.section}>
            <View style={styles.sectionHeader}>
              <Text allowFontScaling={false} style={styles.sectionTitle}>Your Assets</Text>
              <View style={styles.hideRow}>
                <Text allowFontScaling={false} style={styles.hideLabel}>Hide small</Text>
                <Switch
                  value={hideSmall}
                  onValueChange={setHideSmall}
                  trackColor={{ false: Colors.surface, true: Colors.brandOrange }}
                  thumbColor={Colors.offWhite}
                  style={styles.toggle}
                />
              </View>
            </View>

            <GradientCard noPadding glowColor="transparent" style={styles.assetCard}>
              {filteredAssets.map((asset, i) => (
                <View key={`${asset.symbol}-${i}`}>
                  {i > 0 && <View style={styles.rowDivider} />}
                  <TokenRow
                    symbol={asset.symbol}
                    name={asset.name}
                    balance={asset.balance}
                    usdValue={asset.usdValue}
                    change24h={asset.change24h}
                    chain={asset.chain}
                  />
                </View>
              ))}
              {filteredAssets.length === 0 && (
                <View style={styles.emptyState}>
                  <Feather name="inbox" size={28} color={Colors.textFaint} />
                  <Text allowFontScaling={false} style={styles.emptyText}>
                    All balances hidden
                  </Text>
                </View>
              )}
            </GradientCard>
          </View>

          {/* ── Recent Activity ───────────────────────────────────────────── */}
          <View style={styles.section}>
            <View style={styles.sectionHeader}>
              <Text allowFontScaling={false} style={styles.sectionTitle}>Recent Activity</Text>
              <Pressable>
                <Text allowFontScaling={false} style={styles.seeAll}>See All</Text>
              </Pressable>
            </View>

            <GradientCard noPadding>
              {TRANSACTIONS.map((tx, i) => (
                <View key={i}>
                  {i > 0 && <View style={styles.rowDivider} />}
                  <TransactionRow
                    direction={tx.direction}
                    amount={tx.amount}
                    asset={tx.asset}
                    address={tx.address}
                    timestamp={tx.timestamp}
                    status={tx.status}
                  />
                </View>
              ))}
            </GradientCard>
          </View>

          <View style={styles.bottomPad} />
        </ScrollView>
      </Animated.View>
    </SafeAreaView>
  );
}

// ── Styles ────────────────────────────────────────────────────────────────────
const styles = StyleSheet.create({
  safe: {
    flex: 1,
    backgroundColor: Colors.deepDark,
  },
  screen: {
    flex: 1,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingTop: 8,
    paddingBottom: 14,
  },
  avatarGrad: {
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarText: {
    fontFamily: FontFamily.interBold,
    fontSize: FontSize.sm,
    color: Colors.offWhite,
  },
  headerTitle: {
    flex: 1,
    textAlign: 'center',
    fontFamily: FontFamily.interSemiBold,
    fontSize: FontSize.md,
    color: Colors.offWhite,
  },
  headerRight: {
    flexDirection: 'row',
    gap: 6,
  },
  headerIcon: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: Colors.surfaceBorder,
    alignItems: 'center',
    justifyContent: 'center',
  },
  divider: {
    height: 1,
    backgroundColor: Colors.divider,
    marginHorizontal: 0,
  },
  scroll: {
    flex: 1,
  },
  scrollContent: {
    paddingHorizontal: 16,
    paddingTop: 20,
  },

  // Hero card
  cardWrap: {
    marginBottom: 20,
  },
  heroCard: {
    borderRadius: Radius.lg,
    overflow: 'hidden',
  },
  shimmer: {
    position: 'absolute',
    top: 0,
    bottom: 0,
    width: 80,
    backgroundColor: 'rgba(253,112,20,0.07)',
    transform: [{ skewX: '-15deg' }],
    zIndex: 1,
  },
  heroPad: {
    paddingHorizontal: 20,
    paddingTop: 20,
    paddingBottom: 8,
  },
  balanceLabel: {
    fontFamily: FontFamily.interMedium,
    fontSize: FontSize.xs,
    color: Colors.textMuted,
    letterSpacing: 1.5,
    marginBottom: 8,
  },
  balanceAmount: {
    fontFamily: FontFamily.syneXBold,
    fontSize: FontSize['3xl'],
    color: Colors.offWhite,
    marginBottom: 6,
  },
  balanceChange: {
    fontFamily: FontFamily.interMedium,
    fontSize: FontSize.base,
    marginBottom: 4,
  },
  chartLabels: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingHorizontal: 12,
    marginTop: 4,
  },
  chartLabel: {
    fontFamily: FontFamily.inter,
    fontSize: FontSize.xs,
    color: Colors.textFaint,
  },
  heroFooter: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingVertical: 14,
    borderTopWidth: 1,
    borderTopColor: Colors.divider,
  },
  refreshBtn: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: Colors.surfaceBorder,
    alignItems: 'center',
    justifyContent: 'center',
  },

  // Actions row
  actionsRow: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    marginBottom: 24,
    paddingHorizontal: 8,
  },
  actionBtn: {
    alignItems: 'center',
    gap: 8,
  },
  actionCircle: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: Colors.orangeDim,
    borderWidth: 1,
    borderColor: Colors.orangeMid,
    alignItems: 'center',
    justifyContent: 'center',
    ...Shadows.subtle,
  },
  actionLabel: {
    fontFamily: FontFamily.interMedium,
    fontSize: FontSize.xs,
    color: Colors.offWhite,
  },

  // Sections
  section: {
    marginBottom: 20,
  },
  sectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 12,
  },
  sectionTitle: {
    fontFamily: FontFamily.interSemiBold,
    fontSize: FontSize.base,
    color: Colors.offWhite,
  },
  hideRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  hideLabel: {
    fontFamily: FontFamily.inter,
    fontSize: FontSize.xs,
    color: Colors.textMuted,
  },
  toggle: {
    transform: [{ scaleX: 0.8 }, { scaleY: 0.8 }],
  },
  seeAll: {
    fontFamily: FontFamily.interMedium,
    fontSize: FontSize.sm,
    color: Colors.brandOrange,
  },

  // Asset card
  assetCard: {
    borderRadius: Radius.md,
  },
  rowDivider: {
    height: 1,
    backgroundColor: Colors.divider,
    marginHorizontal: 16,
  },

  // Empty state
  emptyState: {
    alignItems: 'center',
    gap: 10,
    padding: 32,
  },
  emptyText: {
    fontFamily: FontFamily.inter,
    fontSize: FontSize.sm,
    color: Colors.textFaint,
  },

  bottomPad: {
    height: 80,
  },
});
