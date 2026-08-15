package de.iltix.patcher

import java.io.File

/**
 * Applies all source code patches (Kotlin files) to hook Iltix into upstream Element X.
 * Each method patches one upstream file with the required Iltix imports and code insertions.
 *
 * IMPORTANT: All patterns use regex anchors against stable upstream code structures,
 * never line numbers. If upstream refactors a file significantly, the patch will fail
 * loudly (PatchVerifier catches it) rather than silently corrupting code.
 */
class SourcePatches(private val engine: PatchEngine) {

    fun applyAll() {
        println("\n--- Applying source patches ---")
        patchMainActivity()
        patchLoggedInFlowNode()
        patchHomeView()
        patchHomeTopBar()
        patchSpaceFiltersPresenter()
        patchRoomListRoomSummaryModel()
        patchRoomListRoomSummaryFactory()
        patchRoomListPresenter()
        patchRoomSummaryRow()
        patchRoomListContentView()
        patchMessagesView()
        patchTypingNotificationPresenter()
        patchMessageComposerEvent()
        patchMessageComposerState()
        patchMessageComposerStateProvider()
        patchMessageComposerPresenter()
        patchMessageComposerView()
        patchTextComposer()
        // Temporarily disabled: upstream top bar signature changed and breaks dmUserStatus patch.
        // patchMessagesViewTopBar()
        patchThreadTopBar()
        patchTimelineItemEventRow()
        patchMessageEventBubble()
        patchTimelineItemVoiceView()
        patchTimelineItemPollView()
        patchTimelineItemPollContent()
        patchTimelineItemContentPollFactory()
        patchPollContentView()
        patchPollAnswerView()
        patchRoomDetailsPresenter()
        patchRoomDetailsView()
        patchUserProfileView()
        patchNotificationRenderer()
        patchNotificationConversationService()
        patchNotificationCreator()
        patchNotificationChannels()
        patchPushManifestBootReceiver()
        patchVectorFirebaseMessagingService()
        patchVectorUnifiedPushMessagingReceiver()
        patchFetchPendingNotificationsWorker()
        patchDefaultNotifiableEventResolver()
        patchSenderName()
        patchPreferencesFlowNode()
        patchPreferencesRootNode()
        patchPreferencesRootView()
        patchMarkdownTextEditorState()
        patchTextEditorState()
        patchTypographyTokens()
        patchElementThemeTypography()

        val results = engine.getResults()
        val failures = engine.failedResults()
        println("  Source patches: ${results.size - failures.size} succeeded, ${failures.size} failed")
        if (failures.isNotEmpty()) {
            failures.forEach { println("    ✗ ${it.file}: ${it.operation} — ${it.message}") }
        }
    }

    // ===== Theme hooks =====

    private fun patchMainActivity() {
        val path = "app/src/main/kotlin/io/element/android/x/MainActivity.kt"
        engine.addImport(path, "de.iltix.theme.IxElementThemeApp")
        engine.replaceText(path, "ElementThemeApp(", "IxElementThemeApp(")
    }

    private fun patchLoggedInFlowNode() {
        val path = "appnav/src/main/kotlin/io/element/android/appnav/LoggedInFlowNode.kt"
        engine.addImport(path, "de.iltix.theme.IxElementThemeApp")
        engine.replaceText(path, "ElementThemeApp(", "IxElementThemeApp(")
    }

    // ===== Home hooks =====

    private fun patchHomeView() {
        val path = "features/home/impl/src/main/kotlin/io/element/android/features/home/impl/HomeView.kt"
        engine.addImport(path, "de.iltix.home.IxHomeChatsContent")
        engine.addImport(path, "de.iltix.home.rememberIxHomeUiConfig")

        // Insert IxHomeUiConfig initialization after the roomListState declaration
        engine.insertAfterLine(
            path,
            """val roomListState.*=.*\.roomListState""",
            """
    val ixHomeUi = rememberIxHomeUiConfig(
        currentHomeNavigationBarItem = state.currentHomeNavigationBarItem,
        roomListState = roomListState,
    )""",
            "HomeView: ixHomeUi initialization"
        )

        // Add useIltixTheme to HomeTopBar call
        engine.replaceText(
            path,
            """            HomeTopBar(
                selectedNavigationItem = state.currentHomeNavigationBarItem,""",
            """            HomeTopBar(
                selectedNavigationItem = state.currentHomeNavigationBarItem,
                useIltixTheme = ixHomeUi.useIltixTheme,
                shouldShowIxSpaceNav = ixHomeUi.shouldShowIxSpaceNav,"""
        )

        // Add showStartChatInTopBar + onStartChatClick to HomeTopBar call
        engine.replaceText(
            path,
            """                canReportBug = state.canReportBug,
                modifier = Modifier.hazeEffect(""",
            """                canReportBug = state.canReportBug,
                showStartChatInTopBar = ixHomeUi.showStartChatInTopBar,
                onStartChatClick = onStartChatClick,
                modifier = Modifier.hazeEffect("""
        )

        // Wrap floatingActionButton content with ixSpaceNav/startChat checks
        engine.replaceText(
            path,
            """        floatingActionButton = {
            val coroutineScope = rememberCoroutineScope()
            HomeBottomBar(""",
            """        floatingActionButton = {
            if (!ixHomeUi.shouldShowIxSpaceNav) {
            val coroutineScope = rememberCoroutineScope()
            HomeBottomBar("""
        )

        // Replace the FAB lambda inside HomeBottomBar to hide when showStartChatInTopBar
        engine.replaceText(
            path,
            """                floatingActionButton = {
                    when (state.currentHomeNavigationBarItem) {
                        HomeNavigationBarItem.Chats -> {
                            HomeFloatingActionButton(onStartChatClick, CommonStrings.action_create_room)
                        }
                        HomeNavigationBarItem.Spaces -> {
                            HomeFloatingActionButton(onCreateSpaceClick, CommonStrings.action_create_space)
                        }
                    }
                },
            )
        },
        floatingActionButtonPosition = FabPosition.Center,""",
            """                floatingActionButton = when (state.currentHomeNavigationBarItem) {
                    HomeNavigationBarItem.Chats -> {
                        if (ixHomeUi.showStartChatInTopBar) {
                            null
                        } else {
                            {
                                HomeFloatingActionButton(onStartChatClick, CommonStrings.action_create_room)
                            }
                        }
                    }
                    HomeNavigationBarItem.Spaces -> {
                        {
                            HomeFloatingActionButton(onCreateSpaceClick, CommonStrings.action_create_space)
                        }
                    }
                },
            )
            } else {
                if (!ixHomeUi.showStartChatInTopBar) {
                    HomeFloatingActionButton(onStartChatClick, CommonStrings.action_create_room)
                }
            }
        },
        floatingActionButtonPosition = if (!ixHomeUi.shouldShowIxSpaceNav) FabPosition.Center else FabPosition.End,"""
        )

        // Replace contentPadding bottom value with Iltix-aware value
        engine.replaceText(
            path,
            "bottom = 96.dp,",
            "bottom = if (ixHomeUi.shouldShowIxSpaceNav) 168.dp else 96.dp,"
        )

        // Replace entire RoomListContentView call (including modifier block) with IxHomeChatsContent
        engine.replaceText(
            path,
            """RoomListContentView(
                        contentState = roomListState.contentState,
                        filtersState = roomListState.filtersState,
                        spaceFiltersState = roomListState.spaceFiltersState,
                        lazyListState = roomsLazyListState,
                        hideInvitesAvatars = roomListState.hideInvitesAvatars,
                        eventSink = roomListState.eventSink,
                        onSetUpRecoveryClick = onSetUpRecoveryClick,
                        onConfirmRecoveryKeyClick = onConfirmRecoveryKeyClick,
                        onRoomClick = ::onRoomClick,
                        onCreateRoomClick = onStartChatClick,
                        contentPadding = lazyColumnContentPadding + contentPadding,
                        modifier = Modifier
                            .padding(outerPadding)
                            .consumeWindowInsets(outerPadding)
                            .hazeSource(state = hazeState)
                    )""",
            """IxHomeChatsContent(
                        roomListState = roomListState,
                        roomsLazyListState = roomsLazyListState,
                        outerPadding = padding,
                        contentPadding = contentPadding,
                        hazeState = hazeState,
                        shouldShowIxSpaceNav = ixHomeUi.shouldShowIxSpaceNav,
                        useGlassTheme = ixHomeUi.useGlassTheme,
                        showNavigationBar = true,
                        onSetUpRecoveryClick = onSetUpRecoveryClick,
                        onConfirmRecoveryKeyClick = onConfirmRecoveryKeyClick,
                        onRoomClick = ::onRoomClick,
                        onOpenSpace = onRoomClick,
                        onCreateRoomClick = onStartChatClick,
                    )"""
        )

        // Suppress upstream SpaceFiltersView bottom sheet when Iltix space nav is active
        engine.replaceText(
            path,
            "                    SpaceFiltersView(roomListState.spaceFiltersState)",
            """                    if (!ixHomeUi.shouldShowIxSpaceNav) {
                        SpaceFiltersView(roomListState.spaceFiltersState)
                    }"""
        )
    }

    private fun patchHomeTopBar() {
        val path = "features/home/impl/src/main/kotlin/io/element/android/features/home/impl/components/HomeTopBar.kt"
        engine.addImport(path, "io.element.android.compound.theme.ElementTheme")

        // Add useIltixTheme and showStartChatInTopBar params to HomeTopBar
        engine.replaceText(
            path,
            """fun HomeTopBar(
    selectedNavigationItem: HomeNavigationBarItem,
    currentUserAndNeighbors: ImmutableList<MatrixUser>,""",
            """fun HomeTopBar(
    selectedNavigationItem: HomeNavigationBarItem,
    useIltixTheme: Boolean = false,
    currentUserAndNeighbors: ImmutableList<MatrixUser>,"""
        )

        engine.replaceText(
            path,
            """    canReportBug: Boolean,
    displayFilters: Boolean,""",
            """    canReportBug: Boolean,
    showStartChatInTopBar: Boolean = false,
    shouldShowIxSpaceNav: Boolean = false,
    onStartChatClick: () -> Unit = {},
    displayFilters: Boolean,"""
        )

        // Hide gradient when Iltix theme is active
        engine.replaceText(
            path,
            """                .backgroundVerticalGradient(
                    isVisible = !areSearchResultsDisplayed,
                )""",
            """                .backgroundVerticalGradient(
                    isVisible = !areSearchResultsDisplayed && !useIltixTheme,
                )"""
        )

        // Use Iltix theme colors for TopAppBar
        engine.replaceText(
            path,
            """                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,""",
            """                containerColor = if (useIltixTheme) ElementTheme.colors.bgCanvasDefault else Color.Transparent,
                scrolledContainerColor = if (useIltixTheme) ElementTheme.colors.bgCanvasDefault else Color.Transparent,"""
        )

        // Pass showStartChatInTopBar and onStartChatClick to RoomListMenuItems
        engine.replaceText(
            path,
            """                    RoomListMenuItems(
                        onToggleSearch = onToggleSearch,
                        onMenuActionClick = onMenuActionClick,""",
            """                    RoomListMenuItems(
                        showStartChatInTopBar = showStartChatInTopBar,
                        shouldShowIxSpaceNav = shouldShowIxSpaceNav,
                        onToggleSearch = onToggleSearch,
                        onMenuActionClick = onMenuActionClick,
                        onStartChatClick = onStartChatClick,"""
        )

        // Add showStartChatInTopBar and onStartChatClick to RoomListMenuItems signature + icon
        engine.replaceText(
            path,
            """private fun RowScope.RoomListMenuItems(
    onToggleSearch: () -> Unit,
    onMenuActionClick: (RoomListMenuAction) -> Unit,
    canReportBug: Boolean,
    spaceFiltersState: SpaceFiltersState,
) {
    IconButton(
        onClick = onToggleSearch,""",
            """private fun RowScope.RoomListMenuItems(
    showStartChatInTopBar: Boolean = false,
            shouldShowIxSpaceNav: Boolean = false,
    onToggleSearch: () -> Unit,
    onMenuActionClick: (RoomListMenuAction) -> Unit,
    onStartChatClick: () -> Unit = {},
    canReportBug: Boolean,
    spaceFiltersState: SpaceFiltersState,
) {
    if (showStartChatInTopBar) {
        IconButton(
            onClick = onStartChatClick,
        ) {
            Icon(
                imageVector = CompoundIcons.Plus(),
                contentDescription = stringResource(CommonStrings.action_create_room),
            )
        }
    }
    if (!shouldShowIxSpaceNav) {
        SpaceFilterButton(spaceFiltersState = spaceFiltersState)
    }
    IconButton(
        onClick = onToggleSearch,"""
        )

        engine.replaceText(
            path,
            """    SpaceFilterButton(spaceFiltersState = spaceFiltersState)
    if (RoomListConfig.HAS_DROP_DOWN_MENU) {""",
            """    if (RoomListConfig.HAS_DROP_DOWN_MENU) {"""
        )
    }

    private fun patchSpaceFiltersPresenter() {
        val path = "features/home/impl/src/main/kotlin/io/element/android/features/home/impl/spacefilters/SpaceFiltersPresenter.kt"
        engine.addImport(path, "io.element.android.libraries.matrix.api.core.RoomId")

        engine.replaceText(
            path,
            """        val availableFilters by remember {
            matrixClient.spaceService.spaceFiltersFlow.map { it.toImmutableList() }
        }.collectAsState(initial = persistentListOf())""",
            """        val availableFilters by remember {
            matrixClient.spaceService.spaceFiltersFlow.map { filters ->
                filters.map { filter ->
                    val recursiveDescendants = mutableSetOf<RoomId>()
                    fun collect(f: SpaceServiceFilter) {
                        recursiveDescendants.addAll(f.descendants)
                        filters.filter { it.level == f.level + 1 && f.descendants.contains(it.spaceRoom.roomId) }
                            .forEach { collect(it) }
                    }
                    collect(filter)
                    filter.copy(descendants = recursiveDescendants.toList())
                }.toImmutableList()
            }
        }.collectAsState(initial = persistentListOf())"""
        )
    }

    private fun patchRoomListPresenter() {
        val path = "features/home/impl/src/main/kotlin/io/element/android/features/home/impl/roomlist/RoomListPresenter.kt"
        engine.addImport(path, "de.iltix.home.IxRoomPrefsSource")
        engine.addImport(path, "de.iltix.lib.preferences.IxPrefs")
        engine.addImport(path, "io.element.android.libraries.matrix.api.room.roomMembers")
        engine.addImport(path, "kotlinx.collections.immutable.persistentListOf")
        engine.addImport(path, "kotlinx.coroutines.flow.combine")
        engine.addImport(path, "kotlinx.coroutines.flow.first")

        // Add IxRoomPrefsSource constructor parameter
        engine.replaceText(
            path,
            """    private val featureFlagService: FeatureFlagService,
) : Presenter<RoomListState> {""",
            """    private val featureFlagService: FeatureFlagService,
    private val ixRoomPrefsSource: IxRoomPrefsSource,
) : Presenter<RoomListState> {"""
        )

        // Collect pinFavorites state after collectAsState(false)
        engine.insertAfterLine(
            path,
            """^\s*\}\.collectAsState\(false\)$""",
            """
        val pinFavorites by ixRoomPrefsSource.pinFavoritesFlow()
            .collectAsState(initial = IxPrefs.PIN_FAVORITES.defaultValue)""",
            "RoomListPresenter: collect pinFavorites"
        )

        engine.insertAfterLine(
            path,
            """collectAsState\(initial = IxPrefs\.PIN_FAVORITES\.defaultValue\)""",
            """

        val showTypingInOverview by ixRoomPrefsSource.showTypingInOverviewFlow()
            .collectAsState(initial = IxPrefs.SHOW_TYPING_IN_OVERVIEW.defaultValue)""",
            "RoomListPresenter: collect showTypingInOverview"
        )

        // Add pinFavorites arg to roomListContentState call
        engine.replaceText(
            path,
            """val contentState = roomListContentState(
            securityBannerDismissed,
            showNewNotificationSoundBanner,
            showUnreadCount,
        )""",
            """val contentState = roomListContentState(
            securityBannerDismissed,
            showNewNotificationSoundBanner,
            showUnreadCount,
            pinFavorites,
            showTypingInOverview,
        )"""
        )

        // Add pinFavorites parameter to roomListContentState function signature
        engine.replaceText(
            path,
            """    private fun roomListContentState(
        securityBannerDismissed: Boolean,
        showNewNotificationSoundBanner: Boolean,
        showUnreadCount: Boolean,
    ): RoomListContentState {""",
            """    private fun roomListContentState(
        securityBannerDismissed: Boolean,
        showNewNotificationSoundBanner: Boolean,
        showUnreadCount: Boolean,
        pinFavorites: Boolean,
        showTypingInOverview: Boolean,
    ): RoomListContentState {"""
        )

        engine.replaceText(
            path,
            """        val seenRoomInvites by remember { seenInvitesStore.seenRoomIds() }.collectAsState(emptySet())
        val securityBannerState by rememberSecurityBannerState(securityBannerDismissed)""",
            """        val typingMemberDisplayNamesByRoom by produceState(
            initialValue = emptyMap<RoomId, List<String>>(),
            key1 = roomSummaries.dataOrNull(),
            key2 = showTypingInOverview,
        ) {
            val summaries = roomSummaries.dataOrNull().orEmpty()
            if (!showTypingInOverview || summaries.isEmpty()) {
                value = emptyMap()
                return@produceState
            }
            val nextValue = mutableMapOf<RoomId, List<String>>()
            value = emptyMap()
            summaries.forEach { summary ->
                launch {
                    client.getJoinedRoom(summary.roomId)?.use { room ->
                        combine(room.roomTypingMembersFlow, room.membersStateFlow) { typingMembers, membersState ->
                            typingMembers
                                .filterNot { client.isMe(it) }
                                .map { userId ->
                                    val displayName = membersState.roomMembers()
                                        ?.firstOrNull { roomMember -> roomMember.userId == userId }
                                        ?.displayName
                                        .orEmpty()
                                    val nickname = ixRoomPrefsSource.localNicknameFlow(userId.value).first().orEmpty()
                                    nickname
                                        .ifBlank { displayName }
                                        .ifBlank { userId.extractedDisplayName }
                                }
                        }
                            .distinctUntilChanged()
                            .collect { names ->
                                if (names.isEmpty()) {
                                    nextValue.remove(summary.roomId)
                                } else {
                                    nextValue[summary.roomId] = names
                                }
                                value = nextValue.toMap()
                            }
                    }
                }
            }
        }
        val seenRoomInvites by remember { seenInvitesStore.seenRoomIds() }.collectAsState(emptySet())
        val securityBannerState by rememberSecurityBannerState(securityBannerDismissed)"""
        )

        // Pin favorites: partition summaries and replace the summaries assignment
        engine.replaceText(
            path,
            """                summaries = roomSummaries.dataOrNull().orEmpty().toImmutableList(),""",
            """                summaries = roomSummaries.dataOrNull().orEmpty().map { summary ->
                    val typingMemberDisplayNames = if (showTypingInOverview) {
                        typingMemberDisplayNamesByRoom[summary.roomId].orEmpty().toImmutableList()
                    } else {
                        persistentListOf()
                    }
                    summary.copy(
                        typingMemberDisplayNames = typingMemberDisplayNames,
                    )
                }.let { summaries ->
                    if (pinFavorites) {
                        val (favorites, others) = summaries.partition { it.isFavorite }
                        favorites + others
                    } else {
                        summaries
                    }
                }.toImmutableList(),"""
        )
    }

    private fun patchRoomListRoomSummaryModel() {
        val path = "features/home/impl/src/main/kotlin/io/element/android/features/home/impl/model/RoomListRoomSummary.kt"
        engine.addImport(path, "kotlinx.collections.immutable.persistentListOf")

        engine.replaceText(
            path,
            """    val timestamp: String?,
    val latestEvent: LatestEvent,
    val avatarData: AvatarData,""",
            """    val timestamp: String?,
    val latestEvent: LatestEvent,
    val latestEventSenderId: String? = null,
    val latestEventSenderDisplayName: String? = null,
    val typingMemberDisplayNames: ImmutableList<String> = persistentListOf(),
    val avatarData: AvatarData,"""
        )
    }

    private fun patchRoomListRoomSummaryFactory() {
        val path = "features/home/impl/src/main/kotlin/io/element/android/features/home/impl/datasource/RoomListRoomSummaryFactory.kt"
        engine.addImport(path, "io.element.android.libraries.matrix.api.timeline.item.event.ProfileDetails")

        engine.replaceText(
            path,
            """            latestEvent = computeLatestEvent(roomSummary.latestEvent, roomInfo.isDm),
            avatarData = avatarData,""",
            """            latestEvent = computeLatestEvent(roomSummary.latestEvent, roomInfo.isDm),
            latestEventSenderId = roomSummary.latestEvent.senderIdOrNull(),
            latestEventSenderDisplayName = roomSummary.latestEvent.senderDisplayNameOrNull(),
            avatarData = avatarData,"""
        )

        engine.replaceText(
            path,
            """            is LatestEventValue.RoomInvite -> LatestEvent.None
        }
    }
}""",
            """            is LatestEventValue.RoomInvite -> LatestEvent.None
        }
    }
}

private fun LatestEventValue.senderIdOrNull(): String? {
    return when (this) {
        is LatestEventValue.Local -> senderId.value
        is LatestEventValue.Remote -> senderId.value
        is LatestEventValue.None,
        is LatestEventValue.RoomInvite,
        -> null
    }
}

private fun LatestEventValue.senderDisplayNameOrNull(): String? {
    val profile = when (this) {
        is LatestEventValue.Local -> senderProfile
        is LatestEventValue.Remote -> senderProfile
        is LatestEventValue.None,
        is LatestEventValue.RoomInvite,
        -> return null
    }
    return (profile as? ProfileDetails.Ready)?.displayName
}"""
        )
    }

    private fun patchRoomSummaryRow() {
        val path = "features/home/impl/src/main/kotlin/io/element/android/features/home/impl/components/RoomSummaryRow.kt"
        engine.addImport(path, "de.iltix.components.badges.IxUnreadBadge")
        engine.addImport(path, "de.iltix.components.nicknames.rememberIxResolvedDisplayName")
        engine.addImport(path, "de.iltix.components.roomlist.IxFavoriteStarIcon")
        engine.addImport(path, "de.iltix.home.rememberIxRoomSummaryConfig")

        // Add rememberIxRoomSummaryConfig in NameAndTimestampRow
        engine.insertAfterLine(
            path,
            """private fun NameAndTimestampRow\(""",
            """    // Iltix: injected by patcher – needed further below""",
            "RoomSummaryRow: NameAndTimestampRow comment"
        )

        // Add isFavorite + localNicknameUserId params, roomSummaryConfig + resolvedName to NameAndTimestampRow
        engine.replaceText(
            path,
            """private fun NameAndTimestampRow(
    // Iltix: injected by patcher – needed further below
    name: String?,
    timestamp: String?,
    isHighlighted: Boolean,
    dmUserStatus: DisplayedStatus?,
    modifier: Modifier = Modifier
) {""",
            """private fun NameAndTimestampRow(
    name: String?,
    localNicknameUserId: String? = null,
    timestamp: String?,
    isHighlighted: Boolean,
    isFavorite: Boolean = false,
    dmUserStatus: DisplayedStatus?,
    modifier: Modifier = Modifier
) {
    val roomSummaryConfig = de.iltix.home.rememberIxRoomSummaryConfig()
    val resolvedName = rememberIxResolvedDisplayName(
        userId = localNicknameUserId,
        fallbackName = name,
    )"""
        )

        // Use resolvedName instead of name for display text
        engine.replaceText(
            path,
            """        val displayName = name?.toSafeLength(ellipsize = true) ?: stringResource(id = CommonStrings.common_no_room_name)
        DisplayNameWithStatus(
            name = displayName,
            status = dmUserStatus,
            modifier = Modifier.weight(1f),
            style = ElementTheme.typography.fontBodyLgMedium,
            nameColor = ElementTheme.colors.roomListRoomName,
            nameFontStyle = FontStyle.Italic.takeIf { name == null },
        )
        // Timestamp""",
            """        val displayName = resolvedName?.toSafeLength(ellipsize = true) ?: stringResource(id = CommonStrings.common_no_room_name)
        DisplayNameWithStatus(
            name = displayName,
            status = dmUserStatus,
            modifier = Modifier.weight(1f),
            style = ElementTheme.typography.fontBodyLgMedium,
            nameColor = ElementTheme.colors.roomListRoomName,
            nameFontStyle = FontStyle.Italic.takeIf { resolvedName == null },
        )
        if (roomSummaryConfig.showFavoriteIndicator && isFavorite) {
            Spacer(modifier = Modifier.width(4.dp))
            IxFavoriteStarIcon()
        }
        // Timestamp"""
        )

        // Pass isFavorite + localNicknameUserId to NameAndTimestampRow from first call site (JOINED/DM type)
        engine.replaceText(
            path,
            """                    NameAndTimestampRow(
                        name = room.name,
                        timestamp = room.timestamp,
                        isHighlighted = room.isHighlighted,
                        dmUserStatus = room.dmUserStatus,
                    )
                    MessagePreviewAndIndicatorRow(room = room, showUnreadCount = showUnreadCount)""",
            """                    NameAndTimestampRow(
                        name = room.name,
                        localNicknameUserId = room.heroes.firstOrNull()?.id?.takeIf { room.isDm },
                        timestamp = room.timestamp,
                        isHighlighted = room.isHighlighted,
                        isFavorite = room.isFavorite,
                        dmUserStatus = room.dmUserStatus,
                    )
                    MessagePreviewAndIndicatorRow(room = room, showUnreadCount = showUnreadCount)"""
        )

        // Replace the UnreadIndicatorAtom with conditional IxUnreadBadge
        engine.replaceText(
            path,
            """            if (room.hasNewContent) {
                val contentDescription = stringResource(CommonStrings.a11y_notifications_new_messages)
                val count = if (showUnreadCount) {
                    if (room.userDefinedNotificationMode == RoomNotificationMode.MUTE) {
                        room.numberOfUnreadMessages
                    } else {
                        room.numberOfUnreadNotifications
                    }
                } else {
                    null
                }
                UnreadIndicatorAtom(
                    color = tint,
                    count = count,
                    contentDescription = contentDescription,
                )
            }""",
            """            if (room.hasNewContent) {
                val contentDescription = stringResource(CommonStrings.a11y_notifications_new_messages)
                val roomSummaryConfig = rememberIxRoomSummaryConfig()
                if (roomSummaryConfig.showUnreadCountBadge && room.numberOfUnreadMessages > 0) {
                    IxUnreadBadge(
                        count = room.numberOfUnreadMessages,
                        backgroundColor = tint,
                        contentDescription = contentDescription,
                    )
                } else {
                    val count = if (showUnreadCount) {
                        if (room.userDefinedNotificationMode == RoomNotificationMode.MUTE) {
                            room.numberOfUnreadMessages
                        } else {
                            room.numberOfUnreadNotifications
                        }
                    } else {
                        null
                    }
                    UnreadIndicatorAtom(
                        color = tint,
                        count = count,
                        contentDescription = contentDescription,
                    )
                }
            }"""
        )

        engine.replaceText(
            path,
            """            if (room.latestEvent is LatestEvent.Error) {""",
            """            val typingPreview = room.typingMemberDisplayNames.toIxTypingPreview()
            if (typingPreview != null) {
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .clipToBounds(),
                    text = typingPreview,
                    color = ElementTheme.colors.textSecondary,
                    style = ElementTheme.typography.fontBodyMdRegular,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            } else if (room.latestEvent is LatestEvent.Error) {"""
        )

        engine.replaceText(
            path,
            """                val messagePreview = room.latestEvent.content()
                val annotatedMessagePreview = messagePreview as? AnnotatedString ?: AnnotatedString(text = messagePreview.orEmpty().toString())
                Text(""",
            """                val messagePreview = room.latestEvent.content()
                val resolvedLatestEventSenderName = rememberIxResolvedDisplayName(
                    userId = room.latestEventSenderId,
                    fallbackName = room.latestEventSenderDisplayName,
                )
                val messagePreviewText = messagePreview.orEmpty().toString()
                val canRewriteSender = !room.isDm &&
                    !resolvedLatestEventSenderName.isNullOrBlank() &&
                    !room.latestEventSenderDisplayName.isNullOrBlank()
                val rewrittenMessagePreview = if (canRewriteSender) {
                    messagePreviewText.replaceFirst(
                        oldValue = "${'$'}{room.latestEventSenderDisplayName}:",
                        newValue = "${'$'}{resolvedLatestEventSenderName}:",
                    )
                } else {
                    messagePreviewText
                }
                val annotatedMessagePreview = when {
                    canRewriteSender -> AnnotatedString(text = rewrittenMessagePreview)
                    messagePreview is AnnotatedString -> messagePreview
                    else -> AnnotatedString(text = messagePreviewText)
                }
                Text("""
        )

        engine.insertBeforeLine(
            path,
            """@PreviewsDayNight""",
            """private fun List<String>.toIxTypingPreview(): String? {
    val names = map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
    return when (names.size) {
        0 -> null
        1 -> "${'$'}{names[0]} schreibt..."
        2 -> "${'$'}{names[0]} und ${'$'}{names[1]} schreiben..."
        else -> "${'$'}{names[0]} und ${'$'}{names.size - 1} weitere schreiben..."
    }
}

""",
            "RoomSummaryRow: typing preview text formatter"
        )
    }

    private fun patchRoomListContentView() {
        val path = "features/home/impl/src/main/kotlin/io/element/android/features/home/impl/components/RoomListContentView.kt"
        engine.addImport(path, "androidx.compose.runtime.collectAsState")
        engine.addImport(path, "androidx.compose.runtime.remember")
        engine.addImport(path, "de.iltix.components.roomlist.IxCardRoomWrapper")

        // Insert IxPreferencesStore + card rows pref at the start of RoomsViewList body
        engine.replaceText(
            path,
            """    modifier: Modifier = Modifier,
) {
    OnVisibleRangeChangeEffect""",
            """    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val ixPreferencesStore = remember(context) {
        de.iltix.lib.preferences.IxPreferencesStore(context)
    }
    val cardRowsMode by ixPreferencesStore
        .settingFlow(de.iltix.lib.preferences.IxPrefs.CARD_ROOM_ROWS)
        .collectAsState(initial = de.iltix.lib.preferences.IxPrefs.CARD_ROOM_ROWS.defaultValue)
    val ixThemeMode by ixPreferencesStore
        .settingFlow(de.iltix.lib.preferences.IxPrefs.ILTIX_THEME_MODE)
        .collectAsState(initial = de.iltix.lib.preferences.IxPrefs.ILTIX_THEME_MODE.defaultValue)

    OnVisibleRangeChangeEffect"""
        )

        // Wrap RoomSummaryRow in IxCardRoomWrapper and remove HorizontalDivider
        engine.replaceText(
            path,
            """        ) { index, room ->
            RoomSummaryRow(
                room = room,
                hideInviteAvatars = hideInvitesAvatars,
                isInviteSeen = room.displayType == RoomSummaryDisplayType.INVITE &&
                    state.seenRoomInvites.contains(room.roomId),
                showUnreadCount = state.showUnreadCount,
                onClick = onRoomClick,
                eventSink = eventSink,
            )
            if (index != state.summaries.lastIndex) {
                HorizontalDivider()
            }""",
            """        ) { index, room ->
            val rowContent = @Composable {
                RoomSummaryRow(
                    room = room,
                    hideInviteAvatars = hideInvitesAvatars,
                    isInviteSeen = room.displayType == RoomSummaryDisplayType.INVITE &&
                        state.seenRoomInvites.contains(room.roomId),
                    showUnreadCount = state.showUnreadCount,
                    onClick = onRoomClick,
                    eventSink = eventSink,
                )
            }
            if (cardRowsMode == "none") {
                rowContent()
                if (index != state.summaries.lastIndex) {
                    HorizontalDivider()
                }
            } else {
                IxCardRoomWrapper(
                    mode = cardRowsMode,
                    themeMode = ixThemeMode,
                    index = index,
                    lastIndex = state.summaries.lastIndex,
                ) {
                    rowContent()
                }
            }"""
        )
    }

    // ===== Messages hooks =====

    private fun patchMessagesView() {
        val path = "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/MessagesView.kt"
        engine.addImport(path, "androidx.activity.compose.BackHandler")
        engine.addImport(path, "androidx.compose.foundation.layout.Column")
        engine.addImport(path, "androidx.compose.foundation.layout.fillMaxWidth")
        engine.addImport(path, "androidx.compose.ui.Modifier")
        engine.addImport(path, "de.iltix.messages.IxEmojiKeyboardPanel")
        engine.addImport(path, "de.iltix.messages.rememberIxEmojiPanelState")
        engine.addImport(path, "io.element.android.libraries.designsystem.theme.LocalBuildMeta")

        // Insert emoji panel state after maxComposerHeightPx
        engine.insertAfterLine(
            path,
            """var maxComposerHeightPx by remember \{ mutableIntStateOf\(120\) \}""",
            """
    val isIltixBuild = LocalBuildMeta.current.applicationId.contains("iltix")
    val emojiPanelState = rememberIxEmojiPanelState(
        composerState = state.composerState,
        isIltixBuild = isIltixBuild,
    )""",
            "MessagesView: emoji panel state init"
        )

        // Insert BackHandler before expandableState
        engine.insertBeforeLine(
            path,
            """val expandableState = rememberExpandableBottomSheetLayoutState\(\)""",
            """    BackHandler(enabled = emojiPanelState.showEmojiPanel) {
        emojiPanelState.hideEmojiPanel()
    }

""",
            "MessagesView: BackHandler for emoji panel"
        )

        // Wrap ExpandableBottomSheetLayout in Column and modify imePadding
        engine.replaceText(
            path,
            """    ExpandableBottomSheetLayout(
        modifier = modifier
                .fillMaxSize()
                .imePadding()
                .systemBarsPadding()""",
            """    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
    ExpandableBottomSheetLayout(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .let { base -> if (emojiPanelState.showEmojiPanel) base else base.imePadding() }"""
        )

        // Close ExpandableBottomSheetLayout and add IxEmojiKeyboardPanel + close Column
        engine.replaceText(
            path,
            """        maxBottomSheetContentHeight = maxComposerHeightPx.toDp(),
    )

    var endPollConfirmingEvent""",
            """        maxBottomSheetContentHeight = maxComposerHeightPx.toDp(),
    )

    state.composerState.emojiPickerState?.let { pickerState ->
        state.composerState.emojiPickerRenderer?.let { pickerRenderer ->
            if (emojiPanelState.showEmojiPanel && emojiPanelState.emojiPickerEnabled) {
                IxEmojiKeyboardPanel(
                    pickerState = pickerState,
                    pickerRenderer = pickerRenderer,
                    panelHeight = emojiPanelState.panelHeight,
                    onSelectEmoji = { emoji ->
                        state.composerState.eventSink(MessageComposerEvent.InsertEmoji(emoji))
                    },
                )
            }
        }
    }

    } // end Column

    var endPollConfirmingEvent"""
        )

        // Add showEmojiButton/showEmojiPanel/onToggleEmojiPanel to MessageComposerView call (inside MessagesViewComposerBottomSheetContents)
        engine.replaceText(
            path,
            """                    MessageComposerView(
                        state = state.composerState,
                        voiceMessageState = state.voiceMessageComposerState,
                        modifier = Modifier.fillMaxWidth(),
                    )""",
            """                    MessageComposerView(
                        state = state.composerState,
                        voiceMessageState = state.voiceMessageComposerState,
                        showEmojiButton = showEmojiButton,
                        showEmojiPanel = showEmojiPanel,
                        onToggleEmojiPanel = onToggleEmojiPanel,
                        modifier = Modifier.fillMaxWidth(),
                    )"""
        )

        // Add emoji params to MessagesViewComposerBottomSheetContents signature
        engine.replaceText(
            path,
            """private fun MessagesViewComposerBottomSheetContents(
    state: MessagesState,
    onRoomSuccessorClick: (RoomId) -> Unit,
    onLinkClick: (String, Boolean) -> Unit,
)""",
            """private fun MessagesViewComposerBottomSheetContents(
    state: MessagesState,
    onRoomSuccessorClick: (RoomId) -> Unit,
    onLinkClick: (String, Boolean) -> Unit,
    showEmojiButton: Boolean = false,
    showEmojiPanel: Boolean = false,
    onToggleEmojiPanel: () -> Unit = {},
)"""
        )

        // Pass emoji params from MessagesViewComposerBottomSheetContents call site
        engine.replaceText(
            path,
            """            MessagesViewComposerBottomSheetContents(
                state = state,
                onLinkClick = { url, customTab -> onLinkClick(url, customTab) },
                onRoomSuccessorClick = { roomId ->
                    state.timelineState.eventSink(TimelineEvent.NavigateToPredecessorOrSuccessorRoom(roomId = roomId))
                },
            )""",
            """            MessagesViewComposerBottomSheetContents(
                state = state,
                onLinkClick = { url, customTab -> onLinkClick(url, customTab) },
                onRoomSuccessorClick = { roomId ->
                    state.timelineState.eventSink(TimelineEvent.NavigateToPredecessorOrSuccessorRoom(roomId = roomId))
                },
                showEmojiButton = emojiPanelState.showEmojiButton,
                showEmojiPanel = emojiPanelState.showEmojiPanel,
                onToggleEmojiPanel = emojiPanelState.onToggleEmojiPanel,
            )"""
        )
    }

    private fun patchTypingNotificationPresenter() {
        val path = "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/typing/TypingNotificationPresenter.kt"
        engine.addImport(path, "androidx.compose.ui.platform.LocalContext")
        engine.addImport(path, "de.iltix.lib.nicknames.IxLocalNicknameStore")
        engine.addImport(path, "kotlinx.coroutines.flow.first")

        // Initialize local nickname store in composable scope with robust pattern matching.
        engine.replacePattern(
            path,
            """val renderTypingNotifications by remember \{\s*sessionPreferencesStore\.isRenderTypingNotificationsEnabled\(\)\s*}\s*\.collectAsState\(initial = true\)\s*val typingMembersState by produceState\(initialValue = persistentListOf\(\), key1 = renderTypingNotifications\) \{\s*if \(renderTypingNotifications\) \{\s*observeRoomTypingMembers\(\)\s*} else \{\s*value = persistentListOf<TypingRoomMember>\(\)\s*}\s*}""",
            """val renderTypingNotifications by remember {
            sessionPreferencesStore.isRenderTypingNotificationsEnabled()
        }.collectAsState(initial = true)
        val appContext = LocalContext.current.applicationContext
        val nicknameStore = remember(appContext) { IxLocalNicknameStore(appContext) }
        val typingMembersState by produceState(initialValue = persistentListOf(), key1 = renderTypingNotifications) {
            if (renderTypingNotifications) {
                observeRoomTypingMembers(nicknameStore)
            } else {
                value = persistentListOf<TypingRoomMember>()
            }
        }""",
            "TypingNotificationPresenter: inject local nickname store"
        )

        // Resolve typing member names via local nickname first, then upstream display name.
        engine.replacePattern(
            path,
            """private fun ProduceStateScope<ImmutableList<TypingRoomMember>>\.observeRoomTypingMembers\(\) \{\s*combine\(room\.roomTypingMembersFlow, room\.membersStateFlow\) \{ typingMembers, membersState ->\s*typingMembers\s*\.map \{ userId ->\s*membersState\.roomMembers\(\)\s*\?\.firstOrNull \{ roomMember -> roomMember\.userId == userId }\s*\?\.toTypingRoomMember\(\)\s*\?: createDefaultRoomMemberForTyping\(userId\)\s*}\s*}""",
            """private fun ProduceStateScope<ImmutableList<TypingRoomMember>>.observeRoomTypingMembers(
        nicknameStore: IxLocalNicknameStore,
    ) {
        combine(room.roomTypingMembersFlow, room.membersStateFlow) { typingMembers, membersState ->
            typingMembers
                .map { userId ->
                    val upstreamName = membersState.roomMembers()
                        ?.firstOrNull { roomMember -> roomMember.userId == userId }
                        ?.disambiguatedDisplayName
                        .orEmpty()
                    val nickname = nicknameStore.nicknameFlow(userId.value).first().orEmpty()
                    TypingRoomMember(
                        disambiguatedDisplayName = nickname
                            .ifBlank { upstreamName }
                            .ifBlank { userId.value },
                    )
                }
        }""",
            "TypingNotificationPresenter: map typing users via local nickname"
        )
    }

    private fun patchMessageComposerEvent() {
        val path = "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/messagecomposer/MessageComposerEvent.kt"
        // Add InsertEmoji event
        engine.replaceText(
            path,
            """    data object SaveDraft : MessageComposerEvent
    data object ClearSlashError : MessageComposerEvent""",
            """    data class InsertEmoji(val emoji: String) : MessageComposerEvent
    data object SaveDraft : MessageComposerEvent
    data object ClearSlashError : MessageComposerEvent"""
        )
    }

    private fun patchMessageComposerState() {
        val path = "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/messagecomposer/MessageComposerState.kt"
        engine.addImport(path, "io.element.android.libraries.emoji.api.picker.EmojiPickerRenderer")
        engine.addImport(path, "io.element.android.libraries.emoji.api.picker.EmojiPickerState")

        engine.replaceText(
            path,
            """    val canShareLocation: Boolean,
    val suggestions: ImmutableList<ResolvedSuggestion>,""",
            """    val canShareLocation: Boolean,
    val emojiPickerState: EmojiPickerState?,
    val emojiPickerRenderer: EmojiPickerRenderer?,
    val suggestions: ImmutableList<ResolvedSuggestion>,"""
        )
    }

    private fun patchMessageComposerStateProvider() {
        val path = "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/messagecomposer/MessageComposerStateProvider.kt"

        engine.replaceText(
            path,
            """    canShareLocation = canShareLocation,
    suggestions = suggestions,""",
            """    canShareLocation = canShareLocation,
    emojiPickerState = null,
    emojiPickerRenderer = null,
    suggestions = suggestions,"""
        )
    }

    private fun patchMessageComposerPresenter() {
        val path = "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/messagecomposer/MessageComposerPresenter.kt"
        engine.addImport(path, "io.element.android.libraries.emoji.api.picker.EmojiPickerPresenter")
        engine.addImport(path, "io.element.android.libraries.emoji.api.picker.EmojiPickerRenderer")
        engine.addImport(path, "io.element.android.libraries.emoji.api.recentemojis.AddRecentEmoji")
        engine.addImport(path, "io.element.android.libraries.emoji.api.recentemojis.GetRecentEmojis")

        engine.replaceText(
            path,
            """    private val suggestionsProcessor: SuggestionsProcessor,
    private val mediaOptimizationConfigProvider: MediaOptimizationConfigProvider,
    private val notificationConversationService: NotificationConversationService,
    private val slashCommandService: SlashCommandService,""",
            """    private val suggestionsProcessor: SuggestionsProcessor,
    private val mediaOptimizationConfigProvider: MediaOptimizationConfigProvider,
    private val notificationConversationService: NotificationConversationService,
    private val emojiPickerPresenterFactory: EmojiPickerPresenter.Factory,
    private val getRecentEmojis: GetRecentEmojis,
    private val addRecentEmoji: AddRecentEmoji,
    private val emojiPickerRenderer: EmojiPickerRenderer,
    private val slashCommandService: SlashCommandService,"""
        )

        engine.insertAfterLine(
            path,
            """val localCoroutineScope = rememberCoroutineScope\(\)""",
            """
        val emojiPickerPresenter = remember {
            emojiPickerPresenterFactory.create(getRecentEmojis)
        }
        val emojiPickerState = emojiPickerPresenter.present()""",
            "MessageComposerPresenter: initialize emoji picker"
        )

        engine.replaceText(
            path,
            """                MessageComposerEvent.SaveDraft -> {""",
            """                is MessageComposerEvent.InsertEmoji -> {
                    localCoroutineScope.launch {
                        textEditorState.insertText(event.emoji)
                        textEditorState.requestFocus()
                        addRecentEmoji(event.emoji)
                    }
                }
                MessageComposerEvent.SaveDraft -> {"""
        )

        engine.replaceText(
            path,
            """            canShareLocation = canShareLocation.value,
            suggestions = suggestions.toImmutableList(),""",
            """            canShareLocation = canShareLocation.value,
            emojiPickerState = emojiPickerState,
            emojiPickerRenderer = emojiPickerRenderer,
            suggestions = suggestions.toImmutableList(),"""
        )
    }

    private fun patchMessageComposerView() {
        val path = "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/messagecomposer/MessageComposerView.kt"
        engine.addImport(path, "androidx.compose.foundation.layout.size")
        engine.addImport(path, "androidx.compose.runtime.collectAsState")
        engine.addImport(path, "androidx.compose.runtime.getValue")
        engine.addImport(path, "androidx.compose.runtime.mutableStateOf")
        engine.addImport(path, "androidx.compose.runtime.remember")
        engine.addImport(path, "androidx.compose.ui.platform.LocalContext")
        engine.addImport(path, "de.iltix.lib.preferences.IxPreferencesStore")
        engine.addImport(path, "de.iltix.lib.preferences.IxPrefs")
        engine.addImport(path, "io.element.android.compound.tokens.generated.CompoundIcons")
        engine.addImport(path, "io.element.android.libraries.designsystem.theme.LocalBuildMeta")
        engine.addImport(path, "io.element.android.libraries.designsystem.theme.components.Icon")
        engine.addImport(path, "io.element.android.libraries.designsystem.theme.components.IconButton")

        // Add emoji + unencrypted params to MessageComposerView signature
        engine.replaceText(
            path,
            """internal fun MessageComposerView(
    state: MessageComposerState,
    voiceMessageState: VoiceMessageComposerState,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current""",
            """internal fun MessageComposerView(
    state: MessageComposerState,
    voiceMessageState: VoiceMessageComposerState,
    showEmojiButton: Boolean = false,
    showEmojiPanel: Boolean = false,
    onToggleEmojiPanel: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current

    val context = LocalContext.current.applicationContext
    val isIltixBuild = LocalBuildMeta.current.applicationId.contains("iltix")
    val ixPreferencesStore = remember(isIltixBuild, context) {
        if (isIltixBuild) IxPreferencesStore(context) else null
    }
    val moveUnencryptedIndicatorToTopBar by remember(ixPreferencesStore) {
        ixPreferencesStore?.settingFlow(IxPrefs.UNENCRYPTED_TOPBAR_ICON)
    }?.collectAsState(initial = IxPrefs.UNENCRYPTED_TOPBAR_ICON.defaultValue) ?: remember {
        mutableStateOf(false)
    }"""
        )

        // Add showNotEncryptedBadge + extraLeadingContent to TextComposer call
        engine.replaceText(
            path,
            """        onSelectRichContent = ::sendUri,
    )""",
            """        onSelectRichContent = ::sendUri,
        showNotEncryptedBadge = !moveUnencryptedIndicatorToTopBar,
        extraLeadingContent = if (showEmojiButton) {
            {
                IconButton(modifier = Modifier.size(48.dp), onClick = onToggleEmojiPanel) {
                    Icon(
                        imageVector = if (showEmojiPanel) CompoundIcons.Keyboard() else CompoundIcons.ReactionAdd(),
                        contentDescription = null,
                    )
                }
            }
        } else { null },
    )"""
        )
    }

    private fun patchTextComposer() {
        val path = "libraries/textcomposer/impl/src/main/kotlin/io/element/android/libraries/textcomposer/TextComposer.kt"

        // Add showNotEncryptedBadge and extraLeadingContent params to TextComposer signature
        engine.replaceText(
            path,
            """    modifier: Modifier = Modifier,
    showTextFormatting: Boolean = false,
) {""",
            """    modifier: Modifier = Modifier,
    showTextFormatting: Boolean = false,
    showNotEncryptedBadge: Boolean = true,
    extraLeadingContent: (@Composable () -> Unit)? = null,
) {"""
        )

        // Pass showNotEncryptedBadge to TextFormattingLayout
        engine.replaceText(
            path,
            """        TextFormattingLayout(
            modifier = layoutModifier,
            isRoomEncrypted = state.isRoomEncrypted,
            textInput = textInput,""",
            """        TextFormattingLayout(
            modifier = layoutModifier,
            isRoomEncrypted = state.isRoomEncrypted,
            showNotEncryptedBadge = showNotEncryptedBadge,
            textInput = textInput,"""
        )

        // Pass showNotEncryptedBadge to StandardLayout
        engine.replaceText(
            path,
            """            composerMode = composerMode,
            voiceMessageState = voiceMessageState,
            isRoomEncrypted = state.isRoomEncrypted,
            modifier = layoutModifier,""",
            """            composerMode = composerMode,
            voiceMessageState = voiceMessageState,
            isRoomEncrypted = state.isRoomEncrypted,
            showNotEncryptedBadge = showNotEncryptedBadge,
            modifier = layoutModifier,"""
        )

        // Pass extraLeadingContent to StandardLayout (after onResetComposerMode line)
        engine.replaceText(
            path,
            """            onVoiceRecorderEvent = onVoiceRecorderEvent,
            onResetComposerMode = onResetComposerMode,
        )
    }

    SoftKeyboardEffect""",
            """            onVoiceRecorderEvent = onVoiceRecorderEvent,
            onResetComposerMode = onResetComposerMode,
            extraLeadingContent = extraLeadingContent,
        )
    }

    SoftKeyboardEffect"""
        )

        // Add showNotEncryptedBadge param to StandardLayout signature
        engine.replaceText(
            path,
            """private fun StandardLayout(
    composerMode: MessageComposerMode,
    voiceMessageState: VoiceMessageState,
    isRoomEncrypted: Boolean?,
    textInput: @Composable () -> Unit,""",
            """private fun StandardLayout(
    composerMode: MessageComposerMode,
    voiceMessageState: VoiceMessageState,
    isRoomEncrypted: Boolean?,
    showNotEncryptedBadge: Boolean,
    textInput: @Composable () -> Unit,"""
        )

        // Add extraLeadingContent param to StandardLayout signature (after onResetComposerMode)
        engine.replaceText(
            path,
            """    onResetComposerMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (isRoomEncrypted == false) {""",
            """    onResetComposerMode: () -> Unit,
    modifier: Modifier = Modifier,
    extraLeadingContent: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier) {
        if (showNotEncryptedBadge && isRoomEncrypted == false) {"""
        )

        // Add extraLeadingContent rendering after the attachment/voice button block
        engine.replaceText(
            path,
            """                    }
                }
            }
            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp, top = 8.dp)""",
            """                    }
                }
            }
            if (extraLeadingContent != null && voiceMessageState is VoiceMessageState.Idle) {
                Box(
                    modifier = Modifier.padding(top = 5.dp, bottom = 5.dp)
                ) {
                    extraLeadingContent()
                }
            }
            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp, top = 8.dp)"""
        )

        // Add showNotEncryptedBadge to TextFormattingLayout signature
        engine.replaceText(
            path,
            """private fun TextFormattingLayout(
    isRoomEncrypted: Boolean?,
    textInput: @Composable () -> Unit,""",
            """private fun TextFormattingLayout(
    isRoomEncrypted: Boolean?,
    showNotEncryptedBadge: Boolean,
    textInput: @Composable () -> Unit,"""
        )

        // Modify TextFormattingLayout's encrypted check
        engine.replaceText(
            path,
            """    ) {
        if (isRoomEncrypted == false) {
            NotEncryptedBadge()
            Spacer(Modifier.height(8.dp))
        }""",
            """    ) {
        if (showNotEncryptedBadge && isRoomEncrypted == false) {
            NotEncryptedBadge()
            Spacer(Modifier.height(8.dp))
        }"""
        )
    }

    private fun patchMessagesViewTopBar() {
        val path = "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/topbars/MessagesViewTopBar.kt"
        engine.addImport(path, "de.iltix.components.nicknames.rememberIxResolvedDisplayName")
        engine.addImport(path, "de.iltix.lib.preferences.IxPreferencesStore")
        engine.addImport(path, "de.iltix.lib.preferences.IxPrefs")
        engine.addImport(path, "androidx.compose.material3.TopAppBarDefaults")
        engine.addImport(path, "androidx.compose.runtime.collectAsState")
        engine.addImport(path, "androidx.compose.runtime.getValue")
        engine.addImport(path, "androidx.compose.runtime.mutableStateOf")
        engine.addImport(path, "androidx.compose.runtime.remember")
        engine.addImport(path, "androidx.compose.ui.graphics.Color")
        engine.addImport(path, "androidx.compose.ui.platform.LocalContext")
        engine.addImport(path, "io.element.android.libraries.designsystem.theme.LocalBuildMeta")

        // Add isRoomEncrypted param to MessagesViewTopBar signature
        engine.replaceText(
            path,
            """    isTombstoned: Boolean,
    heroes: ImmutableList<AvatarData>,""",
            """    isTombstoned: Boolean,
    isRoomEncrypted: Boolean? = null,
    heroes: ImmutableList<AvatarData>,"""
        )

        // Insert IxPreferencesStore + theme + unencrypted prefs before TopAppBar(
        engine.replacePattern(
            path,
            """modifier: Modifier = Modifier,\s+menuActions: @Composable RowScope\.\(\) -> Unit,\s+\)\s+\{\s+TopAppBar\(\s+modifier = modifier""",
            """    modifier: Modifier = Modifier,
    menuActions: @Composable RowScope.() -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val isIltixBuild = LocalBuildMeta.current.applicationId.contains("iltix")
    val ixPreferencesStore = remember(isIltixBuild, context) {
        if (isIltixBuild) IxPreferencesStore(context) else null
    }
    val iltixThemeMode by remember(ixPreferencesStore) {
        ixPreferencesStore?.settingFlow(IxPrefs.ILTIX_THEME_MODE)
    }?.collectAsState(initial = IxPrefs.ILTIX_THEME_MODE.defaultValue) ?: remember {
        mutableStateOf("off")
    }
    val useIltixTheme = iltixThemeMode != "off"
    val moveUnencryptedIndicatorToTopBar by remember(ixPreferencesStore) {
        ixPreferencesStore?.settingFlow(IxPrefs.UNENCRYPTED_TOPBAR_ICON)
    }?.collectAsState(initial = IxPrefs.UNENCRYPTED_TOPBAR_ICON.defaultValue) ?: remember {
        mutableStateOf(false)
    }

    TopAppBar(
        modifier = modifier""",
            "MessagesViewTopBar: preferences before TopAppBar"
        )

        // Add TopAppBar colors before windowInsets
        engine.replaceText(
            path,
            """        actions = menuActions,
        windowInsets = WindowInsets(0.dp)""",
            """        actions = menuActions,
        colors = if (useIltixTheme) {
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            )
        } else {
            TopAppBarDefaults.topAppBarColors()
        },
        windowInsets = WindowInsets(0.dp)"""
        )

        // Add nickname resolution in RoomAvatarAndNameRow
        engine.replaceText(
            path,
            """    dmUserStatus: DisplayedStatus?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            avatarData = roomAvatar,
            avatarType = AvatarType.Room(
                heroes = heroes,
                isTombstoned = isTombstoned,
            ),
        )
        DisplayNameWithStatus(
            name = roomName ?: stringResource(CommonStrings.common_no_room_name),
            status = dmUserStatus,
            modifier = Modifier.padding(start = 8.dp),
            style = ElementTheme.typography.fontBodyLgMedium,
            nameColor = ElementTheme.colors.textPrimary,
            nameFontStyle = FontStyle.Italic.takeIf { roomName == null },""",
            """    modifier: Modifier = Modifier
) {
    val localNicknameUserId = heroes.singleOrNull()?.id
    val resolvedRoomName = rememberIxResolvedDisplayName(userId = localNicknameUserId, fallbackName = roomName)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            avatarData = roomAvatar,
            avatarType = AvatarType.Room(
                heroes = heroes,
                isTombstoned = isTombstoned,
            ),
        )
        DisplayNameWithStatus(
            name = resolvedRoomName ?: stringResource(CommonStrings.common_no_room_name),
            status = dmUserStatus,
            modifier = Modifier.padding(start = 8.dp),
            style = ElementTheme.typography.fontBodyLgMedium,
            nameColor = ElementTheme.colors.textPrimary,
            nameFontStyle = FontStyle.Italic.takeIf { resolvedRoomName == null },"""
        )

        // Add lock icon for unencrypted rooms before iconModifier
        engine.replaceText(
            path,
            """                val iconModifier = Modifier.size(16.dp)

                when (dmUserIdentityState) {""",
            """                if (moveUnencryptedIndicatorToTopBar && isRoomEncrypted == false) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        imageVector = CompoundIcons.LockOff(),
                        tint = ElementTheme.colors.iconSecondary,
                        contentDescription = stringResource(CommonStrings.common_not_encrypted),
                    )
                }

                val iconModifier = Modifier.size(16.dp)

                when (dmUserIdentityState) {"""
        )
    }

    private fun patchThreadTopBar() {
        val path = "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/topbars/ThreadTopBar.kt"
        engine.addImport(path, "de.iltix.components.nicknames.rememberIxResolvedDisplayName")
        engine.addImport(path, "de.iltix.lib.preferences.IxPreferencesStore")
        engine.addImport(path, "de.iltix.lib.preferences.IxPrefs")
        engine.addImport(path, "androidx.compose.material3.TopAppBarDefaults")
        engine.addImport(path, "androidx.compose.runtime.collectAsState")
        engine.addImport(path, "androidx.compose.runtime.getValue")
        engine.addImport(path, "androidx.compose.runtime.mutableStateOf")
        engine.addImport(path, "androidx.compose.runtime.remember")
        engine.addImport(path, "androidx.compose.ui.graphics.Color")
        engine.addImport(path, "androidx.compose.ui.platform.LocalContext")
        engine.addImport(path, "io.element.android.libraries.designsystem.theme.LocalBuildMeta")

        // Insert IxPreferencesStore + theme + nickname before TopAppBar(
        engine.replaceText(
            path,
            """    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,""",
            """    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val isIltixBuild = LocalBuildMeta.current.applicationId.contains("iltix")
    val ixPreferencesStore = remember(isIltixBuild, context) {
        if (isIltixBuild) IxPreferencesStore(context) else null
    }
    val iltixThemeMode by remember(ixPreferencesStore) {
        ixPreferencesStore?.settingFlow(IxPrefs.ILTIX_THEME_MODE)
    }?.collectAsState(initial = IxPrefs.ILTIX_THEME_MODE.defaultValue) ?: remember {
        mutableStateOf("off")
    }
    val useIltixTheme = iltixThemeMode != "off"
    val resolvedRoomName = rememberIxResolvedDisplayName(userId = null, fallbackName = roomName)

    TopAppBar(
        modifier = modifier,"""
        )

        // Replace roomName with resolvedRoomName in the thread subtitle
        engine.replaceText(
            path,
            """            val name = roomName ?: stringResource(CommonStrings.common_no_room_name)""",
            """            val name = resolvedRoomName ?: stringResource(CommonStrings.common_no_room_name)"""
        )

        // Add TopAppBar colors before closing paren
        engine.replaceText(
            path,
            """            }
        }
    )
}

@PreviewsDayNight
@Composable
internal fun ThreadTopBarPreview""",
            """            }
        },
        colors = if (useIltixTheme) {
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            )
        } else {
            TopAppBarDefaults.topAppBarColors()
        },
    )
}

@PreviewsDayNight
@Composable
internal fun ThreadTopBarPreview"""
        )
    }

    private fun patchTimelineItemEventRow() {
        val path = "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/timeline/components/TimelineItemEventRow.kt"
        engine.addImport(path, "de.iltix.theme.LocalIxBubbleStyle")

        // Replace thread summary backgroundBubbleColor call to pass bubbleStyle
        engine.replaceText(
            path,
            ".background(MessageEventBubbleDefaults.backgroundBubbleColor(isOutgoing))",
            """.background(MessageEventBubbleDefaults.backgroundBubbleColor(isOutgoing, LocalIxBubbleStyle.current))"""
        )

        // Increase timestamp overlay padding to match content padding and prevent clipping
        engine.replaceText(
            path,
            """Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)""",
            """Modifier.padding(start = 11.dp, end = 11.dp, bottom = 11.dp)"""
        )

        // Increase overlay timestamp outer padding so it doesn't get clipped by rounded bubble corners.
        // Keep this match to a single line so it remains stable across small upstream formatting changes.
        engine.replaceText(
            path,
            """.padding(horizontal = 4.dp, vertical = 4.dp)""",
            """.padding(end = 8.dp, bottom = 8.dp)"""
        )

        // Increase rounded-bubble content padding to avoid text clipping near bottom-right edge.
        engine.replaceText(
            path,
            """Modifier.padding(start = 12.dp, end = 12.dp, top = topPadding, bottom = 8.dp)""",
            """Modifier.padding(start = 15.dp, end = 15.dp, top = topPadding + 3.dp, bottom = 11.dp)"""
        )
        engine.replaceText(
            path,
            """Modifier.padding(start = 8.dp, end = 8.dp, top = topPadding, bottom = 8.dp)""",
            """Modifier.padding(start = 11.dp, end = 11.dp, top = topPadding + 3.dp, bottom = 11.dp)"""
        )
    }

    private fun patchTimelineItemVoiceView() {
        val path = "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/timeline/components/event/TimelineItemVoiceView.kt"
        engine.addImport(path, "de.iltix.messages.IxVoiceMessageBody")
        engine.addImport(path, "de.iltix.theme.LocalIxBubbleStyle")
        engine.addImport(path, "de.iltix.messages.IxVoiceMessageView")
        engine.addImport(path, "de.iltix.messages.rememberIxVoiceMessageUiConfig")

        // Replace the Row body with IxVoiceMessageBody delegate
        engine.replaceText(
            path,
            """    ) {
        if (!isTalkbackActive()) {
            if (contentValidationValue.isValid()) {
                when (state.buttonType) {
                    VoiceMessageState.ButtonType.Play -> PlayButton(onClick = ::playPause)
                    VoiceMessageState.ButtonType.Pause -> PauseButton(onClick = ::playPause)
                    VoiceMessageState.ButtonType.Downloading -> ProgressButton()
                    VoiceMessageState.ButtonType.Retry -> RetryButton(onClick = ::playPause)
                    VoiceMessageState.ButtonType.Disabled -> PlayButton(onClick = {}, enabled = false)
                }
            } else {
                ProgressButton(displayImmediately = true)
            }
        }""",
            """    ) {
        IxVoiceMessageBody(
            state = state,
            content = content,
            onPlayPause = ::playPause,
        )
        if (false && !isTalkbackActive()) { // Iltix: replaced by IxVoiceMessageBody above
            when (state.buttonType) {
                VoiceMessageState.ButtonType.Play -> PlayButton(onClick = ::playPause)
                VoiceMessageState.ButtonType.Pause -> PauseButton(onClick = ::playPause)
                VoiceMessageState.ButtonType.Downloading -> ProgressButton()
                VoiceMessageState.ButtonType.Retry -> RetryButton(onClick = ::playPause)
                VoiceMessageState.ButtonType.Disabled -> PlayButton(onClick = {}, enabled = false)
            }
        }"""
        )
    }

    private fun patchTimelineItemPollContent() {
        val path = "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/timeline/model/event/TimelineItemPollContent.kt"
        engine.addImport(path, "io.element.android.libraries.matrix.api.core.UserId")
        engine.addImport(path, "kotlinx.collections.immutable.ImmutableList")
        engine.addImport(path, "kotlinx.collections.immutable.ImmutableMap")

        // Add votes field to data class
        engine.replaceText(
            path,
            """    val answerItems: List<PollAnswerItem>,
    val pollKind: PollKind,""",
            """    val answerItems: List<PollAnswerItem>,
    val votes: ImmutableMap<String, ImmutableList<UserId>>,
    val pollKind: PollKind,"""
        )

        // Also fix the provider
        val providerPath = "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/timeline/model/event/TimelineItemPollContentProvider.kt"
        engine.addImport(providerPath, "kotlinx.collections.immutable.persistentMapOf")

        engine.replaceText(
            providerPath,
            """        answerItems = answerItems,
        isMine = isMine,""",
            """        answerItems = answerItems,
        votes = persistentMapOf(),
        isMine = isMine,"""
        )
    }

    private fun patchTimelineItemContentPollFactory() {
        val path = "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/timeline/factories/event/TimelineItemContentPollFactory.kt"

        // Add votes = content.votes to TimelineItemPollContent constructor
        engine.replaceText(
            path,
            """            answerItems = pollContentState.answerItems,
            pollKind = pollContentState.pollKind,""",
            """            answerItems = pollContentState.answerItems,
            votes = content.votes,
            pollKind = pollContentState.pollKind,"""
        )
    }

    private fun patchTimelineItemPollView() {
        val path = "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/timeline/components/event/TimelineItemPollView.kt"
        engine.addImport(path, "de.iltix.components.poll.IxPollVoteViewerSheet")
        engine.addImport(path, "de.iltix.lib.R as IltixR")
        engine.addImport(path, "de.iltix.lib.preferences.IxPreferencesStore")
        engine.addImport(path, "de.iltix.lib.preferences.IxPrefs")
        engine.addImport(path, "androidx.compose.runtime.mutableStateOf")
        engine.addImport(path, "androidx.compose.runtime.setValue")
        engine.addImport(path, "androidx.compose.runtime.getValue")
        engine.addImport(path, "androidx.compose.runtime.remember")
        engine.addImport(path, "androidx.compose.runtime.collectAsState")
        engine.addImport(path, "androidx.compose.ui.platform.LocalContext")
        engine.addImport(path, "androidx.compose.ui.res.stringResource")
        engine.addImport(path, "io.element.android.libraries.designsystem.theme.LocalBuildMeta")
        engine.addImport(path, "io.element.android.libraries.matrix.api.core.UserId")

        // Add Iltix poll vote viewer state before PollContentView
        engine.replaceText(
            path,
            """    PollContentView(
        eventId = content.eventId,""",
            """    val context = LocalContext.current.applicationContext
    val isIltixBuild = LocalBuildMeta.current.applicationId.contains("iltix")
    val ixPreferencesStore = remember(isIltixBuild, context) {
        if (isIltixBuild) IxPreferencesStore(context) else null
    }
    val pollVoteViewerEnabled by remember(ixPreferencesStore) {
        ixPreferencesStore?.settingFlow(IxPrefs.POLL_VOTE_VIEWER)
    }?.collectAsState(initial = IxPrefs.POLL_VOTE_VIEWER.defaultValue) ?: remember {
        mutableStateOf(false)
    }
    var selectedVotes by remember {
        mutableStateOf<Pair<String, List<UserId>>?>(null)
    }

    val viewVotesLabel = if (pollVoteViewerEnabled) {
        stringResource(id = IltixR.string.iltix_poll_view_votes_button)
    } else null

    selectedVotes?.let { (answerText, voters) ->
        IxPollVoteViewerSheet(
            answerText = answerText,
            voters = voters,
            onDismiss = { selectedVotes = null },
        )
    }

    PollContentView(
        eventId = content.eventId,"""
        )

        // Add onViewVotes and viewVotesLabel to PollContentView call
        engine.replaceText(
            path,
            """        onEndPoll = ::onEndPoll,
        modifier = modifier,
    )
}""",
            """        onEndPoll = ::onEndPoll,
        onViewVotes = if (pollVoteViewerEnabled) { answerItem ->
            selectedVotes = answerItem.answer.text to content.votes[answerItem.answer.id].orEmpty()
        } else null,
        viewVotesLabel = viewVotesLabel,
        modifier = modifier,
    )
}"""
        )
    }

    private fun patchPollContentView() {
        val path = "features/poll/api/src/main/kotlin/io/element/android/features/poll/api/pollcontent/PollContentView.kt"
        engine.addImport(path, "io.element.android.features.poll.api.pollcontent.PollAnswerItem")

        // Add onViewVotes + viewVotesLabel to first PollContentView overload
        engine.replaceText(
            path,
            """    onEndPoll: (pollStartId: EventId) -> Unit,
    modifier: Modifier = Modifier,
) {
    PollContentView(
        eventId = state.eventId,""",
            """    onEndPoll: (pollStartId: EventId) -> Unit,
    onViewVotes: ((PollAnswerItem) -> Unit)? = null,
    viewVotesLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    PollContentView(
        eventId = state.eventId,"""
        )

        // Pass onViewVotes + viewVotesLabel in delegation call
        engine.replaceText(
            path,
            """        onEndPoll = onEndPoll,
        modifier = modifier,
    )
}""",
            """        onEndPoll = onEndPoll,
        onViewVotes = onViewVotes,
        viewVotesLabel = viewVotesLabel,
        modifier = modifier,
    )
}"""
        )

        // Add onViewVotes + viewVotesLabel to second PollContentView overload
        engine.replaceText(
            path,
            """    onEndPoll: (pollStartId: EventId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val votesCount = remember(answerItems)""",
            """    onEndPoll: (pollStartId: EventId) -> Unit,
    onViewVotes: ((PollAnswerItem) -> Unit)? = null,
    viewVotesLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    val votesCount = remember(answerItems)"""
        )

        // Pass onViewVotes + viewVotesLabel to PollAnswers call
        engine.replaceText(
            path,
            """        PollAnswers(answerItems = answerItems, onSelectAnswer = ::onSelectAnswer)""",
            """        PollAnswers(
            answerItems = answerItems,
            onSelectAnswer = ::onSelectAnswer,
            onViewVotes = onViewVotes,
            viewVotesLabel = viewVotesLabel,
        )"""
        )

        // Add onViewVotes + viewVotesLabel to PollAnswers function
        engine.replaceText(
            path,
            """private fun PollAnswers(
    answerItems: ImmutableList<PollAnswerItem>,
    onSelectAnswer: (PollAnswer) -> Unit,
) {""",
            """private fun PollAnswers(
    answerItems: ImmutableList<PollAnswerItem>,
    onSelectAnswer: (PollAnswer) -> Unit,
    onViewVotes: ((PollAnswerItem) -> Unit)? = null,
    viewVotesLabel: String? = null,
) {"""
        )

        // Pass onViewVotes + viewVotesLabel to PollAnswerView call
        engine.replaceText(
            path,
            """            PollAnswerView(
                answerItem = it,
                modifier = Modifier""",
            """            PollAnswerView(
                answerItem = it,
                onViewVotes = onViewVotes,
                viewVotesLabel = viewVotesLabel,
                modifier = Modifier"""
        )
    }

    private fun patchPollAnswerView() {
        val path = "features/poll/api/src/main/kotlin/io/element/android/features/poll/api/pollcontent/PollAnswerView.kt"
        engine.addImport(path, "io.element.android.libraries.designsystem.theme.components.TextButton")
        engine.addImport(path, "androidx.compose.foundation.layout.Spacer")
        engine.addImport(path, "androidx.compose.foundation.layout.height")
        engine.addImport(path, "androidx.compose.ui.Alignment")

        // Add onViewVotes + viewVotesLabel params to PollAnswerView
        engine.replaceText(
            path,
            """internal fun PollAnswerView(
    answerItem: PollAnswerItem,
    modifier: Modifier = Modifier,
) {""",
            """internal fun PollAnswerView(
    answerItem: PollAnswerItem,
    onViewVotes: ((PollAnswerItem) -> Unit)? = null,
    viewVotesLabel: String? = null,
    modifier: Modifier = Modifier,
) {"""
        )

        // Add view votes button after LinearProgressIndicator
        engine.replaceText(
            path,
            """            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = if (answerItem.isWinner) ElementTheme.colors.textSuccessPrimary else answerItem.isEnabled.toEnabledColor(),
                progress = {
                    when {
                        answerItem.showVotes -> answerItem.percentage
                        answerItem.isSelected -> 1f
                        else -> 0f
                    }
                },
                trackColor = ElementTheme.colors.progressIndicatorTrackColor,
                strokeCap = StrokeCap.Round,
            )
        }
    }
}""",
            """            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = if (answerItem.isWinner) ElementTheme.colors.textSuccessPrimary else answerItem.isEnabled.toEnabledColor(),
                progress = {
                    when {
                        answerItem.showVotes -> answerItem.percentage
                        answerItem.isSelected -> 1f
                        else -> 0f
                    }
                },
                trackColor = ElementTheme.colors.progressIndicatorTrackColor,
                strokeCap = StrokeCap.Round,
            )
            if (answerItem.showVotes && answerItem.votesCount > 0 && onViewVotes != null) {
                Spacer(modifier = Modifier.height(2.dp))
                TextButton(
                    text = viewVotesLabel ?: stringResource(CommonStrings.action_view),
                    onClick = { onViewVotes(answerItem) },
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}"""
        )
    }

    // ===== Room Details hooks =====

    private fun patchRoomDetailsPresenter() {
        val path = "features/roomdetails/impl/src/main/kotlin/io/element/android/features/roomdetails/impl/RoomDetailsPresenter.kt"
        engine.addImport(path, "de.iltix.lib.preferences.IxPreferencesStore")
        engine.addImport(path, "de.iltix.lib.preferences.IxPrefs")
        engine.addImport(path, "de.iltix.lib.preferences.IxRoomMediaAutoDownloadStore")

        // Add @ApplicationContext appContext constructor param
        engine.replaceText(
            path,
            """class RoomDetailsPresenter(
    @Assisted private val navigator: RoomDetailsNavigator,
    private val client: MatrixClient,""",
            """class RoomDetailsPresenter(
    @Assisted private val navigator: RoomDetailsNavigator,
    @io.element.android.libraries.di.annotations.ApplicationContext private val appContext: android.content.Context,
    private val client: MatrixClient,"""
        )

        // Initialize stores after rememberCoroutineScope
        engine.insertAfterLine(
            path,
            """val scope = rememberCoroutineScope\(\)""",
            """        val ixPreferencesStore = remember(appContext) { IxPreferencesStore(appContext) }
        val roomMediaAutoDownloadStore = remember(appContext) { IxRoomMediaAutoDownloadStore(appContext) }""",
            "RoomDetailsPresenter: init Iltix stores"
        )

        // Collect media auto-download settings after roomNotificationSettingsStateFlow
        engine.insertAfterLine(
            path,
            """val roomNotificationSettingsState by room\.roomNotificationSettingsStateFlow\.collectAsState\(\)""",
            """
        val isMediaAutoDownloadModuleEnabled by remember(ixPreferencesStore) {
            ixPreferencesStore.settingFlow(IxPrefs.MEDIA_AUTO_DOWNLOAD)
        }.collectAsState(initial = IxPrefs.MEDIA_AUTO_DOWNLOAD.defaultValue)
        val mediaAutoDownloadEnabled by roomMediaAutoDownloadStore.enabledFlow(room.roomId.value).collectAsState(initial = false)""",
            "RoomDetailsPresenter: collect media auto-download settings"
        )

        // Add SetMediaAutoDownload event handling
        engine.insertAfterLine(
            path,
            """is RoomDetailsEvent\.SetFavorite -> scope\.setFavorite\(event\.isFavorite\)""",
            """                is RoomDetailsEvent.SetMediaAutoDownload -> {
                    scope.launch(dispatchers.io) {
                        roomMediaAutoDownloadStore.setEnabled(room.roomId.value, event.enabled)
                    }
                }""",
            "RoomDetailsPresenter: handle SetMediaAutoDownload event"
        )

        // Add media auto-download fields to return state
        engine.replaceText(
            path,
            """            roomHistoryVisibility = roomInfo.historyVisibility,
            hasNewContent = hasNewContent,
            eventSink = ::handleEvent,""",
            """            roomHistoryVisibility = roomInfo.historyVisibility,
            hasNewContent = hasNewContent,
            isMediaAutoDownloadModuleEnabled = isMediaAutoDownloadModuleEnabled,
            mediaAutoDownloadEnabled = mediaAutoDownloadEnabled,
            eventSink = ::handleEvent,"""
        )

        // Patch RoomDetailsEvent to add SetMediaAutoDownload
        val eventPath = "features/roomdetails/impl/src/main/kotlin/io/element/android/features/roomdetails/impl/RoomDetailsEvent.kt"
        engine.replaceText(
            eventPath,
            """    data object MarkAsUnread : RoomDetailsEvent
}""",
            """    data object MarkAsUnread : RoomDetailsEvent
    data class SetMediaAutoDownload(val enabled: Boolean) : RoomDetailsEvent
}"""
        )

        // Patch RoomDetailsState to add media auto-download fields
        val statePath = "features/roomdetails/impl/src/main/kotlin/io/element/android/features/roomdetails/impl/RoomDetailsState.kt"
        engine.replaceText(
            statePath,
            """    val roomHistoryVisibility: RoomHistoryVisibility,
    val hasNewContent: Boolean,
    val eventSink: (RoomDetailsEvent) -> Unit""",
            """    val roomHistoryVisibility: RoomHistoryVisibility,
    val hasNewContent: Boolean,
    val isMediaAutoDownloadModuleEnabled: Boolean = false,
    val mediaAutoDownloadEnabled: Boolean = false,
    val eventSink: (RoomDetailsEvent) -> Unit"""
        )
    }

    private fun patchRoomDetailsView() {
        val path = "features/roomdetails/impl/src/main/kotlin/io/element/android/features/roomdetails/impl/RoomDetailsView.kt"
        engine.addImport(path, "de.iltix.lib.R as IltixR")

        // Add media auto-download switch after MediaGalleryItem
        engine.replaceText(
            path,
            """            // Room content
            PreferenceCategory {
                MediaGalleryItem(
                    onClick = openMediaGallery
                )""",
            """            // Room content
            PreferenceCategory {
                MediaGalleryItem(
                    onClick = openMediaGallery
                )
                if (state.isMediaAutoDownloadModuleEnabled) {
                    PreferenceSwitch(
                        title = stringResource(id = IltixR.string.iltix_media_auto_download_room_title),
                        subtitle = stringResource(id = IltixR.string.iltix_media_auto_download_room_subtitle),
                        isChecked = state.mediaAutoDownloadEnabled,
                        onCheckedChange = { enabled ->
                            state.eventSink(RoomDetailsEvent.SetMediaAutoDownload(enabled))
                        },
                    )
                }"""
        )
    }

    // ===== Push/Notification hooks =====

    private fun patchNotificationRenderer() {
        val path = "libraries/push/impl/src/main/kotlin/io/element/android/libraries/push/impl/notifications/NotificationRenderer.kt"
        engine.addImport(path, "android.content.Context")
        engine.addImport(path, "de.iltix.push.ixLiveNotificationKey")
        engine.addImport(path, "de.iltix.push.syncIxLiveNotificationMirrors")
        engine.addImport(path, "io.element.android.libraries.di.annotations.ApplicationContext")
        engine.addImport(path, "io.element.android.libraries.push.api.notifications.conversations.NotificationConversationService")

        // Inject conversation service so shortcut refresh code compiles.
        engine.replaceText(
            path,
            """    private val notificationDisplayer: NotificationDisplayer,
    private val notificationDataFactory: NotificationDataFactory,
    private val enterpriseService: EnterpriseService,""",
            """    private val notificationDisplayer: NotificationDisplayer,
    private val notificationDataFactory: NotificationDataFactory,
    private val notificationConversationService: NotificationConversationService,
    @ApplicationContext private val context: Context,
    private val enterpriseService: EnterpriseService,"""
        )

        // Ensure shortcuts exist for room notifications, so setShortcutId() resolves to a valid dynamic shortcut.
        engine.replaceText(
            path,
            """        val summaryNotification = notificationDataFactory.createSummaryNotification(
            roomNotifications = roomNotifications,
            invitationNotifications = invitationNotifications,
            simpleNotifications = simpleNotifications,
            notificationAccountParams = notificationAccountParams,
        )""",
            """        val summaryNotification = notificationDataFactory.createSummaryNotification(
            roomNotifications = roomNotifications,
            invitationNotifications = invitationNotifications,
            simpleNotifications = simpleNotifications,
            notificationAccountParams = notificationAccountParams,
        )

        // Ensure shortcut IDs referenced by notifications are backed by dynamic shortcuts.
        // Without this, Android/Samsung may not classify them as valid conversations.
        groupedEvents.roomEvents
            .groupBy { it.roomId }
            .values
            .mapNotNull { events -> events.maxByOrNull { it.timestamp } }
            .forEach { latestEvent ->
                runCatching {
                    notificationConversationService.onSendMessage(
                        sessionId = latestEvent.sessionId,
                        roomId = latestEvent.roomId,
                        roomName = latestEvent.roomName ?: latestEvent.roomId.value,
                        roomIsDirect = latestEvent.roomIsDm,
                        roomAvatarUrl = latestEvent.roomAvatarPath,
                    )
                }.onFailure {
                    Timber.tag(loggerTag.value).w(it, "Failed to refresh conversation shortcut for room ${'$'}{latestEvent.roomId}")
                }
            }

        syncIxLiveNotificationMirrors(
            context = context,
            activeRoomKeys = groupedEvents.roomEvents
                .filter { !it.outGoingMessage }
                .map(::ixLiveNotificationKey)
                .toSet(),
        )"""
        )
    }

    private fun patchNotificationConversationService() {
        val path = "libraries/push/impl/src/main/kotlin/io/element/android/libraries/push/impl/notifications/conversations/DefaultNotificationConversationService.kt"

        // In Iltix builds, keep conversation shortcuts available even with app lock enabled,
        // otherwise notification shortcut IDs become invalid for ranking.
        engine.replaceText(
            path,
            """        if (lockScreenService.isPinSetup().first()) {
            // We don't create shortcuts when a pin code is set for privacy reasons
            return
        }
""",
            """        val hasPinCode = lockScreenService.isPinSetup().first()
        val isIltixBuild = context.packageName.contains("iltix")
        if (hasPinCode && !isIltixBuild) {
            // Keep upstream privacy behavior for non-Iltix builds.
            return
        }
"""
        )

    }

    private fun patchNotificationCreator() {
        val path = "libraries/push/impl/src/main/kotlin/io/element/android/libraries/push/impl/notifications/factories/NotificationCreator.kt"
        engine.addImport(path, "de.iltix.push.publishIxLiveNotification")
        engine.addImport(path, "de.iltix.push.resolveIxNotificationRoute")
        engine.addImport(path, "de.iltix.push.resolveIxRankingTimestamp")
        engine.addImport(path, "de.iltix.push.resolveIxSummaryNotificationRoute")
        engine.addImport(path, "de.iltix.push.resolveIxNotificationSenderName")

        // Add ixNotificationRoute resolution after containsMissedCall
        engine.replaceText(
            path,
            """        val containsMissedCall = events.any { it.type == EventType.RTC_NOTIFICATION }
        val channelId = if (containsMissedCall) {
            notificationChannels.getChannelForIncomingCall(false)
        } else {
            notificationChannels.getChannelIdForMessage(
                sessionId = roomInfo.sessionId,
                noisy = roomInfo.shouldBing,
            )
        }""",
            """        val containsMissedCall = events.any { it.type == EventType.RTC_NOTIFICATION }
        val ixNotificationRoute = if (containsMissedCall) null else resolveIxNotificationRoute(
            context = context,
            buildMeta = buildMeta,
            roomInfo = roomInfo,
        )
        val channelId = if (containsMissedCall) {
            notificationChannels.getChannelForIncomingCall(false)
        } else if (ixNotificationRoute != null) {
            ixNotificationRoute.channelId
        } else {
            notificationChannels.getChannelIdForMessage(
                sessionId = roomInfo.sessionId,
                noisy = roomInfo.shouldBing,
            )
        }"""
        )

        // Add ranking timestamp
        engine.replaceText(
            path,
            """        messagingStyle.addMessagesFromEvents(events, imageLoader)
        return builder
            .setCategory(category)
            .setNumber(events.size)
            .setOnlyAlertOnce(roomInfo.isUpdated)
            .setWhen(lastMessageTimestamp)""",
            """        val rankingTimestamp = resolveIxRankingTimestamp(
            baseTimestamp = lastMessageTimestamp,
            ixRoute = ixNotificationRoute,
        )
        messagingStyle.addMessagesFromEvents(events, imageLoader)
        return builder
            .setCategory(category)
            .setNumber(events.size)
            .setOnlyAlertOnce(if (ixNotificationRoute != null) false else roomInfo.isUpdated)
            .setWhen(rankingTimestamp)"""
        )

        // Replace priority block to use Iltix routing
        engine.replaceText(
            path,
            """                if (roomInfo.shouldBing) {
                    priority = NotificationCompat.PRIORITY_DEFAULT
                    setLights(notificationAccountParams.color, 500, 500)
                } else {
                    priority = NotificationCompat.PRIORITY_LOW
                }""",
            """                if (ixNotificationRoute != null) {
                    priority = ixNotificationRoute.priority
                    if (ixNotificationRoute.shouldSetLights) {
                        setLights(notificationAccountParams.color, 500, 500)
                    }
                } else if (roomInfo.shouldBing) {
                    priority = NotificationCompat.PRIORITY_DEFAULT
                    setLights(notificationAccountParams.color, 500, 500)
                } else {
                    priority = NotificationCompat.PRIORITY_LOW
                }"""
        )

        // Patch createSummaryListNotification with Iltix routing
        engine.replaceText(
            path,
            """        val userId = notificationAccountParams.user.userId
        val channelId = notificationChannels.getChannelIdForMessage(
            sessionId = userId,
            noisy = noisy,
        )
        return NotificationCompat.Builder(context, channelId)
            .setOnlyAlertOnce(true)
            // used in compat < N, after summary is built based on child notifications
            .setWhen(lastMessageTimestamp)""",
            """        val userId = notificationAccountParams.user.userId
        val ixSummaryRoute = resolveIxSummaryNotificationRoute(
            context = context,
            buildMeta = buildMeta,
        )
        val channelId = ixSummaryRoute?.channelId ?: notificationChannels.getChannelIdForMessage(
            sessionId = userId,
            noisy = noisy,
        )
        val rankingTimestamp = resolveIxRankingTimestamp(
            baseTimestamp = lastMessageTimestamp,
            ixRoute = ixSummaryRoute,
        )
        return NotificationCompat.Builder(context, channelId)
            .setOnlyAlertOnce(ixSummaryRoute == null)
            // used in compat < N, after summary is built based on child notifications
            .setWhen(rankingTimestamp)"""
        )

        // Replace summary priority block with Iltix routing
        engine.replaceText(
            path,
            """                if (noisy) {
                    // Compat
                    priority = NotificationCompat.PRIORITY_DEFAULT
                    setLights(notificationAccountParams.color, 500, 500)
                } else {
                    // compat
                    priority = NotificationCompat.PRIORITY_LOW
                }""",
            """                if (ixSummaryRoute != null) {
                    priority = ixSummaryRoute.priority
                    if (ixSummaryRoute.shouldSetLights) {
                        setLights(notificationAccountParams.color, 500, 500)
                    }
                } else if (noisy) {
                    // Compat
                    priority = NotificationCompat.PRIORITY_DEFAULT
                    setLights(notificationAccountParams.color, 500, 500)
                } else {
                    // compat
                    priority = NotificationCompat.PRIORITY_LOW
                }"""
        )

        // Replace sender name resolution in addMessagesFromEvents
        engine.replaceText(
            path,
            """                val senderName = event.senderDisambiguatedDisplayName.orEmpty()""",
            """                val senderName = resolveIxNotificationSenderName(context, buildMeta, event)"""
        )

        // Add ixConversationHintsEnabled + roomIsDm to addMessagesFromEvents signature
        engine.replaceText(
            path,
            """    private suspend fun MessagingStyle.addMessagesFromEvents(
        events: List<NotifiableMessageEvent>,
        imageLoader: ImageLoader,
    ) {""",
            """    private suspend fun MessagingStyle.addMessagesFromEvents(
        events: List<NotifiableMessageEvent>,
        imageLoader: ImageLoader,
        ixConversationHintsEnabled: Boolean = false,
        roomIsDm: Boolean = false,
    ) {"""
        )

        // Add isImportant to Person.Builder (before .build())
        engine.replaceText(
            path,
            """                    .setKey(key)
                    .build()""",
            """                    .setKey(key)
                    .setImportant(ixConversationHintsEnabled && (roomIsDm || event.hasMentionOrReply))
                    .build()"""
        )

        // Pass ixConversationHintsEnabled + roomIsDm to addMessagesFromEvents call
        engine.replaceText(
            path,
            """        messagingStyle.addMessagesFromEvents(events, imageLoader)""",
            """        messagingStyle.addMessagesFromEvents(
            events = events,
            imageLoader = imageLoader,
            ixConversationHintsEnabled = ixNotificationRoute != null,
            roomIsDm = roomInfo.isDm,
        )"""
        )

        // Add setChannelId + setShortcutId to existing notification builder
        engine.replaceText(
            path,
            """        val builder = if (existingNotification != null) {
            NotificationCompat.Builder(context, existingNotification)
                // Clear existing actions
                .clearActions()
        } else {""",
            """        val builder = if (existingNotification != null) {
            NotificationCompat.Builder(context, existingNotification)
                // Clear existing actions
                .clearActions()
                .apply {
                    // When Iltix priority is active, ensure the high-importance channel is used
                    if (ixNotificationRoute != null) setChannelId(channelId)
                    if (threadId == null) {
                        setShortcutId(createShortcutId(roomInfo.sessionId, roomInfo.roomId))
                    }
                }
        } else {"""
        )

            // When Iltix priority route is active, add extra Person ranking hint
            // to help Android rank the notification higher in the shade (same as old working version).
            engine.replaceText(
                path,
                """            .setTicker(tickerText)
            .build()""",
                """            .apply {
                // Extra conversation ranking hint (API-dependent).
                if (ixNotificationRoute != null) {
                    events.lastOrNull { !it.outGoingMessage }?.let { latestEvent ->
                        val senderName = resolveIxNotificationSenderName(context, buildMeta, latestEvent)
                        val isImportant = roomInfo.isDm || latestEvent.hasMentionOrReply
                        val displayName = if (latestEvent.hasMentionOrReply) {
                            stringProvider.getString(R.string.notification_sender_mention_reply, senderName)
                        } else {
                            senderName
                        }
                        val key = if (latestEvent.hasMentionOrReply) {
                            "mention-or-reply:${'$'}{latestEvent.eventId.value}"
                        } else {
                            latestEvent.senderId.value
                        }
                        addPerson(
                            Person.Builder()
                                .setName(displayName)
                                .setIcon(null)
                                .setKey(key)
                                .setImportant(isImportant)
                                .build()
                        )
                    }
                }
            }
            .setTicker(tickerText)
            .build()
            .also { notification ->
                if (threadId == null) {
                    events.lastOrNull { !it.outGoingMessage }?.let { latestEvent ->
                        publishIxLiveNotification(
                            context = context,
                            buildMeta = buildMeta,
                            roomInfo = roomInfo,
                            latestEvent = latestEvent,
                            normalNotification = notification,
                        )
                    }
                }
            }"""
            )

            // When Iltix priority route is active, use GROUP_ALERT_ALL so the child notification
            // can alert directly.
            engine.replaceText(
                path,
                """                .setGroupSummary(false)
                // In order to avoid notification making sound twice (due to the summary notification)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)""",
                """                .setGroupSummary(false)
                // In order to avoid notification making sound twice (due to the summary notification)
                .setGroupAlertBehavior(if (ixNotificationRoute != null) NotificationCompat.GROUP_ALERT_ALL else NotificationCompat.GROUP_ALERT_CHILDREN)"""
            )
    }

    private fun patchNotificationChannels() {
        val path = "libraries/push/impl/src/main/kotlin/io/element/android/libraries/push/impl/notifications/channels/NotificationChannels.kt"
        engine.addImport(path, "de.iltix.push.createIxPriorityNotificationChannels")

        // Insert createIxPriorityNotificationChannels between silent and call channel creation
        engine.replaceText(
            path,
            """        // Register a channel for incoming and in progress call notifications with no ringing""",
            """        createIxPriorityNotificationChannels(
            context = context,
            notificationManager = notificationManager,
            stringProvider = stringProvider,
            accentColor = accentColor,
        )

        // Register a channel for incoming and in progress call notifications with no ringing"""
        )
    }

    private fun patchPushManifestBootReceiver() {
        val path = "libraries/push/impl/src/main/AndroidManifest.xml"

        engine.replaceText(
            path,
            """    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />""",
            """    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />"""
        )

        engine.replaceText(
            path,
            """        <receiver
            android:name=".notifications.NotificationBroadcastReceiver"
            android:enabled="true"
            android:exported="false" />

        <provider""",
            """        <receiver
            android:name=".notifications.NotificationBroadcastReceiver"
            android:enabled="true"
            android:exported="false" />
        <receiver
            android:name="de.iltix.push.IxBootReceiver"
            android:enabled="true"
            android:exported="false"
            android:directBootAware="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.LOCKED_BOOT_COMPLETED" />
            </intent-filter>
        </receiver>

        <provider"""
        )
    }

    private fun patchVectorFirebaseMessagingService() {
        val path = "libraries/pushproviders/firebase/src/main/kotlin/io/element/android/libraries/pushproviders/firebase/VectorFirebaseMessagingService.kt"
        engine.addImport(path, "kotlinx.coroutines.runBlocking")

        engine.replaceText(
            path,
            """        coroutineScope.launch {
            val pushData = pushParser.parse(message.data)
            if (pushData == null) {
                Timber.tag(loggerTag.value).w("Invalid data received from Firebase")
                pushHandler.handleInvalid(
                    providerInfo = FirebaseConfig.NAME,
                    data = message.data.keys.joinToString("\n") {
                        "${'$'}it: ${'$'}{message.data[it]}"
                    },
                )
                if (isHighPriority) {
                    fetchPushForegroundServiceManager.stop()
                }
            } else {
                val handled = pushHandler.handle(
                    pushData = pushData,
                    providerInfo = FirebaseConfig.NAME,
                )

                // If we failed to handle the push, we should release the wakelock early to avoid keeping the device awake for too long.
                if (!handled && isHighPriority) {
                    fetchPushForegroundServiceManager.stop()
                }
            }
        }""",
            """        // Firebase invokes this service callback on a background binder thread,
        // so blocking here is safe and prevents process death before push persistence/enqueueing.
        runBlocking {
            val pushData = pushParser.parse(message.data)
            if (pushData == null) {
                Timber.tag(loggerTag.value).w("Invalid data received from Firebase")
                pushHandler.handleInvalid(
                    providerInfo = FirebaseConfig.NAME,
                    data = message.data.keys.joinToString("\n") {
                        "${'$'}it: ${'$'}{message.data[it]}"
                    },
                )
                if (isHighPriority) {
                    fetchPushForegroundServiceManager.stop()
                }
            } else {
                val handled = pushHandler.handle(
                    pushData = pushData,
                    providerInfo = FirebaseConfig.NAME,
                )

                // If we failed to handle the push, we should release the wakelock early to avoid keeping the device awake for too long.
                if (!handled && isHighPriority) {
                    fetchPushForegroundServiceManager.stop()
                }
            }
        }"""
        )
    }

    private fun patchVectorUnifiedPushMessagingReceiver() {
        val path = "libraries/pushproviders/unifiedpush/src/main/kotlin/io/element/android/libraries/pushproviders/unifiedpush/VectorUnifiedPushMessagingReceiver.kt"

        engine.replaceText(
            path,
            """        coroutineScope.launch {
            val pushData = pushParser.parse(message.content, instance)
            if (pushData == null) {
                Timber.tag(loggerTag.value).w("Invalid data received from UnifiedPush")
                pushHandler.handleInvalid(
                    providerInfo = "${'$'}{UnifiedPushConfig.NAME} - ${'$'}instance",
                    data = String(message.content),
                )
                fetchPushForegroundServiceManager.stop()
            } else {
                val handled = pushHandler.handle(
                    pushData = pushData,
                    providerInfo = "${'$'}{UnifiedPushConfig.NAME} - ${'$'}instance",
                )

                // If we failed to handle the push, we should stop the foreground service early to avoid keeping the device awake for too long.
                if (!handled) {
                    fetchPushForegroundServiceManager.stop()
                }
            }
        }""",
            """        val pendingResult = goAsync()
        coroutineScope.launch {
            try {
                val pushData = pushParser.parse(message.content, instance)
                if (pushData == null) {
                    Timber.tag(loggerTag.value).w("Invalid data received from UnifiedPush")
                    pushHandler.handleInvalid(
                        providerInfo = "${'$'}{UnifiedPushConfig.NAME} - ${'$'}instance",
                        data = String(message.content),
                    )
                    fetchPushForegroundServiceManager.stop()
                } else {
                    val handled = pushHandler.handle(
                        pushData = pushData,
                        providerInfo = "${'$'}{UnifiedPushConfig.NAME} - ${'$'}instance",
                    )

                    // If we failed to handle the push, we should stop the foreground service early to avoid keeping the device awake for too long.
                    if (!handled) {
                        fetchPushForegroundServiceManager.stop()
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }"""
        )
    }

    private fun patchFetchPendingNotificationsWorker() {
        val path = "libraries/push/impl/src/main/kotlin/io/element/android/libraries/push/impl/workmanager/FetchPendingNotificationsWorker.kt"
        engine.addImport(path, "de.iltix.push.IxMediaAutoDownloadService")

        // Add IxMediaAutoDownloadService constructor param
        engine.replaceText(
            path,
            """    private val resultProcessor: NotificationResultProcessor,
    private val analyticsService: AnalyticsService,""",
            """    private val resultProcessor: NotificationResultProcessor,
    private val ixMediaAutoDownloadService: IxMediaAutoDownloadService,
    private val analyticsService: AnalyticsService,"""
        )

        // Call ixMediaAutoDownloadService after resultProcessor.emit
        engine.replaceText(
            path,
            """                    resultProcessor.emit(results)

                    results""",
            """                    resultProcessor.emit(results)
                    ixMediaAutoDownloadService.handleResolvedResults(results)

                    results"""
        )
    }

    private fun patchDefaultNotifiableEventResolver() {
        val path = "libraries/push/impl/src/main/kotlin/io/element/android/libraries/push/impl/notifications/DefaultNotifiableEventResolver.kt"

        // Enable video download in fetchImageIfPresent: replace null stub with actual download
        engine.replaceText(
            path,
            """            is VideoMessageType -> null // Use the thumbnail here?
            else -> null
        }
            ?: return null""",
            """            is VideoMessageType -> {
                notificationMediaRepoFactory.create(client).getMediaFile(
                    mediaSource = messageType.source,
                    mimeType = messageType.info?.mimetype,
                    filename = messageType.filename,
                )
            }
            else -> null
        }
            ?: return null"""
        )

        // Enable video mimetype in getImageMimetype
        engine.replaceText(
            path,
            """            is VideoMessageType -> null // Use the thumbnail here?
            else -> null
        }
    }
}""",
            """            is VideoMessageType -> messageType.info?.mimetype
            else -> null
        }
    }
}"""
        )
    }

    // ===== Matrix UI hooks =====

    private fun patchSenderName() {
        val path = "libraries/matrixui/src/main/kotlin/io/element/android/libraries/matrix/ui/messages/sender/SenderName.kt"
        engine.addImport(path, "de.iltix.components.nicknames.rememberIxResolvedDisplayName")

        // Replace sender name with Iltix nickname-resolved name
        engine.replaceText(
            path,
            "val displayName = senderProfile.displayName",
            """val displayName = rememberIxResolvedDisplayName(
                    userId = senderId.value,
                    fallbackName = senderProfile.displayName,
                ) ?: senderProfile.displayName"""
        )
    }

    // ===== Preferences hooks — wire Iltix Modules settings screen =====

    private fun patchPreferencesFlowNode() {
        val path = "features/preferences/impl/src/main/kotlin/io/element/android/features/preferences/impl/PreferencesFlowNode.kt"
        engine.addImport(path, "de.iltix.preferences.IxModuleSettingsNode")

        // Add NavTarget.IltixModules
        engine.insertAfterLine(
            path,
            """data object Labs : NavTarget""",
            """
        @Parcelize
        data object IltixModules : NavTarget
""",
            "PreferencesFlowNode: IltixModules NavTarget"
        )

        // Add navigateToIltixModules callback in resolve()
        engine.insertAfterLine(
            path,
            """backstack\.push\(NavTarget\.Labs\)""",
            """                    }

                    override fun navigateToIltixModules() {
                        backstack.push(NavTarget.IltixModules)""",
            "PreferencesFlowNode: navigateToIltixModules callback"
        )

        // Add IltixModules case in resolve() switch
        engine.insertAfterLine(
            path,
            """createNode<LabsNode>\(buildContext, listOf\(callback\)\)""",
            """            }
            NavTarget.IltixModules -> {
                createNode<IxModuleSettingsNode>(buildContext)""",
            "PreferencesFlowNode: IltixModules resolve case"
        )
    }

    private fun patchPreferencesRootNode() {
        val path = "features/preferences/impl/src/main/kotlin/io/element/android/features/preferences/impl/root/PreferencesRootNode.kt"

        // Add navigateToIltixModules to Callback interface
        engine.insertAfterLine(
            path,
            """fun navigateToBlockedUsers\(\)""",
            """        fun navigateToIltixModules()""",
            "PreferencesRootNode: navigateToIltixModules in Callback"
        )

        // Add onOpenIltixModules parameter to PreferencesRootView call
        engine.insertAfterLine(
            path,
            """onOpenBlockedUsers = callback::navigateToBlockedUsers""",
            """            onOpenIltixModules = callback::navigateToIltixModules,""",
            "PreferencesRootNode: onOpenIltixModules parameter"
        )
    }

    private fun patchPreferencesRootView() {
        val path = "features/preferences/impl/src/main/kotlin/io/element/android/features/preferences/impl/root/PreferencesRootView.kt"
        engine.addImport(path, "de.iltix.lib.R as IltixR")

        // Add onOpenIltixModules parameter to PreferencesRootView function
        engine.insertAfterLine(
            path,
            """onOpenBlockedUsers: \(\) -> Unit,""",
            """    onOpenIltixModules: () -> Unit,""",
            "PreferencesRootView: onOpenIltixModules parameter"
        )

        // Pass onOpenIltixModules to GeneralSection
        engine.insertAfterLine(
            path,
            """onOpenLabs = onOpenLabs,""",
            """            onOpenIltixModules = onOpenIltixModules,""",
            "PreferencesRootView: pass onOpenIltixModules to GeneralSection"
        )

        // Add onOpenIltixModules parameter to GeneralSection function
        engine.replaceText(
            path,
            """    onOpenDeveloperSettings: () -> Unit,
    onSignOutClick: () -> Unit,
    onDeactivateClick: () -> Unit,
) {
    ListItem(""",
            """    onOpenDeveloperSettings: () -> Unit,
    onOpenIltixModules: () -> Unit,
    onSignOutClick: () -> Unit,
    onDeactivateClick: () -> Unit,
) {
    ListItem(""",
        )

        // Add Iltix Modules ListItem after Advanced Settings
        engine.insertAfterLine(
            path,
            """onClick = onOpenAdvancedSettings,""",
            """    )

    ListItem(
        content = { Text(stringResource(id = IltixR.string.iltix_modules_title)) },
        leadingContent = ListItemContent.Icon(IconSource.Resource(IltixR.drawable.ic_iltix)),
        onClick = onOpenIltixModules,""",
            "PreferencesRootView: Iltix Modules menu item"
        )

        // Add onOpenIltixModules = {} in ContentToPreview
        engine.insertAfterLine(
            path,
            """onOpenBlockedUsers = \{\},""",
            """        onOpenIltixModules = {},""",
            "PreferencesRootView: onOpenIltixModules in preview"
        )
    }

    // ===== User Profile hooks =====

    private fun patchUserProfileView() {
        val path = "features/userprofile/shared/src/main/kotlin/io/element/android/features/userprofile/shared/UserProfileView.kt"
        engine.addImport(path, "de.iltix.components.nicknames.IxLocalNicknameAction")
        engine.addImport(path, "de.iltix.components.nicknames.rememberIxResolvedDisplayName")

        // Add local nickname action after the Spacer between actions and verify section
        engine.insertAfterLine(
            path,
            """Spacer\(modifier = Modifier\.height\(26\.dp\)\)""",
            """            // Iltix: local nickname management
            IxLocalNicknameAction(
                userId = state.userId.value,
                fallbackName = state.userName,
            )""",
            "UserProfileView: IxLocalNicknameAction"
        )
    }

    // ===== Message Bubble hooks =====

    private fun patchMessageEventBubble() {
        val path = "features/messages/impl/src/main/kotlin/io/element/android/features/messages/impl/timeline/components/MessageEventBubble.kt"
        engine.addImport(path, "de.iltix.theme.LocalIxBubbleStyle")
        engine.addImport(path, "androidx.compose.ui.unit.Dp")

        // Add bubbleStyle read + pass to backgroundBubbleColor and shape
        engine.replaceText(
            path,
            """    val cutTopStart = state.cutTopStart
    // Ignore state.isHighlighted for now, we need a design decision on it.
    val backgroundBubbleColor by rememberUpdatedState(customBackgroundColor ?: MessageEventBubbleDefaults.backgroundBubbleColor(state.isMine))
    val bubbleShape = remember(state) { MessageEventBubbleDefaults.shape(state.cutTopStart, state.groupPosition, state.isMine) }""",
            """    val cutTopStart = state.cutTopStart
    val bubbleStyle = LocalIxBubbleStyle.current
    // Ignore state.isHighlighted for now, we need a design decision on it.
    val backgroundBubbleColor = customBackgroundColor ?: MessageEventBubbleDefaults.backgroundBubbleColor(
        isMine = state.isMine,
        bubbleStyle = bubbleStyle,
    )
    val bubbleShape = remember(state, bubbleStyle.cornerRadius) {
        MessageEventBubbleDefaults.shape(
            cutTopStart = state.cutTopStart,
            groupPosition = state.groupPosition,
            isMine = state.isMine,
            bubbleRadius = bubbleStyle.cornerRadius,
        )
    }"""
        )

        // Update shape function to accept bubbleRadius parameter
        engine.replaceText(
            path,
            """    fun shape(cutTopStart: Boolean, groupPosition: TimelineItemGroupPosition, isMine: Boolean): Shape {
        val topLeftCorner = if (cutTopStart) 0.dp else BUBBLE_RADIUS""",
            """    fun shape(cutTopStart: Boolean, groupPosition: TimelineItemGroupPosition, isMine: Boolean, bubbleRadius: Dp = BUBBLE_RADIUS): Shape {
        val topLeftCorner = if (cutTopStart) 0.dp else bubbleRadius"""
        )

        // Replace BUBBLE_RADIUS usages inside shape() with bubbleRadius parameter
        engine.replacePattern(
            path,
            """RoundedCornerShape\(BUBBLE_RADIUS, BUBBLE_RADIUS, 0\.dp, BUBBLE_RADIUS\)""",
            """RoundedCornerShape(bubbleRadius, bubbleRadius, 0.dp, bubbleRadius)""",
            "MessageEventBubble: shape BUBBLE_RADIUS -> bubbleRadius (First)"
        )
        engine.replacePattern(
            path,
            """RoundedCornerShape\(topLeftCorner, BUBBLE_RADIUS, BUBBLE_RADIUS, 0\.dp\)""",
            """RoundedCornerShape(topLeftCorner, bubbleRadius, bubbleRadius, 0.dp)""",
            "MessageEventBubble: shape BUBBLE_RADIUS -> bubbleRadius (First else)"
        )
        engine.replacePattern(
            path,
            """RoundedCornerShape\(BUBBLE_RADIUS, 0\.dp, 0\.dp, BUBBLE_RADIUS\)""",
            """RoundedCornerShape(bubbleRadius, 0.dp, 0.dp, bubbleRadius)""",
            "MessageEventBubble: shape BUBBLE_RADIUS -> bubbleRadius (Middle)"
        )
        engine.replacePattern(
            path,
            """RoundedCornerShape\(0\.dp, BUBBLE_RADIUS, BUBBLE_RADIUS, 0\.dp\)""",
            """RoundedCornerShape(0.dp, bubbleRadius, bubbleRadius, 0.dp)""",
            "MessageEventBubble: shape BUBBLE_RADIUS -> bubbleRadius (Middle else)"
        )
        engine.replacePattern(
            path,
            """RoundedCornerShape\(BUBBLE_RADIUS, 0\.dp, BUBBLE_RADIUS, BUBBLE_RADIUS\)""",
            """RoundedCornerShape(bubbleRadius, 0.dp, bubbleRadius, bubbleRadius)""",
            "MessageEventBubble: shape BUBBLE_RADIUS -> bubbleRadius (Last)"
        )
        engine.replacePattern(
            path,
            """RoundedCornerShape\(0\.dp, BUBBLE_RADIUS, BUBBLE_RADIUS, BUBBLE_RADIUS\)""",
            """RoundedCornerShape(0.dp, bubbleRadius, bubbleRadius, bubbleRadius)""",
            "MessageEventBubble: shape BUBBLE_RADIUS -> bubbleRadius (Last else)"
        )
        // None case: topLeftCorner, BUBBLE_RADIUS, BUBBLE_RADIUS, BUBBLE_RADIUS
        engine.replaceText(
            path,
            """                    topLeftCorner,
                    BUBBLE_RADIUS,
                    BUBBLE_RADIUS,
                    BUBBLE_RADIUS""",
            """                    topLeftCorner,
                    bubbleRadius,
                    bubbleRadius,
                    bubbleRadius"""
        )

        // Update backgroundBubbleColor to accept IxBubbleStyle
        engine.replaceText(
            path,
            """    @Composable
    fun backgroundBubbleColor(isMine: Boolean): Color {
        return if (isMine) {
            ElementTheme.colors.messageFromMeBackground
        } else {
            ElementTheme.colors.messageFromOtherBackground
        }
    }""",
            """    @Composable
    fun backgroundBubbleColor(isMine: Boolean, bubbleStyle: de.iltix.theme.IxBubbleStyle = LocalIxBubbleStyle.current): Color {
        return if (isMine) {
            bubbleStyle.ownBackgroundColor
        } else {
            bubbleStyle.otherBackgroundColor
        }
    }"""
        )
    }

    // ===== TextEditor insertText support =====

    private fun patchMarkdownTextEditorState() {
        val path = "libraries/textcomposer/impl/src/main/kotlin/io/element/android/libraries/textcomposer/model/MarkdownTextEditorState.kt"
        engine.addImport(path, "android.text.SpannableStringBuilder")

        // Add insertText method before getMessageMarkdown
        engine.insertBeforeLine(
            path,
            """fun getMessageMarkdown\(permalinkBuilder: PermalinkBuilder\): String \{""",
            """    fun insertText(insertedText: String) {
        val currentText = SpannableStringBuilder(text.value())
        val start = selection.first.coerceAtLeast(0)
        val end = selection.last.coerceAtLeast(start)
        currentText.replace(start, end, insertedText)
        text.update(currentText, true)
        val cursor = start + insertedText.length
        selection = cursor..cursor
    }

""",
            "Add insertText method for emoji insertion"
        )
    }

    private fun patchTextEditorState() {
        val path = "libraries/textcomposer/impl/src/main/kotlin/io/element/android/libraries/textcomposer/model/TextEditorState.kt"

        // Add insertText method before reset
        engine.insertBeforeLine(
            path,
            """suspend fun reset\(\) \{""",
            """    suspend fun insertText(text: String) {
        when (this) {
            is Markdown -> state.insertText(text)
            is Rich -> richTextEditorState.setMarkdown(richTextEditorState.messageMarkdown + text)
        }
        requestFocus()
    }

""",
            "Add insertText method for emoji insertion"
        )
    }

    // ===== Typography token override =====

    private fun patchTypographyTokens() {
        val path = "libraries/compound/src/main/kotlin/io/element/android/compound/tokens/generated/TypographyTokens.kt"

        // Change object to open class with FontFamily constructor parameter + companion object
        engine.replaceText(
            path,
            "object TypographyTokens {",
            """open class TypographyTokens(fontFamily: FontFamily = FontFamily.Default) {
    companion object : TypographyTokens()"""
        )

        // Replace all hardcoded FontFamily.Default usages with the constructor parameter
        engine.replaceText(
            path,
            "fontFamily = FontFamily.Default,",
            "fontFamily = fontFamily,"
        )
    }

    private fun patchElementThemeTypography() {
        val path = "libraries/compound/src/main/kotlin/io/element/android/compound/theme/ElementTheme.kt"

        // Change typography from static val to composable getter backed by CompositionLocal
        engine.replaceText(
            path,
            """    /**
     * Compound [Typography] tokens. In Figma, these have the `Android/font/` prefix.
     */
    val typography: TypographyTokens = TypographyTokens""",
            """    /**
     * Compound [Typography] tokens. In Figma, these have the `Android/font/` prefix.
     */
    val typography: TypographyTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalCompoundTypography.current"""
        )

        // Add LocalCompoundTypography after LocalCompoundColors
        engine.replaceText(
            path,
            "internal val LocalCompoundColors = staticCompositionLocalOf { compoundColorsLight }",
            """internal val LocalCompoundColors = staticCompositionLocalOf { compoundColorsLight }
internal val LocalCompoundTypography = staticCompositionLocalOf<TypographyTokens> { TypographyTokens }"""
        )

        // Add compoundTypographyTokens parameter to ElementTheme() function
        engine.replaceText(
            path,
            """    typography: Typography = compoundTypography,
    content: @Composable () -> Unit,""",
            """    typography: Typography = compoundTypography,
    compoundTypographyTokens: TypographyTokens = LocalCompoundTypography.current,
    content: @Composable () -> Unit,"""
        )

        // Provide compoundTypographyTokens via CompositionLocalProvider
        engine.replaceText(
            path,
            """    CompositionLocalProvider(
        LocalCompoundColors provides currentCompoundColor,
        LocalContentColor provides colorScheme.onSurface,""",
            """    CompositionLocalProvider(
        LocalCompoundColors provides currentCompoundColor,
        LocalCompoundTypography provides compoundTypographyTokens,
        LocalContentColor provides colorScheme.onSurface,"""
        )
    }
}
