package net.dv8tion.jda;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.description.JavadocDescription;
import com.github.javaparser.javadoc.description.JavadocInlineTag;
import com.github.javaparser.utils.SourceRoot;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.override.GenericPermissionOverrideEvent;
import net.dv8tion.jda.api.events.guild.override.PermissionOverrideCreateEvent;
import net.dv8tion.jda.api.events.guild.override.PermissionOverrideDeleteEvent;
import net.dv8tion.jda.api.events.guild.override.PermissionOverrideUpdateEvent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CacheFlagEventsTest
{
    private static final Logger LOGGER = JDALogger.getLog(CacheFlagEventsTest.class);
    private static final Pattern LINK_SPLIT_PATTERN = Pattern.compile("(?<!,)\\s+");
    private static final Pattern CACHE_FLAG_REFERENCE_PATTERN = Pattern.compile("CacheFlag#(\\w+)");

    private static final Set<String> IGNORED_CLASSES = new HashSet<>(Arrays.asList(
            PermissionOverrideCreateEvent.class.getName(),
            PermissionOverrideUpdateEvent.class.getName(),
            PermissionOverrideDeleteEvent.class.getName(),
            GenericPermissionOverrideEvent.class.getName()
    ));

    private static List<CompilationUnit> compilationUnits;
    private static Reflections events;

    //TODO Check flags given by #fromEvents not documented

    @BeforeAll
    static void setup() throws IOException
    {
        final SourceRoot root = new SourceRoot(Paths.get("src", "main", "java"));
        final List<ParseResult<CompilationUnit>> parseResults = root.tryToParse(Event.class.getPackage().getName());
        compilationUnits = parseResults.stream()
                .filter(p ->
                {
                    if (!p.getProblems().isEmpty())
                        throw new RuntimeException("Problems when parsing were encountered:\n" + p.getProblems());
                    else if (!p.getResult().isPresent())
                        throw new AssertionError("No result was present but no problems were either");

                    return p.getResult().isPresent();
                })
                .map(r -> r.getResult().get())
                .collect(Collectors.toList());

        events = new Reflections("net.dv8tion.jda.api.events");
    }

    @Test
    public void testFlagMismatches() throws Exception
    {
        // Check that #fromEvents gives the same flags that are documented
        // First find what flags each class uses
        final Map<Class<GenericEvent>, EnumSet<CacheFlag>> flagsByClass = new HashMap<>();
        for (TypeDeclaration<?> typeDeclaration : compilationUnits.stream().flatMap(c -> c.findAll(TypeDeclaration.class).stream()).collect(Collectors.toList()))
        {
            final String qualifiedEventName = typeDeclaration.getFullyQualifiedName().orElseThrow(AssertionError::new);

            final JavadocDescription description = typeDeclaration
                    .getJavadoc()
                    .map(Javadoc::getDescription)
                    .orElse(null);
            if (description == null)
            {
                LOGGER.warn("Undocumented class at {}", qualifiedEventName);
                continue;
            }

            final List<String> links = getLinks(description);
            final EnumSet<CacheFlag> expectedFlags = mapLinks(CacheFlag.class, links, CACHE_FLAG_REFERENCE_PATTERN);
            flagsByClass.put((Class<GenericEvent>) Class.forName(qualifiedEventName), expectedFlags);
        }

        // Then find subtypes of events and check the map
        for (Class<GenericEvent> eventClass : flagsByClass.keySet())
        {
            if (IGNORED_CLASSES.contains(eventClass.getName()))
                continue;

            final EnumSet<CacheFlag> documentedFlags = flagsByClass.get(eventClass);
            final EnumSet<CacheFlag> requiredFlags = CacheFlag.fromEvents(eventClass);

            Assertions.assertEquals(
                    documentedFlags,
                    requiredFlags,
                    "Documented flags from " + eventClass.getSimpleName() + " " + documentedFlags + " does not correspond to flags given by #fromEvents: " + requiredFlags
            );
        }
    }

    @Test
    public void testInheritedFlags() throws Exception
    {
        // Check that documented flags appear in subtypes
        // First find what flags each class uses
        final Map<Class<?>, EnumSet<CacheFlag>> flagsByQualifiedName = new HashMap<>();
        for (TypeDeclaration<?> typeDeclaration : compilationUnits.stream().flatMap(c -> c.findAll(TypeDeclaration.class).stream()).collect(Collectors.toList()))
        {
            final String qualifiedEventName = typeDeclaration.getFullyQualifiedName().orElseThrow(AssertionError::new);

            final JavadocDescription description = typeDeclaration
                    .getJavadoc()
                    .map(Javadoc::getDescription)
                    .orElse(null);
            if (description == null)
            {
                LOGGER.warn("Undocumented class at {}", qualifiedEventName);
                continue;
            }

            final List<String> links = getLinks(description);
            final EnumSet<CacheFlag> expectedFlags = mapLinks(CacheFlag.class, links, CACHE_FLAG_REFERENCE_PATTERN);
            flagsByQualifiedName.put(Class.forName(qualifiedEventName), expectedFlags);
        }

        // Then find subtypes of events and check the map
        for (Class<?> eventClass : flagsByQualifiedName.keySet())
        {
            final EnumSet<CacheFlag> typeFlags = flagsByQualifiedName.get(eventClass);
            @SuppressWarnings("unchecked")
            final Set<Class<? extends GenericEvent>> subtypes = events.getSubTypesOf(((Class<GenericEvent>) eventClass));
            for (Class<? extends GenericEvent> subtype : subtypes)
            {
                final EnumSet<CacheFlag> subtypeFlags = flagsByQualifiedName.get(subtype);
                if (subtypeFlags == null)
                    continue;

                final EnumSet<CacheFlag> missingFlags = EnumSet.copyOf(typeFlags);
                missingFlags.removeAll(subtypeFlags);

                Assertions.assertTrue(missingFlags.isEmpty(), subtype.getSimpleName() + " does not document " + missingFlags + " inherited from " + eventClass.getSimpleName());
            }
        }
    }

    @Nonnull
    private static List<String> getLinks(JavadocDescription description)
    {
        return description.getElements().stream()
                .filter(e -> e instanceof JavadocInlineTag)
                .map(JavadocInlineTag.class::cast)
                .filter(tag -> tag.getType() == JavadocInlineTag.Type.LINK)
                .map(JavadocInlineTag::getContent)
                .map(String::trim)
                .map(s -> LINK_SPLIT_PATTERN.split(s, 2)[0]) //Take left part
                .collect(Collectors.toList());
    }

    @Nonnull
    private static <E extends Enum<E>> EnumSet<E> mapLinks(Class<E> type, List<String> links, Pattern pattern)
    {
        final EnumSet<E> enumSet = EnumSet.noneOf(type);
        for (String link : links)
        {
            final Matcher matcher = pattern.matcher(link);
            while (matcher.find())
            {
                final String elementName = matcher.group(1);
                enumSet.add(Enum.valueOf(type, elementName));
            }
        }
        return enumSet;
    }
}
