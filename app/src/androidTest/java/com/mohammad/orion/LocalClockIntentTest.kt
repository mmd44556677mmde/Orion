package com.mohammad.orion

import android.provider.AlarmClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mohammad.orion.domain.tools.OfflineCommandParser
import com.mohammad.orion.domain.tools.OfflineParseResult
import com.mohammad.orion.ui.tools.clockIntent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalClockIntentTest {
    @Test fun parsedTimerProducesReviewableClockIntent() {
        val intent = clockIntent(OfflineCommandParser().parse("تایمر برای ۵ دقیقه"))!!
        assertEquals(AlarmClock.ACTION_SET_TIMER, intent.action)
        assertEquals(300, intent.getIntExtra(AlarmClock.EXTRA_LENGTH, -1))
        assertFalse(intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, true))
    }
    @Test fun alarmAndInvalidOrUnrelatedCommandsAreBounded() {
        val intent = clockIntent(OfflineCommandParser().parse("alarm at 18:30"))!!
        assertEquals(AlarmClock.ACTION_SET_ALARM, intent.action)
        assertEquals(18, intent.getIntExtra(AlarmClock.EXTRA_HOUR, -1))
        assertEquals(30, intent.getIntExtra(AlarmClock.EXTRA_MINUTES, -1))
        assertFalse(intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, true))
        assertNull(clockIntent(OfflineParseResult.Timer(0)))
        assertNull(clockIntent(OfflineParseResult.Alarm(24, 0)))
        assertNull(clockIntent(OfflineParseResult.Flashlight(true)))
    }
}
