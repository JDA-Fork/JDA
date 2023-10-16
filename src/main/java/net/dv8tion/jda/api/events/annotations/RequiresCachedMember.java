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

package net.dv8tion.jda.api.events.annotations;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import java.lang.annotation.*;

/**
 * Annotation used by events, specifying that a cached member is required for the event to fire for said member.
 *
 * <p>There are multiple ways a member/user would be cached,
 * the prerequisite being that the {@link MemberCachePolicy} needs to allow it to be cached first.
 * <br>Assuming the cache policy allows a member to be cached, the member will be loaded in the cache when either:
 * <ul>
 *     <li>JDA loads it on startup, using {@link ChunkingFilter}</li>
 *     <li>It is loaded explicitly, using {@link Guild#retrieveMemberById(long)} for example</li>
 *     <li>An event from the member is received, such as {@link SlashCommandInteractionEvent} for example</li>
 * </ul>
 *
 * @see MemberCachePolicy
 * @see ChunkingFilter
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresCachedMember
{
}
