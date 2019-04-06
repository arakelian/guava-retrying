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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;

/**
 * Factory class for instances of {@link AttemptTimeLimiter}
 *
 * @author Jason Dunkelberger (dirkraft)
 */
@SuppressWarnings("WeakerAccess")
public class AttemptTimeLimiters {

    @Immutable
    private static final class FixedAttemptTimeLimit implements AttemptTimeLimiter {

        /**
         * ExecutorService used when no ExecutorService is specified in the constructor
         */
        private static final ExecutorService defaultExecutorService = Executors.newCachedThreadPool();

        private final TimeLimiter timeLimiter;
        private final long duration;
        private final TimeUnit timeUnit;

        private FixedAttemptTimeLimit(
                @Nonnull final TimeLimiter timeLimiter,
                final long duration,
                @Nonnull final TimeUnit timeUnit) {
            Preconditions.checkNotNull(timeLimiter);
            Preconditions.checkNotNull(timeUnit);
            this.timeLimiter = timeLimiter;
            this.duration = duration;
            this.timeUnit = timeUnit;
        }

        FixedAttemptTimeLimit(final long duration, @Nonnull final TimeUnit timeUnit) {
            this(duration, timeUnit, defaultExecutorService);
        }

        FixedAttemptTimeLimit(
                final long duration,
                @Nonnull final TimeUnit timeUnit,
                @Nonnull final ExecutorService executorService) {
            this(SimpleTimeLimiter.create(executorService), duration, timeUnit);
        }

        @Override
        public <T> T call(final Callable<T> callable) throws Exception {
            return timeLimiter.callWithTimeout(callable, duration, timeUnit);
        }
    }

    @Immutable
    private static final class NoAttemptTimeLimit implements AttemptTimeLimiter {
        @Override
        public <T> T call(final Callable<T> callable) throws Exception {
            return callable.call();
        }
    }

    /**
     * For control over thread management, it is preferable to offer an {@link ExecutorService}
     * through the other factory method, {@link #fixedTimeLimit(long, TimeUnit, ExecutorService)}.
     * All calls to this method use the same cached thread pool created by
     * {@link Executors#newCachedThreadPool()}. It is unbounded, meaning there is no limit to the
     * number of threads it will create. It will reuse idle threads if they are available, and idle
     * threads remain alive for 60 seconds.
     *
     * @param duration
     *            that an attempt may persist before being circumvented
     * @param timeUnit
     *            of the 'duration' arg
     * @return an {@link AttemptTimeLimiter} with a fixed time limit for each attempt
     */
    public static AttemptTimeLimiter fixedTimeLimit(final long duration, @Nonnull final TimeUnit timeUnit) {
        Preconditions.checkNotNull(timeUnit);
        return new FixedAttemptTimeLimit(duration, timeUnit);
    }

    /**
     * @param duration
     *            that an attempt may persist before being circumvented
     * @param timeUnit
     *            of the 'duration' arg
     * @param executorService
     *            used to enforce time limit
     * @return an {@link AttemptTimeLimiter} with a fixed time limit for each attempt
     */
    public static AttemptTimeLimiter fixedTimeLimit(
            final long duration,
            @Nonnull final TimeUnit timeUnit,
            @Nonnull final ExecutorService executorService) {
        Preconditions.checkNotNull(timeUnit);
        return new FixedAttemptTimeLimit(duration, timeUnit, executorService);
    }

    /**
     * @return an {@link AttemptTimeLimiter} impl which has no time limit
     */
    public static AttemptTimeLimiter noTimeLimit() {
        return new NoAttemptTimeLimit();
    }

    private AttemptTimeLimiters() {
    }
}
