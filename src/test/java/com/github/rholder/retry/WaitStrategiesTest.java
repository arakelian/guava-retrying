/*
 * Copyright 2012-2015 Ray Holder
 * Modifications copyright 2017-2018 Robert Huffman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.rholder.retry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.Test;

import com.google.common.collect.Sets;

public class WaitStrategiesTest {

    public class RetryAfterException extends RuntimeException {
        private final long retryAfter = 29L;

        long getRetryAfter() {
            return retryAfter;
        }
    }

    @Test
    public void testExceptionWait() {
        final WaitStrategy exceptionWait = WaitStrategies
                .exceptionWait(RuntimeException.class, zeroSleepFunction());
        assertEquals(0L, exceptionWait.computeSleepTime(failedAttempt(42, 7227)));

        final WaitStrategy oneMinuteWait = WaitStrategies
                .exceptionWait(RuntimeException.class, oneMinuteSleepFunction());
        assertEquals(3600 * 1000L, oneMinuteWait.computeSleepTime(failedAttempt(42, 7227)));

        final WaitStrategy noMatchRetryAfterWait = WaitStrategies
                .exceptionWait(RetryAfterException.class, customSleepFunction());
        assertEquals(0L, noMatchRetryAfterWait.computeSleepTime(failedAttempt(42, 7227)));

        final WaitStrategy retryAfterWait = WaitStrategies
                .exceptionWait(RetryAfterException.class, customSleepFunction());
        final Attempt<Boolean> failedAttempt = new Attempt<>(new RetryAfterException(), 42, 7227L);
        assertEquals(29L, retryAfterWait.computeSleepTime(failedAttempt));
    }

    @Test
    public void testExponential() {
        final WaitStrategy exponentialWait = WaitStrategies.exponentialWait();
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(1, 0)) == 2);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(2, 0)) == 4);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(3, 0)) == 8);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(4, 0)) == 16);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(5, 0)) == 32);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(6, 0)) == 64);
    }

    @Test
    public void testExponentialWithMaximumWait() {
        final WaitStrategy exponentialWait = WaitStrategies.exponentialWait(40, TimeUnit.MILLISECONDS);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(1, 0)) == 2);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(2, 0)) == 4);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(3, 0)) == 8);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(4, 0)) == 16);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(5, 0)) == 32);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(6, 0)) == 40);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(7, 0)) == 40);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(8, 0)) == 40);
    }

    @Test
    public void testExponentialWithMultiplierAndMaximumWait() {
        final WaitStrategy exponentialWait = WaitStrategies
                .exponentialWait(1000, 50000, TimeUnit.MILLISECONDS);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(1, 0)) == 2000);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(2, 0)) == 4000);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(3, 0)) == 8000);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(4, 0)) == 16000);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(5, 0)) == 32000);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(6, 0)) == 50000);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(7, 0)) == 50000);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(8, 0)) == 50000);
    }

    @Test
    public void testFibonacci() {
        final WaitStrategy fibonacciWait = WaitStrategies.fibonacciWait();
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(1, 0L)) == 1L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(2, 0L)) == 1L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(3, 0L)) == 2L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(4, 0L)) == 3L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(5, 0L)) == 5L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(6, 0L)) == 8L);
    }

    @Test
    public void testFibonacciWithMaximumWait() {
        final WaitStrategy fibonacciWait = WaitStrategies.fibonacciWait(10L, TimeUnit.MILLISECONDS);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(1, 0L)) == 1L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(2, 0L)) == 1L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(3, 0L)) == 2L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(4, 0L)) == 3L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(5, 0L)) == 5L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(6, 0L)) == 8L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(7, 0L)) == 10L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(8, 0L)) == 10L);
    }

    @Test
    public void testFibonacciWithMultiplierAndMaximumWait() {
        final WaitStrategy fibonacciWait = WaitStrategies.fibonacciWait(1000L, 50000L, TimeUnit.MILLISECONDS);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(1, 0L)) == 1000L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(2, 0L)) == 1000L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(3, 0L)) == 2000L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(4, 0L)) == 3000L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(5, 0L)) == 5000L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(6, 0L)) == 8000L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(7, 0L)) == 13000L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(10, 0L)) == 50000L);
    }

    @Test
    public void testFixedWait() {
        final WaitStrategy fixedWait = WaitStrategies.fixedWait(1000L, TimeUnit.MILLISECONDS);
        assertEquals(1000L, fixedWait.computeSleepTime(failedAttempt(12, 6546L)));
    }

    @Test
    public void testIncrementingWait() {
        final WaitStrategy incrementingWait = WaitStrategies
                .incrementingWait(500L, TimeUnit.MILLISECONDS, 100L, TimeUnit.MILLISECONDS);
        assertEquals(500L, incrementingWait.computeSleepTime(failedAttempt(1, 6546L)));
        assertEquals(600L, incrementingWait.computeSleepTime(failedAttempt(2, 6546L)));
        assertEquals(700L, incrementingWait.computeSleepTime(failedAttempt(3, 6546L)));
    }

    @Test
    public void testNoWait() {
        final WaitStrategy noWait = WaitStrategies.noWait();
        assertEquals(0L, noWait.computeSleepTime(failedAttempt(18, 9879L)));
    }

    @Test
    public void testRandomWait() {
        final WaitStrategy randomWait = WaitStrategies
                .randomWait(1000L, TimeUnit.MILLISECONDS, 2000L, TimeUnit.MILLISECONDS);
        final Set<Long> times = Sets.newHashSet();
        times.add(randomWait.computeSleepTime(failedAttempt(1, 6546L)));
        times.add(randomWait.computeSleepTime(failedAttempt(1, 6546L)));
        times.add(randomWait.computeSleepTime(failedAttempt(1, 6546L)));
        times.add(randomWait.computeSleepTime(failedAttempt(1, 6546L)));
        assertTrue(times.size() > 1); // if not, the random is not random
        for (final long time : times) {
            assertTrue(time >= 1000L);
            assertTrue(time <= 2000L);
        }
    }

    @Test
    public void testRandomWaitWithoutMinimum() {
        final WaitStrategy randomWait = WaitStrategies.randomWait(2000L, TimeUnit.MILLISECONDS);
        final Set<Long> times = Sets.newHashSet();
        times.add(randomWait.computeSleepTime(failedAttempt(1, 6546L)));
        times.add(randomWait.computeSleepTime(failedAttempt(1, 6546L)));
        times.add(randomWait.computeSleepTime(failedAttempt(1, 6546L)));
        times.add(randomWait.computeSleepTime(failedAttempt(1, 6546L)));
        assertTrue(times.size() > 1); // if not, the random is not random
        for (final long time : times) {
            assertTrue(time >= 0L);
            assertTrue(time <= 2000L);
        }
    }

    private Function<RetryAfterException, Long> customSleepFunction() {
        return RetryAfterException::getRetryAfter;
    }

    private Attempt<Boolean> failedAttempt(final int attemptNumber, final long delaySinceFirstAttempt) {
        return new Attempt<>(new RuntimeException(), attemptNumber, delaySinceFirstAttempt);
    }

    private Function<RuntimeException, Long> oneMinuteSleepFunction() {
        return input -> 3600 * 1000L;
    }

    private Function<RuntimeException, Long> zeroSleepFunction() {
        return input -> 0L;
    }
}
