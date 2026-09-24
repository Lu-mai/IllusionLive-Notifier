package com.illusionlive.notifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 한 글의 댓글 목록에서 알릴 것을 고른다.
 *
 * <p>Android API 를 쓰지 않는다. {@code SelfTest} 가 android.jar 없이 컴파일한다.
 *
 * <p>사이트가 댓글을 평면으로 저장해 답변 대상을 기록하지 않는다. 그래서 "내 댓글에 글쓴이가 단
 * 답"은 두 조건으로 추정한다 — 내 댓글 바로 다음 한 건이고, 그 댓글을 글쓴이가 썼을 것. 글당
 * 댓글이 평균 1.8 개라 사이에 제삼자가 끼어들 여지가 작다. 끼어들면 놓치고, 내 댓글이 마지막일
 * 때 글쓴이가 무관한 댓글을 달면 헛알림이 한 건 난다.
 * ponytail: 사이트가 부모 댓글을 기록하기 시작하면 그 값으로 바꾼다.
 */
final class CommentRules {
    private CommentRules() {}

    /**
     * @param comments  화면에 보이는 순서 그대로의 댓글
     * @param seen      이미 알린 댓글 코드
     * @param nickname   내 닉네임. 비어 있으면 아무것도 고르지 않는다
     * @param postAuthor 이 글을 쓴 사람의 닉네임. 비어 있으면(캐시에 없는 글) 답글은 고르지 않는다
     * @param reason     이 글을 추적하는 사유. {@link TrackedPosts#MY_POST} 와
     *                   {@link TrackedPosts#MY_COMMENT} 의 비트합
     * @param myPosts    "내 글에 달린 댓글" 스위치
     * @param myReplies  "내 댓글에 글쓴이가 단 답" 스위치
     * @return 알릴 댓글. 입력 순서를 지키고 같은 댓글이 두 번 들어가지 않는다
     */
    static List<CommentParser.Comment> pick(List<CommentParser.Comment> comments, Set<String> seen,
                                            String nickname, String postAuthor, int reason,
                                            boolean myPosts, boolean myReplies) {
        Map<String, CommentParser.Comment> picked = new LinkedHashMap<>();
        if (nickname == null || nickname.isEmpty()) return new ArrayList<>(picked.values());

        boolean onMyPost = myPosts && (reason & TrackedPosts.MY_POST) != 0;
        boolean afterMyComment = myReplies && (reason & TrackedPosts.MY_COMMENT) != 0;

        for (int i = 0; i < comments.size(); i++) {
            CommentParser.Comment comment = comments.get(i);
            if (seen.contains(comment.code)) continue;
            if (nickname.equals(comment.author)) continue; // 내가 쓴 것

            // 글쓴이를 모르면(캐시 밖 글) 빈 문자열끼리 맞아떨어지지 않게 막는다.
            boolean byPostAuthor = postAuthor != null && !postAuthor.isEmpty()
                    && postAuthor.equals(comment.author);
            boolean follows = i > 0 && nickname.equals(comments.get(i - 1).author);
            if (onMyPost || (afterMyComment && follows && byPostAuthor)) {
                picked.put(comment.code, comment);
            }
        }
        return new ArrayList<>(picked.values());
    }

    /**
     * 날짜가 가장 최근인 댓글 코드 {@code max} 개. 코드는 {@code c} 뒤 여덟 자리가 작성 날짜라
     * 문자열 역순이 곧 최신순이다. 해시 순서로 자르면 추적 중인 글의 코드가 밀려나 같은 댓글을
     * 다시 알린다.
     */
    static Set<String> newestCodes(Set<String> codes, int max) {
        List<String> sorted = new ArrayList<>(codes);
        Collections.sort(sorted, Collections.<String>reverseOrder());
        return new HashSet<>(sorted.subList(0, Math.min(max, sorted.size())));
    }
}
