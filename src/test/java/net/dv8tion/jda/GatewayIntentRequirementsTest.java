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

package net.dv8tion.jda;

import com.github.javaparser.ast.CompilationUnit;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.automod.AutoModExecutionEvent;
import net.dv8tion.jda.api.events.message.react.*;
import net.dv8tion.jda.api.events.user.update.UserUpdateActivitiesEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateActivityOrderEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateOnlineStatusEvent;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import java.io.IOException;
import java.util.*;

public class GatewayIntentRequirementsTest
{
    // Classes which will not be required to inherit intent requirements from supertypes
    private static final Set<Class<? extends GenericEvent>> IGNORE_INHERITANCE = new HashSet<>(Arrays.asList(
            MessageReactionRemoveEmojiEvent.class, //Extends GenericMessageEvent, but does not require the same intents
            MessageReactionRemoveAllEvent.class, //Extends GenericMessageEvent, but does not require the same intents
            UserUpdateActivityOrderEvent.class, // Does not require members
            UserUpdateActivitiesEvent.class, // Does not require members
            UserUpdateOnlineStatusEvent.class, // Does not require members
            GenericMessageReactionEvent.class, // Does not require messages
            MessageReactionRemoveEvent.class, // Does not require messages
            MessageReactionAddEvent.class // Does not require messages
    ));
    // Classes which have documented intents, but are optional, and thus not given by #fromEvents
    private static final Map<Class<? extends GenericEvent>, EnumSet<GatewayIntent>> OPTIONAL_INTENTS = new HashMap<Class<? extends GenericEvent>, EnumSet<GatewayIntent>>() {{
        put(AutoModExecutionEvent.class, EnumSet.of(GatewayIntent.MESSAGE_CONTENT));
    }};

    private static Reflections events;
    private static Map<Class<GenericEvent>, EnumSet<GatewayIntent>> intentsByClass;

    @BeforeAll
    static void setup() throws IOException, ClassNotFoundException
    {
        final List<CompilationUnit> compilationUnits = CacheFlagRequirementsTest.parseEventCompilationUnits();

        intentsByClass = CacheFlagRequirementsTest.getEnumEntriesByClass(GatewayIntent.class, compilationUnits);

        events = new Reflections("net.dv8tion.jda.api.events");
    }

    @Test
    public void testIntentMismatches()
    {
        // Check that #fromEvents gives the same intents that are documented
        for (Class<GenericEvent> eventClass : intentsByClass.keySet())
        {
            final EnumSet<GatewayIntent> documentedIntents = intentsByClass.get(eventClass);
            documentedIntents.removeAll(OPTIONAL_INTENTS.getOrDefault(eventClass, EnumSet.noneOf(GatewayIntent.class)));
            documentedIntents.removeAll(EnumSet.of(
                    GatewayIntent.DIRECT_MESSAGES, GatewayIntent.DIRECT_MESSAGE_REACTIONS, GatewayIntent.DIRECT_MESSAGE_TYPING,
                    GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_MESSAGE_TYPING
            ));
            final EnumSet<GatewayIntent> requiredIntents = GatewayIntent.fromEvents(eventClass);

            Assertions.assertEquals(
                    documentedIntents,
                    requiredIntents,
                    "Documented intents from " + eventClass.getSimpleName() + " " + documentedIntents + " does not correspond to intents given by #fromEvents: " + requiredIntents
            );
        }
    }

    @Test
    public void testInheritedIntents()
    {
        // Check that documented intents appear in subtypes
        for (Class<GenericEvent> eventClass : intentsByClass.keySet())
        {
            final EnumSet<GatewayIntent> typeIntents = intentsByClass.get(eventClass);
            for (Class<? extends GenericEvent> subtype : events.getSubTypesOf(eventClass))
            {
                if (IGNORE_INHERITANCE.contains(subtype))
                    continue;

                final EnumSet<GatewayIntent> subtypeIntents = intentsByClass.get(subtype);
                if (subtypeIntents == null)
                    continue;

                final EnumSet<GatewayIntent> missingIntents = EnumSet.copyOf(typeIntents);
                missingIntents.removeAll(subtypeIntents);

                Assertions.assertTrue(missingIntents.isEmpty(), subtype.getSimpleName() + " does not document " + missingIntents + " inherited from " + eventClass.getSimpleName());
            }
        }
    }
}
