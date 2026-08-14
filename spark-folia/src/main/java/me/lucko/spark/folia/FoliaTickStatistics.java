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

package me.lucko.spark.folia;

import io.papermc.paper.threadedregions.ThreadedRegionizer;
import io.papermc.paper.threadedregions.ThreadedRegionizer.ThreadedRegion;
import io.papermc.paper.threadedregions.TickRegions.TickRegionData;
import io.papermc.paper.threadedregions.TickRegions.TickRegionSectionData;
import me.lucko.spark.api.statistic.StatisticWindow;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import me.lucko.spark.common.monitor.tick.TickStatistics;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class FoliaTickStatistics implements TickStatistics {
    private final Supplier<List<ThreadedRegion<TickRegionData, TickRegionSectionData>>> regionSupplier;
    private static final Method GET_TPS_DATA_METHOD;
    private static final Method GET_TIME_PER_TICK_METHOD;

    static {
        Method tpsDataMethod = null;
        Method timePerTickMethod = null;
        try {
            Class<?> tickReportDataClass = Class.forName("ca.spottedleaf.moonrise.common.time.TickData$TickReportData");
            tpsDataMethod = tickReportDataClass.getMethod("tpsData");
            timePerTickMethod = tickReportDataClass.getMethod("timePerTickData");
        } catch (Exception e) {
        }
        GET_TPS_DATA_METHOD = tpsDataMethod;
        GET_TIME_PER_TICK_METHOD = timePerTickMethod;
    }

    public FoliaTickStatistics(Server server) {
        this.regionSupplier = new WeakReferenceExpiringSupplier<>(() -> getRegions(server), 5, TimeUnit.MILLISECONDS);
    }

    @Override
    public int gameTargetTps() {
        return 20;
    }

    @Override
    public double tps5Sec() {
        return tps(StatisticWindow.TicksPerSecond.SECONDS_5);
    }

    @Override
    public double tps10Sec() {
        return tps(StatisticWindow.TicksPerSecond.SECONDS_10);
    }

    @Override
    public double tps1Min() {
        return tps(StatisticWindow.TicksPerSecond.MINUTES_1);
    }

    @Override
    public double tps5Min() {
        return tps(StatisticWindow.TicksPerSecond.MINUTES_5);
    }

    @Override
    public double tps15Min() {
        return tps(StatisticWindow.TicksPerSecond.MINUTES_15);
    }

    @Override
    public boolean isDurationSupported() {
        return true;
    }

    @Override
    public DoubleAverageInfo duration10Sec() {
        return mspt(StatisticWindow.MillisPerTick.SECONDS_10);
    }

    @Override
    public DoubleAverageInfo duration1Min() {
        return mspt(StatisticWindow.MillisPerTick.MINUTES_1);
    }

    @Override
    public DoubleAverageInfo duration5Min() {
        return mspt(StatisticWindow.MillisPerTick.MINUTES_5);
    }

    private static List<ThreadedRegion<TickRegionData, TickRegionSectionData>> getRegions(Server server) {
        List<ThreadedRegion<TickRegionData, TickRegionSectionData>> regions = new ArrayList<>();
        for (World world : server.getWorlds()) {
            ThreadedRegionizer<TickRegionData, TickRegionSectionData> regionizer = ((CraftWorld) world).getHandle().regioniser;
            regionizer.computeForAllRegions(regions::add);
        }
        return regions;
    }

    public double tps(StatisticWindow.TicksPerSecond window) {
        if (GET_TPS_DATA_METHOD == null) {
            return 20.0;
        }
        long nanoTime = System.nanoTime();
        return this.regionSupplier.get().stream()
                .map(region -> region.getData().getRegionSchedulingHandle())
                .map(handle -> switch (window) {
                    case SECONDS_5 -> handle.getTickReport5s(nanoTime);
                    case SECONDS_10 -> handle.getTickReport15s(nanoTime);
                    case MINUTES_1 -> handle.getTickReport1m(nanoTime);
                    case MINUTES_5 -> handle.getTickReport5m(nanoTime);
                    case MINUTES_15 -> handle.getTickReport15m(nanoTime);
                })
                .filter(Objects::nonNull)
                .mapToDouble(data -> {
                    try {
                        Object tpsData = GET_TPS_DATA_METHOD.invoke(data);
                        Method segmentAllMethod = tpsData.getClass().getMethod("segmentAll");
                        Object segmentAll = segmentAllMethod.invoke(tpsData);
                        Method averageMethod = segmentAll.getClass().getMethod("average");
                        return (double) averageMethod.invoke(segmentAll);
                    } catch (Exception e) {
                        return 20.0;
                    }
                })
                .average()
                .orElse(20.0);
    }

    public DoubleAverageInfo mspt(StatisticWindow.MillisPerTick window) {
        if (GET_TIME_PER_TICK_METHOD == null) {
            return new SegmentedDoubleAverageInfo(List.of());
        }
        long nanoTime = System.nanoTime();
        List<Object> averages = this.regionSupplier.get().stream()
                .map(region -> region.getData().getRegionSchedulingHandle())
                .map(handle -> switch (window) {
                    case SECONDS_10 -> handle.getTickReport15s(nanoTime);
                    case MINUTES_1 -> handle.getTickReport1m(nanoTime);
                    case MINUTES_5 -> handle.getTickReport5m(nanoTime);
                })
                .filter(Objects::nonNull)
                .map(data -> {
                    try {
                        return GET_TIME_PER_TICK_METHOD.invoke(data);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
        return new SegmentedDoubleAverageInfo(averages);
    }

    private record SegmentedDoubleAverageInfo(List<Object> averages) implements DoubleAverageInfo {

        @Override
        public double mean() {
            return this.averages.stream()
                    .mapToDouble(avg -> {
                        try {
                            Method segmentAllMethod = avg.getClass().getMethod("segmentAll");
                            Object segmentAll = segmentAllMethod.invoke(avg);
                            Method averageMethod = segmentAll.getClass().getMethod("average");
                            return (double) averageMethod.invoke(segmentAll) / 1.0E6;
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .average()
                    .orElse(0);
        }

        @Override
        public double max() {
            return this.averages.stream()
                    .mapToDouble(avg -> {
                        try {
                            Method segmentAllMethod = avg.getClass().getMethod("segmentAll");
                            Object segmentAll = segmentAllMethod.invoke(avg);
                            Method greatestMethod = segmentAll.getClass().getMethod("greatest");
                            return (double) greatestMethod.invoke(segmentAll) / 1.0E6;
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .max()
                    .orElse(0);
        }

        @Override
        public double min() {
            return this.averages.stream()
                    .mapToDouble(avg -> {
                        try {
                            Method segmentAllMethod = avg.getClass().getMethod("segmentAll");
                            Object segmentAll = segmentAllMethod.invoke(avg);
                            Method leastMethod = segmentAll.getClass().getMethod("least");
                            return (double) leastMethod.invoke(segmentAll) / 1.0E6;
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .min()
                    .orElse(0);
        }

        @Override
        public double percentile(double percentile) {
            if (percentile == 0.50d) {
                return this.averages.stream()
                        .mapToDouble(avg -> {
                            try {
                                Method segmentAllMethod = avg.getClass().getMethod("segmentAll");
                                Object segmentAll = segmentAllMethod.invoke(avg);
                                Method medianMethod = segmentAll.getClass().getMethod("median");
                                return (double) medianMethod.invoke(segmentAll) / 1.0E6;
                            } catch (Exception e) {
                                return 0;
                            }
                        })
                        .average()
                        .orElse(0);
            } else if (percentile == 0.95d) {
                return this.averages.stream()
                        .mapToDouble(avg -> {
                            try {
                                Method segment5PercentWorstMethod = avg.getClass().getMethod("segment5PercentWorst");
                                Object segment5PercentWorst = segment5PercentWorstMethod.invoke(avg);
                                Method averageMethod = segment5PercentWorst.getClass().getMethod("average");
                                return (double) averageMethod.invoke(segment5PercentWorst) / 1.0E6;
                            } catch (Exception e) {
                                return 0;
                            }
                        })
                        .average()
                        .orElse(0);
            }

            throw new UnsupportedOperationException("Unsupported percentile: " + percentile);
        }
    }

    private static final class WeakReferenceExpiringSupplier<T> implements Supplier<T> {
        private final Supplier<T> delegate;
        private final long durationNanos;
        private volatile WeakReference<T> ref = new WeakReference<>(null);
        private volatile long expiryTime = 0;

        WeakReferenceExpiringSupplier(Supplier<T> delegate, long duration, TimeUnit unit) {
            this.delegate = delegate;
            this.durationNanos = unit.toNanos(duration);
        }

        @Override
        public T get() {
            long now = System.nanoTime();
            T value = this.ref.get();
            if (value == null || now >= this.expiryTime) {
                value = this.delegate.get();
                this.ref = new WeakReference<>(value);
                this.expiryTime = now + this.durationNanos;
            }
            return value;
        }
    }
}
