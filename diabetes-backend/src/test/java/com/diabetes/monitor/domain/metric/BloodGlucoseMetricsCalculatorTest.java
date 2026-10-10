package com.diabetes.monitor.domain.metric;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;


@DisplayName("血糖指标计算器")
public class BloodGlucoseMetricsCalculatorTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 10, 1, 0, 0);
    private static final LocalDateTime FROM = BASE;
    private static final LocalDateTime TO = BASE.plusDays(14);

    private final BloodGlucoseMetricsCalculator calculator =
            new BloodGlucoseMetricsCalculator(new GlucoseThresholds(3.9, 10.0, 70, 36, 120));
    /** 构造一条记录：value 血糖值，type 1=空腹 2=餐后 0=随机，minutes 相对 BASE 的分钟偏移 */
    private static  GlucoseReading r(double value,int type,int minutes) {
        return new GlucoseReading(value,type,BASE.plusMinutes(minutes));
    }

    /** 断言 Double 指标：先判非空再比数值（避免 null 自动拆箱 NPE，也避开 assertEquals 重载歧义） */
    private static void assertClose(double expected, Double actual) {
        assertNotNull(actual,"指标不应为null");
        assertEquals(expected, actual, 0.05, "期望 " + expected + "，实际 " + actual);
    }

    @Test
    @DisplayName("空数据：不抛异常，所有派生指标为 null")
    void emptyInput() {
        BloodGlucoseMetrics m = calculator.calculate(List.of(), FROM, TO);

        assertEquals(0, m.getSampleCount());
        assertEquals(0, m.getDroppedCount());
        assertFalse(m.isEnough());
        assertNull(m.getMeanGlucose());
        assertNull(m.getTirPercent());
        assertNull(m.getTarPercent());
        assertNull(m.getTbrPercent());
        assertNull(m.getSd());
        assertNull(m.getCv());
        assertNull(m.getGmiPercent());
        assertNull(m.getMaxGlucose());
        assertNull(m.getMinGlucose());
        assertEquals(0, m.getHypoEventCount());
        assertFalse(m.isTirAchieved());
    }

    @Test
    @DisplayName("单条记录：算得出均值，算不出标准差和 GMI")
    void singleReading() {
        BloodGlucoseMetrics m = calculator.calculate(List.of(r(5.5, 1, 0)), FROM, TO);

        assertEquals(1, m.getSampleCount());
        assertFalse(m.isEnough());              // 少于 5 条
        assertClose(5.5, m.getMeanGlucose());
        assertClose(100.0, m.getTirPercent());
        assertClose(5.5, m.getFastingAvg());
        assertNull(m.getOtherAvg());            // 只有空腹记录，"其余"组为空
        assertNull(m.getSd());                  // n=1 算不出样本标准差
        assertNull(m.getCv());
        assertNull(m.getGmiPercent());          // 样本不足 5 条
    }

    @Test
    @DisplayName("3.9 和 10.0 都算范围内（闭区间）")
    void closedInterval() {
        BloodGlucoseMetrics m = calculator.calculate(List.of(r(3.9, 1, 0), r(10.0, 1, 60)), FROM, TO);

        assertClose(100.0, m.getTirPercent());
        assertClose(0.0, m.getTarPercent());
        assertClose(0.0, m.getTbrPercent());
        assertEquals(0, m.getHyperCount());
    }

    @Test
    @DisplayName("3.89 和 10.01 都算超范围")
    void justOutside() {
        BloodGlucoseMetrics m = calculator.calculate(List.of(r(3.89, 1, 0), r(10.01, 1, 60)), FROM, TO);

        assertClose(0.0, m.getTirPercent());
        assertClose(50.0, m.getTarPercent());
        assertClose(50.0, m.getTbrPercent());
        assertEquals(1, m.getHyperCount());
    }

    private List<GlucoseReading> golden() {
        return List.of(
                r(6.2, 1, 0),     // 空腹
                r(5.8, 1, 60),    // 空腹
                r(7.4, 1, 120),   // 空腹
                r(9.1, 3, 180),   // 餐后2h
                r(11.3, 3, 240),  // 餐后2h
                r(8.6, 3, 300),   // 餐后2h
                r(4.1, 1, 360),   // 空腹
                r(5.5, 4, 420),   // 睡前（归入"其余"）
                r(12.8, 3, 480),  // 餐后2h
                r(3.6, 1, 540)    // 空腹
        );
    }

    @Test
    @DisplayName("各项指标与手算结果一致")
    void allMetrics() {
        BloodGlucoseMetrics m = calculator.calculate(golden(), FROM, TO);

        assertEquals(10, m.getSampleCount());
        assertEquals(0, m.getDroppedCount());
        assertTrue(m.isEnough());

        assertClose(7.4, m.getMeanGlucose());          // 74.4 / 10
        assertClose(5.4, m.getFastingAvg());           // 27.1 / 5
        assertClose(10.5, m.getPostprandialAvg());     // 41.8 / 4
        assertClose(5.5, m.getOtherAvg());     // 睡前那条归入"其余"

        assertClose(70.0, m.getTirPercent());          // 7 / 10，正好卡达标线
        assertClose(20.0, m.getTarPercent());          // 11.3、12.8
        assertClose(10.0, m.getTbrPercent());          // 3.6

        assertClose(3.0, m.getSd());                   // 样本标准差 3.0115
        assertClose(40.5, m.getCv());                  // 40.48%
        assertEquals(1, m.getHypoEventCount());
        assertEquals(0, m.getSevereHypoCount());       // 无 <3.0
        assertEquals(2, m.getHyperCount());
        assertClose(6.5, m.getGmiPercent());

        assertClose(12.8, m.getMaxGlucose());
        assertClose(3.6, m.getMinGlucose());
        assertTrue(m.isTirAchieved());                 // 70.0 >= 70
    }

    @Test
    @DisplayName("打乱输入顺序，结果不变")
    void orderIndependent() {
        List<GlucoseReading> shuffled = new ArrayList<>(golden());
        Collections.shuffle(shuffled, new Random(42));   // 固定种子，失败可复现

        assertEquals(calculator.calculate(golden(), FROM, TO),
                calculator.calculate(shuffled, FROM, TO));
    }

    @Test
    @DisplayName("同一输入连续算两次，结果完全相同")
    void idempotent() {
        assertEquals(calculator.calculate(golden(), FROM, TO),
                calculator.calculate(golden(), FROM, TO));
    }

    @Test
    @DisplayName("低血糖事件按 120 分钟归并")
    void hypoEventsMerged() {
        List<GlucoseReading> rs = List.of(
                r(3.6, 1, 8 * 60),        // 08:00
                r(3.5, 1, 8 * 60 + 45),   // 08:45  与前一条隔 45 分钟  -> 同一事件
                r(3.8, 1, 10 * 60 + 30),  // 10:30  隔 105 分钟          -> 同一事件
                r(3.7, 1, 14 * 60)        // 14:00  隔 210 分钟          -> 新事件
        );
        BloodGlucoseMetrics m = calculator.calculate(rs, FROM, TO);

        assertEquals(2, m.getHypoEventCount());
        assertClose(100.0, m.getTbrPercent());   // 4 条全部 <3.9
        assertEquals(0, m.getSevereHypoCount()); // 都 >=3.0
        assertFalse(m.isEnough());            // n=4 < 5
        assertNull(m.getGmiPercent());        // 样本不足，GMI 不给
    }

    @Test
    @DisplayName("脏数据被丢弃，且不影响其他指标")
    void dirtyDataDropped() {
        List<GlucoseReading> rs = new ArrayList<>(List.of(
                r(5.0, 1, 0), r(6.0, 1, 60), r(7.0, 1, 120), r(8.0, 1, 180), r(9.0, 1, 240)));
        rs.add(new GlucoseReading(null, 1, BASE.plusMinutes(300)));   // 值为 null
        rs.add(new GlucoseReading(0.0, 1, BASE.plusMinutes(360)));    // 0
        rs.add(new GlucoseReading(-1.0, 1, BASE.plusMinutes(420)));   // 负数
        rs.add(new GlucoseReading(40.0, 1, BASE.plusMinutes(480)));   // 超出 33.3
        rs.add(new GlucoseReading(6.5, 1, null));                     // 没有测量时间

        BloodGlucoseMetrics m = calculator.calculate(rs, FROM, TO);

        assertEquals(5, m.getSampleCount());
        assertEquals(5, m.getDroppedCount());
        assertTrue(m.isEnough());
        assertClose(7.0, m.getMeanGlucose());     // (5+6+7+8+9)/5
        assertClose(100.0, m.getTirPercent());
    }

    @Test
    @DisplayName("窗口外记录被忽略，也不算脏数据（左闭右开）")
    void outsideWindowIgnored() {
        List<GlucoseReading> rs = List.of(
                new GlucoseReading(5.0, 1, FROM.minusDays(1)),   // 太早
                new GlucoseReading(6.0, 1, FROM.plusDays(1)),    // 窗口内
                new GlucoseReading(7.0, 1, TO)                   // 正好等于 TO -> 不算
        );

        BloodGlucoseMetrics m = calculator.calculate(rs, FROM, TO);

        assertEquals(1, m.getSampleCount());
        assertEquals(0, m.getDroppedCount());
        assertClose(6.0, m.getMeanGlucose());
    }


    @Test
    @DisplayName("归并窗口边界：恰好 120 分钟算同一事件，121 分钟算新事件")
    void hypoMergeBoundary() {
        List<GlucoseReading> rs = List.of(
                r(3.6, 1, 8 * 60),          // 08:00
                r(3.5, 1, 10 * 60),         // 10:00  恰好 120 分钟 -> 同一事件
                r(3.4, 1, 10 * 60 + 121)    // 12:01  隔 121 分钟    -> 新事件
        );
        BloodGlucoseMetrics m = calculator.calculate(rs, FROM, TO);

        assertEquals(2, m.getHypoEventCount());
    }

    @Test
    @DisplayName("严重低血糖计数，且 n=5 恰好样本充足")
    void severeHypoCounted() {
        List<GlucoseReading> rs = List.of(
                r(2.8, 1, 0), r(3.5, 1, 60), r(6.0, 1, 120), r(2.9, 1, 180), r(7.0, 1, 240));

        BloodGlucoseMetrics m = calculator.calculate(rs, FROM, TO);

        assertEquals(2, m.getSevereHypoCount());   // 2.8、2.9
        assertClose(60.0, m.getTbrPercent());      // 3/5 低于 3.9
        assertTrue(m.isEnough());                  // 恰好 5 条 -> 充足
        assertNotNull(m.getGmiPercent());          // 恰好够算 GMI
    }

    @Test
    @DisplayName("中间回到范围内会打断事件")
    void normalReadingBreaksEvent() {
        List<GlucoseReading> rs = List.of(
                r(3.6, 1, 8 * 60),    // 08:00 低
                r(6.0, 1, 9 * 60),    // 09:00 回到范围内 -> 事件结束
                r(3.5, 1, 10 * 60)    // 10:00 低 -> 新事件（虽然只隔 60 分钟）
        );

        BloodGlucoseMetrics m = calculator.calculate(rs, FROM, TO);

        assertEquals(2, m.getHypoEventCount());
    }
}
