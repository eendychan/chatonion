package com.flxrs.dankchat.data.repo.data

import android.content.res.Resources
import androidx.annotation.StringRes
import com.flxrs.dankchat.R
import com.flxrs.dankchat.data.DisplayName
import com.flxrs.dankchat.data.UserId
import com.flxrs.dankchat.data.UserName
import com.flxrs.dankchat.utils.extensions.partitionIsInstance

sealed interface DataLoadingStep {
    @get:StringRes
    val displayNameRes: Int
    val channel: UserName? get() = null

    data object DankChatBadges : DataLoadingStep {
        override val displayNameRes = R.string.data_loading_step_dankchat_badges
    }

    data object HomiesBadges : DataLoadingStep {
        override val displayNameRes = R.string.data_loading_step_homies_badges
    }

    data object FFZBadges : DataLoadingStep {
        override val displayNameRes = R.string.data_loading_step_ffz_badges
    }

    data object BTTVBadges : DataLoadingStep {
        override val displayNameRes = R.string.data_loading_step_bttv_badges
    }

    data object GlobalBadges : DataLoadingStep {
        override val displayNameRes = R.string.data_loading_step_global_badges
    }

    data object GlobalFFZEmotes : DataLoadingStep {
        override val displayNameRes = R.string.data_loading_step_global_ffz_emotes
    }

    data object GlobalBTTVEmotes : DataLoadingStep {
        override val displayNameRes = R.string.data_loading_step_global_bttv_emotes
    }

    data object GlobalSevenTVEmotes : DataLoadingStep {
        override val displayNameRes = R.string.data_loading_step_global_7tv_emotes
    }

    data object TwitchEmotes : DataLoadingStep {
        override val displayNameRes = R.string.data_loading_step_twitch_emotes
    }

    data class ChannelBadges(
        override val channel: UserName,
        val channelId: UserId,
    ) : DataLoadingStep {
        override val displayNameRes = R.string.data_loading_step_channel_badges
    }

    data class ChannelFFZEmotes(
        override val channel: UserName,
        val channelId: UserId,
    ) : DataLoadingStep {
        override val displayNameRes = R.string.data_loading_step_ffz_emotes
    }

    data class ChannelBTTVEmotes(
        override val channel: UserName,
        val channelDisplayName: DisplayName,
        val channelId: UserId,
    ) : DataLoadingStep {
        override val displayNameRes = R.string.data_loading_step_bttv_emotes
    }

    data class ChannelSevenTVEmotes(
        override val channel: UserName,
        val channelId: UserId,
    ) : DataLoadingStep {
        override val displayNameRes = R.string.data_loading_step_7tv_emotes
    }

    data class ChannelCheermotes(
        override val channel: UserName,
        val channelId: UserId,
    ) : DataLoadingStep {
        override val displayNameRes = R.string.data_loading_step_cheermotes
    }

    data class ChannelFollowerEmotes(
        override val channel: UserName,
        val channelId: UserId,
    ) : DataLoadingStep {
        override val displayNameRes = R.string.data_loading_step_follower_emotes
    }
}

fun List<DataLoadingStep>.toDisplayStrings(resources: Resources): List<String> {
    val (badges, notBadges) = partitionIsInstance<DataLoadingStep.ChannelBadges, _>()
    val (ffz, notFfz) = notBadges.partitionIsInstance<DataLoadingStep.ChannelFFZEmotes, _>()
    val (bttv, notBttv) = notFfz.partitionIsInstance<DataLoadingStep.ChannelBTTVEmotes, _>()
    val (sevenTv, rest) = notBttv.partitionIsInstance<DataLoadingStep.ChannelSevenTVEmotes, _>()

    return buildList {
        addAll(rest.map { resources.getString(it.displayNameRes) })

        if (badges.isNotEmpty()) {
            val channels = badges.joinToString { it.channel.value }
            add(resources.getString(R.string.data_loading_step_with_channels, resources.getString(R.string.data_loading_step_channel_badges), channels))
        }
        if (ffz.isNotEmpty()) {
            val channels = ffz.joinToString { it.channel.value }
            add(resources.getString(R.string.data_loading_step_with_channels, resources.getString(R.string.data_loading_step_ffz_emotes), channels))
        }
        if (bttv.isNotEmpty()) {
            val channels = bttv.joinToString { it.channel.value }
            add(resources.getString(R.string.data_loading_step_with_channels, resources.getString(R.string.data_loading_step_bttv_emotes), channels))
        }
        if (sevenTv.isNotEmpty()) {
            val channels = sevenTv.joinToString { it.channel.value }
            add(resources.getString(R.string.data_loading_step_with_channels, resources.getString(R.string.data_loading_step_7tv_emotes), channels))
        }
    }
}
