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

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.description.JavadocDescription;
import com.github.javaparser.javadoc.description.JavadocInlineTag;
import com.github.javaparser.utils.SourceRoot;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.annotations.Requirements;
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

public class CacheFlagRequirementsTest
{
    private static final Logger LOGGER = JDALogger.getLog(CacheFlagRequirementsTest.class);
    private static final Pattern LINK_SPLIT_PATTERN = Pattern.compile("(?<!,)\\s+");

    // Classes which have documented intents, but are optional, and thus not given by #fromEvents
    private static final Map<Class<? extends GenericEvent>, EnumSet<CacheFlag>> OPTIONAL_FLAGS = new HashMap<Class<? extends GenericEvent>, EnumSet<CacheFlag>>() {{
        put(PermissionOverrideCreateEvent.class, EnumSet.of(CacheFlag.MEMBER_OVERRIDES));
        put(PermissionOverrideUpdateEvent.class, EnumSet.of(CacheFlag.MEMBER_OVERRIDES));
        put(PermissionOverrideDeleteEvent.class, EnumSet.of(CacheFlag.MEMBER_OVERRIDES));
        put(GenericPermissionOverrideEvent.class, EnumSet.of(CacheFlag.MEMBER_OVERRIDES));
    }};

    private static Reflections events;
    private static Map<Class<GenericEvent>, EnumSet<CacheFlag>> flagsByClass;

    @BeforeAll
    static void setup() throws IOException, ClassNotFoundException
    {
        final List<CompilationUnit> compilationUnits = parseEventCompilationUnits();

        flagsByClass = getEnumEntriesByClass(CacheFlag.class, compilationUnits);

        events = new Reflections("net.dv8tion.jda.api.events");
    }

    @Nonnull
    public static List<CompilationUnit> parseEventCompilationUnits() throws IOException
    {
        final SourceRoot root = new SourceRoot(Paths.get("src", "main", "java"));
        final List<ParseResult<CompilationUnit>> parseResults = root.tryToParse(Event.class.getPackage().getName());
        if (parseResults.isEmpty())
            throw new AssertionError("Could not find any source file");
        return parseResults.stream()
                .filter(p ->
                {
                    if (!p.getProblems().isEmpty())
                        throw new RuntimeException("Problems when parsing were encountered:\n" + p.getProblems());
                    else if (!p.getResult().isPresent())
                        throw new AssertionError("No result was present but no problems were either");

                    return p.getResult().isPresent();
                })
                .map(r -> r.getResult().get())
                // Exclude annotations
                .filter(c -> !c.getPackageDeclaration().map(PackageDeclaration::getNameAsString).equals(Optional.of(Requirements.class.getPackage().getName())))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> Map<Class<GenericEvent>, EnumSet<E>> getEnumEntriesByClass(Class<E> enumType, List<CompilationUnit> compilationUnits) throws ClassNotFoundException
    {
        final Pattern enumEntryReferencePattern = Pattern.compile(Pattern.quote(enumType.getSimpleName()) + "#(\\w+)");
        final Map<Class<GenericEvent>, EnumSet<E>> enumEntriesByClass = new HashMap<>();

        // Read documented cache flags of scanned classes
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
            final EnumSet<E> expectedFlags = mapLinks(enumType, links, enumEntryReferencePattern);
            enumEntriesByClass.put((Class<GenericEvent>) Class.forName(qualifiedEventName), expectedFlags);
        }

        return enumEntriesByClass;
    }

    @Nonnull
    private static List<String> getLinks(JavadocDescription description)
    {
        return description.getElements().stream()
                .filter(JavadocInlineTag.class::isInstance)
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

    @Test
    public void testFlagMismatches()
    {
        // Check that #fromEvents gives the same flags that are documented
        for (Class<GenericEvent> eventClass : flagsByClass.keySet())
        {
            final EnumSet<CacheFlag> documentedFlags = flagsByClass.get(eventClass);
            documentedFlags.removeAll(OPTIONAL_FLAGS.getOrDefault(eventClass, EnumSet.noneOf(CacheFlag.class)));
            final EnumSet<CacheFlag> requiredFlags = CacheFlag.fromEvents(eventClass);

            Assertions.assertEquals(
                    documentedFlags,
                    requiredFlags,
                    "Documented flags from " + eventClass.getSimpleName() + " " + documentedFlags + " does not correspond to flags given by #fromEvents: " + requiredFlags
            );
        }
    }

    @Test
    public void testInheritedFlags()
    {
        // Check that documented flags appear in subtypes
        for (Class<GenericEvent> eventClass : flagsByClass.keySet())
        {
            final EnumSet<CacheFlag> typeFlags = flagsByClass.get(eventClass);
            for (Class<? extends GenericEvent> subtype : events.getSubTypesOf(eventClass))
            {
                final EnumSet<CacheFlag> subtypeFlags = flagsByClass.get(subtype);
                if (subtypeFlags == null)
                    continue;

                final EnumSet<CacheFlag> missingFlags = EnumSet.copyOf(typeFlags);
                missingFlags.removeAll(subtypeFlags);

                Assertions.assertTrue(missingFlags.isEmpty(), subtype.getSimpleName() + " does not document " + missingFlags + " inherited from " + eventClass.getSimpleName());
            }
        }
    }
}
