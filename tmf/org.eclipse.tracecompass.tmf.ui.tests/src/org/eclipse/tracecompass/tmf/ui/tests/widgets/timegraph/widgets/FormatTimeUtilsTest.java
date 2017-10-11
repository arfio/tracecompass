/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.tests.widgets.timegraph.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils.Resolution;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils.TimeFormat;
import org.junit.Test;

/**
 * Test the format time methods in {@link Utils}
 *
 * @author Simon Delisle
 */
public class FormatTimeUtilsTest {

    /**
     * Test methods FormatTime using TimeFormat.CALENDAR
     */
    @Test
    public void testFormatTimeCalendar() {
        String calendarTime = Utils.formatTime(37935447675l, TimeFormat.CALENDAR, Resolution.NANOSEC);
        assertTrue(calendarTime.endsWith("37.935447675"));
    }

    /**
     * Test methods FormatTime using TimeFormat.RELATIVE
     */
    @Test
    public void testFormatTimeRelative() {
        // 5 ns
        String relativeNs = Utils.formatTime(5l, TimeFormat.RELATIVE, Resolution.NANOSEC);
        assertEquals("0.000000005s (5 ns)", relativeNs);

        // 15 µs
        String relativeUs = Utils.formatTime(15000l, TimeFormat.RELATIVE, Resolution.NANOSEC);
        assertEquals("0.000015000s (15 µs)", relativeUs);

        // 250 ms
        String relativeMs = Utils.formatTime(250000000l, TimeFormat.RELATIVE, Resolution.NANOSEC);
        assertEquals("0.250000000s (250 ms)", relativeMs);

        // 3.123456s
        String relativeSeconds = Utils.formatTime(3123456000l, TimeFormat.RELATIVE, Resolution.NANOSEC);
        assertEquals("3.123456000s", relativeSeconds);

        // 2 min
        String relativeMinutes = Utils.formatTime(120000000000l, TimeFormat.RELATIVE, Resolution.NANOSEC);
        assertEquals("120.000000000s", relativeMinutes);

        // 2 min 18s
        relativeMinutes = Utils.formatTime(138000000000l, TimeFormat.RELATIVE, Resolution.NANOSEC);
        assertEquals("138.000000000s", relativeMinutes);

        // 1 hours
        String relativeHours = Utils.formatTime(3600000000000l, TimeFormat.RELATIVE, Resolution.NANOSEC);
        assertEquals("3600.000000000s", relativeHours);
    }
}