/*
 * Joda Software License, Version 1.0
 *
 *
 * Copyright (c) 2001-03 Stephen Colebourne.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Joda project (http://www.joda.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The name "Joda" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact licence@joda.org.
 *
 * 5. Products derived from this software may not be called "Joda",
 *    nor may "Joda" appear in their name, without prior written
 *    permission of the Joda project.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE JODA AUTHORS OR THE PROJECT
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Joda project and was originally
 * created by Stephen Colebourne <scolebourne@joda.org>. For more
 * information on the Joda project, please see <http://www.joda.org/>.
 */

package org.joda.time.tz;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeField;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;
import org.joda.time.format.DateTimeParser;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.chrono.LenientChronology;
import org.joda.time.chrono.iso.ISOChronology;

/**
 * Compiles Olson ZoneInfo database files into binary files for each time zone
 * in the database. {@link DateTimeZoneBuilder} is used to construct and encode
 * compiled data files. {@link ZoneInfoProvider} loads the encoded files and
 * converts them back into {@link DateTimeZone} objects.
 * <p>
 * Although this tool is similar to zic, the binary formats are not
 * compatible. The latest Olson database files may be obtained
 * <a href="http://www.twinsun.com/tz/tz-link.htm">here</a>.
 * <p>
 * ZoneInfoCompiler is mutable and not thread-safe, although the main method
 * may be safely invoked by multiple threads.
 *
 * @author Brian S O'Neill
 */
public class ZoneInfoCompiler {
    static final DateTimeOfYear START_OF_YEAR = new DateTimeOfYear();

    /**
     * Usage: java ZoneInfoCompiler [-d outputDirectory] sourceFile ...
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String[] sources = args;
        String outputDir = null;

        if ("-d".equals(args[0])) {
            if (args.length < 3) {
                printUsage();
                return;
            }
            outputDir = args[1];
            String[] newSources = new String[args.length - 2];
            System.arraycopy(args, 2, newSources, 0, newSources.length);
            sources = newSources;
        }

        ZoneInfoCompiler zic = new ZoneInfoCompiler();
        zic.compile(outputDir, sources);
    }

    private static void printUsage() {
        System.out.println
            ("Usage: java ZoneInfoCompiler [-d outputDirectory] sourceFile ...");
    }

    /**
     * @param zimap maps string ids to DateTimeZone objects.
     */
    static void writeZoneInfoMap(DataOutputStream dout, Map zimap) throws IOException {
        // Build the string pool.
        Map idToIndex = new HashMap(zimap.size());
        TreeMap indexToId = new TreeMap();

        Iterator it = zimap.entrySet().iterator();
        short count = 0;
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            String id = (String)entry.getKey();
            if (!idToIndex.containsKey(id)) {
                Short index = new Short(count);
                idToIndex.put(id, index);
                indexToId.put(index, id);
                if (++count == 0) {
                    throw new InternalError("Too many time zone ids");
                }
            }
            id = ((DateTimeZone)entry.getValue()).getID();
            if (!idToIndex.containsKey(id)) {
                Short index = new Short(count);
                idToIndex.put(id, index);
                indexToId.put(index, id);
                if (++count == 0) {
                    throw new InternalError("Too many time zone ids");
                }
            }
        }

        // Write the string pool, ordered by index.
        dout.writeShort(indexToId.size());
        it = indexToId.values().iterator();
        while (it.hasNext()) {
            dout.writeUTF((String)it.next());
        }

        // Write the mappings.
        dout.writeShort(zimap.size());
        it = zimap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            String id = (String)entry.getKey();
            dout.writeShort(((Short)idToIndex.get(id)).shortValue());
            id = ((DateTimeZone)entry.getValue()).getID();
            dout.writeShort(((Short)idToIndex.get(id)).shortValue());
        }
    }

    /**
     * @param zimap gets filled with string id to string id mappings
     */
    static void readZoneInfoMap(DataInputStream din, Map zimap) throws IOException {
        // Read the string pool.
        int size = din.readUnsignedShort();
        String[] pool = new String[size];
        for (int i=0; i<size; i++) {
            pool[i] = din.readUTF().intern();
        }

        // Read the mappings.
        size = din.readUnsignedShort();
        for (int i=0; i<size; i++) {
            try {
                zimap.put(pool[din.readUnsignedShort()], pool[din.readUnsignedShort()]);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new IOException("Corrupt zone info map");
            }
        }
    }

    static int parseYear(String str, int def) {
        str = str.toLowerCase();
        if (str.equals("minimum") || str.equals("min")) {
            return Integer.MIN_VALUE;
        } else if (str.equals("maximum") || str.equals("max")) {
            return Integer.MAX_VALUE;
        } else if (str.equals("only")) {
            return def;
        }
        return Integer.parseInt(str);
    }

    static int parseMonth(String str) {
        DateTimeField field = ISOChronology.getInstanceUTC().monthOfYear();
        return field.get(field.set(0, str, Locale.ENGLISH));
    }

    static int parseDayOfWeek(String str) {
        DateTimeField field = ISOChronology.getInstanceUTC().dayOfWeek();
        return field.get(field.set(0, str, Locale.ENGLISH));
    }
    
    static String parseOptional(String str) {
        return (str.equals("-")) ? null : str;
    }

    static int parseTime(String str) {
        Chronology chrono = new LenientChronology(ISOChronology.getInstanceUTC());
        DateTimeParser p = ISODateTimeFormat
            .getInstance(chrono)
            .hourMinuteSecondFraction();
        MutableDateTime mdt = new MutableDateTime(0, chrono);
        int pos = 0;
        if (str.startsWith("-")) {
            pos = 1;
        }
        int newPos = p.parseInto(mdt, str, pos);
        if (newPos == ~pos) {
            throw new IllegalArgumentException(str);
        }
        int millis = (int)mdt.getMillis();
        if (pos == 1) {
            millis = -millis;
        }
        return millis;
    }

    static char parseZoneChar(char c) {
        switch (c) {
        case 's': case 'S':
            // Standard time
            return 's';
        case 'u': case 'U': case 'g': case 'G': case 'z': case 'Z':
            // UTC
            return 'u';
        case 'w': case 'W': default:
            // Wall time
            return 'w';
        }
    }

    /**
     * @return false if error.
     */
    static boolean test(String id, DateTimeZone tz) {
        if (!id.equals(tz.getID())) {
            return true;
        }

        // Test to ensure that reported transitions are not duplicated.

        long millis = ISOChronology.getInstanceUTC().year().set(0, 1850);
        long end = ISOChronology.getInstanceUTC().year().set(0, 2050);

        int offset = tz.getOffset(millis);
        int standardOffset = tz.getStandardOffset(millis);
        String key = tz.getNameKey(millis);

        List transitions = new ArrayList();

        while (true) {
            long next = tz.nextTransition(millis);
            if (next == millis || next > end) {
                break;
            }

            millis = next;

            int nextOffset = tz.getOffset(millis);
            String nextKey = tz.getNameKey(millis);

            if (offset == nextOffset
                && key.equals(nextKey)) {
                System.out.println("*d* Error in " + tz.getID() + " "
                                   + new DateTime(millis,
                                                  ISOChronology.getInstanceUTC()));
                return false;
            }

            if (nextKey == null || (nextKey.length() < 3 && !"??".equals(nextKey))) {
                System.out.println("*s* Error in " + tz.getID() + " "
                                   + new DateTime(millis,
                                                  ISOChronology.getInstanceUTC())
                                   + ", nameKey=" + nextKey);
                return false;
            }

            transitions.add(new Long(millis));

            offset = nextOffset;
            key = nextKey;
        }

        // Now verify that reverse transitions match up.

        millis = ISOChronology.getInstanceUTC().year().set(0, 2050);
        end = ISOChronology.getInstanceUTC().year().set(0, 1850);

        for (int i=transitions.size(); --i>= 0; ) {
            long prev = tz.previousTransition(millis);
            if (prev == millis || prev < end) {
                break;
            }

            millis = prev;

            long trans = ((Long)transitions.get(i)).longValue();
            
            if (trans - 1 != millis) {
                System.out.println("*r* Error in " + tz.getID() + " "
                                   + new DateTime(millis,
                                                  ISOChronology.getInstanceUTC()) + " != "
                                   + new DateTime(trans - 1,
                                                  ISOChronology.getInstanceUTC()));
                                   
                return false;
            }
        }

        return true;
    }

    // Maps names to RuleSets.
    private Map iRuleSets;

    // List of Zone objects.
    private List iZones;

    // List String pairs to link.
    private List iLinks;

    public ZoneInfoCompiler() {
        iRuleSets = new HashMap();
        iZones = new ArrayList();
        iLinks = new ArrayList();
    }

    /**
     * Returns a map of ids to DateTimeZones.
     *
     * @param outputDir optional directory to write compiled data files to
     * @param sources optional list of source file paths to parse
     */
    public Map compile(String outputDir, String[] sources) throws IOException {
        if (sources != null) {
            for (int i=0; i<sources.length; i++) {
                BufferedReader in = new BufferedReader(new FileReader(sources[i]));
                parseDataFile(in);
                in.close();
            }
        }

        File dir = null;
        if (outputDir != null) {
            dir = new File(outputDir);
            if (!dir.exists()) {
                throw new IOException("Destination directory doesn't exist: " + outputDir);
            }
            if (!dir.isDirectory()) {
                throw new IOException("Destination is not a directory: " + outputDir);
            }
        }

        Map map = new TreeMap();

        for (int i=0; i<iZones.size(); i++) {
            Zone zone = (Zone)iZones.get(i);
            DateTimeZoneBuilder builder = new DateTimeZoneBuilder();
            zone.addToBuilder(builder, iRuleSets);
            final DateTimeZone original = builder.toDateTimeZone(zone.iName);
            DateTimeZone tz = original;
            if (test(tz.getID(), tz)) {
                map.put(tz.getID(), tz);
                if (dir != null) {
                    System.out.println("Writing " + tz.getID());
                    File file = new File(dir, tz.getID());
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    OutputStream out = new FileOutputStream(file);
                    builder.writeTo(out);
                    out.close();

                    // Test if it can be read back.
                    InputStream in = new FileInputStream(file);
                    DateTimeZone tz2 = DateTimeZoneBuilder.readFrom(in, tz.getID());
                    in.close();

                    if (!original.equals(tz2)) {
                        System.out.println("*e* Error in " + tz.getID() +
                                           ": Didn't read properly from file");
                    }
                }
            }
        }

        for (int pass=0; pass<2; pass++) {
            for (int i=0; i<iLinks.size(); i += 2) {
                String id = (String)iLinks.get(i);
                String alias = (String)iLinks.get(i + 1);
                DateTimeZone tz = (DateTimeZone)map.get(id);
                if (tz == null) {
                    if (pass > 0) {
                        System.out.println("Cannot find time zone '" + id +
                                           "' to link alias '" + alias + "' to");
                    }
                } else {
                    map.put(alias, tz);
                }
            }
        }

        if (dir != null) {
            System.out.println("Writing ZoneInfoMap");
            File file = new File(dir, "ZoneInfoMap");
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            OutputStream out = new FileOutputStream(file);
            DataOutputStream dout = new DataOutputStream(out);
            // Sort and filter out any duplicates that match case.
            Map zimap = new TreeMap(String.CASE_INSENSITIVE_ORDER);
            zimap.putAll(map);
            writeZoneInfoMap(dout, zimap);
            dout.close();
        }

        return map;
    }

    public void parseDataFile(BufferedReader in) throws IOException {
        Zone zone = null;
        String line;
        while ((line = in.readLine()) != null) {
            if (line.length() == 0 || line.charAt(0) == '#') {
                continue;
            }

            int index = line.indexOf('#');
            if (index >= 0) {
                line = line.substring(0, index);
            }

            //System.out.println(line);

            StringTokenizer st = new StringTokenizer(line, " \t");

            if (Character.isWhitespace(line.charAt(0)) && st.hasMoreTokens()) {
                if (zone != null) {
                    // Zone continuation
                    zone.chain(st);
                }
                continue;
            } else {
                if (zone != null) {
                    iZones.add(zone);
                }
                zone = null;
            }

            if (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (token.equalsIgnoreCase("Rule")) {
                    Rule r = new Rule(st);
                    RuleSet rs = (RuleSet)iRuleSets.get(r.iName);
                    if (rs == null) {
                        rs = new RuleSet(r);
                        iRuleSets.put(r.iName, rs);
                    } else {
                        rs.addRule(r);
                    }
                } else if (token.equalsIgnoreCase("Zone")) {
                    zone = new Zone(st);
                } else if (token.equalsIgnoreCase("Link")) {
                    iLinks.add(st.nextToken());
                    iLinks.add(st.nextToken());
                } else {
                    System.out.println("Unknown line: " + line);
                }
            }
        }

        if (zone != null) {
            iZones.add(zone);
        }
    }

    private static class DateTimeOfYear {
        public final int iMonthOfYear;
        public final int iDayOfMonth;
        public final int iDayOfWeek;
        public final boolean iAdvanceDayOfWeek;
        public final int iMillisOfDay;
        public final char iZoneChar;

        DateTimeOfYear() {
            iMonthOfYear = 1;
            iDayOfMonth = 1;
            iDayOfWeek = 0;
            iAdvanceDayOfWeek = false;
            iMillisOfDay = 0;
            iZoneChar = 'w';
        }

        DateTimeOfYear(StringTokenizer st) {
            int month = 1;
            int day = 1;
            int dayOfWeek = 0;
            int millis = 0;
            boolean advance = false;
            char zoneChar = 'w';

            if (st.hasMoreTokens()) {
                month = parseMonth(st.nextToken());

                if (st.hasMoreTokens()) {
                    String str = st.nextToken();
                    if (str.startsWith("last")) {
                        day = -1;
                        dayOfWeek = parseDayOfWeek(str.substring(4));
                        advance = false;
                    } else {
                        try {
                            day = Integer.parseInt(str);
                            dayOfWeek = 0;
                            advance = false;
                        } catch (NumberFormatException e) {
                            int index = str.indexOf(">=");
                            if (index > 0) {
                                day = Integer.parseInt(str.substring(index + 2));
                                dayOfWeek = parseDayOfWeek(str.substring(0, index));
                                advance = true;
                            } else {
                                index = str.indexOf("<=");
                                if (index > 0) {
                                    day = Integer.parseInt(str.substring(index + 2));
                                    dayOfWeek = parseDayOfWeek(str.substring(0, index));
                                    advance = false;
                                } else {
                                    throw new IllegalArgumentException(str);
                                }
                            }
                        }
                    }

                    if (st.hasMoreTokens()) {
                        str = st.nextToken();
                        zoneChar = parseZoneChar(str.charAt(str.length() - 1));
                        millis = parseTime(str);
                    }
                }
            }

            iMonthOfYear = month;
            iDayOfMonth = day;
            iDayOfWeek = dayOfWeek;
            iAdvanceDayOfWeek = advance;
            iMillisOfDay = millis;
            iZoneChar = zoneChar;
        }

        /**
         * Adds a recurring savings rule to the builder.
         */
        public void addRecurring(DateTimeZoneBuilder builder, String nameKey,
                                 int saveMillis, int fromYear, int toYear)
        {
            builder.addRecurringSavings(nameKey, saveMillis,
                                        fromYear, toYear,
                                        iZoneChar,
                                        iMonthOfYear,
                                        iDayOfMonth,
                                        iDayOfWeek,
                                        iAdvanceDayOfWeek,
                                        iMillisOfDay);
        }

        /**
         * Adds a cutover to the builder.
         */
        public void addCutover(DateTimeZoneBuilder builder, int year) {
            builder.addCutover(year,
                               iZoneChar,
                               iMonthOfYear,
                               iDayOfMonth,
                               iDayOfWeek,
                               iAdvanceDayOfWeek,
                               iMillisOfDay);
        }

        public String toString() {
            return
                "MonthOfYear: " + iMonthOfYear + "\n" +
                "DayOfMonth: " + iDayOfMonth + "\n" +
                "DayOfWeek: " + iDayOfWeek + "\n" +
                "AdvanceDayOfWeek: " + iAdvanceDayOfWeek + "\n" +
                "MillisOfDay: " + iMillisOfDay + "\n" +
                "ZoneChar: " + iZoneChar + "\n";
        }
    }

    private static class Rule {
        public final String iName;
        public final int iFromYear;
        public final int iToYear;
        public final String iType;
        public final DateTimeOfYear iDateTimeOfYear;
        public final int iSaveMillis;
        public final String iLetterS;

        Rule(StringTokenizer st) {
            iName = st.nextToken().intern();
            iFromYear = parseYear(st.nextToken(), 0);
            iToYear = parseYear(st.nextToken(), iFromYear);
            if (iToYear < iFromYear) {
                throw new IllegalArgumentException();
            }
            iType = parseOptional(st.nextToken());
            iDateTimeOfYear = new DateTimeOfYear(st);
            iSaveMillis = parseTime(st.nextToken());
            iLetterS = parseOptional(st.nextToken());
        }

        /**
         * Adds a recurring savings rule to the builder.
         */
        public void addRecurring(DateTimeZoneBuilder builder, String nameFormat) {
            String nameKey = formatName(nameFormat);
            iDateTimeOfYear.addRecurring
                (builder, nameKey, iSaveMillis, iFromYear, iToYear);
        }

        private String formatName(String nameFormat) {
            int index = nameFormat.indexOf('/');
            if (index > 0) {
                if (iSaveMillis == 0) {
                    // Extract standard name.
                    return nameFormat.substring(0, index).intern();
                } else {
                    return nameFormat.substring(index + 1).intern();
                }
            }
            index = nameFormat.indexOf("%s");
            if (index < 0) {
                return nameFormat;
            }
            String left = nameFormat.substring(0, index);
            String right = nameFormat.substring(index + 2);
            String name;
            if (iLetterS == null) {
                name = left.concat(right);
            } else {
                name = left + iLetterS + right;
            }
            return name.intern();
        }

        public String toString() {
            return
                "[Rule]\n" + 
                "Name: " + iName + "\n" +
                "FromYear: " + iFromYear + "\n" +
                "ToYear: " + iToYear + "\n" +
                "Type: " + iType + "\n" +
                iDateTimeOfYear +
                "SaveMillis: " + iSaveMillis + "\n" +
                "LetterS: " + iLetterS + "\n";
        }
    }

    private static class RuleSet {
        private List iRules;

        RuleSet(Rule rule) {
            iRules = new ArrayList();
            iRules.add(rule);
        }

        void addRule(Rule rule) {
            if (!(rule.iName.equals(((Rule)iRules.get(0)).iName))) {
                throw new IllegalArgumentException("Rule name mismatch");
            }
            iRules.add(rule);
        }

        /**
         * Adds recurring savings rules to the builder.
         */
        public void addRecurring(DateTimeZoneBuilder builder, String nameFormat) {
            for (int i=0; i<iRules.size(); i++) {
                Rule rule = (Rule)iRules.get(i);
                rule.addRecurring(builder, nameFormat);
            }
        }
    }

    private static class Zone {
        public final String iName;
        public final int iOffsetMillis;
        public final String iRules;
        public final String iFormat;
        public final int iUntilYear;
        public final DateTimeOfYear iUntilDateTimeOfYear;

        private Zone iNext;

        Zone(StringTokenizer st) {
            this(st.nextToken(), st);
        }

        private Zone(String name, StringTokenizer st) {
            iName = name.intern();
            iOffsetMillis = parseTime(st.nextToken());
            iRules = parseOptional(st.nextToken());
            iFormat = st.nextToken().intern();

            int year = Integer.MAX_VALUE;
            DateTimeOfYear dtOfYear = START_OF_YEAR;

            if (st.hasMoreTokens()) {
                year = Integer.parseInt(st.nextToken());
                if (st.hasMoreTokens()) {
                    dtOfYear = new DateTimeOfYear(st);
                }
            }

            iUntilYear = year;
            iUntilDateTimeOfYear = dtOfYear;
        }

        void chain(StringTokenizer st) {
            if (iNext != null) {
                iNext.chain(st);
            } else {
                iNext = new Zone(iName, st);
            }
        }

        public DateTimeZone buildDateTimeZone(Map ruleSets) {
            DateTimeZoneBuilder builder = new DateTimeZoneBuilder();
            addToBuilder(builder, ruleSets);
            return builder.toDateTimeZone(iName);
        }

        /**
         * Adds zone info to the builder.
         */
        public void addToBuilder(DateTimeZoneBuilder builder, Map ruleSets) {
            addToBuilder(this, builder, ruleSets);
        }

        private static void addToBuilder(Zone zone,
                                         DateTimeZoneBuilder builder,
                                         Map ruleSets)
        {
            for (; zone != null; zone = zone.iNext) {
                builder.setStandardOffset(zone.iOffsetMillis);

                if (zone.iRules == null) {
                    builder.setFixedSavings(zone.iFormat, 0);
                } else {
                    try {
                        // Check if iRules actually just refers to a savings.
                        int saveMillis = parseTime(zone.iRules);
                        builder.setFixedSavings(zone.iFormat, saveMillis);
                    }
                    catch (Exception e) {
                        RuleSet rs = (RuleSet)ruleSets.get(zone.iRules);
                        if (rs == null) {
                            throw new IllegalArgumentException
                                ("Rules not found: " + zone.iRules);
                        }
                        rs.addRecurring(builder, zone.iFormat);
                    }
                }

                if (zone.iUntilYear == Integer.MAX_VALUE) {
                    break;
                }

                zone.iUntilDateTimeOfYear.addCutover(builder, zone.iUntilYear);
            }
        }

        public String toString() {
            String str =
                "[Zone]\n" + 
                "Name: " + iName + "\n" +
                "OffsetMillis: " + iOffsetMillis + "\n" +
                "Rules: " + iRules + "\n" +
                "Format: " + iFormat + "\n" +
                "UntilYear: " + iUntilYear + "\n" +
                iUntilDateTimeOfYear;

            if (iNext == null) {
                return str;
            }

            return str + "...\n" + iNext.toString();
        }
    }
}
