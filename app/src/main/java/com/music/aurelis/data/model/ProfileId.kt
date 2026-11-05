package com.music.aurelis.data.model

/** Stable local identity, independent of the editable display name or avatar. */
data class ProfileId(val value: String) {
    init {
        require(value.matches(Regex("[a-z0-9][a-z0-9_-]{0,63}"))) {
            "Profile IDs must contain 1–64 lowercase letters, digits, underscores or hyphens"
        }
    }

    companion object {
        /** Existing installations belong to this profile without copying their data. */
        val DEFAULT = ProfileId("default")
    }
}
