package arile.toy.stocksystem.bffserver.discussion.service;

import arile.toy.stocksystem.bffserver.discussion.dto.*;
import arile.toy.stocksystem.bffserver.discussion.entity.*;
import arile.toy.stocksystem.bffserver.discussion.repository.DiscussionCommentRepository;
import arile.toy.stocksystem.bffserver.discussion.repository.DiscussionPostRepository;
import arile.toy.stocksystem.bffserver.discussion.repository.DiscussionReactionRepository;
import arile.toy.stocksystem.bffserver.discussion.repository.DiscussionScrapRepository;
import arile.toy.stocksystem.bffserver.exception.discussion.DiscussionCommentNotFoundException;
import arile.toy.stocksystem.bffserver.exception.discussion.DiscussionForbiddenException;
import arile.toy.stocksystem.bffserver.exception.discussion.DiscussionPostNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscussionService {

    private static final int PAGE_SIZE = 20;

    private final DiscussionPostRepository postRepository;
    private final DiscussionCommentRepository commentRepository;
    private final DiscussionReactionRepository reactionRepository;
    private final DiscussionScrapRepository scrapRepository;

    @Transactional
    public PostDetail createPost(String authorId, PostCreateRequest request) {
        var entity = DiscussionPostEntity.of(
                request.stockCode(), request.stockName(), request.title(), authorId, request.content());

        var saved = postRepository.save(entity);

        return toDetail(saved);
    }

    public PostDetail getPost(Long postId) {
        var post = getPostEntity(postId);
        return toDetail(post);
    }

    @Transactional
    public PostDetail editPost(String authorId, Long postId, PostEditRequest request) {
        var post = getPostEntity(postId);
        validateAuthor(post.getAuthorId(), authorId);

        post.edit(request.title(), request.content());

        return toDetail(post);
    }

    @Transactional
    public void deletePost(String authorId, Long postId) {
        var post = getPostEntity(postId);
        validateAuthor(post.getAuthorId(), authorId);

        List<Long> commentIds = commentRepository.findByPostIdOrderByCommentIdAsc(postId)
                .stream()
                .map(DiscussionCommentEntity::getCommentId)
                .toList();

        if (!commentIds.isEmpty()) {
            reactionRepository.deleteByTargetTypeAndTargetIdIn(TargetType.COMMENT, commentIds);
        }
        commentRepository.deleteByPostId(postId);

        reactionRepository.deleteByTargetTypeAndTargetIdIn(TargetType.POST, List.of(postId));
        scrapRepository.deleteByPostId(postId);

        postRepository.delete(post);
    }

    public CursorPage<PostSummary> getPostsByStock(String stockCode, Long cursor) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE + 1);
        List<DiscussionPostEntity> posts = postRepository.findByStockCode(stockCode, cursor, pageable);
        return toCursorPage(posts);
    }

    public CursorPage<PostSummary> getMyPosts(String authorId, Long cursor) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE + 1);
        List<DiscussionPostEntity> posts = postRepository.findByAuthorId(authorId, cursor, pageable);
        return toCursorPage(posts);
    }

    public CursorPage<PostSummary> getPostsICommentedOn(String authorId, Long cursor) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE + 1);
        List<DiscussionPostEntity> posts = postRepository.findByCommentAuthor(authorId, cursor, pageable);
        return toCursorPage(posts);
    }

    public CursorPage<PostSummary> getScrappedPosts(String userId, Long cursor) {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE + 1);
        List<DiscussionPostEntity> posts = postRepository.findScrappedByUser(userId, cursor, pageable);
        return toCursorPage(posts);
    }

    @Transactional
    public CommentResponse addComment(String authorId, Long postId, CommentCreateRequest request) {
        getPostEntity(postId); // 게시글 존재 확인

        var comment = DiscussionCommentEntity.of(postId, authorId, request.content());
        var saved = commentRepository.save(comment);

        return CommentResponse.of(saved, 0, 0);
    }

    @Transactional
    public CommentResponse editComment(String authorId, Long postId, Long commentId, CommentEditRequest request) {
        var comment = getCommentEntity(postId, commentId);
        validateAuthor(comment.getAuthorId(), authorId);

        comment.edit(request.content());

        return toCommentResponse(comment);
    }

    @Transactional
    public void deleteComment(String authorId, Long postId, Long commentId) {
        var comment = getCommentEntity(postId, commentId);
        validateAuthor(comment.getAuthorId(), authorId);

        reactionRepository.deleteByTargetTypeAndTargetIdAndUserId(TargetType.COMMENT, commentId, authorId);
        commentRepository.delete(comment);
    }

    @Transactional
    public ReactionResponse reactToPost(String userId, Long postId, ReactionRequest request) {
        getPostEntity(postId);
        toggleReaction(TargetType.POST, postId, userId, request.reactionType());

        int likes = countReaction(TargetType.POST, postId, ReactionType.LIKE);
        int dislikes = countReaction(TargetType.POST, postId, ReactionType.DISLIKE);
        return new ReactionResponse(likes, dislikes);
    }

    @Transactional
    public ReactionResponse reactToComment(String userId, Long postId, Long commentId, ReactionRequest request) {
        getCommentEntity(postId, commentId);
        toggleReaction(TargetType.COMMENT, commentId, userId, request.reactionType());

        int likes = countReaction(TargetType.COMMENT, commentId, ReactionType.LIKE);
        int dislikes = countReaction(TargetType.COMMENT, commentId, ReactionType.DISLIKE);
        return new ReactionResponse(likes, dislikes);
    }

    private void toggleReaction(TargetType targetType, Long targetId, String userId, ReactionType reactionType) {
        var existing = reactionRepository.findByTargetTypeAndTargetIdAndUserId(targetType, targetId, userId);

        if (existing.isPresent()) {
            if (existing.get().getReactionType() == reactionType) {
                reactionRepository.delete(existing.get()); // 같은 반응 재요청 -> 취소
            } else {
                existing.get().changeReactionType(reactionType); // 다른 반응 -> 교체
            }
        } else {
            reactionRepository.save(DiscussionReactionEntity.of(targetType, targetId, userId, reactionType));
        }
    }

    @Transactional
    public ScrapResponse toggleScrap(String userId, Long postId) {
        getPostEntity(postId);

        boolean scrapped;
        var existing = scrapRepository.findByPostIdAndUserId(postId, userId);

        if (existing.isPresent()) {
            scrapRepository.delete(existing.get());
            scrapped = false;
        } else {
            scrapRepository.save(DiscussionScrapEntity.of(postId, userId));
            scrapped = true;
        }

        int scraps = (int) scrapRepository.countByPostId(postId);
        return new ScrapResponse(scraps, scrapped);
    }

    private DiscussionPostEntity getPostEntity(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new DiscussionPostNotFoundException(postId));
    }

    private DiscussionCommentEntity getCommentEntity(Long postId, Long commentId) {
        var comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new DiscussionCommentNotFoundException(commentId));

        if (!comment.getPostId().equals(postId)) {
            throw new DiscussionCommentNotFoundException(commentId);
        }
        return comment;
    }

    private void validateAuthor(String actualAuthorId, String requestUserId) {
        if (!actualAuthorId.equals(requestUserId)) {
            throw new DiscussionForbiddenException();
        }
    }

    private int countReaction(TargetType targetType, Long targetId, ReactionType reactionType) {
        return (int) reactionRepository.countByTargetTypeAndTargetIdAndReactionType(targetType, targetId, reactionType);
    }

    private PostDetail toDetail(DiscussionPostEntity post) {
        List<DiscussionCommentEntity> commentEntities =
                commentRepository.findByPostIdOrderByCommentIdAsc(post.getPostId());

        List<CommentResponse> comments;
        if (commentEntities.isEmpty()) {
            comments = List.of();
        } else {
            List<Long> commentIds = commentEntities.stream()
                    .map(DiscussionCommentEntity::getCommentId)
                    .toList();

            Map<Long, Integer> likeMap = new HashMap<>();
            Map<Long, Integer> dislikeMap = new HashMap<>();
            reactionRepository.countGroupByTargetIds(TargetType.COMMENT, commentIds)
                    .forEach(row -> {
                        if (row.getReactionType() == ReactionType.LIKE) {
                            likeMap.put(row.getTargetId(), (int) row.getCnt());
                        } else {
                            dislikeMap.put(row.getTargetId(), (int) row.getCnt());
                        }
                    });

            comments = commentEntities.stream()
                    .map(comment -> CommentResponse.of(
                            comment,
                            likeMap.getOrDefault(comment.getCommentId(), 0),
                            dislikeMap.getOrDefault(comment.getCommentId(), 0)))
                    .toList();
        }

        int likes = countReaction(TargetType.POST, post.getPostId(), ReactionType.LIKE);
        int dislikes = countReaction(TargetType.POST, post.getPostId(), ReactionType.DISLIKE);
        int scraps = (int) scrapRepository.countByPostId(post.getPostId());

        return PostDetail.of(post, likes, dislikes, scraps, comments);
    }

    private CommentResponse toCommentResponse(DiscussionCommentEntity comment) {
        int likes = countReaction(TargetType.COMMENT, comment.getCommentId(), ReactionType.LIKE);
        int dislikes = countReaction(TargetType.COMMENT, comment.getCommentId(), ReactionType.DISLIKE);
        return CommentResponse.of(comment, likes, dislikes);
    }

    private CursorPage<PostSummary> toCursorPage(List<DiscussionPostEntity> posts) {
        boolean hasNext = posts.size() > PAGE_SIZE;
        List<DiscussionPostEntity> page = hasNext ? posts.subList(0, PAGE_SIZE) : posts;

        if (page.isEmpty()) {
            return new CursorPage<>(List.of(), null, false);
        }

        List<Long> postIds = page.stream().map(DiscussionPostEntity::getPostId).toList();

        Map<Long, Integer> likeMap = new HashMap<>();
        Map<Long, Integer> dislikeMap = new HashMap<>();
        reactionRepository.countGroupByTargetIds(TargetType.POST, postIds)
                .forEach(row -> {
                    if (row.getReactionType() == ReactionType.LIKE) {
                        likeMap.put(row.getTargetId(), (int) row.getCnt());
                    } else {
                        dislikeMap.put(row.getTargetId(), (int) row.getCnt());
                    }
                });

        Map<Long, Integer> scrapMap = scrapRepository.countGroupByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        DiscussionScrapRepository.ScrapCountRow::getPostId,
                        row -> (int) row.getCnt()));

        Map<Long, Integer> commentCountMap = commentRepository.countGroupByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        DiscussionCommentRepository.CommentCountRow::getPostId,
                        row -> (int) row.getCnt()));

        List<PostSummary> items = page.stream()
                .map(post -> PostSummary.of(
                        post,
                        likeMap.getOrDefault(post.getPostId(), 0),
                        dislikeMap.getOrDefault(post.getPostId(), 0),
                        commentCountMap.getOrDefault(post.getPostId(), 0),
                        scrapMap.getOrDefault(post.getPostId(), 0)))
                .toList();

        Long nextCursor = hasNext ? page.get(page.size() - 1).getPostId() : null;
        return new CursorPage<>(items, nextCursor, hasNext);
    }
}
