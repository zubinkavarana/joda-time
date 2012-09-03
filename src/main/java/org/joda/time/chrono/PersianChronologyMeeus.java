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
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;

/**
 * An implementation of the Persian calendar using Vernal equinox times
 * calculated from formulas published in <i>Astronomical Tables of the Sun,
 * Moon, and Planets</i>, by Jean Meeus, 1983. Meeus based his calculations on
 * Simon Newcomb's theory as given in <i>Astronomical Papers American
 * Ephemeris</i>, 1895. The equinox accuracy matches records from <a
 * href="http://aa.usno.navy.mil/data/docs/EarthSeasons.php">USNO</a> data for
 * the past decade.
 * 
 * </br> Thus the following points should be taken into consideration when using
 * this implementation of the Persian calendar ~
 * <ul>
 * <li>The max year supported by this implementation is 2379 AP (3000 CE), and
 * the min is -1621 AP (1000 BC)</li>
 * <li>The equinox time calculation is split into two steps. The first step is a
 * standard method to find the equinox in Terrestrial Time - TT denotes time in
 * a theoretical situation that the Earths angle of rotation is constant. In
 * reality the Earth wobbles as it revolves on it's axis and the real time it
 * takes between equinoxes is called Universal Time. The second step is to
 * convert TT to UT. The formula to convert TT to UT differs in various theories
 * and texts. The formula used in this implementation converts TT to UT using
 * formula from <a
 * href="http://eclipse.gsfc.nasa.gov/SEhelp/deltatpoly2004.html">Polynomial
 * Expressions for Delta T</a></li>
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
public class PersianChronologyMeeus extends PersianChronology {

    /**
     * A helper class that provides astronomical calculations to help in
     * calculating the instant of the Vernal equinox.
     * 
     * @author Zubin
     * @since 2.1
     * 
     */
    public class Astronomical {

        /**
         * 
         * Method Description: Returns the difference between Terrestrial Time
         * and Universal Time for a given year and month. Adapted from <a
         * href="http://eclipse.gsfc.nasa.gov/SEhelp/deltatpoly2004.html">NASA
         * polynomial expressions for Delta T</a>.
         * 
         * For further information see -
         * 
         * <a href="http://eclipse.gsfc.nasa.gov/SEhelp/deltaT.html">Delta T and
         * Universal Time</a>,
         * 
         * <a href="http://eclipse.gsfc.nasa.gov/SEhelp/deltat2004.html">
         * Historical Values of Delta T</a>,
         * 
         * <a href="http://asa.usno.navy.mil/SecK/DeltaT.html">Delta T: Past,
         * Present and Future</a>,
         * 
         * 
         * @param isoYear
         * @return deltaT in seconds
         */
        public Double getDeltaT(int isoYear, int month) {
            Double y = isoYear + new Double(month - 0.5) / 12.0;
            Double deltaT = 0.0;
            if (isoYear < -500) {
                Double u = new Double(isoYear - 1820.0) / 100.0;
                deltaT = -20.0 + (32.0 * Math.pow(u, 2));
            }
            if (isoYear >= -500 && isoYear < 500) {
                Double u = y / 100.0;
                deltaT = 10583.6 - (1014.41 * u) + (33.78311 * Math.pow(u, 2)) - (5.952053 * Math.pow(u, 3))
                        - (0.1798452 * Math.pow(u, 4)) + (0.022174192 * Math.pow(u, 5)) + (0.0090316521 * Math.pow(u, 6));
            }
            if (isoYear >= 500 && isoYear < 1600) {
                Double u = (y - 1000.0) / 100.0;
                deltaT = 1574.2 - (556.01 * u) + (71.23472 * Math.pow(u, 2)) + (0.319781 * Math.pow(u, 3))
                        - (0.8503463 * Math.pow(u, 4)) - (0.005050998 * Math.pow(u, 5)) + (0.0083572073 * Math.pow(u, 6));
            }
            if (isoYear >= 1600 && isoYear < 1700) {
                Double t = y - 1600.0;
                deltaT = 120 - (0.9808 * t) - (0.01532 * Math.pow(t, 2)) + (Math.pow(t, 3) / 7129.0);
            }
            if (isoYear >= 1700 && isoYear < 1800) {
                Double t = y - 1700.0;
                deltaT = 8.83 + (0.1603 * t) - (0.0059285 * Math.pow(t, 2)) + (0.00013336 * Math.pow(t, 3))
                        - (Math.pow(t, 4) / 1174000.0);
            }
            if (isoYear >= 1800 && isoYear < 1860) {
                Double t = y - 1800.0;
                deltaT = 13.72 - (0.332447 * t) + (0.0068612 * Math.pow(t, 2)) + (0.0041116 * Math.pow(t, 3))
                        - (0.00037436 * Math.pow(t, 4)) + (0.0000121272 * Math.pow(t, 5)) - (0.0000001699 * Math.pow(t, 6))
                        + (0.000000000875 * Math.pow(t, 7));
            }
            if (isoYear >= 1860 && isoYear < 1900) {
                Double t = y - 1860.0;
                deltaT = 7.62 + (0.5737 * t) - (0.251754 * Math.pow(t, 2)) + (0.01680668 * Math.pow(t, 3))
                        - (0.0004473624 * Math.pow(t, 4)) + (Math.pow(t, 5) / 233174.0);
            }
            if (isoYear >= 1900 && isoYear < 1920) {
                Double t = y - 1900.0;
                deltaT = -2.79 + (1.494119 * t) - (0.0598939 * Math.pow(t, 2)) + (0.0061966 * Math.pow(t, 3))
                        - (0.000197 * Math.pow(t, 4));
            }
            if (isoYear >= 1920 && isoYear < 1941) {
                Double t = y - 1920.0;
                deltaT = 21.20 + (0.84493 * t) - (0.076100 * Math.pow(t, 2)) + (0.0020936 * Math.pow(t, 3));
            }
            if (isoYear >= 1941 && isoYear < 1961) {
                Double t = y - 1950.0;
                deltaT = 29.07 + (0.407 * t) - (Math.pow(t, 2) / 233.0) + (Math.pow(t, 3) / 2547.0);
            }
            if (isoYear >= 1961 && isoYear < 1986) {
                Double t = y - 1975.0;
                deltaT = 45.45 + (1.067 * t) - (Math.pow(t, 2) / 260.0) - (Math.pow(t, 3) / 718.0);
            }
            if (isoYear >= 1986 && isoYear < 2005) {
                Double t = y - 2000.0;
                deltaT = 63.86 + (0.3345 * t) - (0.060374 * Math.pow(t, 2)) + (0.0017275 * Math.pow(t, 3))
                        + (0.000651814 * Math.pow(t, 4)) + (0.00002373599 * Math.pow(t, 5));
            }
            if (isoYear >= 2005 && isoYear < 2050) {
                Double t = y - 2000.0;
                deltaT = 62.92 + (0.32217 * t) + (0.005589 * Math.pow(t, 2));
            }
            if (isoYear >= 2050 && isoYear < 2150) {
                deltaT = -20.0 + (32.0 * (Math.pow(((y - 1820.0) / 100.0), 2))) - (0.5628 * (2150.0 - y));
            }
            if (isoYear >= 2150) {
                Double u = (isoYear - 1820.0) / 100.0;
                deltaT = -20.0 + (32.0 * Math.pow(u, 2));
            }
            if (isoYear >= 1955 && isoYear <= 2005) {
                return deltaT;
            } else {
                Double c = -0.000012932 * Math.pow((y - 1955), 2);
                return deltaT + c;
            }
        }

        /**
         * 
         * Method Description: gets the spring equinox instant for a year using
         * formula from <a
         * href="http://www.hermetic.ch/cal_stud/cassidy/calquest.htm">Meeus
         * ("Astronomical Algorithms", 1991)</a> and <a
         * href="http://www.jgiesen.de/astro/astroJS/seasons/index.htm"
         * >Equinoxes and Solstices</a>
         * 
         * @param isoYear
         *            - year in ISO chronology
         * @return DateTime instance in ISOChronology UTC zone representing the
         *         Vernal equinox for the given year
         */
        public DateTime getSpringVernalEquinoxInstantAtUTC(int isoYear) {
            Double jdme = 0.0;
            if (isoYear < 1000) {
                // from -1000 BC to 999 AD
                // convert AD year to millenia, from 0 AD (1 BC).
                Double millenia = new Double(isoYear) / 1000.0;
                jdme = 1721139.29189 + (365242.13740 * millenia) + (0.06134 * Math.pow(millenia, 2))
                        - (0.00111 * Math.pow(millenia, 3)) - (0.00071 * Math.pow(millenia, 4));
            } else {
                // 1000 AD onwards
                // convert AD year to millenia, from 2000 AD.
                Double millenia = new Double(isoYear - 2000.0) / 1000.0;
                jdme = 2451623.80984 + (365242.37404 * millenia) + (0.05169 * Math.pow(millenia, 2))
                        - (0.00411 * Math.pow(millenia, 3)) - (0.00057 * Math.pow(millenia, 4));
            }

            Double t = new Double(jdme - 2451545.0) / 36525;
            Double w = ((35999.373 * t) - 2.47);// degrees
            w = w * piBy180;
            Double deltaLambda = 1.0 + (0.0334 * Math.cos(w)) + (0.0007 * Math.cos(2 * w));
            Double sumPerturbations = getSumOfPerturbations(t);

            Double julianDayTerrestrialTime = jdme + ((0.00001 * sumPerturbations) / deltaLambda)
                    - ((66.0 + (isoYear - 2000) * 1.0) / 86400.0);
            int secondsToCorrect = getDeltaT(isoYear, 3).intValue();
            if (secondsToCorrect > 0) {
                return DateTimeUtils.julianDayToISODateTimeAtUTC(julianDayTerrestrialTime).minusSeconds(secondsToCorrect);
            } else if (secondsToCorrect < 0) {
                secondsToCorrect = -1 * secondsToCorrect;
                return DateTimeUtils.julianDayToISODateTimeAtUTC(julianDayTerrestrialTime).plusSeconds(secondsToCorrect);
            } else {
                return DateTimeUtils.julianDayToISODateTimeAtUTC(julianDayTerrestrialTime);
            }
        }

        private Double getSumOfPerturbations(Double t) {
            Double sumPerturbations = (485 * Math.cos(piBy180 * (324.96 + (1934.136 * t))))
                    + (203 * Math.cos(piBy180 * (337.23 + (32964.467 * t))))
                    + (199 * Math.cos(piBy180 * (342.08 + (20.186 * t))))
                    + (182 * Math.cos(piBy180 * (27.85 + (445267.112 * t))))
                    + (156 * Math.cos(piBy180 * (73.14 + (45036.886 * t))))
                    + (136 * Math.cos(piBy180 * (171.52 + (22518.443 * t))))
                    + (77 * Math.cos(piBy180 * (222.54 + (65928.934 * t))))
                    + (74 * Math.cos(piBy180 * (296.72 + (3034.906 * t)))) + (70 * Math.cos(piBy180 * (243.58 + (9037.513 * t))))
                    + (58 * Math.cos(piBy180 * (119.81 + (33718.147 * t)))) + (52 * Math.cos(piBy180 * (297.17 + (150.678 * t))))
                    + (50 * Math.cos(piBy180 * (21.02 + (2281.226 * t)))) + (45 * Math.cos(piBy180 * (247.54 + (29929.562 * t))))
                    + (44 * Math.cos(piBy180 * (325.15 + (31555.956 * t)))) + (29 * Math.cos(piBy180 * (60.93 + (4443.417 * t))))
                    + (18 * Math.cos(piBy180 * (155.12 + (67555.328 * t))))
                    + (17 * Math.cos(piBy180 * (288.79 + (4562.452 * t))))
                    + (16 * Math.cos(piBy180 * (198.04 + (62894.029 * t))))
                    + (14 * Math.cos(piBy180 * (199.76 + (31436.921 * t))))
                    + (12 * Math.cos(piBy180 * (95.39 + (14577.848 * t))))
                    + (12 * Math.cos(piBy180 * (287.11 + (31931.756 * t))))
                    + (12 * Math.cos(piBy180 * (320.81 + (34777.259 * t)))) + (9 * Math.cos(piBy180 * (227.73 + (1222.114 * t))))
                    + (8 * Math.cos(piBy180 * (15.45 + (16859.074 * t))));
            return sumPerturbations;
        }
    }

    private static final double AVERAGE_DAYS_PER_YEAR = 365.24223034734916;

    private static final String KEY = "AS";

    private static final int PERSIAN_TO_ISO_YEAR_DIFFERENCE = 621;

    private static final Double piBy180 = Math.PI / 180.0;

    private static final long serialVersionUID = -952499136332981902L;

    /**
     * 
     * Method Description: Returns an instance of a PersianChronologyMeeus in
     * the default JVM time zone, using Iran Standard Time for equinox
     * calculations
     * 
     * @return - PersianChronologyMeeus instance
     */
    public static PersianChronology getInstance() {
        return getInstance(DateTimeZone.getDefault(), new PersianChronologyMeeus());
    }

    /**
     * 
     * Method Description: Returns an instance of a PersianChronologyMeeus in
     * the given time zone, using Iran Standard Time for equinox calculations
     * 
     * @param zone
     *            - the time zone to be used by the chronology
     * @return - PersianChronologyMeeus instance
     */
    public static PersianChronology getInstance(DateTimeZone zone) {
        return getInstance(zone, new PersianChronologyMeeus());
    }

    /**
     * 
     * Method Description: The Persian New Year starts based on the instant of
     * the Vernal Equinox (Spring equinox in the Northern Hemisphere). That
     * instant may be different depending on if the instant is measured from the
     * Iran Standard Time longitude or from the Tehran Longitude.
     * 
     * @param zone
     *            - the time zone to be used by the chronology
     * @param calendarLongitudeDegrees
     *            - the degrees component of the longitude to be used
     * @param calendarLongitudeMinutes
     *            - the minutes component of the longitude to be used
     * @param calendarLongitudeSeconds
     *            - the seconds component of the longitude to be used
     * @return - PersianChronologyMeeus instance that uses the set longitude to
     *         calculate Vernal equinoxes
     */
    public static PersianChronology getInstance(DateTimeZone zone, int calendarLongitudeDegrees, int calendarLongitudeMinutes,
            int calendarLongitudeSeconds) {
        return PersianChronology.getInstance(zone, new PersianChronologyMeeus(calendarLongitudeDegrees, calendarLongitudeMinutes,
                calendarLongitudeSeconds));
    }

    /**
     * 
     * Method Description: Returns an instance of a PersianChronologyMeeus in
     * the UTC time zone, using Iran Standard Time for equinox calculations
     * 
     * @return - PersianChronologyMeeus instance
     */
    public static PersianChronology getInstanceUTC() {
        return getInstance(DateTimeZone.UTC, new PersianChronologyMeeus());
    }

    private final int utcToJalaalihourOffset;

    private final int utcToJalaaliLongitudeDegreeOffset;

    private final int utcToJalaaliLongitudeMinuteOffset;

    private final int utcToJalaaliLongitudeSecondOffset;

    // restricted constructor
    PersianChronologyMeeus() {
        super(null, null);
        this.utcToJalaaliLongitudeDegreeOffset = 51;
        this.utcToJalaalihourOffset = 3;
        this.utcToJalaaliLongitudeMinuteOffset = 30;
        this.utcToJalaaliLongitudeSecondOffset = 0;
    }

    // restricted constructor
    PersianChronologyMeeus(Chronology base, Object param, int calendarLongitudeDegrees, int calendarLongitudeMinutes,
            int calendarLongitudeSeconds) {
        super(base, param);
        this.utcToJalaaliLongitudeDegreeOffset = calendarLongitudeDegrees;
        this.utcToJalaalihourOffset = 3;
        this.utcToJalaaliLongitudeMinuteOffset = calendarLongitudeMinutes;
        this.utcToJalaaliLongitudeSecondOffset = calendarLongitudeSeconds;
    }

    // restricted constructor
    // some might prefer the Tehran longitude 51degrees 25minutes 33seconds
    PersianChronologyMeeus(int calendarLongitudeDegrees, int calendarLongitudeMinutes, int calendarLongitudeSeconds) {
        super(null, null);
        this.utcToJalaaliLongitudeDegreeOffset = calendarLongitudeDegrees;
        this.utcToJalaalihourOffset = 3;
        this.utcToJalaaliLongitudeMinuteOffset = calendarLongitudeMinutes;
        this.utcToJalaaliLongitudeSecondOffset = calendarLongitudeSeconds;
    }

    @Override
    long calculateFirstDayOfYearMillis(int persianYear) {
        int isoYear = persianYear + PERSIAN_TO_ISO_YEAR_DIFFERENCE;
        int yearStart = getFirstDayOfISOYearAtJalaaliLongitude(isoYear);
        long millisUTC = new DateTime(isoYear, 3, yearStart, 0, 0, 0, 0, ISOChronology.getInstanceUTC()).getMillis();
        return millisUTC;
    }

    @Override
    protected Double getAverageDaysPerYear() {
        return AVERAGE_DAYS_PER_YEAR;
    }

    private int getFirstDayOfISOYearAtJalaaliLongitude(int isoYear) {
        DateTime springEquinox = new Astronomical().getSpringVernalEquinoxInstantAtUTC(isoYear);
        int day = springEquinox.getDayOfMonth();
        int hour = springEquinox.getHourOfDay() + utcToJalaalihourOffset;

        if (hour > 23) {
            hour = hour - 24;
            day++;
        }

        int minute = springEquinox.getMinuteOfHour() + utcToJalaaliLongitudeMinuteOffset;

        // some correction to be done here but how
        int second = springEquinox.getSecondOfMinute();
        if (second > 30) {
            // round up the minute
            minute++;
        }
        if (minute > 59) {
            minute = minute - 60;
            hour++;
        }
        if (!(hour < 12)) {
            day++;
        }
        return day;
    }

    @Override
    protected String getKey() {
        return KEY;
    }

    @Override
    int getMaxYear() {
        return 3000 - PERSIAN_TO_ISO_YEAR_DIFFERENCE;
    }

    @Override
    int getMinYear() {
        return -1000 - PERSIAN_TO_ISO_YEAR_DIFFERENCE;
    }

    @Override
    boolean isLeapYear(int persianYear) {
        int isoYear = persianYear + PERSIAN_TO_ISO_YEAR_DIFFERENCE;
        int thisYearStart = getFirstDayOfISOYearAtJalaaliLongitude(isoYear);
        int nextYearStart = getFirstDayOfISOYearAtJalaaliLongitude(isoYear + 1);
        return Days.daysBetween(new DateTime(isoYear, 3, thisYearStart, 0, 0), new DateTime(isoYear + 1, 3, nextYearStart, 0, 0))
                .getDays() > 365;
    }

    @Override
    protected PersianChronology newInstance(Chronology base, Object param) {
        return new PersianChronologyMeeus(base, param, utcToJalaaliLongitudeDegreeOffset, utcToJalaaliLongitudeMinuteOffset,
                utcToJalaaliLongitudeSecondOffset);
    }
}
