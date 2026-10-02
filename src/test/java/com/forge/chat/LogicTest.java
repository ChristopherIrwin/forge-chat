package com.forge.chat;

import java.util.List;
import java.util.Map;

/**
 * Plain-JVM checks for the Bukkit-free logic: duration parsing, spam
 * detectors and mention parsing. Run with: java -cp <classes>:<adventure jars> com.forge.chat.LogicTest
 */
public final class LogicTest {

    private record Fake(String name) implements MentionParser.Named {
    }

    private static int failures = 0;

    private static void check(String label, Object actual, Object expected) {
        if (java.util.Objects.equals(expected, actual)) {
            System.out.println("PASS " + label);
        } else {
            failures++;
            System.out.println("FAIL " + label + " — expected <" + expected + "> but got <" + actual + ">");
        }
    }

    public static void main(String[] args) {
        // durations
        check("parse 10s", TextUtil.parseDuration("10s"), 10L);
        check("parse 5m", TextUtil.parseDuration("5m"), 300L);
        check("parse 2h", TextUtil.parseDuration("2h"), 7200L);
        check("parse 1d", TextUtil.parseDuration("1d"), 86400L);
        check("parse 1w", TextUtil.parseDuration("1w"), 604800L);
        check("parse perm", TextUtil.parseDuration("perm"), -1L);
        check("parse PERMANENT", TextUtil.parseDuration("PERMANENT"), -1L);
        check("parse bogus", TextUtil.parseDuration("bogus"), -2L);
        check("parse empty", TextUtil.parseDuration(""), -2L);
        check("format 90", TextUtil.formatDuration(90), "1m 30s");
        check("format 3661", TextUtil.formatDuration(3661), "1h 1m");
        check("format 90061", TextUtil.formatDuration(90061), "1d 1h 1m");
        check("format perm", TextUtil.formatDuration(-1), "permanent");

        // spam detectors
        check("repeat 6/5", SpamFilter.hasRepeatChars("helloooooo", 5), true);
        check("repeat 5/5", SpamFilter.hasRepeatChars("hellooooo", 5), false);
        check("repeat none", SpamFilter.hasRepeatChars("hello world", 3), false);
        check("caps ratio", SpamFilter.capsRatio("HELLO world"), 0.5);
        check("caps none", SpamFilter.capsRatio("hello"), 0.0);
        check("caps no letters", SpamFilter.capsRatio("123 !!!"), 0.0);
        check("link blocked", SpamFilter.containsBlockedLink("visit evil.com now", List.of("myserver.com")), true);
        check("link whitelisted", SpamFilter.containsBlockedLink("visit myserver.com/rules", List.of("myserver.com")), false);
        check("link http", SpamFilter.containsBlockedLink("see https://evil.com/x", List.of()), true);
        check("no link", SpamFilter.containsBlockedLink("hello world", List.of()), false);

        // mention spans
        List<MentionParser.Span> spans = MentionParser.findSpans("hi @Steve and @Alex_99, meet @steve2");
        check("span count", spans.size(), 3);
        check("span token", spans.get(1).token(), "Alex_99");
        check("span roundtrip", "hi @Steve and @Alex_99, meet @steve2"
                .substring(spans.get(0).start(), spans.get(0).end()), "@Steve");

        // mention resolution
        Fake steve = new Fake("Steve");
        Fake steph = new Fake("Steph");
        Fake alex = new Fake("Alex_99");
        Fake self = new Fake("Me");
        List<Fake> all = List.of(steve, steph, alex, self);
        check("resolve exact", MentionParser.resolve("steve", self, all), steve);
        check("resolve case-insensitive", MentionParser.resolve("ALEX_99", self, all), alex);
        check("resolve ambiguous prefix", MentionParser.resolve("ste", self, all), null);
        check("resolve unique prefix", MentionParser.resolve("steph", self, all), steph);
        check("resolve self skipped", MentionParser.resolve("me", self, all), null);
        check("resolve unknown", MentionParser.resolve("nobody", self, all), null);
        check("broadcast token", MentionParser.isBroadcastToken("everyone"), true);
        check("not broadcast", MentionParser.isBroadcastToken("steve"), false);

        // map-backed lookup shape used by the pipeline
        Map<String, Fake> byName = Map.of("steve", steve, "alex_99", alex);
        check("map lookup", byName.get("steve"), steve);

        if (failures > 0) {
            System.out.println(failures + " FAILURES");
            System.exit(1);
        }
        System.out.println("ALL CHECKS PASS");
    }
}
