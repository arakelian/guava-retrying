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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * A builder used to configure and create a {@link Retryer}.
 *
 * @author JB
 * @author Jason Dunkelberger (dirkraft)
 */
@SuppressWarnings("WeakerAccess")
public class RetryerBuilder {
    private static final class ExceptionClassPredicate implements Predicate<Attempt<?>> {

        private final Class<? extends Throwable> exceptionClass;

        ExceptionClassPredicate(final Class<? extends Throwable> exceptionClass) {
            this.exceptionClass = exceptionClass;
        }

        @Override
        public boolean test(final Attempt<?> attempt) {
            return attempt.hasException()
                    && exceptionClass.isAssignableFrom(attempt.getException().getClass());
        }
    }

    private static final class ExceptionPredicate implements Predicate<Attempt<?>> {

        private final Predicate<Throwable> delegate;

        ExceptionPredicate(final Predicate<Throwable> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean test(final Attempt<?> attempt) {
            return attempt.hasException() && delegate.test(attempt.getException());
        }
    }

    private static final class ResultPredicate<T> implements Predicate<Attempt<?>> {

        private final Predicate<T> delegate;

        ResultPredicate(final Predicate<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean test(final Attempt<?> attempt) {
            if (!attempt.hasResult()) {
                return false;
            }
            try {
                @SuppressWarnings("unchecked")
                final T result = (T) attempt.getResult();
                return delegate.test(result);
            } catch (final ClassCastException e) {
                return false;
            }
        }
    }

    public static RetryerBuilder newBuilder() {
        return new RetryerBuilder();
    }

    private AttemptTimeLimiter attemptTimeLimiter;
    private StopStrategy stopStrategy;

    private WaitStrategy waitStrategy;

    private BlockStrategy blockStrategy;

    private final List<Predicate<Attempt<?>>> retryPredicates = Lists.newArrayList();

    private final List<RetryListener> listeners = new ArrayList<>();

    private RetryerBuilder() {
    }

    /**
     * Builds the retryer.
     *
     * @return the built retryer.
     */
    public Retryer build() {
        final AttemptTimeLimiter theAttemptTimeLimiter = attemptTimeLimiter == null
                ? AttemptTimeLimiters.noTimeLimit()
                : attemptTimeLimiter;
        final StopStrategy theStopStrategy = stopStrategy == null ? StopStrategies.neverStop() : stopStrategy;
        final WaitStrategy theWaitStrategy = waitStrategy == null ? WaitStrategies.noWait() : waitStrategy;
        final BlockStrategy theBlockStrategy = blockStrategy == null ? BlockStrategies.threadSleepStrategy()
                : blockStrategy;

        return new Retryer(theAttemptTimeLimiter, theStopStrategy, theWaitStrategy, theBlockStrategy,
                retryPredicates, listeners);
    }

    /**
     * Configures the retryer to retry if an exception (i.e. any <code>Exception</code> or subclass
     * of <code>Exception</code>) is thrown by the call.
     *
     * @return <code>this</code>
     */
    public RetryerBuilder retryIfException() {
        retryPredicates.add(new ExceptionClassPredicate(Exception.class));
        return this;
    }

    /**
     * Configures the retryer to retry if an exception satisfying the given predicate is thrown by
     * the call.
     *
     * @param exceptionPredicate
     *            the predicate which causes a retry if satisfied
     * @return <code>this</code>
     */
    public RetryerBuilder retryIfException(@Nonnull final Predicate<Throwable> exceptionPredicate) {
        Preconditions.checkNotNull(exceptionPredicate, "exceptionPredicate may not be null");
        retryPredicates.add(new ExceptionPredicate(exceptionPredicate));
        return this;
    }

    /**
     * Configures the retryer to retry if an exception of the given class (or subclass of the given
     * class) is thrown by the call.
     *
     * @param exceptionClass
     *            the type of the exception which should cause the retryer to retry
     * @return <code>this</code>
     */
    public RetryerBuilder retryIfExceptionOfType(@Nonnull final Class<? extends Throwable> exceptionClass) {
        Preconditions.checkNotNull(exceptionClass, "exceptionClass may not be null");
        retryPredicates.add(new ExceptionClassPredicate(exceptionClass));
        return this;
    }

    /**
     * Configures the retryer to retry if the result satisfies the given predicate.
     *
     * @param <T>
     *            The type of object tested by the predicate
     * @param resultPredicate
     *            a predicate applied to the result, and which causes the retryer to retry if the
     *            predicate is satisfied
     * @return <code>this</code>
     */
    public <T> RetryerBuilder retryIfResult(@Nonnull final Predicate<T> resultPredicate) {
        Preconditions.checkNotNull(resultPredicate, "resultPredicate may not be null");
        retryPredicates.add(new ResultPredicate<>(resultPredicate));
        return this;
    }

    /**
     * Configures the retryer to retry if a runtime exception (i.e. any
     * <code>RuntimeException</code> or subclass of <code>RuntimeException</code>) is thrown by the
     * call.
     *
     * @return <code>this</code>
     */
    public RetryerBuilder retryIfRuntimeException() {
        retryPredicates.add(new ExceptionClassPredicate(RuntimeException.class));
        return this;
    }

    /**
     * Configures the retryer to limit the duration of any particular attempt by the given duration.
     *
     * @param attemptTimeLimiter
     *            to apply to each attempt
     * @return <code>this</code>
     */
    public RetryerBuilder withAttemptTimeLimiter(@Nonnull final AttemptTimeLimiter attemptTimeLimiter) {
        Preconditions.checkNotNull(attemptTimeLimiter);
        this.attemptTimeLimiter = attemptTimeLimiter;
        return this;
    }

    /**
     * Sets the block strategy used to decide how to block between retry attempts. The default
     * strategy is to use Thread#sleep().
     *
     * @param blockStrategy
     *            the strategy used to decide how to block between retry attempts
     * @return <code>this</code>
     * @throws IllegalStateException
     *             if a block strategy has already been set.
     */
    public RetryerBuilder withBlockStrategy(@Nonnull final BlockStrategy blockStrategy)
            throws IllegalStateException {
        Preconditions.checkNotNull(blockStrategy, "blockStrategy may not be null");
        Preconditions.checkState(
                this.blockStrategy == null,
                "a block strategy has already been set %s",
                this.blockStrategy);
        this.blockStrategy = blockStrategy;
        return this;
    }

    /**
     * Adds a listener that will be notified of each attempt that is made
     *
     * @param listener
     *            Listener to add
     * @return <code>this</code>
     */
    public RetryerBuilder withRetryListener(@Nonnull final RetryListener listener) {
        Preconditions.checkNotNull(listener, "listener may not be null");
        listeners.add(listener);
        return this;
    }

    /**
     * Sets the stop strategy used to decide when to stop retrying. The default strategy is to not
     * stop at all .
     *
     * @param stopStrategy
     *            the strategy used to decide when to stop retrying
     * @return <code>this</code>
     * @throws IllegalStateException
     *             if a stop strategy has already been set.
     */
    public RetryerBuilder withStopStrategy(@Nonnull final StopStrategy stopStrategy)
            throws IllegalStateException {
        Preconditions.checkNotNull(stopStrategy, "stopStrategy may not be null");
        Preconditions.checkState(
                this.stopStrategy == null,
                "a stop strategy has already been set %s",
                this.stopStrategy);
        this.stopStrategy = stopStrategy;
        return this;
    }

    /**
     * Sets the wait strategy used to decide how long to sleep between failed attempts. The default
     * strategy is to retry immediately after a failed attempt.
     *
     * @param waitStrategy
     *            the strategy used to sleep between failed attempts
     * @return <code>this</code>
     * @throws IllegalStateException
     *             if a wait strategy has already been set.
     */
    public RetryerBuilder withWaitStrategy(@Nonnull final WaitStrategy waitStrategy)
            throws IllegalStateException {
        Preconditions.checkNotNull(waitStrategy, "waitStrategy may not be null");
        Preconditions.checkState(
                this.waitStrategy == null,
                "a wait strategy has already been set %s",
                this.waitStrategy);
        this.waitStrategy = waitStrategy;
        return this;
    }
}
