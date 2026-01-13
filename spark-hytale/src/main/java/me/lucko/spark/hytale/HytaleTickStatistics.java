/*
 * This file is part of spark.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.lucko.spark.hytale;

import com.hypixel.hytale.metrics.metric.HistoricMetric;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.util.thread.TickingThread;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import me.lucko.spark.common.monitor.tick.TickStatistics;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Provides tick statistics by reading from {@link World#getBufferedTickLengthMetricSet()}.
 */
public class HytaleTickStatistics implements TickStatistics {

    @Override
    public int gameTargetTps() {
        return TickingThread.TPS;
    }

    @Override
    public double tps5Sec() {
        // close enough, easier to make use of the buffered tick length metric set periods
        return tps10Sec();
    }

    @Override
    public double tps10Sec() {
        return calculateAverageTps(BufferedTickLengthMetricSetPeriod.SECONDS_10);
    }

    @Override
    public double tps1Min() {
        return calculateAverageTps(BufferedTickLengthMetricSetPeriod.MINUTES_1);
    }

    @Override
    public double tps5Min() {
        return calculateAverageTps(BufferedTickLengthMetricSetPeriod.MINUTES_5);
    }

    @Override
    public double tps15Min() {
        // hytale only stores 5 minutes of history, so that's the best we can do for now
        return tps5Min();
    }

    @Override
    public boolean isDurationSupported() {
        return true;
    }

    @Override
    public DoubleAverageInfo duration10Sec() {
        return new AggregatedMsptInfo(getActiveWorlds(), BufferedTickLengthMetricSetPeriod.SECONDS_10);
    }

    @Override
    public DoubleAverageInfo duration1Min() {
        return new AggregatedMsptInfo(getActiveWorlds(), BufferedTickLengthMetricSetPeriod.MINUTES_1);
    }

    @Override
    public DoubleAverageInfo duration5Min() {
        return new AggregatedMsptInfo(getActiveWorlds(), BufferedTickLengthMetricSetPeriod.MINUTES_5);
    }

    private static List<World> getActiveWorlds() {
        return Universe.get().getWorlds().values().stream()
                .filter(world -> world.isStarted() && world.getBufferedTickLengthMetricSet() != null)
                .collect(Collectors.toList());
    }

    private double calculateAverageTps(BufferedTickLengthMetricSetPeriod period) {
        List<World> worlds = getActiveWorlds();
        if (worlds.isEmpty()) {
            return TickingThread.TPS;
        }

        BufferedTickLengthMetricSetPeriod.checkWorldMetricPeriodsMatch(worlds);

        double totalTps = 0;
        for (World world : worlds) {
            totalTps += calculateWorldTps(world, period);
        }

        return totalTps / worlds.size();
    }

    private double calculateWorldTps(World world, BufferedTickLengthMetricSetPeriod period) {
        HistoricMetric metrics = world.getBufferedTickLengthMetricSet();
        double ticksProcessed = metrics.getTimestamps(period.ordinal()).length;
        if (ticksProcessed == 0) {
            return world.getTps();
        }

        return ticksProcessed / period.seconds();
    }

    private static class AggregatedMsptInfo implements DoubleAverageInfo {
        private static final double NANOS_PER_MILLI = TimeUnit.MILLISECONDS.toNanos(1);

        private final List<World> worlds;
        private final BufferedTickLengthMetricSetPeriod period;

        AggregatedMsptInfo(List<World> worlds, BufferedTickLengthMetricSetPeriod period) {
            this.worlds = worlds;
            this.period = period;
            BufferedTickLengthMetricSetPeriod.checkWorldMetricPeriodsMatch(this.worlds);
        }

        @Override
        public double mean() {
            if (this.worlds.isEmpty()) {
                return 0;
            }

            double total = 0;
            int count = 0;

            for (World world : this.worlds) {
                HistoricMetric metric = world.getBufferedTickLengthMetricSet();
                double avgNanos = metric.getAverage(this.period.ordinal());
                if (avgNanos > 0) {
                    total += avgNanos / NANOS_PER_MILLI;
                    count++;
                }
            }

            return count > 0 ? total / count : 0;
        }

        @Override
        public double max() {
            double max = 0;
            for (World world : this.worlds) {
                long maxNanos = world.getBufferedTickLengthMetricSet().calculateMax(this.period.ordinal());
                if (maxNanos > 0 && maxNanos < Long.MAX_VALUE) {
                    double ms = maxNanos / NANOS_PER_MILLI;
                    if (ms > max) {
                        max = ms;
                    }
                }
            }
            return max;
        }

        @Override
        public double min() {
            double min = Double.MAX_VALUE;
            for (World world : this.worlds) {
                long minNanos = world.getBufferedTickLengthMetricSet().calculateMin(this.period.ordinal());
                if (minNanos > 0 && minNanos < Long.MAX_VALUE) {
                    double ms = minNanos / NANOS_PER_MILLI;
                    if (ms < min) {
                        min = ms;
                    }
                }
            }
            return min == Double.MAX_VALUE ? 0 : min;
        }

        @Override
        public double percentile(double percentile) {
            if (percentile < 0 || percentile > 1) {
                throw new IllegalArgumentException("Invalid percentile " + percentile);
            }

            long[] samples = new long[0];
            for (World world : this.worlds) {
                long[] values = world.getBufferedTickLengthMetricSet().getValues(this.period.ordinal());
                samples = concat(samples, values);
            }

            if (samples.length == 0) {
                return 0;
            }

            Arrays.sort(samples);

            int rank = (int) Math.ceil(percentile * (samples.length - 1));
            long sampleNanos = samples[rank];
            return sampleNanos / NANOS_PER_MILLI;
        }

        public static long[] concat(long[] a1, long[] a2) {
            if (a1 != null && a1.length != 0) {
                if (a2 != null && a2.length != 0) {
                    long[] newArray = Arrays.copyOf(a1, a1.length + a2.length);
                    System.arraycopy(a2, 0, newArray, a1.length, a2.length);
                    return newArray;
                } else {
                    return a1;
                }
            } else {
                return a2;
            }
        }
    }

    /**
     * The periods that are used by Hytale's {@link World#getBufferedTickLengthMetricSet()}
     */
    private enum BufferedTickLengthMetricSetPeriod {
        SECONDS_10(10),
        MINUTES_1(60),
        MINUTES_5(60 * 5);

        private static final AtomicBoolean CHECKED = new AtomicBoolean(false);

        private final int seconds;
        private final long nanos;

        BufferedTickLengthMetricSetPeriod(int seconds) {
            this.seconds = seconds;
            this.nanos = TimeUnit.SECONDS.toNanos(seconds);
        }

        public int seconds() {
            return this.seconds;
        }

        public long nanos() {
            return this.nanos;
        }

        static void checkWorldMetricPeriodsMatch(List<World> worlds) {
            if (CHECKED.get() || worlds.isEmpty()) {
                return;
            }

            World world = worlds.getFirst();
            HistoricMetric metric = world.getBufferedTickLengthMetricSet();

            final long[] expectedPeriods = Arrays.stream(BufferedTickLengthMetricSetPeriod.values())
                    .mapToLong(BufferedTickLengthMetricSetPeriod::nanos)
                    .toArray();
            final long[] actualPeriods = metric.getPeriodsNanos();

            final boolean match = Arrays.equals(expectedPeriods, actualPeriods);
            if (CHECKED.compareAndSet(false, true) && !match) {
                System.err.println("[spark] warning: Hytale's BufferedTickLengthMetricSet periods do not match expected values.");
            }
        }
    }
}
