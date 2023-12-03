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

package net.dv8tion.jda.api.events.entitlement;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Entitlement;

import javax.annotation.Nonnull;

/**
 * Indicates that a user's entitlement was deleted. Entitlement deletions are infrequent, and occur when:
 * <ul>
 *     <li>Discord issues a refund for a subscription</li>
 *     <li>Discord removes an entitlement from a user via internal tooling</li>
 * </ul>
 * <p><b>Notice</b><br>
 * Entitlements are not deleted when they expire.
 * <br><br>
 * The ending date is updated for active subscriptions at the end of a billing period, to indicate a renewal.
 * When a subscription is cancelled, the ending date will not be updated again as it is not renewed for the next billing period.
 * Cancellation does not cause an immediate event and cancelled subscriptions do not delete an existing entitlement, since it is still active for the current billing period.
 *
 * @see #getEntitlement()
 * @see EntitlementUpdateEvent
 */
public class EntitlementDeleteEvent extends GenericEntitlementEvent
{
    public EntitlementDeleteEvent(@Nonnull JDA api, long responseNumber, @Nonnull Entitlement entitlement)
    {
        super(api, responseNumber, entitlement);
    }
}
