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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CacheFlagEventsTest
{
    private static final Pattern LINK_SPLIT_PATTERN = Pattern.compile("(?<!,)\\s+");
    private static final Pattern CACHE_FLAG_REFERENCE_PATTERN = Pattern.compile("CacheFlag#(\\w+)");

    private static final Set<String> IGNORED_CLASSES = new HashSet<>(Arrays.asList(
            PermissionOverrideCreateEvent.class.getName(),
            PermissionOverrideUpdateEvent.class.getName(),
            PermissionOverrideDeleteEvent.class.getName(),
            GenericPermissionOverrideEvent.class.getName()
    ));

    private static List<CompilationUnit> compilationUnits;

    //TODO Check supertypes for documented flags not present in subtype javadoc
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
    }

    @Test
    public void testDocumentedFlags() throws Exception
    {
        // Check for documented flags retrieved with #fromEvents
        for (CompilationUnit unit : compilationUnits)
        {
            final TypeDeclaration<?> primaryType = unit.getPrimaryType().orElse(null);
            if (primaryType == null)
                continue;

            final JavadocDescription description = primaryType
                    .getJavadoc()
                    .map(Javadoc::getDescription)
                    .orElse(null);
            if (description == null)
                continue;

            final List<String> links = description.getElements().stream()
                    .filter(e -> e instanceof JavadocInlineTag)
                    .map(JavadocInlineTag.class::cast)
                    .filter(tag -> tag.getType() == JavadocInlineTag.Type.LINK)
                    .map(JavadocInlineTag::getContent)
                    .map(String::trim)
                    .map(s -> LINK_SPLIT_PATTERN.split(s, 2)[0]) //Take left part
                    .collect(Collectors.toList());

            final EnumSet<CacheFlag> expectedFlags = EnumSet.noneOf(CacheFlag.class);
            for (String link : links)
            {
                final Matcher matcher = CACHE_FLAG_REFERENCE_PATTERN.matcher(link);
                while (matcher.find())
                {
                    final String flagName = matcher.group(1);
                    final CacheFlag expectedFlag = CacheFlag.valueOf(flagName);
                    expectedFlags.add(expectedFlag);
                }
            }

            if (expectedFlags.isEmpty()) continue;

            final String qualifiedEventName = primaryType.getFullyQualifiedName().orElseThrow(AssertionError::new);
            if (IGNORED_CLASSES.contains(qualifiedEventName))
                continue;

            @SuppressWarnings("unchecked")
            final Class<? extends GenericEvent> eventClass = (Class<? extends GenericEvent>) Class.forName(qualifiedEventName);

            Assertions.assertEquals(expectedFlags, CacheFlag.fromEvents(eventClass), "Documented flags in " + primaryType.getNameAsString() + " are not returned by #fromEvents");
        }
    }
}
