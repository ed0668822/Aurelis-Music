package com.music.aurelis.ui.navigation

import com.music.aurelis.AurelisApplication
import com.music.aurelis.ui.AppUi
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = AurelisApplication::class)
class LocalAppHostTest {
    @Test fun applicationInstallsTheActualPersistentLocalLibraryBeforeUiAccess() {
        val app = RuntimeEnvironment.getApplication() as AurelisApplication
        assertSame(app.userLibrary, AppUi.host.libraries.value)
        val id = app.userLibrary.createPlaylist("Lista local")
        assertEquals(id, AppUi.host.libraries.value.state.value.playlists.single().id)
    }
}
