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

import java.util.List;

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
        List<CommentResponse> comments = commentRepository.findByPostIdOrderByCommentIdAsc(post.getPostId())
                .stream()
                .map(this::toCommentResponse)
                .toList();

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

        List<PostSummary> items = page.stream()
                .map(post -> {
                    int likes = countReaction(TargetType.POST, post.getPostId(), ReactionType.LIKE);
                    int dislikes = countReaction(TargetType.POST, post.getPostId(), ReactionType.DISLIKE);
                    int scraps = (int) scrapRepository.countByPostId(post.getPostId());
                    int commentCount = (int) commentRepository.countByPostId(post.getPostId());
                    return PostSummary.of(post, likes, dislikes, commentCount, scraps);
                })
                .toList();

        Long nextCursor = hasNext ? page.get(page.size() - 1).getPostId() : null;
        return new CursorPage<>(items, nextCursor, hasNext);
    }
}
