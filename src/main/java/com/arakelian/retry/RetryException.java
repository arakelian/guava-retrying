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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * An exception indicating that none of the attempts of the {@link Retryer} succeeded. If the last
 * {@link Attempt} resulted in an Exception, it is set as the cause of the {@link RetryException}.
 *
 * @author JB
 */
@SuppressWarnings("WeakerAccess")
@Immutable
public final class RetryException extends Exception {

    private final Attempt<?> lastFailedAttempt;

    /**
     * If the last {@link Attempt} had an Exception, ensure it is available in the stack trace.
     *
     * @param message
     *            Exception description to be added to the stack trace
     * @param attempt
     *            what happened the last time we failed
     */
    private RetryException(final String message, final Attempt<?> attempt) {
        super(message, attempt.hasException() ? attempt.getException() : null);
        this.lastFailedAttempt = attempt;
    }

    /**
     * If the last {@link Attempt} had an Exception, ensure it is available in the stack trace.
     *
     * @param attempt
     *            what happened the last time we failed
     */
    RetryException(@Nonnull final Attempt<?> attempt) {
        this("Retrying failed to complete successfully after " + attempt.getAttemptNumber() + " attempts.",
                attempt);
    }

    /**
     * Returns the last failed attempt
     *
     * @return the last failed attempt
     */
    public Attempt<?> getLastFailedAttempt() {
        return lastFailedAttempt;
    }

    /**
     * Returns the number of failed attempts
     *
     * @return the number of failed attempts
     */
    public int getNumberOfFailedAttempts() {
        return lastFailedAttempt.getAttemptNumber();
    }
}
