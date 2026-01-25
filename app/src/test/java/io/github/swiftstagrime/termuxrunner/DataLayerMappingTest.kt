package io.github.swiftstagrime.termuxrunner

import io.github.swiftstagrime.termuxrunner.data.local.dto.AutomationExportDto
import io.github.swiftstagrime.termuxrunner.data.local.dto.CategoryExportDto
import io.github.swiftstagrime.termuxrunner.data.local.dto.ScriptExportDto
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationLogEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.CategoryEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.ScriptEntity
import io.github.swiftstagrime.termuxrunner.domain.model.Automation
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationLog
import io.github.swiftstagrime.termuxrunner.domain.model.Category
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import junit.framework.TestCase.fail
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

/**
 * Sanity check tests to ensure that all fields from an Entity are considered
 * when mapping to a Domain Model or a DTO.
 * The Entity is always treated as the source of truth.
 */
class DataLayerMappingTest {
    @Test
    fun `Script - All entity fields are mapped to domain and DTO`() {
        assertAllPropertiesMapped(
            source = ScriptEntity::class,
            destination = Script::class,
        )

        assertAllPropertiesMapped(
            source = Script::class,
            destination = ScriptEntity::class,
        )

        assertAllPropertiesMapped(
            source = ScriptEntity::class,
            destination = ScriptExportDto::class,
            ignoreProperties = setOf("iconPath"),
        )
    }

    @Test
    fun `Automation - All entity fields are mapped to domain and DTO`() {
        assertAllPropertiesMapped(
            source = AutomationEntity::class,
            destination = Automation::class,
        )

        assertAllPropertiesMapped(
            source = Automation::class,
            destination = AutomationEntity::class,
        )

        assertAllPropertiesMapped(
            source = AutomationEntity::class,
            destination = AutomationExportDto::class,
            ignoreProperties =
                setOf(
                    "id",
                    "lastRunTimestamp",
                    "nextRunTimestamp",
                ),
        )
    }

    @Test
    fun `Category - All entity fields are mapped to domain and DTO`() {
        assertAllPropertiesMapped(
            source = CategoryEntity::class,
            destination = Category::class,
        )

        assertAllPropertiesMapped(
            source = Category::class,
            destination = CategoryEntity::class,
        )

        assertAllPropertiesMapped(
            source = CategoryEntity::class,
            destination = CategoryExportDto::class,
        )
    }

    @Test
    fun `AutomationLog - All entity fields are mapped to domain`() {
        assertAllPropertiesMapped(
            source = AutomationLogEntity::class,
            destination = AutomationLog::class,
        )

        assertAllPropertiesMapped(
            source = AutomationLog::class,
            destination = AutomationLogEntity::class,
        )
    }

    private fun assertAllPropertiesMapped(
        source: KClass<*>,
        destination: KClass<*>,
        ignoreProperties: Set<String> = emptySet(),
    ) {
        val sourceProperties = source.memberProperties.map { it.name }.toSet()
        val destProperties = destination.memberProperties.map { it.name }.toSet()

        val missingProperties = (sourceProperties - ignoreProperties) - destProperties

        if (missingProperties.isNotEmpty()) {
            fail(
                "MAPPING INCOMPLETE: The following properties from `${source.simpleName}` " +
                    "are missing in `${destination.simpleName}`: $missingProperties",
            )
        }
    }
}
