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

import org.joda.time.Chronology;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;

/**
 * A <i>proleptic</i> implementation of the Persian calendar using Omar
 * Khayyam's 33 year cycle for it's leap year calculations. The leap years are
 * <a href="http://tehran.stanford.edu/Iran_Lib/Calendar/intro.html"> those with
 * a remainder (after dividing by 33) of 1, 5, 9, 13, 17, 22, 26, and 30.</a>
 * The intercalation method is expected to match the Iranian official calendar
 * based on the Vernal equinox times to the year 1634 AP, and backwards to 1178
 * AP. </br> Thus the following points should be taken into consideration when
 * using this implementation of the Persian calendar ~
 * <ul>
 * <li>The leap year cycle is accurate within the years 1178 AP (Anno Persico)
 * to 1634 AP</li>
 * <li>The max year supported by this implementation is 4503626 AP, and the min
 * is -1. This is because this implementation of the calendar is proleptic and
 * assumes the same leap year cycle forwards and backwards in time</li>
 * <li>From a historical point of view, the calendar follows the current Iranian
 * Civil calendar from 1304 AP (1925 CE) onwards as this is when the Iranian
 * calendar law came in to effect. Before this, <a
 * href="http://www.farsiweb.ir/wiki/FAQ">the length of the Persian months were
 * different from their current lengths. For example, all years included at
 * least one 32-day month</a></li>
 * </ul>
 * 
 * @author Zubin Kavarana
 * @since 2.1
 * 
 */
public class PersianChronologyKhayyam extends PersianChronology {

    private static final double AVERAGE_DAYS_PER_YEAR = 365.24219858156;

    private static final String KEY = "OK";

    /*
     * we use a year when the 33 year cycle starts to calculate leap years
     * passed by
     */
    private static final int REFERENCE_YEAR = 1155;

    private static final long REFERENCE_YEAR_MILLIS = -6115219200000L;

    private static final long serialVersionUID = -5619654259691027165L;

    /**
     * 
     * Method Description: Returns an instance of a PersianChronologyKhayyam in
     * the default JVM time zone
     * 
     * @return - PersianChronologyKhayyam instance
     */
    public static PersianChronology getInstance() {
        return getInstance(DateTimeZone.getDefault(), new PersianChronologyKhayyam());
    }

    /**
     * 
     * Method Description: Returns an instance of a PersianChronologyKhayyam in
     * the given JVM time zone
     * 
     * @return - PersianChronologyKhayyam instance
     */
    public static PersianChronology getInstance(DateTimeZone zone) {
        return getInstance(zone, new PersianChronologyKhayyam());
    }

    /**
     * 
     * Method Description: Returns an instance of a PersianChronologyKhayyam in
     * the UTC time zone
     * 
     * @return - PersianChronologyKhayyam instance
     */
    public static PersianChronology getInstanceUTC() {
        return getInstance(DateTimeZone.UTC, new PersianChronologyKhayyam());
    }

    PersianChronologyKhayyam() {
        super(null, null);
    }

    PersianChronologyKhayyam(Chronology base, Object param) {
        super(base, param);
    }

    @Override
    long calculateFirstDayOfYearMillis(int persianYear) {
        if (persianYear > REFERENCE_YEAR) {
            return calculateFirstDayOfYearMillisIfAfterReferenceYear(persianYear);
        } else if (persianYear < REFERENCE_YEAR) {
            return calculateFirstDayOfYearMillisIfBeforeReferenceYear(persianYear);
        } else {
            return REFERENCE_YEAR_MILLIS;
        }
    }

    private long calculateFirstDayOfYearMillisIfAfterReferenceYear(int persianYear) {
        int yearDifferenceToReferenceYear = persianYear - REFERENCE_YEAR;
        int current33YearCycleStart = persianYear - (persianYear % 33);
        int leapYears = ((current33YearCycleStart - REFERENCE_YEAR) / 33) * 8;
        for (int i = current33YearCycleStart; i < persianYear; i++) {
            if (isLeapYear(i)) {
                leapYears = leapYears + 1;
            }
        }
        long numberOfDays = (yearDifferenceToReferenceYear * 365) + leapYears;
        long millis = numberOfDays * DateTimeConstants.MILLIS_PER_DAY;
        return REFERENCE_YEAR_MILLIS + millis;
    }

    private long calculateFirstDayOfYearMillisIfBeforeReferenceYear(int persianYear) {
        int yearDifferenceToReferenceYear = REFERENCE_YEAR - persianYear;
        int current33YearCycleStart = persianYear - (persianYear % 33);
        int leapYears = ((REFERENCE_YEAR - current33YearCycleStart) / 33) * 8;
        for (int i = current33YearCycleStart; i < persianYear; i++) {
            if (isLeapYear(i)) {
                leapYears = leapYears - 1;
            }
        }
        long numberOfDays = (yearDifferenceToReferenceYear * 365) + leapYears;
        long millis = numberOfDays * DateTimeConstants.MILLIS_PER_DAY;
        return REFERENCE_YEAR_MILLIS - millis;
    }

    @Override
    protected Double getAverageDaysPerYear() {
        return AVERAGE_DAYS_PER_YEAR;
    }

    @Override
    protected String getKey() {
        return KEY;
    }

    @Override
    int getMaxYear() {
        return 4503626;
    }

    @Override
    int getMinYear() {
        return -1;
    }

    @Override
    boolean isLeapYear(int persianYear) {
        return ((persianYear * 8 + 29) % 33) < 8;
    }

    @Override
    protected PersianChronology newInstance(Chronology base, Object param) {
        return new PersianChronologyKhayyam(base, param);
    }

}
