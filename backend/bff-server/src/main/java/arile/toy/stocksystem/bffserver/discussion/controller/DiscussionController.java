package arile.toy.stocksystem.bffserver.discussion.controller;

import arile.toy.stocksystem.bffserver.discussion.dto.*;
import arile.toy.stocksystem.bffserver.discussion.service.DiscussionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/discussions")
@RequiredArgsConstructor
public class DiscussionController {

    private final DiscussionService discussionService;

    @PostMapping
    public ResponseEntity<PostDetail> createPost(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody PostCreateRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var created = discussionService.createPost(user.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDetail> getPost(@PathVariable Long postId) {
        return ResponseEntity.ok(discussionService.getPost(postId));
    }

    @PatchMapping("/{postId}")
    public ResponseEntity<PostDetail> editPost(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long postId,
            @Valid @RequestBody PostEditRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var edited = discussionService.editPost(user.getUsername(), postId, request);
        return ResponseEntity.ok(edited);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long postId
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        discussionService.deletePost(user.getUsername(), postId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stocks/{stockCode}")
    public ResponseEntity<CursorPage<PostSummary>> getPostsByStock(
            @PathVariable String stockCode,
            @RequestParam(required = false) Long cursor
    ) {
        return ResponseEntity.ok(discussionService.getPostsByStock(stockCode, cursor));
    }

    @GetMapping("/my/posts")
    public ResponseEntity<CursorPage<PostSummary>> getMyPosts(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) Long cursor
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(discussionService.getMyPosts(user.getUsername(), cursor));
    }

    @GetMapping("/my/commented")
    public ResponseEntity<CursorPage<PostSummary>> getPostsICommentedOn(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) Long cursor
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(discussionService.getPostsICommentedOn(user.getUsername(), cursor));
    }

    @GetMapping("/my/scraps")
    public ResponseEntity<CursorPage<PostSummary>> getScrappedPosts(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) Long cursor
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(discussionService.getScrappedPosts(user.getUsername(), cursor));
    }

    @PostMapping("/{postId}/reactions")
    public ResponseEntity<ReactionResponse> reactToPost(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long postId,
            @Valid @RequestBody ReactionRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(discussionService.reactToPost(user.getUsername(), postId, request));
    }

    @PostMapping("/{postId}/scrap")
    public ResponseEntity<ScrapResponse> toggleScrap(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long postId
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(discussionService.toggleScrap(user.getUsername(), postId));
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var created = discussionService.addComment(user.getUsername(), postId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<CommentResponse> editComment(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentEditRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var edited = discussionService.editComment(user.getUsername(), postId, commentId, request);
        return ResponseEntity.ok(edited);
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        discussionService.deleteComment(user.getUsername(), postId, commentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/comments/{commentId}/reactions")
    public ResponseEntity<ReactionResponse> reactToComment(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody ReactionRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(discussionService.reactToComment(user.getUsername(), postId, commentId, request));
    }
}
