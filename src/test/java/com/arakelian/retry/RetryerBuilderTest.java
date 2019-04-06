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

package com.arakelian.retry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.arakelian.retry.Attempt;
import com.arakelian.retry.BlockStrategy;
import com.arakelian.retry.RetryException;
import com.arakelian.retry.RetryListener;
import com.arakelian.retry.Retryer;
import com.arakelian.retry.RetryerBuilder;
import com.arakelian.retry.StopStrategies;
import com.arakelian.retry.WaitStrategies;
import com.arakelian.retry.Retryer.RetryerCallable;

public class RetryerBuilderTest {

    @Test
    public void testInterruption() throws Exception {
        final AtomicBoolean result = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(1);
        final Runnable r = () -> {
            final Retryer retryer = RetryerBuilder.newBuilder()
                    .withWaitStrategy(WaitStrategies.fixedWait(1000L, TimeUnit.MILLISECONDS))
                    .retryIfResult(Objects::isNull).build();
            try {
                retryer.call(alwaysNull(latch));
                fail("Exception expected");
            } catch (final InterruptedException e) {
                result.set(true);
            } catch (final Exception e) {
                System.out.println("Unexpected exception in test runnable: " + e);
                e.printStackTrace();
            }
        };
        final Thread t = new Thread(r);
        t.start();
        latch.countDown();
        t.interrupt();
        t.join();
        assertTrue(result.get());
    }

    @Test
    public void testMultipleRetryConditions() throws Exception {
        Callable<Boolean> callable = notNullResultOrIOExceptionOrRuntimeExceptionAfter5Attempts();
        Retryer retryer = RetryerBuilder.newBuilder().retryIfResult(Objects::isNull)
                .retryIfExceptionOfType(IOException.class).retryIfRuntimeException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(3)).build();
        try {
            retryer.call(callable);
            fail("Exception expected");
        } catch (final RetryException ignored) {
        }

        callable = notNullResultOrIOExceptionOrRuntimeExceptionAfter5Attempts();
        retryer = RetryerBuilder.newBuilder().retryIfResult(Objects::isNull)
                .retryIfExceptionOfType(IOException.class).retryIfRuntimeException().build();
        assertTrue(retryer.call(callable));
    }

    @Test
    public void testMultipleRetryListeners() throws Exception {
        final Callable<Boolean> callable = () -> true;

        final AtomicBoolean listenerOne = new AtomicBoolean(false);
        final AtomicBoolean listenerTwo = new AtomicBoolean(false);

        final Retryer retryer = RetryerBuilder.newBuilder()
                .withRetryListener(attempt -> listenerOne.set(true))
                .withRetryListener(attempt -> listenerTwo.set(true)).build();

        assertTrue(retryer.call(callable));
        assertTrue(listenerOne.get());
        assertTrue(listenerTwo.get());
    }

    @Test
    public void testRetryIfException() throws Exception {
        Callable<Boolean> callable = noIOExceptionAfter5Attempts();
        Retryer retryer = RetryerBuilder.newBuilder().retryIfException().build();
        final boolean result = retryer.call(callable);
        assertTrue(result);

        callable = noIOExceptionAfter5Attempts();
        retryer = RetryerBuilder.newBuilder().retryIfException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(3)).build();
        try {
            retryer.call(callable);
            fail("Exception expected");
        } catch (final RetryException ignored) {
        }

        callable = noIllegalStateExceptionAfter5Attempts();
        retryer = RetryerBuilder.newBuilder().retryIfException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(3)).build();
        try {
            retryer.call(callable);
            fail("Exception expected");
        } catch (final RetryException ignored) {
        }
    }

    @Test
    public void testRetryIfExceptionOfType() throws Exception {
        Callable<Boolean> callable = noIOExceptionAfter5Attempts();
        Retryer retryer = RetryerBuilder.newBuilder().retryIfExceptionOfType(IOException.class).build();
        assertTrue(retryer.call(callable));

        callable = noIllegalStateExceptionAfter5Attempts();
        try {
            retryer.call(callable);
            fail("IllegalStateException expected");
        } catch (final RetryException ignored) {
        }

        callable = noIOExceptionAfter5Attempts();
        retryer = RetryerBuilder.newBuilder().retryIfExceptionOfType(IOException.class)
                .withStopStrategy(StopStrategies.stopAfterAttempt(3)).build();
        try {
            retryer.call(callable);
            fail("Exception expected");
        } catch (final RetryException ignored) {
        }
    }

    @Test
    public void testRetryIfExceptionWithPredicate() throws Exception {
        Callable<Boolean> callable = noIOExceptionAfter5Attempts();
        Retryer retryer = RetryerBuilder.newBuilder().retryIfException(t -> t instanceof IOException).build();
        assertTrue(retryer.call(callable));

        callable = noIllegalStateExceptionAfter5Attempts();
        try {
            retryer.call(callable);
            fail("ExecutionException expected");
        } catch (final RetryException ignored) {
        }

        callable = noIOExceptionAfter5Attempts();
        retryer = RetryerBuilder.newBuilder().retryIfException(t -> t instanceof IOException)
                .withStopStrategy(StopStrategies.stopAfterAttempt(3)).build();
        try {
            retryer.call(callable);
            fail("Exception expected");
        } catch (final RetryException ignored) {
        }
    }

    @Test
    public void testRetryIfNotOfExceptionType() {
    }

    @Test
    public void testRetryIfResult() throws Exception {
        Callable<Boolean> callable = notNullAfter5Attempts();
        Retryer retryer = RetryerBuilder.newBuilder().retryIfResult(Objects::isNull).build();
        assertTrue(retryer.call(callable));

        callable = notNullAfter5Attempts();
        retryer = RetryerBuilder.newBuilder().retryIfResult(Objects::isNull)
                .withStopStrategy(StopStrategies.stopAfterAttempt(3)).build();
        try {
            retryer.call(callable);
            fail("Exception expected");
        } catch (final RetryException e) {
            assertEquals(3, e.getNumberOfFailedAttempts());
            assertTrue(e.getLastFailedAttempt().hasResult());
            assertNull(e.getLastFailedAttempt().getResult());
            assertNull(e.getCause());
        }
    }

    @Test
    public void testRetryIfRuntimeException() throws Exception {
        Callable<Boolean> callable = noIOExceptionAfter5Attempts();
        Retryer retryer = RetryerBuilder.newBuilder().retryIfRuntimeException().build();
        try {
            retryer.call(callable);
            fail("IOException expected");
        } catch (final RetryException ignored) {
        }

        callable = noIllegalStateExceptionAfter5Attempts();
        assertTrue(retryer.call(callable));

        callable = noIllegalStateExceptionAfter5Attempts();
        retryer = RetryerBuilder.newBuilder().retryIfRuntimeException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(3)).build();
        try {
            retryer.call(callable);
            fail("Exception expected");
        } catch (final RetryException ignored) {
        }
    }

    @Test
    public void testRetryListener_SuccessfulAttempt() throws Exception {
        final Map<Integer, Attempt> attempts = new HashMap<>();

        final RetryListener listener = attempt -> attempts.put(attempt.getAttemptNumber(), attempt);

        final Callable<Boolean> callable = notNullAfter5Attempts();

        final Retryer retryer = RetryerBuilder.newBuilder().retryIfResult(Objects::isNull)
                .withRetryListener(listener).build();
        assertTrue(retryer.call(callable));

        assertEquals(6, attempts.size());

        assertResultAttempt(attempts.get(1), null);
        assertResultAttempt(attempts.get(2), null);
        assertResultAttempt(attempts.get(3), null);
        assertResultAttempt(attempts.get(4), null);
        assertResultAttempt(attempts.get(5), null);
        assertResultAttempt(attempts.get(6), true);
    }

    @Test
    public void testRetryListener_WithException() throws Exception {
        final Map<Integer, Attempt> attempts = new HashMap<>();

        final RetryListener listener = attempt -> attempts.put(attempt.getAttemptNumber(), attempt);

        final Callable<Boolean> callable = noIOExceptionAfter5Attempts();

        final Retryer retryer = RetryerBuilder.newBuilder().retryIfResult(Objects::isNull).retryIfException()
                .withRetryListener(listener).build();
        assertTrue(retryer.call(callable));

        assertEquals(6, attempts.size());

        assertExceptionAttempt(attempts.get(1), IOException.class);
        assertExceptionAttempt(attempts.get(2), IOException.class);
        assertExceptionAttempt(attempts.get(3), IOException.class);
        assertExceptionAttempt(attempts.get(4), IOException.class);
        assertExceptionAttempt(attempts.get(5), IOException.class);
        assertResultAttempt(attempts.get(6), true);
    }

    @Test
    public void testWhetherBuilderFailsForNullWaitStrategyWithCompositeStrategies() {
        try {
            RetryerBuilder.newBuilder().withWaitStrategy(WaitStrategies.join(null, null)).build();
            fail("Exepcted to fail for null wait strategy");
        } catch (final IllegalStateException exception) {
            assertTrue(exception.getMessage().contains("Cannot have a null wait strategy"));
        }
    }

    @Test
    public void testWithBlockStrategy() throws Exception {
        final Callable<Boolean> callable = notNullAfter5Attempts();
        final AtomicInteger counter = new AtomicInteger();
        final BlockStrategy blockStrategy = sleepTime -> counter.incrementAndGet();

        final Retryer retryer = RetryerBuilder.newBuilder().withBlockStrategy(blockStrategy)
                .retryIfResult(Objects::isNull).build();
        final int retryCount = 5;
        final boolean result = retryer.call(callable);
        assertTrue(result);
        assertEquals(counter.get(), retryCount);
    }

    @Test
    public void testWithMoreThanOneWaitStrategyOneBeingFixed() throws Exception {
        final Callable<Boolean> callable = notNullAfter5Attempts();
        final Retryer retryer = RetryerBuilder.newBuilder()
                .withWaitStrategy(
                        WaitStrategies.join(
                                WaitStrategies.fixedWait(50L, TimeUnit.MILLISECONDS),
                                WaitStrategies.fibonacciWait(10, Long.MAX_VALUE, TimeUnit.MILLISECONDS)))
                .retryIfResult(Objects::isNull).build();
        final long start = System.currentTimeMillis();
        final boolean result = retryer.call(callable);
        assertTrue(System.currentTimeMillis() - start >= 370L);
        assertTrue(result);
    }

    @Test
    public void testWithMoreThanOneWaitStrategyOneBeingIncremental() throws Exception {
        final Callable<Boolean> callable = notNullAfter5Attempts();
        final Retryer retryer = RetryerBuilder.newBuilder().withWaitStrategy(
                WaitStrategies.join(
                        WaitStrategies
                                .incrementingWait(10L, TimeUnit.MILLISECONDS, 10L, TimeUnit.MILLISECONDS),
                        WaitStrategies.fibonacciWait(10, Long.MAX_VALUE, TimeUnit.MILLISECONDS)))
                .retryIfResult(Objects::isNull).build();
        final long start = System.currentTimeMillis();
        final boolean result = retryer.call(callable);
        assertTrue(System.currentTimeMillis() - start >= 270L);
        assertTrue(result);
    }

    @Test
    public void testWithStopStrategy() throws Exception {
        final Callable<Boolean> callable = notNullAfter5Attempts();
        final Retryer retryer = RetryerBuilder.newBuilder()
                .withStopStrategy(StopStrategies.stopAfterAttempt(3)).retryIfResult(Objects::isNull).build();
        try {
            retryer.call(callable);
            fail("RetryException expected");
        } catch (final RetryException e) {
            assertEquals(3, e.getNumberOfFailedAttempts());
        }
    }

    @Test
    public void testWithWaitStrategy() throws Exception {
        final Callable<Boolean> callable = notNullAfter5Attempts();
        final Retryer retryer = RetryerBuilder.newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(50L, TimeUnit.MILLISECONDS))
                .retryIfResult(Objects::isNull).build();
        final long start = System.currentTimeMillis();
        final boolean result = retryer.call(callable);
        assertTrue(System.currentTimeMillis() - start >= 250L);
        assertTrue(result);
    }

    @Test
    public void testWrap() throws Exception {
        final Callable<Boolean> callable = notNullAfter5Attempts();
        final Retryer retryer = RetryerBuilder.newBuilder().retryIfResult(Objects::isNull).build();
        final RetryerCallable<Boolean> wrapped = retryer.wrap(callable);
        assertTrue(wrapped.call());
    }

    private Callable<Boolean> alwaysNull(final CountDownLatch latch) {
        return () -> {
            latch.countDown();
            return null;
        };
    }

    private void assertExceptionAttempt(final Attempt actualAttempt, final Class<?> expectedExceptionClass) {
        assertFalse(actualAttempt.hasResult());
        assertTrue(actualAttempt.hasException());
        assertTrue(expectedExceptionClass.isInstance(actualAttempt.getException()));
    }

    private void assertResultAttempt(final Attempt actualAttempt, final Object expectedResult) {
        assertFalse(actualAttempt.hasException());
        assertTrue(actualAttempt.hasResult());
        assertEquals(expectedResult, actualAttempt.getResult());
    }

    private Callable<Boolean> noIllegalStateExceptionAfter5Attempts() {
        return new Callable<>() {
            int counter = 0;

            @Override
            public Boolean call() throws Exception {
                if (counter < 5) {
                    counter++;
                    throw new IllegalStateException();
                }
                return true;
            }
        };
    }

    private Callable<Boolean> noIOExceptionAfter5Attempts() {
        return new Callable<>() {
            int counter = 0;

            @Override
            public Boolean call() throws IOException {
                if (counter < 5) {
                    counter++;
                    throw new IOException();
                }
                return true;
            }
        };
    }

    private Callable<Boolean> notNullAfter5Attempts() {
        return new Callable<>() {
            int counter = 0;

            @Override
            public Boolean call() throws Exception {
                if (counter < 5) {
                    counter++;
                    return null;
                }
                return true;
            }
        };
    }

    private Callable<Boolean> notNullResultOrIOExceptionOrRuntimeExceptionAfter5Attempts() {
        return new Callable<>() {
            int counter = 0;

            @Override
            public Boolean call() throws IOException {
                if (counter < 1) {
                    counter++;
                    return null;
                } else if (counter < 2) {
                    counter++;
                    throw new IOException();
                } else if (counter < 5) {
                    counter++;
                    throw new IllegalStateException();
                }
                return true;
            }
        };
    }
}
