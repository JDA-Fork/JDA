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
    // Classes which have document intents, but are optional, and thus not given by #fromEvents
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
