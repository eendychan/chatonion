package com.flxrs.dankchat.ui.main.dialog

import androidx.compose.runtime.Immutable
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.data.repo.crash.CrashEntry

@Immutable
data class DialogState(
    val showAddChannel: Boolean = false,
    val showManageChannels: Boolean = false,
    val showRemoveChannel: Boolean = false,
    val removeChannelTarget: UserName? = null,
    val showBlockChannel: Boolean = false,
    val blockChannelTarget: UserName? = null,
    val showLogout: Boolean = false,
    val showNewWhisper: Boolean = false,
    val pendingUploadAction: (() -> Unit)? = null,
    val isUploading: Boolean = false,
    val crashEntry: CrashEntry? = null,
)
