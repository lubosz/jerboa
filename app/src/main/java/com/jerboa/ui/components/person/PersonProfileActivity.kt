package com.jerboa.ui.components.person

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.jerboa.*
import com.jerboa.db.Account
import com.jerboa.db.AccountViewModel
import com.jerboa.ui.components.comment.CommentNode
import com.jerboa.ui.components.comment.edit.CommentEditViewModel
import com.jerboa.ui.components.comment.edit.commentEditClickWrapper
import com.jerboa.ui.components.comment.reply.CommentReplyViewModel
import com.jerboa.ui.components.comment.reply.commentReplyClickWrapper
import com.jerboa.ui.components.common.BottomAppBarAll
import com.jerboa.ui.components.common.getCurrentAccount
import com.jerboa.ui.components.community.CommunityLink
import com.jerboa.ui.components.community.CommunityViewModel
import com.jerboa.ui.components.community.communityClickWrapper
import com.jerboa.ui.components.home.HomeViewModel
import com.jerboa.ui.components.inbox.InboxViewModel
import com.jerboa.ui.components.inbox.inboxClickWrapper
import com.jerboa.ui.components.post.PostListings
import com.jerboa.ui.components.post.PostViewModel
import com.jerboa.ui.components.post.edit.PostEditViewModel
import com.jerboa.ui.components.post.edit.postEditClickWrapper
import com.jerboa.ui.components.post.postClickWrapper
import com.jerboa.ui.theme.MEDIUM_PADDING
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun PersonProfileActivity(
    navController: NavController,
    personProfileViewModel: PersonProfileViewModel,
    postViewModel: PostViewModel,
    communityViewModel: CommunityViewModel,
    accountViewModel: AccountViewModel,
    homeViewModel: HomeViewModel,
    inboxViewModel: InboxViewModel,
    commentEditViewModel: CommentEditViewModel,
    commentReplyViewModel: CommentReplyViewModel,
    postEditViewModel: PostEditViewModel,
) {

    Log.d("jerboa", "got to person activity")

    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()
    val ctx = LocalContext.current
    val accounts by accountViewModel.allAccounts.observeAsState()
    val account = getCurrentAccount(accounts = accounts)

    Surface(color = MaterialTheme.colors.background) {
        Scaffold(
            scaffoldState = scaffoldState,
            topBar = {
                personProfileViewModel.res?.person_view?.person?.name?.also {
                    PersonProfileHeader(
                        personName = it,
                        selectedSortType = personProfileViewModel.sortType.value,
                        onClickSortType = { sortType ->
                            personProfileViewModel.fetchPersonDetails(
                                id = personProfileViewModel.personId.value!!,
                                account = account,
                                clear = true,
                                changeSortType = sortType,
                                ctx = ctx,
                            )
                        },
                        navController = navController,
                    )
                }
            },
            content = {
                UserTabs(
                    padding = it,
                    navController = navController,
                    personProfileViewModel = personProfileViewModel,
                    postViewModel = postViewModel,
                    communityViewModel = communityViewModel,
                    ctx = ctx,
                    account = account,
                    scope = scope,
                    commentEditViewModel = commentEditViewModel,
                    commentReplyViewModel = commentReplyViewModel,
                    postEditViewModel = postEditViewModel,
                )
            },
            bottomBar = {
                BottomAppBarAll(
                    unreadCounts = homeViewModel.unreadCountResponse,
                    onClickProfile = {
                        account?.id?.also {
                            personClickWrapper(
                                personProfileViewModel = personProfileViewModel,
                                personId = it,
                                account = account,
                                navController = navController,
                                ctx = ctx,
                            )
                        }
                    },
                    onClickInbox = {
                        inboxClickWrapper(inboxViewModel, account, navController, ctx)
                    },
                    navController = navController,
                )
            }
        )
    }
}

enum class UserTab {
    About,
    Posts,
    Comments,
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun UserTabs(
    navController: NavController,
    personProfileViewModel: PersonProfileViewModel,
    communityViewModel: CommunityViewModel,
    ctx: Context,
    account: Account?,
    scope: CoroutineScope,
    postViewModel: PostViewModel,
    commentEditViewModel: CommentEditViewModel,
    commentReplyViewModel: CommentReplyViewModel,
    postEditViewModel: PostEditViewModel,
    padding: PaddingValues,
) {
    val tabTitles = UserTab.values().map { it.toString() }
    val pagerState = rememberPagerState()

    Column(
        modifier = Modifier.padding(padding)
    ) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.pagerTabIndicatorOffset(
                        pagerState,
                        tabPositions
                    )
                )
            }
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(text = title) }
                )
            }
        }
        if (personProfileViewModel.loading.value) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        HorizontalPager(
            count = tabTitles.size,
            state = pagerState,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxSize()
        ) { tabIndex ->
            when (tabIndex) {
                UserTab.About.ordinal -> {
                    LazyColumn(
                        state = rememberLazyListState(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            personProfileViewModel.res?.person_view?.also {
                                PersonProfileTopSection(
                                    personView = it
                                )
                            }
                        }
                        personProfileViewModel.res?.moderates?.also { moderates ->
                            if (moderates.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Moderates",
                                        style = MaterialTheme.typography.subtitle1,
                                        modifier = Modifier.padding(MEDIUM_PADDING),
                                    )
                                }
                            }
                            items(moderates) { cmv ->
                                CommunityLink(
                                    community = cmv.community,
                                    modifier = Modifier.padding(MEDIUM_PADDING),
                                    onClick = { community ->
                                        communityClickWrapper(
                                            communityViewModel,
                                            community.id,
                                            account,
                                            navController,
                                            ctx = ctx,
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                UserTab.Posts.ordinal -> {
                    PostListings(
                        posts = personProfileViewModel.posts,
                        onUpvoteClick = { postView ->
                            personProfileViewModel.likePost(
                                voteType = VoteType.Upvote,
                                postView = postView,
                                account = account,
                                ctx = ctx,
                            )
                        },
                        onDownvoteClick = { postView ->
                            personProfileViewModel.likePost(
                                voteType = VoteType.Downvote,
                                postView = postView,
                                account = account,
                                ctx = ctx,
                            )
                        },
                        onPostClick = { postView ->
                            postClickWrapper(
                                postViewModel = postViewModel,
                                postId = postView.post.id,
                                account = account,
                                navController = navController,
                                ctx = ctx,
                            )
                        },
                        onPostLinkClick = { url ->
                            openLink(url, ctx)
                        },
                        onSaveClick = { postView ->
                            personProfileViewModel.savePost(
                                postView = postView,
                                account = account,
                                ctx = ctx,
                            )
                        },
                        onEditPostClick = { postView ->
                            postEditClickWrapper(
                                postEditViewModel,
                                postView,
                                navController,
                            )
                        },
                        onCommunityClick = { community ->
                            communityClickWrapper(
                                communityViewModel,
                                community.id,
                                account,
                                navController,
                                ctx = ctx,
                            )
                        },
                        onPersonClick = { personId ->
                            personClickWrapper(
                                personProfileViewModel = personProfileViewModel,
                                personId = personId,
                                account = account,
                                navController = navController,
                                ctx = ctx,
                            )
                        },
                        onSwipeRefresh = {
                            personProfileViewModel.personId.value?.also {
                                personProfileViewModel.fetchPersonDetails(
                                    id = it,
                                    account = account,
                                    clear = true,
                                    ctx = ctx,
                                )
                            }
                        },
                        loading = personProfileViewModel.loading.value &&
                            personProfileViewModel.page.value == 1 &&
                            personProfileViewModel.posts.isNotEmpty(),
                        isScrolledToEnd = {
                            if (personProfileViewModel.posts.size > 0) {
                                personProfileViewModel.personId.value?.also {
                                    personProfileViewModel.fetchPersonDetails(
                                        id = it,
                                        account = account,
                                        nextPage = true,
                                        ctx = ctx,
                                    )
                                }
                            }
                        },
                        account = account,
                    )
                }
                UserTab.Comments.ordinal -> {
                    val nodes = sortNodes(commentsToFlatNodes(personProfileViewModel.comments))

                    val listState = rememberLazyListState()
                    val loading = personProfileViewModel.loading.value &&
                        personProfileViewModel.page.value == 1 &&
                        personProfileViewModel.comments.isNotEmpty()

                    // observer when reached end of list
                    val endOfListReached by remember {
                        derivedStateOf {
                            listState.isScrolledToEnd()
                        }
                    }

                    // act when end of list reached
                    if (endOfListReached) {
                        LaunchedEffect(endOfListReached) {
                            if (personProfileViewModel.comments.size > 0) {
                                personProfileViewModel.personId.value?.also {
                                    personProfileViewModel.fetchPersonDetails(
                                        id = it,
                                        account = account,
                                        nextPage = true,
                                        ctx = ctx,
                                    )
                                }
                            }
                        }
                    }

                    SwipeRefresh(
                        state = rememberSwipeRefreshState(loading),
                        onRefresh = {
                            personProfileViewModel.personId.value?.also {
                                personProfileViewModel.fetchPersonDetails(
                                    id = it,
                                    account = account,
                                    clear = true,
                                    ctx = ctx,
                                )
                            }
                        },
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(nodes) { node ->
                                CommentNode(
                                    node = node,
                                    onUpvoteClick = { commentView ->
                                        account?.also { acct ->
                                            personProfileViewModel.likeComment(
                                                commentView = commentView,
                                                voteType = VoteType.Upvote,
                                                account = acct,
                                                ctx = ctx,
                                            )
                                        }
                                    },
                                    onDownvoteClick = { commentView ->
                                        account?.also { acct ->
                                            personProfileViewModel.likeComment(
                                                commentView = commentView,
                                                voteType = VoteType.Downvote,
                                                account = acct,
                                                ctx = ctx,
                                            )
                                        }
                                    },
                                    onReplyClick = { commentView ->
                                        commentReplyClickWrapper(
                                            commentReplyViewModel = commentReplyViewModel,
                                            parentCommentView = commentView,
                                            postId = commentView.post.id,
                                            navController = navController,
                                        )
                                    },
                                    onSaveClick = { commentView ->
                                        account?.also { acct ->
                                            personProfileViewModel.saveComment(
                                                commentView = commentView,
                                                account = acct,
                                                ctx = ctx,
                                            )
                                        }
                                    },
                                    onPersonClick = { personId ->
                                        personClickWrapper(
                                            personProfileViewModel,
                                            personId,
                                            account,
                                            navController,
                                            ctx
                                        )
                                    },
                                    onCommunityClick = { community ->
                                        communityClickWrapper(
                                            communityViewModel = communityViewModel,
                                            communityId = community.id,
                                            account = account,
                                            navController = navController,
                                            ctx = ctx,
                                        )
                                    },
                                    onPostClick = { postId ->
                                        postClickWrapper(
                                            postViewModel = postViewModel,
                                            postId = postId,
                                            account = account,
                                            navController = navController,
                                            ctx = ctx,
                                        )
                                    },
                                    onEditCommentClick = { commentView ->
                                        commentEditClickWrapper(
                                            commentEditViewModel,
                                            commentView,
                                            navController,
                                        )
                                    },
                                    showPostAndCommunityContext = true,
                                    account = account,
                                    moderators = listOf()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}