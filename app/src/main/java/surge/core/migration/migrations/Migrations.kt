package surge.core.migration.migrations

import surge.core.migration.Migration

val migrations: List<Migration>
    get() = listOf(
        SetupBackupCreateMigration(),
        SetupLibraryUpdateMigration(),
        TrustExtensionRepositoryMigration(),
        CategoryPreferencesCleanupMigration(),
        InstallationIdMigration(),
        VerticalNavigatorMigration(),
    )
