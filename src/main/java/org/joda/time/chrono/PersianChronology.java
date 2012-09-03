/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.joda.time.chrono;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeField;
import org.joda.time.DateTimeZone;

/**
 * Base abstract class for a Persian calendar implementation. The Persian
 * calendar is characterized by the fact that it is an observation based
 * calendar (the start of the new year is based on the time of the day when the
 * sun is directly overhead in the northern hemisphere's spring season ), and
 * because of this there are various mathematical and astronomical formulae that
 * predict the leap year cycles, each with differing accuracy, and each with
 * differing popularity.</br></br> Four implementyations have been provided,
 * with the option for the user to extend the abstract Persian chronology to
 * provide their own implementation.
 * 
 * For concrete Persian calendar implementations provided, see</br>
 * {@link org.joda.time.chrono.PersianChronologyKhayyam}
 * {@link org.joda.time.chrono.PersianChronologyKhayyamBorkowski}
 * {@link org.joda.time.chrono.PersianChronologyBirashk}
 * {@link org.joda.time.chrono.PersianChronologyMeeus} </br>
 * 
 * </br></br>The concrete calendar implementation can be <a
 * href="http://calendars.wikia.com/wiki/Proleptic">proleptic</a>, therefore the
 * the min year supported by this class is -2147482492 and the max year is
 * 2134300219 (the years when the getMillis() method will overflow), however
 * each instance of a PersianChronology is initialized with the
 * {@link org.joda.time.chrono.LimitChronology} so that extending classes
 * (concrete implementation) can impose stricter limits by overriding the
 * getMinYear() and getMaxYear() methods of PersianChronology.
 * 
 * @author Zubin Kavarana
 * @since 2.1
 */
public abstract class PersianChronology extends BasicChronology {

    enum PersianWeekDay {
        Chaharshanbeh(5), Doshanbeh(3), Jomeh(7), Panjshanbeh(6), Seshanbeh(4), Shanbeh(1), Yekshanbeh(2);

        private final int weekDay;

        private PersianWeekDay(int weekDay) {
            this.weekDay = weekDay;
        }

        int getGregorianWeekDay() {
            int gregWeekDay = weekDay - 2;
            if (gregWeekDay < 1) {
                gregWeekDay = gregWeekDay + 7;
            }
            return gregWeekDay;
        }

        int getWeekDay() {
            return weekDay;
        }
    }

    /**
     * Constant value for 'Anno Persico' equivalent to the value returned for
     * AD/CE.
     */
    public static final int AP = DateTimeConstants.CE;

    /** Cache of zone chronology arrays */
    private static final Map<String, PersianChronology> cCache = Collections
            .synchronizedMap(new HashMap<String, PersianChronology>());

    private static final DateTimeField ERA_FIELD = new BasicSingleEraDateTimeField("AP");

    private static final int FARVARDIN_TO_SHAHRIVAR_MONTH_DAYS = 31;

    private static final int MEHR_TO_BAHMAN_MONTH_DAYS = 30;

    private static final long serialVersionUID = 2255315679766318086L;

    private static final long[] TOTAL_MILLIS_BY_MONTH_ARRAY;

    private static final PersianWeekDay[] WEEKDAYS_IN_GREG_ORDER = { PersianWeekDay.Doshanbeh, PersianWeekDay.Seshanbeh,
            PersianWeekDay.Chaharshanbeh, PersianWeekDay.Panjshanbeh, PersianWeekDay.Jomeh, PersianWeekDay.Shanbeh,
            PersianWeekDay.Yekshanbeh };

    static {
        TOTAL_MILLIS_BY_MONTH_ARRAY = new long[12];
        TOTAL_MILLIS_BY_MONTH_ARRAY[0] = 0L;
        long milliSum = 0L;
        for (int i = 0; i < 6; i++) {
            long millis = FARVARDIN_TO_SHAHRIVAR_MONTH_DAYS * (long) DateTimeConstants.MILLIS_PER_DAY;
            milliSum += millis;
            TOTAL_MILLIS_BY_MONTH_ARRAY[i + 1] = milliSum;
        }
        for (int i = 6; i < 11; i++) {
            long millis = MEHR_TO_BAHMAN_MONTH_DAYS * (long) DateTimeConstants.MILLIS_PER_DAY;
            milliSum += millis;
            TOTAL_MILLIS_BY_MONTH_ARRAY[i + 1] = milliSum;
        }
    }

    /**
     * Method Description: Gets an instance of the PersianChronology in the
     * given time zone and PersianChronology concrete implementation instance.
     * The concrete instance provides the leap year rule and first day of year
     * in milli-seconds, and allows the caller to inject their own preferred
     * implementation.
     * 
     * @param zone
     *            the time zone to get the chronology in, null is default *
     * @param persianChronologyImpl
     *            the implementation instance of the methodology used to resolve
     *            leap years in the Persian calendar
     * @return a chronology in the specified time zone, which will calculate
     *         Persian leap years and first day of the year based on it's
     *         intercalation method implementation
     */
    protected static PersianChronology getInstance(DateTimeZone zone, PersianChronology persianChronologyImpl) {
        if (zone == null) {
            zone = DateTimeZone.getDefault();
        }

        PersianChronology chrono = cCache.get(zone + persianChronologyImpl.getKey());
        if (chrono == null) {
            if (zone == DateTimeZone.UTC) {
                // First create without a lower limit.
                chrono = persianChronologyImpl;
                // Impose lower limit and make another PersianChronology.
                DateTime lowerLimit = new DateTime(chrono.getMinYear(), 1, 1, 0, 0, 0, 0, chrono);
                LimitChronology limitChronology = LimitChronology.getInstance(chrono, lowerLimit, null);
                chrono = persianChronologyImpl.newInstance(limitChronology, lowerLimit);
            } else {
                chrono = getInstance(DateTimeZone.UTC, persianChronologyImpl);
                chrono = persianChronologyImpl.newInstance(ZonedChronology.getInstance(chrono, zone), null);
            }
            cCache.put(zone + persianChronologyImpl.getKey(), chrono);
        }
        return chrono;
    }

    /**
     * 
     * Method Description: Gets an instance of the PersianChronology in the UTC
     * time zone and PersianChronology concrete implementation instance. The
     * concrete instance provides the leap year rule and first day of year in
     * milli-seconds, and allows the caller to inject their own preferred
     * implementation.
     * 
     * @param persianChronologyImpl
     *            the implementation instance of the methodology used to resolve
     *            leap years in the Persian calendar
     * @return a chronology in the default time zone, which will calculate
     *         Persian leap years and first day of the year based on it's
     *         intercalation method implementation
     */
    protected static PersianChronology getInstance(PersianChronology persianChronologyImpl) {
        return getInstance(DateTimeZone.getDefault(), persianChronologyImpl);
    }

    /**
     * Method Description: Gets an instance of the PersianChronology in the UTC
     * time zone and PersianChronology concrete implementation instance. The
     * concrete instance provides the leap year rule and first day of year in
     * milli-seconds, and allows the caller to inject their own preferred
     * implementation.
     * 
     * @return a singleton UTC instance of the chronology
     */
    protected static PersianChronology getInstanceUTC(PersianChronology persianChronologyImpl) {
        return getInstance(DateTimeZone.UTC, persianChronologyImpl);
    }

    /**
     * Restricted constructor.
     */
    PersianChronology(Chronology base, Object param) {
        super(base, param, 1);
    }

    @Override
    protected void assemble(Fields fields) {
        if (getBase() == null) {
            super.assemble(fields);
            fields.monthOfYear = new PersianMonthOfYearDateTimeField(this);
            fields.months = fields.monthOfYear.getDurationField();
            fields.era = ERA_FIELD;
        }
    }

    @Override
    abstract long calculateFirstDayOfYearMillis(int persianYear);

    @Override
    long getApproxMillisAtEpochDividedByTwo() {
        // where epoch is 1970-01-01
        return (1348L * getAverageMillisPerYear()) / 2;
    }

    protected abstract Double getAverageDaysPerYear();

    @Override
    long getAverageMillisPerMonth() {
        return getAverageMillisPerYear() / 12;
    }

    @Override
    long getAverageMillisPerYear() {
        return (long) (getAverageDaysPerYear() * DateTimeConstants.MILLIS_PER_DAY);
    }

    @Override
    long getAverageMillisPerYearDividedByTwo() {
        return getAverageMillisPerYear() / 2;
    }

    @Override
    int getDayOfWeek(long instant) {
        return WEEKDAYS_IN_GREG_ORDER[super.getDayOfWeek(instant) - 1].getWeekDay();
    }

    @Override
    int getDaysInMonthMax(int month) {
        if (month < 7) {
            return FARVARDIN_TO_SHAHRIVAR_MONTH_DAYS;
        } else {
            return MEHR_TO_BAHMAN_MONTH_DAYS;
        }
    }

    @Override
    int getDaysInYearMonth(int year, int month) {
        if (month < 7) {
            return FARVARDIN_TO_SHAHRIVAR_MONTH_DAYS;
        } else if (month < 12 || isLeapYear(year)) {
            return MEHR_TO_BAHMAN_MONTH_DAYS;
        } else {
            return 29;
        }
    }

    protected abstract String getKey();

    @Override
    int getMaxYear() {
        return 2134300219;
    }

    @Override
    int getMinYear() {
        return -2147482492;
    }

    @Override
    int getMonthOfYear(long millis, int year) {
        // Perform a binary search to get the month. To make it go even faster,
        // compare using integers instead of longs. The number of milliseconds
        // per year exceeds the limit of a 32-bit int's capacity, so divide by
        // 1024. No precision is lost (except time of day) since the number of
        // milliseconds per day contains 1024 as a factor. After the division,
        // the instant isn't measured in milliseconds, but in units of
        // (128/125)seconds.
        int i = (int) ((millis - getYearMillis(year)) >> 10);

        // There are 86400000 milliseconds per day, but divided by 1024 is
        // 84375. There are 84375 (128/125)seconds per day.

        return ((i < 186 * 84375) ? ((i < 93 * 84375) ? ((i < FARVARDIN_TO_SHAHRIVAR_MONTH_DAYS * 84375) ? 1
                : (i < 62 * 84375) ? 2 : 3) : ((i < 124 * 84375) ? 4 : (i < 155 * 84375) ? 5 : 6))
                : ((i < 276 * 84375) ? ((i < 216 * 84375) ? 7 : (i < 246 * 84375) ? 8 : 9) : ((i < 306 * 84375) ? 10
                        : (i < 336 * 84375) ? 11 : 12)));
    }

    @Override
    long getTotalMillisByYearMonth(int year, int month) {
        return TOTAL_MILLIS_BY_MONTH_ARRAY[month - 1];
    }

    @Override
    long getYearDifference(long minuendInstant, long subtrahendInstant) {
        // optimsed implementation of getDifference, due to fixed months
        int minuendYear = getYear(minuendInstant);
        int subtrahendYear = getYear(subtrahendInstant);

        // Inlined remainder method to avoid duplicate calls to get.
        long minuendRem = minuendInstant - getYearMillis(minuendYear);
        long subtrahendRem = subtrahendInstant - getYearMillis(subtrahendYear);

        int difference = minuendYear - subtrahendYear;
        if (minuendRem < subtrahendRem) {
            difference--;
        }
        return difference;
    }

    @Override
    abstract boolean isLeapYear(int persianYear);

    protected abstract PersianChronology newInstance(Chronology base, Object param);

    @Override
    long setYear(long instant, int year) {
        int thisYear = getYear(instant);
        int dayOfYear = getDayOfYear(instant, thisYear);
        int millisOfDay = getMillisOfDay(instant);

        if (dayOfYear > 365) {
            // Current year is leap, and day is leap.
            if (!isLeapYear(year)) {
                // Moving to a non-leap year, leap day doesn't exist.
                dayOfYear--;
            }
        }

        instant = getYearMonthDayMillis(year, 1, dayOfYear);
        instant += millisOfDay;
        return instant;
    }

    @Override
    public Chronology withUTC() {
        return withZone(DateTimeZone.UTC);
    }

    @Override
    public Chronology withZone(DateTimeZone zone) {
        if (zone == null) {
            zone = DateTimeZone.getDefault();
        }
        if (zone == getZone()) {
            return this;
        }
        return getInstance(zone, this);
    }

}
