package io.github.swiftstagrime.termuxrunner

import android.content.ComponentName
import io.github.swiftstagrime.termuxrunner.data.service.MasterScriptTileService
import io.github.swiftstagrime.termuxrunner.data.service.ScriptTileService1
import io.github.swiftstagrime.termuxrunner.data.service.ScriptTileService2
import io.github.swiftstagrime.termuxrunner.data.service.ScriptTileService3
import io.github.swiftstagrime.termuxrunner.data.service.ScriptTileService4
import io.github.swiftstagrime.termuxrunner.data.service.ScriptTileService5
import io.github.swiftstagrime.termuxrunner.data.service.tileIndexForComponent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(application = TestApplication::class, sdk = [34])
@RunWith(RobolectricTestRunner::class)
class ScriptTileMappingTest {
    @Test
    fun `each script tile component maps to its index`() {
        val components =
            mapOf(
                ScriptTileService1::class.java to 1,
                ScriptTileService2::class.java to 2,
                ScriptTileService3::class.java to 3,
                ScriptTileService4::class.java to 4,
                ScriptTileService5::class.java to 5,
            )

        for ((serviceClass, expectedIndex) in components) {
            val component =
                ComponentName("io.github.swiftstagrime.termuxrunner", serviceClass.name)
            assertEquals(
                "unexpected index for ${serviceClass.simpleName}",
                expectedIndex,
                tileIndexForComponent(component),
            )
        }
    }

    @Test
    fun `master tile and unknown components have no index`() {
        assertNull(
            tileIndexForComponent(
                ComponentName(
                    "io.github.swiftstagrime.termuxrunner",
                    MasterScriptTileService::class.java.name,
                ),
            ),
        )
        assertNull(
            tileIndexForComponent(
                ComponentName("io.github.swiftstagrime.termuxrunner", "OtherService"),
            ),
        )
        assertNull(tileIndexForComponent(null))
    }
}
