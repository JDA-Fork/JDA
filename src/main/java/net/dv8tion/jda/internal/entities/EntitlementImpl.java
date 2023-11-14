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

package net.dv8tion.jda.internal.entities;

import net.dv8tion.jda.api.entities.Entitlement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.OffsetDateTime;

public class EntitlementImpl implements Entitlement
{
    private long id;
    private long skuId;
    private long applicationId;
    private Long userId;
    private Long guildId;
    private int type;
    private boolean deleted;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;

    public EntitlementImpl(long id, long skuId, long applicationId, Long userId, Long guildId, int type, boolean deleted, @Nullable OffsetDateTime startsAt, @Nullable OffsetDateTime endsAt)
    {
        this.id = id;
        this.skuId = skuId;
        this.applicationId = applicationId;
        this.userId = userId;
        this.guildId = guildId;
        this.type = type;
        this.deleted = deleted;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    @Override
    public long getIdLong()
    {
        return id;
    }

    @Override
    public long getSkuIdLong()
    {
        return skuId;
    }

    @Nonnull
    @Override
    public String getSkuId()
    {
        return Long.toUnsignedString(skuId);
    }

    @Override
    public long getApplicationIdLong()
    {
        return applicationId;
    }

    @Nonnull
    @Override
    public String getApplicationId()
    {
        return Long.toUnsignedString(applicationId);
    }

    @Nullable
    @Override
    public Long getUserIdLong()
    {
        return userId;
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public String getUserId()
    {
        if (userId == null)
        {
            return null;
        }

        return Long.toUnsignedString(userId);
    }

    @Nullable
    @Override
    public Long getGuildIdLong()
    {
        return guildId;
    }

    @Nullable
    @Override
    public String getGuildId()
    {
        if (guildId == null)
        {
            return null;
        }

        return Long.toUnsignedString(guildId);
    }

    @Override
    public int getType()
    {
        return type;
    }

    @Override
    public boolean getDeleted()
    {
        return deleted;
    }

    @Nullable
    @Override
    public OffsetDateTime getStartsAt()
    {
        return startsAt;
    }

    @Nullable
    @Override
    public OffsetDateTime getEndsAt()
    {
        return endsAt;
    }
}
