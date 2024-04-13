/*
 * Copyright 2015 Austin Keener, Michael Ritter, Florian Spieß, and the JDA contributors
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

package net.dv8tion.jda.test.assertions.checks;

import org.junit.jupiter.api.function.ThrowingConsumer;

import java.time.Duration;

import static net.dv8tion.jda.test.ChecksHelper.notPositiveError;

public class DurationChecksAssertions extends AbstractChecksAssertions<Duration, DurationChecksAssertions>
{
    public DurationChecksAssertions(String name, ThrowingConsumer<Duration> callable)
    {
        super(name, callable);
    }

    public DurationChecksAssertions checksPositive()
    {
        throwsFor(Duration.ofSeconds(-1), notPositiveError(name));
        throwsFor(Duration.ZERO, notPositiveError(name));
        return this;
    }
}
